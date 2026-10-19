package io.github.reposo.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Detección de ronquidos.
 *
 * Es la lógica que decide si el micrófono graba algo, y no se podía verificar en el
 * emulador porque no acepta audio inyectado. Estas pruebas cubren lo que sí importa:
 * que un sonido fuerte y sostenido abra un clip, que el silencio lo cierre, y que el
 * ruido de fondo no dispare grabaciones sin parar.
 */
class SnoreDetectorTest {

    private val chunkMs = 100L

    private fun detector() = SnoreDetector(
        loudThreshold = 0.02,
        quietThreshold = 0.008,
        triggerMs = 350,
        quietMs = 4_000,
        maxSamples = 16_000 * 30,
    )

    @Test
    fun `el silencio no abre ningun clip`() {
        val d = detector()
        repeat(100) { assertNull(d.feed(0.001, 1600, chunkMs)) }
        assertFalse(d.recording)
    }

    @Test
    fun `un ruido de fondo bajo el umbral no dispara`() {
        val d = detector()
        // Justo por debajo del umbral de 0,02: no debe acumular.
        repeat(200) { assertNull(d.feed(0.015, 1600, chunkMs)) }
        assertFalse(d.recording)
    }

    @Test
    fun `un sonido fuerte sostenido abre el clip`() {
        val d = detector()
        var started = false
        repeat(20) {
            if (d.feed(0.30, 1600, chunkMs) == SnoreDetector.Event.StartClip) started = true
        }
        assertTrue("debería haber abierto un clip", started)
        assertTrue(d.recording)
    }

    @Test
    fun `un ronquido suave a un metro ya dispara`() {
        // 0,03 de RMS es lo que da un ronquido real grabado desde la mesita. Con el
        // umbral antiguo de 0,06 no se detectaba nada.
        val d = detector()
        var started = false
        repeat(10) {
            if (d.feed(0.03, 1600, chunkMs) == SnoreDetector.Event.StartClip) started = true
        }
        assertTrue("un ronquido suave debería abrir clip", started)
    }

    @Test
    fun `el silencio de una habitacion no dispara`() {
        // 0,006 es el ruido de fondo de una habitación de noche.
        val d = detector()
        repeat(300) { assertNull(d.feed(0.006, 1600, chunkMs)) }
        assertFalse(d.recording)
    }

    @Test
    fun `un pico aislado no abre el clip`() {
        val d = detector()
        // Un golpe corto y luego silencio: el acumulador se reinicia y no llega al umbral.
        assertNull(d.feed(0.50, 1600, chunkMs))
        assertNull(d.feed(0.001, 1600, chunkMs))
        repeat(10) { assertNull(d.feed(0.001, 1600, chunkMs)) }
        assertFalse(d.recording)
    }

    @Test
    fun `un ronquido corto de 400 ms abre el clip`() {
        // Un ronquido entrecortado de menos de medio segundo antes se perdía porque el
        // umbral de disparo era de 800 ms.
        val d = detector()
        var started = false
        repeat(4) {
            if (d.feed(0.05, 1600, chunkMs) == SnoreDetector.Event.StartClip) started = true
        }
        assertTrue(started)
    }

    @Test
    fun `el silencio sostenido cierra el clip abierto`() {
        val d = detector()
        while (!d.recording) d.feed(0.30, 1600, chunkMs)

        var ended = false
        repeat(60) {
            if (d.feed(0.001, 1600, chunkMs) == SnoreDetector.Event.EndClip) ended = true
        }
        assertTrue("debería haber cerrado el clip", ended)
        assertFalse(d.recording)
    }

    @Test
    fun `un clip largo se corta al alcanzar el maximo`() {
        val d = detector()
        while (!d.recording) d.feed(0.30, 1600, chunkMs)

        // Audio fuerte continuo: nunca hay silencio, así que solo el tope lo cierra.
        var ended = false
        repeat(400) {
            if (d.feed(0.30, 1600, chunkMs) == SnoreDetector.Event.EndClip) ended = true
        }
        assertTrue("debería haberse cortado por duración máxima", ended)
    }

    @Test
    fun `tras cerrar un clip se puede abrir otro`() {
        val d = detector()
        while (!d.recording) d.feed(0.30, 1600, chunkMs)
        while (d.recording) d.feed(0.001, 1600, chunkMs)

        var restarted = false
        repeat(20) {
            if (d.feed(0.30, 1600, chunkMs) == SnoreDetector.Event.StartClip) restarted = true
        }
        assertTrue("debería abrir un segundo clip", restarted)
    }

    @Test
    fun `el rms de silencio es cero`() {
        assertEquals(0.0, SnoreDetector.rmsOf(ShortArray(100)), 0.0001)
    }

    @Test
    fun `el rms de una senal a media escala ronda la mitad`() {
        // Onda cuadrada a ±16384: RMS = 16384/32768 = 0,5.
        val samples = ShortArray(100) { if (it % 2 == 0) 16384 else (-16384).toShort() }
        assertEquals(0.5, SnoreDetector.rmsOf(samples), 0.001)
    }

    @Test
    fun `el rms ignora las muestras fuera del rango indicado`() {
        val samples = ShortArray(100)
        // Solo las primeras 50 son fuertes; el resto, silencio.
        for (i in 0 until 50) samples[i] = 32000
        val rms = SnoreDetector.rmsOf(samples, 50)
        assertTrue("debería medir solo la parte fuerte", rms > 0.9)
    }
}
