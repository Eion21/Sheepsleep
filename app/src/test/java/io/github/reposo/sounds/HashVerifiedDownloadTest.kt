package io.github.reposo.sounds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

/**
 * Integridad de audio: el caso que importa es que un SHA-256 incorrecto **no** deje el
 * archivo disponible. Si esto falla, un archivo corrupto o alterado se reproduciría.
 */
class HashVerifiedDownloadTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val payload = "audio de prueba".toByteArray()

    @Test
    fun `un hash correcto publica el archivo`() {
        val dir = temp.newFolder("sounds")
        val expected = HashVerifiedDownload.sha256Hex(payload)

        val result = HashVerifiedDownload.downloadTo(
            ByteArrayInputStream(payload), dir, "rain.ogg", expected,
        )

        assertTrue(result is HashVerifiedDownload.Result.Success)
        val target = File(dir, "rain.ogg")
        assertTrue("el archivo debe existir", target.isFile)
        assertEquals("audio de prueba", target.readText())
    }

    @Test
    fun `un hash incorrecto no publica el archivo y limpia el part`() {
        val dir = temp.newFolder("sounds")
        val wrong = "0".repeat(64)

        val result = HashVerifiedDownload.downloadTo(
            ByteArrayInputStream(payload), dir, "rain.ogg", wrong,
        )

        assertTrue(result is HashVerifiedDownload.Result.HashMismatch)
        assertFalse("no debe quedar el archivo destino", File(dir, "rain.ogg").exists())
        assertFalse("no debe quedar el .part", File(dir, "rain.ogg.part").exists())
        assertEquals("no debe quedar nada en el directorio", 0, dir.listFiles().orEmpty().size)
    }

    @Test
    fun `el hash se compara sin distinguir mayusculas`() {
        val dir = temp.newFolder("sounds")
        val expected = HashVerifiedDownload.sha256Hex(payload).uppercase()

        val result = HashVerifiedDownload.downloadTo(
            ByteArrayInputStream(payload), dir, "rain.ogg", expected,
        )

        assertTrue(result is HashVerifiedDownload.Result.Success)
    }

    @Test
    fun `un fallo de lectura a mitad no deja archivo a medias`() {
        val dir = temp.newFolder("sounds")
        val expected = HashVerifiedDownload.sha256Hex(payload)
        val failing = object : ByteArrayInputStream(payload) {
            private var reads = 0
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (++reads > 1) throw IOException("conexión cortada")
                return super.read(b, off, len)
            }
        }

        val result = HashVerifiedDownload.downloadTo(failing, dir, "rain.ogg", expected)

        assertTrue(result is HashVerifiedDownload.Result.IoError)
        assertFalse(File(dir, "rain.ogg").exists())
        assertFalse(File(dir, "rain.ogg.part").exists())
    }

    @Test
    fun `el hash calculado sobre archivo coincide con el calculado sobre bytes`() {
        val dir = temp.newFolder("sounds")
        val file = File(dir, "muestra.bin").apply { writeBytes(payload) }

        assertEquals(HashVerifiedDownload.sha256Hex(payload), HashVerifiedDownload.sha256Hex(file))
    }
}
