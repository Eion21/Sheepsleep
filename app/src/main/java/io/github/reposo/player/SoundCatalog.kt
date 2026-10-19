package io.github.reposo.player

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** [rawName] es el nombre del archivo en res/raw (sin extensión). [filePath] es la ruta local si es propio. */
data class Sound(
    val id: String,
    val label: String,
    val rawName: String?,
    val filePath: String? = null,
    /** Agrupación para la UI: "Ruido", "Naturaleza", "Hogar". */
    val category: String = "Ruido",
)

object SoundCatalog {
    val builtin = listOf(
        Sound("white", "Ruido blanco", "white_noise", category = "Ruido"),
        Sound("pink", "Ruido rosa", "pink_noise", category = "Ruido"),
        Sound("brown", "Ruido marrón", "brown_noise", category = "Ruido"),
        Sound("rain", "Lluvia", "rain", category = "Naturaleza"),
        Sound("waves", "Olas", "waves", category = "Naturaleza"),
        Sound("wind", "Viento", "wind", category = "Naturaleza"),
        Sound("forest", "Bosque de noche", "forest", category = "Naturaleza"),
        Sound("fire", "Fogata", "fire", category = "Hogar"),
    )

    private fun customSoundsFile(context: Context): File = File(context.filesDir, "custom_sounds.json")

    fun getCustomSounds(context: Context): List<Sound> {
        val file = customSoundsFile(context)
        if (!file.exists()) return emptyList()
        return try {
            val json = JSONArray(file.readText())
            val list = mutableListOf<Sound>()
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                list.add(Sound(obj.getString("id"), obj.getString("label"), null, obj.getString("path")))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addCustomSound(context: Context, label: String, uri: Uri) {
        val id = "custom_${System.currentTimeMillis()}"
        val destDir = File(context.filesDir, "custom_audio").apply { mkdirs() }
        val destFile = File(destDir, "$id.audio")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val current = getCustomSounds(context).toMutableList()
            current.add(Sound(id, label, null, destFile.absolutePath))
            val json = JSONArray()
            current.forEach { s ->
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("label", s.label)
                obj.put("path", s.filePath)
                json.put(obj)
            }
            customSoundsFile(context).writeText(json.toString())
        } catch (_: Exception) {}
    }

    fun all(context: Context): List<Sound> {
        val availableBuiltin = builtin.filter { resId(context, it) != 0 }
        return availableBuiltin + getCustomSounds(context)
    }

    fun resId(context: Context, sound: Sound): Int =
        if (sound.rawName != null) context.resources.getIdentifier(sound.rawName, "raw", context.packageName) else 0

    fun available(context: Context): List<Sound> = all(context)

    fun byId(context: Context, id: String): Sound? = all(context).firstOrNull { it.id == id }
}
