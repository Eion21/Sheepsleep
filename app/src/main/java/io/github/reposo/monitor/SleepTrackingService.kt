package io.github.reposo.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.github.reposo.R

/**
 * Foreground service que mantiene el muestreo de movimiento mientras hay una
 * sesión de sueño abierta. Antes el monitor dependía de la Activity: al cerrar
 * la app de noche (deslizarla de recientes, muerte del proceso) el acelerómetro
 * dejaba de apuntar aunque la sesión siguiera abierta, y al depender de
 * PlaybackService una noche sin sonido no registraba nada.
 *
 * Es idempotente: arrancarlo con la sesión ya activa no cambia nada, y
 * START_STICKY lo reanuda si el sistema mata el proceso. El tipo specialUse está
 * declarado para Android 14+; en versiones anteriores el seguimiento de sensores
 * no exige tipo.
 */
class SleepTrackingService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Seguimiento del sueño", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Siempre antes de cualquier return: si se llegó vía startForegroundService,
        // no llamarlo en <5 s provoca crash en Android 12+.
        startAsForeground()
        MovementMonitor.start(this)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Vuelca el último bucket pendiente a la base de datos.
        MovementMonitor.stop()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        super.onDestroy()
    }

    private fun startAsForeground() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sheep_sleep_a)
            .setContentTitle("Registrando tu noche")
            .setContentText("El movimiento se apunta aunque cierres la app")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        // Sin esto, Doze suspende los sensores con la pantalla apagada y la noche
        // vuelve a quedar a cero. Mismo enfoque que SnoreService.
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "reposo:sleep_tracking").apply {
                acquire()
            }
        }
    }

    companion object {
        const val ACTION_STOP = "io.github.reposo.action.STOP_SLEEP_TRACKING"
        private const val CHANNEL_ID = "sleep_tracking"
        private const val NOTIF_ID = 28

        fun start(context: Context) {
            context.startForegroundService(Intent(context, SleepTrackingService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, SleepTrackingService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
