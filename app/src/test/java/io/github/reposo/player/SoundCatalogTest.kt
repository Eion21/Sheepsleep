package io.github.reposo.player

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.github.reposo.sounds.SoundLibrary

/**
 * Catálogo de sonidos incluidos.
 *
 * `SoundCatalog.builtin` declara más sonidos de los que hay en `res/raw` (`rain`,
 * `waves`, `wind`, `fire`, `forest`). Lo importante no es que existan, sino que una
 * entrada sin archivo **no llegue a la interfaz**: si apareciera en la lista, el
 * usuario tocaría una opción que no suena.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SoundCatalogTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `los tres ruidos generados estan disponibles`() {
        val available = SoundCatalog.available(context)
        val ids = available.map { it.id }

        assertTrue("falta el ruido blanco", ids.contains("white"))
        assertTrue("falta el ruido rosa", ids.contains("pink"))
        assertTrue("falta el ruido marrón", ids.contains("brown"))
    }

    @Test
    fun `cada sonido disponible resuelve una uri reproducible`() {
        // El contrato con el reproductor: si está en la lista, se puede abrir.
        SoundCatalog.available(context).forEach { sound ->
            val uri = SoundLibrary.resolveUri(context, sound.id)
            assertNotNull("el sonido ${sound.id} está en la lista pero no resuelve", uri)
        }
    }

    @Test
    fun `los sonidos sin archivo no se ofrecen`() {
        // No se afirma cuáles faltan (pueden añadirse), sino que ninguno de los que
        // aparecen carece de archivo.
        val availableIds = SoundCatalog.available(context).map { it.id }.toSet()
        val builtinIds = SoundCatalog.builtin.map { it.id }.toSet()

        availableIds.forEach { id ->
            assertTrue(
                "el id $id no pertenece al catálogo declarado",
                builtinIds.contains(id) || id.startsWith("custom_"),
            )
        }
    }

    @Test
    fun `pedir un sonido inexistente devuelve null en lugar de fallar`() {
        // Una mezcla guardada con un sonido que ya no existe no debe tumbar la app.
        assertEquals(null, SoundLibrary.resolveUri(context, "no_existe"))
        assertEquals(null, SoundCatalog.byId(context, "no_existe"))
    }

    @Test
    fun `todos los sonidos disponibles tienen etiqueta visible`() {
        SoundCatalog.available(context).forEach { sound ->
            assertTrue("el sonido ${sound.id} no tiene etiqueta", sound.label.isNotBlank())
        }
    }
}
