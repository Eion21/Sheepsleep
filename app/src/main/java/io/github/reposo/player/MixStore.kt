package io.github.reposo.player

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedMix(val title: String, val layers: Map<String, Float>)

/** Guarda la última mezcla reproducida y la lista de mezclas guardadas del usuario. */
class MixStore(context: Context) {

    private val prefs = context.getSharedPreferences("mixes", Context.MODE_PRIVATE)

    fun saveLast(mix: SavedMix) {
        prefs.edit()
            .putString("last_title", mix.title)
            .putString("last_layers", mix.layers.entries.joinToString(";") { "${it.key}:${it.value}" })
            .apply()
    }

    fun loadLast(): SavedMix? {
        val title = prefs.getString("last_title", null) ?: return null
        val layers = prefs.getString("last_layers", "").orEmpty().split(";").mapNotNull { part ->
            val bits = part.split(":")
            if (bits.size != 2) null else bits[1].toFloatOrNull()?.let { bits[0] to it }
        }.toMap()
        return if (layers.isEmpty()) null else SavedMix(title, layers)
    }

    /** Guarda o reemplaza (por título) una mezcla del usuario. */
    fun saveMix(mix: SavedMix) {
        writeMixes(loadMixes().filterNot { it.title == mix.title } + mix)
    }

    fun loadMixes(): List<SavedMix> {
        val raw = prefs.getString(KEY_SAVED_MIXES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val title = obj.optString("title")
                if (title.isBlank()) return@mapNotNull null
                val layersJson = obj.optJSONObject("layers") ?: return@mapNotNull null
                val layers = layersJson.keys().asSequence().mapNotNull { key ->
                    val value = layersJson.optDouble(key, Double.NaN).toFloat()
                    if (value.isNaN()) null else key to value
                }.toMap()
                SavedMix(title, layers)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun deleteMix(title: String) {
        writeMixes(loadMixes().filterNot { it.title == title })
    }

    fun renameMix(oldTitle: String, newTitle: String) {
        val mix = loadMixes().firstOrNull { it.title == oldTitle } ?: return
        writeMixes(
            loadMixes()
                .filterNot { it.title == oldTitle || it.title == newTitle } +
                SavedMix(newTitle, mix.layers),
        )
    }

    private fun writeMixes(mixes: List<SavedMix>) {
        val array = JSONArray()
        mixes.forEach { mix ->
            val layers = JSONObject()
            mix.layers.forEach { (id, vol) -> layers.put(id, vol.toDouble()) }
            array.put(JSONObject().put("title", mix.title).put("layers", layers))
        }
        prefs.edit().putString(KEY_SAVED_MIXES, array.toString()).apply()
    }

    private companion object {
        const val KEY_SAVED_MIXES = "saved_mixes"
    }
}
