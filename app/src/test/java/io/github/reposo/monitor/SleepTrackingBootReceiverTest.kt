package io.github.reposo.monitor

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.github.reposo.data.ReposoDatabase
import io.github.reposo.data.SleepSession
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Si el teléfono se reinicia de noche, el proceso muere con una sesión abierta.
 * El receiver debe reanudar el seguimiento SOLO en ese caso: con todas las noches
 * cerradas, arrancar el servicio sería una notificación y un wakelock gratis.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SleepTrackingBootReceiverTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // ReposoDatabase es un singleton estático: sin este reset, la BD de la prueba
        // anterior se colaría en la siguiente dentro del mismo JVM. @Volatile hace que
        // Kotlin compile el campo como estático de la clase externa, no del Companion.
        ReposoDatabase::class.java.getDeclaredField("instance").apply {
            isAccessible = true
            (get(null) as? ReposoDatabase)?.close()
            set(null, null)
        }
    }

    @Test
    fun `tras reiniciar con una noche abierta reanuda el seguimiento`() = runTest {
        val dao = ReposoDatabase.get(context).sleepSessions()
        dao.insert(SleepSession(startMs = System.currentTimeMillis()))

        SleepTrackingBootReceiver.resumeIfNeeded(context)

        val started = shadowOf(context).getNextStartedService()
        assertEquals(
            SleepTrackingService::class.java.name,
            started?.component?.className,
        )
    }

    @Test
    fun `tras reiniciar sin noches abiertas no arranca nada`() = runTest {
        SleepTrackingBootReceiver.resumeIfNeeded(context)

        assertNull(shadowOf(context).getNextStartedService())
    }

    @Test
    fun `un intent que no sea de reinicio se ignora`() {
        val receiver = SleepTrackingBootReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_TIME_TICK))

        assertNull(shadowOf(context).getNextStartedService())
    }
}
