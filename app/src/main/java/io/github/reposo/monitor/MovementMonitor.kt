package io.github.reposo.monitor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.github.reposo.data.MovementBucket
import io.github.reposo.data.ReposoDatabase
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Muestrea el acelerómetro mientras está activo (lo llama PlaybackService) y
 * agrega lecturas en buckets de 10 s con un score de actividad 0–3, que se
 * persisten en lote en Room cada minuto y al parar. Los callbacks del sensor
 * llegan en el hilo principal: el estado de agregación no necesita bloqueos.
 */
object MovementMonitor : SensorEventListener {

    private const val BUCKET_MS = 10_000L
    private const val FLUSH_MS = 60_000L

    /**
     * Umbrales en m/s² sobre la gravedad estimada.
     *
     * Estaban en 0.18 / 0.6 / 1.5 y no detectaban nada: dormir en una cama quieta da
     * medias de 0.02–0.10, y girarse en la cama rara vez sostiene 0.6 de media durante
     * 10 s. Ahora 0.05 ya marca actividad ligera, que es lo que separa "dormido quieto"
     * de "me moví".
     */
    private const val MEAN_LIGHT = 0.035f
    private const val MEAN_MEDIUM = 0.18f
    private const val MEAN_HIGH = 0.55f

    /**
     * Un pico aislado tampoco basta para marcar "inquieto": girarse en la cama produce
     * picos de 2–4 m/s² varias veces por noche, y eso no debería pintar la noche de rojo.
     * Con 6 m/s² hace falta un golpe claro (caerse, levantarse de golpe).
     */
    private const val PEAK_HIGH = 6f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var sensorManager: SensorManager? = null
    private var appContext: Context? = null
    private var running = false

    private val gravity = FloatArray(3)
    private var gravityInit = false
    private var bucketStartMs = 0L
    private var lastFlushMs = 0L
    private var sum = 0f
    private var peak = 0f
    private var count = 0
    private val pending = mutableListOf<MovementBucket>()

    fun start(context: Context) {
        if (running) return
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) return // sin acelerómetro: monitor deshabilitado

        sensorManager = manager
        appContext = context.applicationContext
        gravityInit = false
        val now = System.currentTimeMillis()
        bucketStartMs = now
        lastFlushMs = now
        sum = 0f
        peak = 0f
        count = 0
        running = manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        if (!running) {
            sensorManager = null
            appContext = null
        }
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager?.unregisterListener(this)
        sensorManager = null
        // Cierra la ventana parcial si aporta algo.
        if (count > 0 && System.currentTimeMillis() - bucketStartMs >= 2_000) {
            closeBucket(System.currentTimeMillis())
        }
        flush()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!running) return
        val values = event.values
        if (!gravityInit) {
            System.arraycopy(values, 0, gravity, 0, 3)
            gravityInit = true
        } else {
            gravity[0] = 0.9f * gravity[0] + 0.1f * values[0]
            gravity[1] = 0.9f * gravity[1] + 0.1f * values[1]
            gravity[2] = 0.9f * gravity[2] + 0.1f * values[2]
        }
        val dx = values[0] - gravity[0]
        val dy = values[1] - gravity[1]
        val dz = values[2] - gravity[2]
        val magnitude = sqrt(dx * dx + dy * dy + dz * dz)
        sum += magnitude
        if (magnitude > peak) peak = magnitude
        count++

        val now = System.currentTimeMillis()
        if (now - bucketStartMs >= BUCKET_MS) {
            closeBucket(now)
            if (now - lastFlushMs >= FLUSH_MS) flush()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun closeBucket(now: Long) {
        if (count > 0) {
            val mean = sum / count
            val score = when {
                mean >= MEAN_HIGH || peak >= PEAK_HIGH -> 3
                mean >= MEAN_MEDIUM -> 2
                mean >= MEAN_LIGHT -> 1
                else -> 0
            }
            pending.add(MovementBucket(startMs = bucketStartMs, score = score))
        }
        bucketStartMs = now
        sum = 0f
        peak = 0f
        count = 0
    }

    private fun flush() {
        if (pending.isEmpty()) return
        // El contexto se comprueba ANTES de vaciar: si es null, antes se descartaba el
        // lote silenciosamente y se perdía hasta un minuto de movimiento.
        val context = appContext ?: return
        val batch = pending.toList()
        pending.clear()
        lastFlushMs = System.currentTimeMillis()
        val dao = ReposoDatabase.get(context).movementBuckets()
        scope.launch {
            try {
                dao.insertAll(batch)
            } catch (_: Exception) {
                // Si falla la escritura, se devuelve el lote para no perder los datos.
                pending.addAll(batch)
            }
        }
    }
}
