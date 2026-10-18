package io.github.reposo.player

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.reposo.data.SettingsStore
import java.util.Calendar

/**
 * Alarma diaria con AlarmManager. Intenta exacta; si el sistema no la permite
 * (SCHEDULE_EXACT_ALARM revocado), cae a inexacta en vez de fallar.
 */
object AlarmScheduler {

    private const val REQUEST_CODE = 1001

    fun schedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context)
        val trigger = nextTrigger(hour, minute)

        // API < 31 no necesita permiso para alarmas exactas. Antes la condición lo
        // mandaba al camino inexacto, así que en Android 8-11 el despertador podía
        // desviarse varios minutos.
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    /** Si la alarma sigue habilitada, programa la siguiente ocurrencia (uso tras sonar). */
    fun rescheduleIfEnabled(context: Context) {
        val settings = SettingsStore(context)
        if (settings.alarmEnabled) {
            schedule(context, settings.alarmHour, settings.alarmMinute)
        }
    }

    private fun nextTrigger(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (trigger.timeInMillis <= now.timeInMillis) {
            trigger.add(Calendar.DAY_OF_MONTH, 1)
        }
        return trigger.timeInMillis
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
