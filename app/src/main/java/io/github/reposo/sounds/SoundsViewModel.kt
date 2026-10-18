package io.github.reposo.sounds

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DownloadState { Remote, Downloading, Ready }

data class SoundItem(val sound: RemoteSound, val state: DownloadState)

class SoundsViewModel(app: Application) : AndroidViewModel(app) {
    private val context = app.applicationContext
    private val repo = CatalogRepository(context)
    private val work = WorkManager.getInstance(context)

    private val catalog = MutableStateFlow(repo.cached())
    private val localChanges = MutableStateFlow(0) // fuerza recalcular al borrar un archivo

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    val items: StateFlow<List<SoundItem>> =
        combine(catalog, work.getWorkInfosByTagFlow(TAG), localChanges) { sounds, infos, _ ->
            val active = infos
                .filter { !it.state.isFinished }
                .flatMap { it.tags }
                .filter { it.startsWith(ID_PREFIX) }
                .map { it.removePrefix(ID_PREFIX) }
                .toSet()
            sounds.map { s ->
                val state = when {
                    SoundLibrary.isDownloaded(context, s.id) -> DownloadState.Ready
                    s.id in active -> DownloadState.Downloading
                    else -> DownloadState.Remote
                }
                SoundItem(s, state)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val fresh = repo.refresh()
            if (fresh != null) {
                catalog.value = fresh
                _offline.value = false
            } else {
                _offline.value = true
            }
            _loading.value = false
        }
    }

    fun download(sound: RemoteSound) {
        val request = OneTimeWorkRequestBuilder<DownloadSoundWorker>()
            .setInputData(
                workDataOf(
                    DownloadSoundWorker.KEY_URL to sound.url,
                    DownloadSoundWorker.KEY_FILE to sound.file,
                    DownloadSoundWorker.KEY_SHA256 to sound.sha256,
                ),
            )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(TAG)
            .addTag(ID_PREFIX + sound.id)
            .build()
        work.enqueueUniqueWork("download_${sound.id}", ExistingWorkPolicy.KEEP, request)
    }

    fun delete(sound: RemoteSound) {
        SoundLibrary.delete(context, sound.id)
        localChanges.value++
    }

    private companion object {
        const val TAG = "sound-download"
        const val ID_PREFIX = "id:"
    }
}
