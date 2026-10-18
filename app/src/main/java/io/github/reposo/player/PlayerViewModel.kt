package io.github.reposo.player

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerState(val title: String? = null, val isPlaying: Boolean = false)

/** Puente entre la UI y [PlaybackService] a través de un MediaController. */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val store = MixStore(app)

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _lastMix = MutableStateFlow(store.loadLast())
    val lastMix: StateFlow<SavedMix?> = _lastMix.asStateFlow()

    private var controller: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController>

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync(player)
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            try {
                controller = controllerFuture.get().also {
                    it.addListener(listener)
                    sync(it)
                }
            } catch (e: Exception) {
                // El servicio no está disponible; el mini-reproductor queda oculto.
            }
        }, ContextCompat.getMainExecutor(app))
    }

    private fun sync(player: Player) {
        _state.value = PlayerState(
            title = player.mediaMetadata.title?.toString(),
            isPlaying = player.playWhenReady,
        )
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.playWhenReady) c.pause() else c.play()
    }

    fun pause() {
        controller?.pause()
    }

    /** [mix]: id de sonido -> volumen (0..1). Reemplaza la mezcla actual y la reproduce. */
    fun playMix(title: String, mix: Map<String, Float>) {
        if (mix.isEmpty()) return
        val saved = SavedMix(title, mix.toMap())
        store.saveLast(saved)
        _lastMix.value = saved
        val args = Bundle().apply {
            putString(PlaybackCommands.KEY_TITLE, title)
            putStringArray(PlaybackCommands.KEY_IDS, mix.keys.toTypedArray())
            putFloatArray(PlaybackCommands.KEY_VOLUMES, mix.values.toFloatArray())
        }
        controller?.sendCustomCommand(PlaybackCommands.SET_MIX, args)
    }

    /** Reanuda la última mezcla (si ya está cargada solo da play). */
    fun playLast() {
        val last = _lastMix.value ?: return
        val c = controller
        if (c != null && c.mediaItemCount > 0 && c.mediaMetadata.title?.toString() == last.title) {
            c.play()
        } else {
            playMix(last.title, last.layers)
        }
    }

    fun setVolume(id: String, volume: Float) {
        val args = Bundle().apply {
            putString(PlaybackCommands.KEY_ID, id)
            putFloat(PlaybackCommands.KEY_VOLUME, volume)
        }
        controller?.sendCustomCommand(PlaybackCommands.SET_VOLUME, args)
    }

    /** Arma (minutos > 0) o cancela (null) el temporizador de apagado del servicio. */
    fun setSleepTimer(minutes: Int?) {
        val args = Bundle().apply {
            putInt(PlaybackCommands.KEY_MINUTES, minutes ?: 0)
        }
        controller?.sendCustomCommand(PlaybackCommands.SET_SLEEP_TIMER, args)
    }

    /** Aplica (y persiste en el servicio) el límite de volumen nocturno. */
    fun setVolumeCap(cap: Float) {
        val args = Bundle().apply {
            putFloat(PlaybackCommands.KEY_CAP, cap)
        }
        controller?.sendCustomCommand(PlaybackCommands.SET_VOLUME_CAP, args)
    }

    /** Guarda una mezcla del usuario en la lista persistente. */
    fun saveMix(title: String, layers: Map<String, Float>) {
        if (layers.isEmpty()) return
        store.saveMix(SavedMix(title, layers))
    }

    override fun onCleared() {
        MediaController.releaseFuture(controllerFuture)
        controller = null
    }
}
