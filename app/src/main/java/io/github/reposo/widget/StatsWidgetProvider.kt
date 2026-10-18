package io.github.reposo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.reposo.MainActivity
import io.github.reposo.R
import io.github.reposo.data.ReposoDatabase
import io.github.reposo.data.SleepSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget de escritorio con el resumen semanal de sueño (Room).
 * Se refresca al terminar cada noche y cada 30 min como máximo.
 */
class StatsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                manager.updateAppWidget(appWidgetIds, buildViews(app))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {

        /** Refresca el widget si hay alguno colocado. Llamar al cambiar sesiones. */
        fun requestUpdate(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, StatsWidgetProvider::class.java))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                manager.updateAppWidget(ids, buildViews(app))
            }
        }

        private suspend fun buildViews(context: Context): RemoteViews {
            val sessions = ReposoDatabase.get(context).sleepSessions().recentCompleted(60)
            val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
            val week = sessions.filter { it.startMs >= weekAgo }

            val views = RemoteViews(context.packageName, R.layout.widget_stats)
            when {
                sessions.isEmpty() -> {
                    views.setTextViewText(R.id.stats_week, context.getString(R.string.widget_week_empty))
                    views.setTextViewText(R.id.stats_last, "")
                    views.setTextViewText(R.id.stats_count, "")
                }
                else -> {
                    val avg = if (week.isNotEmpty()) {
                        week.map { it.durationMs() }.average().toLong()
                    } else {
                        null
                    }
                    views.setTextViewText(
                        R.id.stats_week,
                        if (avg != null) "Esta semana · ${formatDuration(avg)} de media"
                        else "Sin noches esta semana",
                    )
                    views.setTextViewText(
                        R.id.stats_last,
                        "Última noche: ${formatDuration(sessions.first().durationMs())}",
                    )
                    views.setTextViewText(
                        R.id.stats_count,
                        "${week.size} noches registradas",
                    )
                }
            }

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            return views
        }

        private fun SleepSession.durationMs(): Long = (endMs ?: 0L) - startMs

        private fun formatDuration(ms: Long): String {
            val totalMinutes = (ms / 60_000L).coerceAtLeast(0L)
            return "${totalMinutes / 60}h ${totalMinutes % 60}m"
        }
    }
}
