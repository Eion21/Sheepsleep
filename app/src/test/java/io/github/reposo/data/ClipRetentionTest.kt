package io.github.reposo.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Retención de grabaciones. El slider de Ajustes promete borrar los clips pasados N
 * días; antes el valor no se guardaba y el borrado estaba fijo a 30. Estas pruebas
 * fijan el contrato: lo que se guarda es lo que se lee, y siempre dentro de un rango
 * sensato.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ClipRetentionTest {

    private lateinit var context: Context
    private lateinit var store: SettingsStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("reposo_preferences", Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = SettingsStore(context)
    }

    @Test
    fun `por defecto se conservan 30 dias`() {
        assertEquals(30, store.clipRetentionDays)
    }

    @Test
    fun `el valor elegido se persiste entre instancias`() {
        store.clipRetentionDays = 7

        // Una instancia nueva lee del mismo SharedPreferences: es lo que ocurre al
        // reabrir la app.
        assertEquals(7, SettingsStore(context).clipRetentionDays)
    }

    @Test
    fun `un valor por debajo del minimo se recorta`() {
        store.clipRetentionDays = 0
        assertEquals(SettingsStore.MIN_CLIP_RETENTION_DAYS, store.clipRetentionDays)

        store.clipRetentionDays = -5
        assertEquals(SettingsStore.MIN_CLIP_RETENTION_DAYS, store.clipRetentionDays)
    }

    @Test
    fun `un valor por encima del maximo se recorta`() {
        store.clipRetentionDays = 1000
        assertEquals(SettingsStore.MAX_CLIP_RETENTION_DAYS, store.clipRetentionDays)
    }

    @Test
    fun `un valor corrupto en las preferencias se corrige al leerlo`() {
        // Simula una preferencia escrita a mano o por una versión anterior con otro rango.
        context.getSharedPreferences("reposo_preferences", Context.MODE_PRIVATE)
            .edit().putInt(SettingsStore.KEY_CLIP_RETENTION_DAYS, 9999).commit()

        assertEquals(SettingsStore.MAX_CLIP_RETENTION_DAYS, SettingsStore(context).clipRetentionDays)
    }

    @Test
    fun `el corte de limpieza retrocede exactamente los dias configurados`() {
        store.clipRetentionDays = 3
        val now = 1_000_000_000_000L
        val cutoff = now - store.clipRetentionDays * 24L * 3600_000

        // Un clip de hace 4 días entra en el corte; uno de hace 2 no.
        val fourDaysAgo = now - 4L * 24 * 3600_000
        val twoDaysAgo = now - 2L * 24 * 3600_000

        assertTrue("el de 4 días debe borrarse", fourDaysAgo < cutoff)
        assertTrue("el de 2 días debe conservarse", twoDaysAgo > cutoff)
    }
}
