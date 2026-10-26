package io.github.reposo.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * El objetivo de sueño alimenta el puntaje de la noche (50 de los 100 puntos
 * dependen de él). Contrato: 8 h por defecto, persiste entre instancias y un
 * valor corrupto no puede desactivar el puntaje.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SleepGoalTest {

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
    fun `por defecto el objetivo son 8 horas`() {
        assertEquals(480, store.sleepGoalMinutes)
    }

    @Test
    fun `el objetivo elegido se persiste entre instancias`() {
        store.sleepGoalMinutes = 420

        assertEquals(420, SettingsStore(context).sleepGoalMinutes)
    }

    @Test
    fun `un objetivo fuera de rango se recorta`() {
        store.sleepGoalMinutes = 60
        assertEquals(SettingsStore.MIN_SLEEP_GOAL_MINUTES, store.sleepGoalMinutes)

        store.sleepGoalMinutes = 3000
        assertEquals(SettingsStore.MAX_SLEEP_GOAL_MINUTES, store.sleepGoalMinutes)
    }
}
