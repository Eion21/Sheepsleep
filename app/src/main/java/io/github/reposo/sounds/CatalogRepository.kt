package io.github.reposo.sounds

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Descarga catalog.json y guarda una copia local para poder verlo sin internet. */
class CatalogRepository(context: Context) {
    private val cacheFile = File(context.applicationContext.filesDir, "catalog.json")

    fun cached(): List<RemoteSound> = try {
        if (cacheFile.exists()) parseCatalog(cacheFile.readText()) else emptyList()
    } catch (e: JSONException) {
        emptyList()
    }

    /** Devuelve null si no hay conexión o el servidor falla (se conserva el caché). */
    suspend fun refresh(): List<RemoteSound>? = withContext(Dispatchers.IO) {
        try {
            val text = fetch() ?: return@withContext null
            val parsed = parseCatalog(text)
            cacheFile.writeText(text)
            parsed
        } catch (e: IOException) {
            null
        } catch (e: JSONException) {
            null
        }
    }

    private fun fetch(): String? {
        val conn = URL(CatalogConfig.CATALOG_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        return try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) null
            else conn.inputStream.bufferedReader().use { it.readText() }.takeIf { it.length <= 1_000_000 }
        } finally {
            conn.disconnect()
        }
    }
}
