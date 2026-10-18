package io.github.reposo.monitor

import kotlin.math.sqrt

/**
 * Decide cuándo empieza y termina un clip de ronquido a partir del nivel de audio.
 *
 * Está separada de [SnoreService] para poder probarla sin micrófono ni Android, que es
 * justo lo que no se podía verificar en el emulador. La lógica es la misma que usa el
 * servicio: un clip empieza cuando el audio supera el umbral de forma sostenida y
 * termina cuando lleva un rato en silencio o alcanza el máximo de muestras.
 *
 * Cada bloque de audio se resume en su RMS (0..1) y se pasa a [feed]. La clase devuelve
 * las transiciones, no escribe archivos.
 */
class SnoreDetector(
    private val loudThreshold: Double = 0.06,
    private val quietThreshold: Double = 0.02,
    private val triggerMs: Long = 800,
    private val quietMs: Long = 4_000,
    private val maxSamples: Int = 16_000 * 30,
) {

    sealed interface Event {
        /** Hay que empezar a escribir un clip. */
        data object StartClip : Event

        /** Hay que cerrar el clip abierto. */
        data object EndClip : Event
    }

    /** Indica si hay un clip en curso. */
    var recording: Boolean = false
        private set

    private var loudAccumMs = 0L
    private var quietAccumMs = 0L
    private var samplesWritten = 0

    /** Muestras que lleva el clip en curso. */
    val clipSamples: Int get() = samplesWritten

    /** Reinicia el estado, por ejemplo al abrir un clip nuevo. */
    fun startClip() {
        recording = true
        samplesWritten = 0
        quietAccumMs = 0
        loudAccumMs = 0
    }

    /**
     * Procesa un bloque de audio.
     *
     * @param rms nivel del bloque entre 0 y 1.
     * @param samples muestras del bloque (para el límite de duración).
     * @param chunkMs duración del bloque en milisegundos.
     * @return evento a ejecutar, o null si no hay cambios.
     */
    fun feed(rms: Double, samples: Int, chunkMs: Long): Event? {
        if (!recording) {
            if (rms > loudThreshold) loudAccumMs += chunkMs else loudAccumMs = 0L
            if (loudAccumMs >= triggerMs) {
                startClip()
                return Event.StartClip
            }
            return null
        }

        samplesWritten += samples
        if (rms > quietThreshold) quietAccumMs = 0L else quietAccumMs += chunkMs

        val done = quietAccumMs >= quietMs || samplesWritten >= maxSamples
        if (done) {
            recording = false
            samplesWritten = 0
            quietAccumMs = 0
            loudAccumMs = 0
            return Event.EndClip
        }
        return null
    }

    companion object {
        /** RMS de un bloque PCM de 16 bits, normalizado a 0..1. */
        fun rmsOf(samples: ShortArray, count: Int = samples.size): Double {
            if (count <= 0) return 0.0
            var sum = 0.0
            for (i in 0 until count) {
                val s = samples[i].toInt()
                sum += s.toDouble() * s
            }
            return sqrt(sum / count) / 32768.0
        }
    }
}
