package io.github.reposo.sounds

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import io.github.reposo.player.Sound
import io.github.reposo.player.SoundCatalog
import java.io.File

/** Une los sonidos incluidos en la app (res/raw) con los descargados (filesDir/sounds). */
@OptIn(UnstableApi::class)
object SoundLibrary {

    fun dir(context: Context): File =
        File(context.applicationContext.filesDir, "sounds").also { it.mkdirs() }

    private fun downloadedFile(context: Context, id: String): File? =
        dir(context).listFiles { f -> f.isFile && f.extension != "part" && f.nameWithoutExtension == id }
            ?.firstOrNull()

    fun isDownloaded(context: Context, id: String) = downloadedFile(context, id) != null

    fun delete(context: Context, id: String) {
        downloadedFile(context, id)?.delete()
    }

    /** Un sonido descargado tiene prioridad; luego el importado por el usuario; luego res/raw. */
    fun resolveUri(context: Context, id: String): Uri? {
        downloadedFile(context, id)?.let { return Uri.fromFile(it) }
        val sound = SoundCatalog.byId(context, id) ?: return null
        // Sonidos propios: se guardan en filesDir/custom_audio y solo el path los resuelve.
        // Antes esta rama no existía, así que la capa se descartaba en silencio.
        sound.filePath?.let { path ->
            val file = File(path)
            if (file.isFile) return Uri.fromFile(file)
        }
        val res = SoundCatalog.resId(context, sound).takeIf { it != 0 } ?: return null
        return RawResourceDataSource.buildRawResourceUri(res)
    }

    /** Todo lo que se puede reproducir ahora mismo (para la pantalla de mezcla). */
    fun available(context: Context): List<Sound> {
        val titles = CatalogRepository(context).cached().associate { it.id to it.title }
        val downloaded = dir(context).listFiles().orEmpty()
            .filter { it.isFile && it.extension != "part" }
            .map { Sound(id = it.nameWithoutExtension, label = titles[it.nameWithoutExtension] ?: it.nameWithoutExtension, rawName = "") }
        return (SoundCatalog.available(context) + downloaded).distinctBy { it.id }
    }
}
