package io.github.reposo.sounds

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga un sonido, comprueba su SHA-256 y solo entonces lo deja disponible.
 * Si el archivo llega corrupto o alterado, se borra y no se usa.
 */
class DownloadSoundWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) { download() }

    private fun retryOrFail(): Result = if (runAttemptCount < 3) Result.retry() else Result.failure()

    private fun download(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE) ?: return Result.failure()
        val expected = inputData.getString(KEY_SHA256) ?: return Result.failure()
        if (!url.startsWith("https://") || fileName != File(fileName).name) return Result.failure()

        val dir = SoundLibrary.dir(applicationContext)
        var conn: HttpURLConnection? = null

        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            val code = conn.responseCode
            if (code in 500..599) return retryOrFail()
            if (code != HttpURLConnection.HTTP_OK) return Result.failure()

            when (val r = HashVerifiedDownload.downloadTo(conn.inputStream, dir, fileName, expected)) {
                is HashVerifiedDownload.Result.Success -> Result.success()
                HashVerifiedDownload.Result.HashMismatch -> Result.failure()
                is HashVerifiedDownload.Result.IoError -> retryOrFail()
            }
        } finally {
            conn?.disconnect()
        }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_FILE = "file"
        const val KEY_SHA256 = "sha256"
    }
}
