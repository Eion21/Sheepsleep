package io.github.reposo.sounds

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Descarga a un archivo `.part` calculando el SHA-256 al vuelo, y solo lo promueve a
 * archivo definitivo si el hash coincide con el esperado.
 *
 * Está aparte del Worker para poder probar el caso importante sin WorkManager ni
 * Android: **un hash incorrecto no debe publicar el archivo**.
 */
internal object HashVerifiedDownload {

    sealed interface Result {
        /** El archivo quedó disponible en [target]. */
        data class Success(val target: File) : Result

        /** El hash no coincide: el `.part` se borra y no hay archivo utilizable. */
        data object HashMismatch : Result

        /** I/O falló a mitad de la copia; también se limpia. */
        data class IoError(val cause: Exception) : Result
    }

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun sha256Hex(file: File): String =
        MessageDigest.getInstance("SHA-256").run {
            file.inputStream().use { input ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    update(buf, 0, n)
                }
            }
            digest().joinToString("") { "%02x".format(it) }
        }

    /**
     * Copia [input] a `dir/part` y, si el SHA-256 coincide con [expectedSha256],
     * lo renombra a `dir/targetName`. En cualquier otro caso no deja archivo.
     */
    fun downloadTo(
        input: InputStream,
        dir: File,
        targetName: String,
        expectedSha256: String,
    ): Result {
        val target = File(dir, targetName)
        val part = File(dir, "$targetName.part")
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            input.use { source ->
                part.outputStream().use { out ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = source.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        digest.update(buf, 0, n)
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(expectedSha256, ignoreCase = true)) {
                part.delete()
                Result.HashMismatch
            } else if (part.renameTo(target)) {
                Result.Success(target)
            } else {
                part.delete()
                Result.IoError(java.io.IOException("No se pudo renombrar $part"))
            }
        } catch (e: Exception) {
            part.delete()
            Result.IoError(e)
        }
    }
}
