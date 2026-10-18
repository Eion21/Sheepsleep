package io.github.reposo.player

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.reposo.data.SettingsStore

/**
 * Sonó la alarma: vibra (si está activo) y sube la última mezcla desde 0
 * con un fundido de 60 s. Reprograma la siguiente alarma diaria.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val settings = SettingsStore(app)

        AlarmScheduler.rescheduleIfEnabled(app)

        if (settings.alarmVibration) {
            vibrate(app)
        }

        val mix = MixStore(app).loadLast() ?: return
        val pending = goAsync()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()

        future.addListener({
            val done = {
                MediaController.releaseFuture(future)
                pending.finish()
                true
            }
            try {
                val controller = future.get()
                val args = Bundle().apply {
                    putString(PlaybackCommands.KEY_TITLE, mix.title)
                    putStringArray(PlaybackCommands.KEY_IDS, mix.layers.keys.toTypedArray())
                    putFloatArray(PlaybackCommands.KEY_VOLUMES, mix.layers.values.toFloatArray())
                    putLong(PlaybackCommands.KEY_FADE_MS, ALARM_FADE_MS)
                }
                val command = controller.sendCustomCommand(PlaybackCommands.SET_MIX, args)
                command.addListener(
                    {
                        // Deja que el servicio arranque y el fade tome el control.
                        Handler(Looper.getMainLooper()).postDelayed({ done() }, 2_000)
                    },
                    ContextCompat.getMainExecutor(app),
                )
            } catch (_: Exception) {
                done()
            }
        }, ContextCompat.getMainExecutor(app))
    }

    private fun vibrate(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager.defaultVibrator.vibrate(ONE_SECOND)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            vibrator.vibrate(ONE_SECOND)
        }
    }

    private companion object {
        const val ALARM_FADE_MS = 60_000L
        val ONE_SECOND = VibrationEffect.createOneShot(1_000, VibrationEffect.DEFAULT_AMPLITUDE)
    }
}
