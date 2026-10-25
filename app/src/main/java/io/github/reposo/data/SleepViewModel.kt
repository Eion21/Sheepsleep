package io.github.reposo.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import io.github.reposo.monitor.SleepTrackingService
import io.github.reposo.widget.StatsWidgetProvider
import java.io.File

class SleepViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = ReposoDatabase.get(app).sleepSessions()
    private val snoreDao = ReposoDatabase.get(app).snoreClips()
    private val movementDao = ReposoDatabase.get(app).movementBuckets()

    val sessions: StateFlow<List<SleepSession>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val snoreClips: StateFlow<List<SnoreClip>> =
        snoreDao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Buckets de las últimas 2 semanas (para la última noche con datos en Stats). */
    val movementBuckets: StateFlow<List<MovementBucket>> =
        movementDao.observeRecent(System.currentTimeMillis() - 14L * 24 * 3600_000)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Borra clips, archivos y buckets más antiguos que la retención configurada.
     * El valor sale de SettingsStore, así que respeta el slider de Ajustes.
     */
    fun cleanupSnore() {
        viewModelScope.launch {
            val days = SettingsStore(getApplication()).clipRetentionDays
            val cutoff = System.currentTimeMillis() - days * 24L * 3600_000
            snoreDao.cleanupOld(cutoff)
            movementDao.cleanupOld(cutoff)
            val dir = File(getApplication<Application>().filesDir, "snore")
            dir.listFiles()?.forEach { f -> if (f.lastModified() < cutoff) f.delete() }
        }
    }

    fun deleteSnore(clip: SnoreClip) {
        viewModelScope.launch {
            snoreDao.delete(clip.id)
            File(getApplication<Application>().filesDir, "snore/${clip.file}").delete()
        }
    }

    /** La sesión en curso, si la hay. */
    val active: StateFlow<SleepSession?> =
        sessions.map { list -> list.firstOrNull { it.endMs == null } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Empieza una noche. No consulta el estado en memoria de la UI: cierra en BD
     * cualquier sesión atascada (proceso muerto, reinicio, "Pausar" sin pulsar) y
     * siempre inserta la nueva. Antes, una sesión abierta hacía que start() no
     * insertara nada y todas las noches siguientes se perdían en silencio.
     */
    fun start(mixTitle: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.closeOpen(now, MAX_SESSION_MS)
            dao.insert(SleepSession(startMs = now, mixTitle = mixTitle))
            // El muestreo de movimiento vive en un servicio en primer plano: sobrevive
            // a cerrar la app y a Doze, que era como se perdían las noches.
            SleepTrackingService.start(getApplication())
            StatsWidgetProvider.requestUpdate(getApplication())
        }
    }

    fun stop() {
        viewModelScope.launch {
            if (dao.closeOpen(System.currentTimeMillis(), MAX_SESSION_MS) > 0) {
                SleepTrackingService.stop(getApplication())
            }
            StatsWidgetProvider.requestUpdate(getApplication())
        }
    }

    /**
     * Borra todo lo que la app guarda del usuario: sesiones, clips de ronquido (incluidos
     * los wav) y buckets de movimiento. Antes el botón de Ajustes no hacía nada.
     */
    fun clearAllData(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteAll()
                snoreDao.deleteAll()
                movementDao.deleteAll()
                val files = getApplication<Application>().filesDir
                File(files, "snore").listFiles()?.forEach { it.delete() }
                File(files, "custom_audio").listFiles()?.forEach { it.delete() }
                File(files, "custom_sounds.json").delete()
            }
            StatsWidgetProvider.requestUpdate(getApplication())
            onDone()
        }
    }
}
