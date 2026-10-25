package io.github.reposo.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.reposo.data.ReposoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Al reiniciar el teléfono muere todo el proceso: si había una noche abierta,
 * el seguimiento de movimiento se perdía hasta que alguien abriera la app.
 * Aquí se reanuda: si quedó una sesión sin cerrar, vuelve a arrancar el servicio.
 */
class SleepTrackingBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                resumeIfNeeded(appContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Reanuda el seguimiento solo si quedó una noche abierta al reiniciar. */
        suspend fun resumeIfNeeded(context: Context) {
            val dao = ReposoDatabase.get(context).sleepSessions()
            if (dao.openSession() != null) {
                SleepTrackingService.start(context)
            }
        }
    }
}
