package io.github.reposo.data

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Estilo de la noche según el patrón de movimiento, el espejo local de los tres
 * tipos de Pokémon Sleep: sosegada (casi todo quieto), sonadora (intermedia) e
 * inquieta (mucho movimiento).
 */
enum class SleepStyle { SOSEGADA, SONADORA, INQUIETA }

/**
 * Resumen de una noche: puntaje 0–100 desglosado en sus tres partes, número de
 * episodios de inquietud y estilo. Lo calcula [SleepAnalysis.analyze] a partir de
 * la sesión y sus buckets de movimiento, sin tocar la base de datos.
 */
data class NightReport(
    val score: Int,
    val durationPart: Int,
    val calmPart: Int,
    val interruptionsPart: Int,
    val interruptions: Int,
    val calmShare: Float,
    val style: SleepStyle,
)

object SleepAnalysis {

    const val BUCKET_MS = 10_000L

    private const val DURATION_MAX = 50
    private const val CALM_MAX = 30
    private const val INTERRUPTIONS_MAX = 20
    private const val INTERRUPTION_COST = 5

    /** Un episodio de inquietud es un minuto o más de movimiento sostenido. */
    private const val EPISODE_BUCKETS = 6

    /** Dos buckets seguidos con un hueco mayor que esto cuentan como rachas separadas. */
    private const val CONTIGUOUS_MS = 2 * BUCKET_MS

    /** Con menos de 5 minutos de muestreo no hay base para juzgar la calidad. */
    private const val MIN_BUCKETS = 30

    private const val DEFAULT_GOAL_MINUTES = 480

    /**
     * Puntaje de una noche, estilo Pokémon Sleep pero con datos propios:
     * - 50 puntos por duración frente al objetivo (dormir de más no puntúa extra).
     * - 30 puntos por quieta: proporción de buckets en calma (score 0 o 1).
     * - 20 puntos que se pierden a razón de 5 por cada episodio largo de inquietud.
     *
     * Sin datos de movimiento (monitor muerto, noches viejas) no se juzga la
     * calidad: mitad de puntos de calma y cero interrupciones.
     */
    fun analyze(
        startMs: Long,
        endMs: Long,
        buckets: List<MovementBucket>,
        goalMinutes: Int,
    ): NightReport {
        val durationMinutes = ((endMs - startMs).coerceAtLeast(0) / 60_000L).toInt()
        val goal = if (goalMinutes > 0) goalMinutes else DEFAULT_GOAL_MINUTES
        val durationPart = (DURATION_MAX * min(1f, durationMinutes.toFloat() / goal)).roundToInt()

        val noche = buckets.filter { it.startMs in startMs until endMs }.sortedBy { it.startMs }
        val sinDatos = noche.size < MIN_BUCKETS

        val calmShare = if (sinDatos) 0.5f else noche.count { it.score <= 1 }.toFloat() / noche.size
        val calmPart = (CALM_MAX * calmShare).roundToInt()

        val interruptions = if (sinDatos) 0 else countInterruptions(noche)
        val interruptionsPart = (INTERRUPTIONS_MAX - INTERRUPTION_COST * interruptions).coerceAtLeast(0)

        val style = when {
            sinDatos -> SleepStyle.SONADORA
            else -> {
                val heavyShare = noche.count { it.score == 3 }.toFloat() / noche.size
                when {
                    heavyShare >= 0.15f || calmShare < 0.55f -> SleepStyle.INQUIETA
                    calmShare >= 0.80f -> SleepStyle.SOSEGADA
                    else -> SleepStyle.SONADORA
                }
            }
        }

        return NightReport(
            score = (durationPart + calmPart + interruptionsPart).coerceIn(0, 100),
            durationPart = durationPart,
            calmPart = calmPart,
            interruptionsPart = interruptionsPart,
            interruptions = interruptions,
            calmShare = calmShare,
            style = style,
        )
    }

    private fun countInterruptions(buckets: List<MovementBucket>): Int {
        var episodes = 0
        var run = 0
        var prevStart = Long.MIN_VALUE
        for (b in buckets) {
            val contiguous = prevStart != Long.MIN_VALUE && b.startMs - prevStart <= CONTIGUOUS_MS
            run = when {
                b.score < 2 -> 0
                contiguous -> run + 1
                else -> 1
            }
            // El episodio se cuenta al llegar al umbral; seguir moviéndose no suma otro.
            if (run == EPISODE_BUCKETS) episodes++
            prevStart = b.startMs
        }
        return episodes
    }
}
