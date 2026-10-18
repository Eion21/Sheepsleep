package io.github.reposo.player

import android.animation.ValueAnimator
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.github.reposo.data.SettingsStore
import io.github.reposo.sounds.SoundLibrary
import io.github.reposo.widget.MediaWidgetProvider
import io.github.reposo.widget.WidgetState
import java.io.File

/**
 * Mezcla de sonidos en bucle: un ExoPlayer por capa, cada uno con su volumen.
 *
 * El volumen lógico de cada capa (lo que el usuario eligió) se guarda aquí y se
 * aplica como `lógico × cap × fadeFactor`, de modo que el límite nocturno y el
 * fundido del temporizador actúan sin pisar la mezcla del usuario.
 */
@OptIn(UnstableApi::class)
class MixEngine(private val context: Context) {

    val master: ExoPlayer = build(handleFocus = true).apply {
        repeatMode = Player.REPEAT_MODE_ONE
        setHandleAudioBecomingNoisy(true)
        addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                extras.values.forEach { it.playWhenReady = playWhenReady }
                // Tras el apagado del temporizador, reproducir de nuevo restaura el volumen.
                // (fadeAnimator != null → hay un fundido activo: fade-out de timer o fade-in de alarma.)
                if (playWhenReady && !timerActive && fadeAnimator == null && fadeFactor < 1f) {
                    fadeFactor = 1f
                    refreshVolumes()
                }
                WidgetState.savePlaying(context, playWhenReady)
                MediaWidgetProvider.requestUpdate(context)
            }
        })
    }

    private val extras = LinkedHashMap<String, ExoPlayer>()
    private val logicalVolumes = LinkedHashMap<String, Float>()
    private var masterId: String? = null

    private val settings = SettingsStore(context)
    private val handler = Handler(Looper.getMainLooper())

    /** Límite de volumen nocturno (persistido). */
    var nightCap: Float = settings.nightVolumeLimit
        private set

    /** Multiplicador del fundido: 1 mientras suena, baja a 0 durante el temporizador. */
    private var fadeFactor = 1f
    private var fadeAnimator: ValueAnimator? = null

    /** Un temporizador está armado (espera o fundiendo). */
    private var timerActive = false

    fun setMix(
        ids: List<String>,
        volumes: List<Float>,
        title: String,
        autoplay: Boolean = true,
        fadeInMs: Long = 0L,
    ) {
        extras.values.forEach { it.release() }
        extras.clear()
        logicalVolumes.clear()
        masterId = null

        val layers = ids.zip(volumes).mapNotNull { (id, vol) ->
            val uri = SoundLibrary.resolveUri(context, id) ?: return@mapNotNull null
            Triple(id, uri, vol.coerceIn(0f, 1f))
        }
        if (layers.isEmpty()) {
            master.clearMediaItems()
            return
        }

        layers.forEach { (id, _, vol) -> logicalVolumes[id] = vol }
        if (fadeInMs > 0 && autoplay) {
            startFadeIn(fadeInMs)
        }

        layers.drop(1).forEach { (id, uri, _) ->
            extras[id] = build(handleFocus = false).apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = volumeFor(id)
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = autoplay
            }
        }

        val (firstId, firstUri, _) = layers.first()
        masterId = firstId
        master.volume = volumeFor(firstId)
        master.setMediaItem(
            MediaItem.Builder()
                .setUri(firstUri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                .build()
        )
        master.prepare()
        master.playWhenReady = autoplay
        refreshVolumes()
        WidgetState.save(context, title, autoplay)
        MediaWidgetProvider.requestUpdate(context)
    }

    fun setVolume(id: String, volume: Float) {
        logicalVolumes[id] = volume.coerceIn(0f, 1f)
        refreshVolumes()
    }

    /** Aplica (y persiste) el límite de volumen nocturno en caliente. */
    fun setNightCap(cap: Float) {
        nightCap = cap.coerceIn(0.2f, 1f)
        settings.nightVolumeLimit = nightCap
        refreshVolumes()
    }

    /**
     * Apaga la mezcla con fundido durante los últimos minutos del temporizador
     * y pausa al llegar a 0. `minutes = 0` cancela.
     */
    fun setSleepTimer(minutes: Int) {
        disarmTimer()
        if (minutes <= 0) return

        val fadeMinutes = settings.fadeOutMinutes
        val totalMs = minutes * 60_000L
        val fadeMs = (fadeMinutes * 60_000L).coerceAtMost(totalMs)
        timerActive = true

        if (fadeMs >= totalMs) {
            startFade(fadeMs)
        } else {
            handler.postDelayed({ startFade(fadeMs) }, totalMs - fadeMs)
        }
    }

    /** Cancela el temporizador y devuelve el volumen a su valor lógico. */
    fun cancelSleepTimer() {
        disarmTimer()
        fadeFactor = 1f
        refreshVolumes()
    }

    fun release() {
        disarmTimer()
        extras.values.forEach { it.release() }
        extras.clear()
        master.release()
    }

    private fun startFade(durationMs: Long) {
        fadeAnimator?.cancel()
        // Si un fundido anterior ya llegó a 0 (mezcla pausada), el nuevo empieza desde arriba.
        val from = if (fadeFactor <= 0f) 1f else fadeFactor
        fadeAnimator = ValueAnimator.ofFloat(from, 0f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                fadeFactor = animation.animatedValue as Float
                refreshVolumes()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (cancelled) return
                    fadeFactor = 0f
                    refreshVolumes()
                    master.pause()
                    timerActive = false
                    fadeAnimator = null
                }
            })
            start()
        }
    }

    /** Sube el volumen de 0 a 1 (alarma): no toca timerActive ni pausa al final. */
    private fun startFadeIn(durationMs: Long) {
        fadeAnimator?.cancel()
        fadeFactor = 0f
        refreshVolumes()
        fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                fadeFactor = animation.animatedValue as Float
                refreshVolumes()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (cancelled) return
                    fadeFactor = 1f
                    refreshVolumes()
                    fadeAnimator = null
                }
            })
            start()
        }
    }

    private fun disarmTimer() {
        handler.removeCallbacksAndMessages(null)
        fadeAnimator?.cancel()
        fadeAnimator = null
        timerActive = false
    }

    private fun refreshVolumes() {
        master.volume = volumeFor(masterId ?: return)
        extras.forEach { (id, player) -> player.volume = volumeFor(id) }
    }

    private fun volumeFor(id: String): Float =
        (logicalVolumes[id] ?: 1f) * nightCap * fadeFactor

    private fun build(handleFocus: Boolean): ExoPlayer =
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                handleFocus,
            )
            .apply { if (handleFocus) setWakeMode(C.WAKE_MODE_LOCAL) }
            .build()
}
