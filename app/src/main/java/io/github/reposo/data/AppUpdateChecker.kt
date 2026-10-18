package io.github.reposo.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.reposo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val releaseUrl: String,
    val apkUrl: String?,
    val notes: String?,
)

/**
 * Comprueba si hay una versión nueva en GitHub Releases (la app se distribuye
 * como APK sideload, sin tienda). Escrito desde cero para esta app.
 */
object AppUpdateChecker {

    /** Slug del repositorio: "usuario/repo". */
    const val REPO = "Eion21/sheepsleep"

    /** Última release publicada, o null si falla el check (sin red, rate limit, REPO sin configurar). */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.github.com/repos/$REPO/releases/latest")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return@withContext null

            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val version = json.optString("tag_name").removePrefix("v")
            if (version.isEmpty() || !isNewer(version, BuildConfig.VERSION_NAME)) {
                return@withContext null
            }

            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name").endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }
            UpdateInfo(
                version = version,
                releaseUrl = json.optString("html_url"),
                apkUrl = apkUrl,
                notes = json.optString("body").takeIf { it.isNotBlank() },
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Descarga el APK a la caché de la app y devuelve el archivo, o null si falla. */
    suspend fun downloadApk(context: Context, url: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                if (conn.responseCode != 200) return@withContext null

                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val target = File(dir, "update.apk")
                conn.inputStream.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                target
            } catch (_: Exception) {
                null
            }
        }

    /** Abre el instalador del sistema para el APK descargado. */
    fun openInstaller(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Sin instalador disponible (o con el permiso revocado) el intent no resuelve.
        // Sin este catch la excepción sube por el scope de la pantalla y tumba la app.
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            openReleasePage(context, "https://github.com/$REPO/releases/latest")
        }
    }

    /** Abre la página de la release en el navegador (fallback). */
    fun openReleasePage(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun isNewer(candidate: String, current: String): Boolean =
        VersionComparator.isNewer(candidate, current)
}
