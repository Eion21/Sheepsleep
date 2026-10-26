package io.github.reposo.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * El puntaje es el corazón del reporte de la mañana: 50 puntos por duración
 * frente al objetivo, 30 por quieta que estuvo la noche y 20 que se pierden con
 * los episodios largos de inquietud. El estilo resume la noche en una palabra,
 * como los tres tipos de Pokémon Sleep (quieto / intermedio / inquieto).
 */
class SleepAnalysisTest {

    private val inicio = 0L

    private fun bucket(i: Int, score: Int) = MovementBucket(startMs = i * 10_000L, score = score)

    /** `minutos` de noche quieta (todos los buckets en 0). */
    private fun nocheQuieta(minutos: Int): List<MovementBucket> =
        (0 until minutos * 6).map { bucket(it, 0) }

    @Test
    fun `una noche completa y quieta puntua 100 y es sosegada`() {
        val reporte = SleepAnalysis.analyze(
            startMs = inicio,
            endMs = inicio + 480L * 60_000,
            buckets = nocheQuieta(480),
            goalMinutes = 480,
        )

        assertEquals(50, reporte.durationPart)
        assertEquals(30, reporte.calmPart)
        assertEquals(20, reporte.interruptionsPart)
        assertEquals(100, reporte.score)
        assertEquals(SleepStyle.SOSEGADA, reporte.style)
    }

    @Test
    fun `dormir la mitad del objetivo resta la mitad de los puntos de duracion`() {
        val reporte = SleepAnalysis.analyze(
            startMs = inicio,
            endMs = inicio + 240L * 60_000,
            buckets = nocheQuieta(240),
            goalMinutes = 480,
        )

        assertEquals(25, reporte.durationPart)
        assertEquals(75, reporte.score)
    }

    @Test
    fun `sin objetivo configurado se usan 8 horas`() {
        val reporte = SleepAnalysis.analyze(
            startMs = inicio,
            endMs = inicio + 480L * 60_000,
            buckets = nocheQuieta(480),
            goalMinutes = 0,
        )

        assertEquals(50, reporte.durationPart)
    }

    @Test
    fun `dormir mas del objetivo no supera los 50 puntos de duracion`() {
        val reporte = SleepAnalysis.analyze(
            startMs = inicio,
            endMs = inicio + 600L * 60_000,
            buckets = nocheQuieta(600),
            goalMinutes = 480,
        )

        assertEquals(50, reporte.durationPart)
        assertEquals(100, reporte.score)
    }

    @Test
    fun `una noche sin datos de movimiento puntua neutral y es sonadora`() {
        val reporte = SleepAnalysis.analyze(
            startMs = inicio,
            endMs = inicio + 480L * 60_000,
            buckets = emptyList(),
            goalMinutes = 480,
        )

        assertEquals(50 + 15 + 20, reporte.score)
        assertEquals(SleepStyle.SONADORA, reporte.style)
    }

    @Test
    fun `pocas muestras se tratan como sin datos`() {
        val reporte = SleepAnalysis.analyze(
            startMs = inicio,
            endMs = inicio + 480L * 60_000,
            buckets = nocheQuieta(1), // 6 buckets = 60 s: muy poco para juzgar
            goalMinutes = 480,
        )

        assertEquals(50 + 15 + 20, reporte.score)
        assertEquals(SleepStyle.SONADORA, reporte.style)
    }

    @Test
    fun `un episodio largo de inquietud resta 5 puntos`() {
        val buckets = nocheQuieta(480).toMutableList()
        // Un minuto entero (6 buckets) moviéndose en mitad de la noche.
        repeat(6) { buckets[1_440 + it] = bucket(1_440 + it, 3) }

        val reporte = SleepAnalysis.analyze(
            startMs = inicio,
            endMs = inicio + 480L * 60_000,
            buckets = buckets,
            goalMinutes = 480,
        )

        assertEquals(1, reporte.interruptions)
        assertEquals(15, reporte.interruptionsPart)
        assertEquals(95, reporte.score)
    }

    @Test
    fun `un episodio muy largo cuenta una sola vez`() {
        val buckets = nocheQuieta(480).toMutableList()
        // Dos minutos seguidos (12 buckets) moviéndose: un episodio, no dos.
        repeat(12) { buckets[1_440 + it] = bucket(1_440 + it, 2) }

        val reporte = SleepAnalysis.analyze(
            startMs = inicio,
            endMs = inicio + 480L * 60_000,
            buckets = buckets,
            goalMinutes = 480,
        )

        assertEquals(1, reporte.interruptions)
    }

    @Test
    fun `picos aislados de un minuto o menos no restan`() {
        val buckets = nocheQuieta(480).toMutableList()
        // Cinco buckets (50 s) moviéndose: por debajo del minuto de episodio.
        repeat(5) { buckets[1_440 + it] = bucket(1_440 + it, 3) }

        val reporte = SleepAnalysis.analyze(
            startMs = inicio,
            endMs = inicio + 480L * 60_000,
            buckets = buckets,
            goalMinutes = 480,
        )

        assertEquals(0, reporte.interruptions)
        assertEquals(20, reporte.interruptionsPart)
    }

    @Test
    fun `mucho movimiento clasifica la noche como inquieta`() {
        val buckets = nocheQuieta(480).toMutableList()
        // 20 % de buckets muy inquietos, alternados para no formar episodios largos.
        for (i in buckets.indices step 5) buckets[i] = bucket(i, 3)

        val reporte = SleepAnalysis.analyze(
            startMs = inicio,
            endMs = inicio + 480L * 60_000,
            buckets = buckets,
            goalMinutes = 480,
        )

        assertEquals(SleepStyle.INQUIETA, reporte.style)
    }

    @Test
    fun `los buckets fuera de la sesion no cuentan`() {
        val buckets = nocheQuieta(480).toMutableList()
        buckets += MovementBucket(startMs = -60_000L, score = 3) // antes de acostarse
        buckets += MovementBucket(startMs = 480L * 60_000, score = 3) // después de levantarse

        val reporte = SleepAnalysis.analyze(
            startMs = inicio,
            endMs = inicio + 480L * 60_000,
            buckets = buckets,
            goalMinutes = 480,
        )

        assertEquals(0, reporte.interruptions)
        assertEquals(100, reporte.score)
    }
}
