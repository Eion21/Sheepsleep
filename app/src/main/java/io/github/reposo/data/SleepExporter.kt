package io.github.reposo.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Exporta las sesiones de sueño a un archivo y devuelve la Uri para compartirlo.
 *
 * Se escribe en `cacheDir/exports/`, que el FileProvider ya expone; así no hay que
 * ampliar la superficie del proveedor a toda la carpeta privada.
 */
object SleepExporter {

    private fun exportDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    /**
     * CSV de una fila por noche. Separador `;` para que Excel en español no lo parta
     * por la coma decimal, y fecha en ISO local para que ordene bien.
     */
    fun exportCsv(context: Context, sessions: List<SleepSession>): Uri {
        val sb = StringBuilder("inicio,fin,duracion_min,mezcla\n")
        sessions.forEach { s ->
            val end = s.endMs
            val minutes = if (end != null) (end - s.startMs) / 60_000 else 0
            sb.append(iso(s.startMs)).append(',')
                .append(end?.let { iso(it) } ?: "")
                .append(',').append(minutes).append(',')
                .append(csvEscape(s.mixTitle ?: ""))
                .append('\n')
        }
        val file = File(exportDir(context), "reposo-sesiones.csv")
        file.writeText(sb.toString())
        return uriFor(context, file)
    }

    fun exportJson(context: Context, sessions: List<SleepSession>, snoreClips: List<SnoreClip>): Uri {
        val nights = JSONArray()
        sessions.forEach { s ->
            nights.put(
                JSONObject().apply {
                    put("inicio", iso(s.startMs))
                    put("fin", s.endMs?.let { iso(it) } ?: JSONObject.NULL)
                    put("duracion_min", if (s.endMs != null) (s.endMs - s.startMs) / 60_000 else 0)
                    put("mezcla", s.mixTitle ?: JSONObject.NULL)
                },
            )
        }
        val clips = JSONArray()
        snoreClips.forEach { c ->
            clips.put(
                JSONObject().apply {
                    put("inicio", iso(c.startMs))
                    put("duracion_ms", c.durationMs)
                    put("pico_db", c.peakDb)
                },
            )
        }
        val root = JSONObject().apply {
            put("app", "Sheepsleep")
            put("exportado", iso(System.currentTimeMillis()))
            put("sesiones", nights)
            // Solo metadatos: el audio de los ronquidos nunca sale del dispositivo.
            put("ronquidos", clips)
        }
        val file = File(exportDir(context), "reposo-datos.json")
        file.writeText(root.toString(2))
        return uriFor(context, file)
    }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun iso(ms: Long): String =
        java.time.Instant.ofEpochMilli(ms)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    private fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
