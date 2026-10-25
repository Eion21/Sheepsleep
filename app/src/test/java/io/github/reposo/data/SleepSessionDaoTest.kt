package io.github.reposo.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La causa de "solo se registraron las primeras noches": una sesión que quedaba
 * abierta (proceso muerto, reinicio del teléfono o simplemente no tocar "Pausar")
 * bloqueaba para siempre start(), que no insertaba nada mientras existiera una
 * sesión con endMs == null. El contrato nuevo: cerrar TODAS las abiertas es una
 * operación del DAO, con tope de duración, sin depender del estado en memoria.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SleepSessionDaoTest {

    private lateinit var db: ReposoDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReposoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `cierra todas las sesiones abiertas y deja intactas las ya cerradas`() = runTest {
        val dao = db.sleepSessions()
        dao.insert(SleepSession(startMs = 0L, endMs = 500L))
        dao.insert(SleepSession(startMs = 1_000L))
        dao.insert(SleepSession(startMs = 2_000L))

        val cerradas = dao.closeOpen(nowMs = 3_000L, maxDurationMs = MAX_SESSION_MS)

        assertEquals(2, cerradas)
        assertNull("no debe quedar ninguna sesión abierta", dao.openSession())
        val completadas = dao.recentCompleted(limit = 10)
        assertEquals(3, completadas.size)
        val yaCerrada = completadas.first { it.startMs == 0L }
        assertEquals("la sesión ya cerrada conserva su fin original", 500L, yaCerrada.endMs)
    }

    @Test
    fun `una sesion atascada de hace dias se cierra con el tope, no con el instante actual`() = runTest {
        val dao = db.sleepSessions()
        val inicio = 0L
        val tresDiasDespues = inicio + 3L * 24 * 3600_000
        dao.insert(SleepSession(startMs = inicio))

        dao.closeOpen(nowMs = tresDiasDespues, maxDurationMs = MAX_SESSION_MS)

        val fin = dao.recentCompleted(limit = 10).single().endMs
        assertEquals("un fin a 3 días convertiría la noche en una sesión absurda", inicio + MAX_SESSION_MS, fin)
    }

    @Test
    fun `una sesion normal de la noche se cierra con el instante actual`() = runTest {
        val dao = db.sleepSessions()
        val inicio = 0L
        val unaHoraDespues = inicio + 3_600_000L
        dao.insert(SleepSession(startMs = inicio))

        dao.closeOpen(nowMs = unaHoraDespues, maxDurationMs = MAX_SESSION_MS)

        val fin = dao.recentCompleted(limit = 10).single().endMs
        assertEquals(unaHoraDespues, fin)
    }
}
