package io.github.reposo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.reposo.MainActivity
import io.github.reposo.R
import io.github.reposo.player.MixStore
import io.github.reposo.player.PlaybackCommands
import io.github.reposo.player.PlaybackService

/**
 * Widget de escritorio: título de la última mezcla + play/pausa.
 * El estado lo escribe MixEngine en [WidgetState]; el clic crea un
 * MediaController temporal contra [PlaybackService].
 */
class MediaWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_PLAY_PAUSE) {
            togglePlay(context)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        manager.updateAppWidget(appWidgetIds, buildViews(context.applicationContext))
    }

    private fun togglePlay(context: Context) {
        val app = context.applicationContext
        val pending = goAsync()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()

        future.addListener({
            val finish = {
                MediaController.releaseFuture(future)
                pending.finish()
                true
            }
            try {
                val controller = future.get()
                if (controller.mediaItemCount > 0) {
                    if (controller.playWhenReady) controller.pause() else controller.play()
                    // Deja que el servicio arranque antes de soltar el controller.
                    Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1_500)
                } else {
                    val mix = MixStore(app).loadLast()
                    if (mix == null) {
                        finish()
                    } else {
                        val args = Bundle().apply {
                            putString(PlaybackCommands.KEY_TITLE, mix.title)
                            putStringArray(PlaybackCommands.KEY_IDS, mix.layers.keys.toTypedArray())
                            putFloatArray(PlaybackCommands.KEY_VOLUMES, mix.layers.values.toFloatArray())
                        }
                        val command = controller.sendCustomCommand(PlaybackCommands.SET_MIX, args)
                        command.addListener(
                            { finish() },
                            ContextCompat.getMainExecutor(app),
                        )
                    }
                }
            } catch (_: Exception) {
                finish()
            }
        }, ContextCompat.getMainExecutor(app))
    }

    companion object {
        private const val ACTION_PLAY_PAUSE = "io.github.reposo.widget.PLAY_PAUSE"

        /** Refresca el widget si hay alguno colocado. Llamar al cambiar título/estado. */
        fun requestUpdate(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, MediaWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                manager.updateAppWidget(ids, buildViews(app))
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_media)
            val playing = WidgetState.isPlaying(context)

            views.setTextViewText(
                R.id.widget_title,
                WidgetState.title(context) ?: context.getString(R.string.widget_idle),
            )
            views.setImageViewResource(
                R.id.widget_play,
                if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)

            val playPause = PendingIntent.getBroadcast(
                context,
                1,
                Intent(context, MediaWidgetProvider::class.java).setAction(ACTION_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_play, playPause)
            return views
        }
    }
}
