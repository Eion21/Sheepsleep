package io.github.reposo.sounds

import org.json.JSONObject

object CatalogConfig {
    /** Cambia esto por la URL pública de tu bucket de R2. Debe ser https y terminar en "/". */
    const val BASE_URL = "https://pub-adf16aee20c94a88a1c2e3d4a0c773ff.r2.dev/"
    const val CATALOG_URL = BASE_URL + "catalog.json"
}

data class RemoteSound(
    val id: String,
    val title: String,
    val category: String,
    val file: String,
    val sizeBytes: Long,
    val sha256: String,
    val author: String,
    val license: String,
    val sourceUrl: String,
) {
    val url: String get() = CatalogConfig.BASE_URL + file
}

private val SAFE_ID = Regex("[a-z0-9_]{1,64}")
private val SAFE_FILE = Regex("[a-z0-9_]{1,64}\\.(ogg|opus|mp3|m4a|wav|flac)")
private val SHA256 = Regex("[0-9a-fA-F]{64}")

/**
 * Comprueba una entrada del catálogo antes de aceptarla.
 *
 * Se rechaza lo que pueda convertirse en una ruta (`../`), un tipo de archivo no
 * previsto o un hash vacío. Esa validación es todo lo que separa un JSON de internet
 * de un nombre de archivo local, así que se prueba aparte.
 */
internal fun isValidCatalogEntry(id: String, file: String, sha256: String): Boolean =
    SAFE_ID.matches(id) && SAFE_FILE.matches(file) &&
        file.substringBeforeLast('.') == id && SHA256.matches(sha256)

/**
 * Lee el catálogo. El JSON viene de internet, así que se descarta cualquier entrada con
 * id, nombre de archivo o hash raro (evita rutas tipo "../" y archivos corruptos).
 */
fun parseCatalog(json: String): List<RemoteSound> {
    val arr = JSONObject(json).getJSONArray("sounds")
    return (0 until arr.length()).mapNotNull { i ->
        val o = arr.getJSONObject(i)
        val id = o.optString("id")
        val file = o.optString("file")
        val sha = o.optString("sha256")
        if (!isValidCatalogEntry(id, file, sha)) return@mapNotNull null
        RemoteSound(
            id = id,
            title = o.optString("title", id),
            category = o.optString("category"),
            file = file,
            sizeBytes = o.optLong("sizeBytes"),
            sha256 = sha,
            author = o.optString("author"),
            license = o.optString("license"),
            sourceUrl = o.optString("sourceUrl"),
        )
    }
}
