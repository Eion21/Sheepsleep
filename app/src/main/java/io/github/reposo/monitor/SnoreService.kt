package io.github.reposo.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.github.reposo.R
import io.github.reposo.data.ReposoDatabase
import io.github.reposo.data.SettingsStore
import io.github.reposo.data.SnoreClip
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service que graba ronquidos mientras está activo: mantiene un ring
 * buffer de 2 s, y cuando el RMS supera el umbral guarda un clip WAV (pre-buffer
 * incluido). Se activa solo desde primer plano (Android 14+ lo exige).
 */
class SnoreService : Service() {

    private val settings by lazy { SettingsStore(this) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureThread: Thread? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile
    private var running = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                if (!settings.snoreActive) settings.snoreActive = true
                if (settings.snoreStartMs == 0L) settings.snoreStartMs = System.currentTimeMillis()
            }
        }

        // Siempre antes de cualquier return: si se llegó vía startForegroundService,
        // no llamarlo en <5 s provoca crash en Android 12+.
        startAsForeground()

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!running) beginCapture()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        captureThread?.interrupt()
        captureThread = null
        settings.snoreActive = false
        settings.snoreStartMs = 0L
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        serviceScope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        super.onDestroy()
    }

    private fun startAsForeground() {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, SnoreService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_snore_mic)
            .setContentTitle("Grabando ronquidos")
            .setContentText("Escuchando mientras duermes")
            .setOngoing(true)
            .addAction(0, "Detener", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }

        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "reposo:snore").apply {
                acquire()
            }
        }
    }

    private fun beginCapture() {
        running = true
        captureThread = Thread(::captureLoop, "snore-capture").also { it.start() }
    }

    private fun captureLoop() {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,
        )
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,
                max(minBuffer, READ_SAMPLES * 2) * 4,
            )
        } catch (_: Exception) {
            failCapture()
            return
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            failCapture()
            return
        }

        val ring = ShortArray(RING_SAMPLES)
        var ringIndex = 0
        var ringFilled = false
        val readBuf = ShortArray(READ_SAMPLES)

        var out: RandomAccessFile? = null
        var clipStartMs = 0L
        var clipPeak = 0
        var clipSamples = 0
        var loudMs = 0L
        var quietMs = 0L
        val chunkMs = READ_SAMPLES * 1000L / SAMPLE_RATE
        val detector = SnoreDetector(
            loudThreshold = LOUD_THRESHOLD,
            quietThreshold = QUIET_THRESHOLD,
            triggerMs = TRIGGER_MS,
            quietMs = QUIET_MS,
            maxSamples = MAX_SAMPLES,
        )

        try {
            record.startRecording()
            while (running && !Thread.currentThread().isInterrupted) {
                val n = record.read(readBuf, 0, READ_SAMPLES)
                if (n <= 0) continue

                var sumSquares = 0.0
                var peak = 0
                for (i in 0 until n) {
                    val s = readBuf[i].toInt()
                    sumSquares += s.toDouble() * s
                    val abs = if (s < 0) -s else s
                    if (abs > peak) peak = abs
                    ring[ringIndex] = s.toShort()
                    ringIndex++
                    if (ringIndex == RING_SAMPLES) {
                        ringIndex = 0
                        ringFilled = true
                    }
                }
                val rms = sqrt(sumSquares / n) / 32768.0
                val currentMs = System.currentTimeMillis()

                // La decisión de abrir y cerrar clips vive en SnoreDetector, que se prueba
                // aparte. Aquí solo se escribe el audio cuando toca.
                if (out != null) {
                    out.writePcm(readBuf, n)
                    clipSamples += n
                    clipPeak = max(clipPeak, peak)
                }
                when (detector.feed(rms, n, chunkMs)) {
                    SnoreDetector.Event.StartClip -> {
                        val back = RING_SAMPLES * 1000L / SAMPLE_RATE
                        out = openClip(currentMs - back, ring, ringIndex, ringFilled)
                        if (out == null) {
                            failCapture()
                            return
                        }
                        clipStartMs = currentMs - back
                        clipPeak = 0
                        clipSamples = 0
                    }

                    SnoreDetector.Event.EndClip -> {
                        out?.let { closeClip(it, clipStartMs, clipPeak) }
                        out = null
                    }

                    null -> Unit
                }
            }
        } catch (_: Exception) {
            // micrófono inválido o interrupción: cierra lo que haya.
        } finally {
            try {
                out?.let { closeClip(it, clipStartMs, clipPeak) }
            } catch (_: Exception) {
            }
            try {
                record.stop()
            } catch (_: Exception) {
            }
            record.release()
        }

        if (running && !Thread.currentThread().isInterrupted) failCapture()
    }

    /** Escribe el header WAV con placeholders y vierte el ring (pre-buffer). */
    private fun openClip(startMs: Long, ring: ShortArray, ringIndex: Int, filled: Boolean): RandomAccessFile? {
        return try {
            val dir = File(filesDir, "snore").apply { mkdirs() }
            val file = File(dir, "clip_$startMs.wav")
            val raf = RandomAccessFile(file, "rw")
            raf.writeWavHeader(SAMPLE_RATE)
            if (filled) {
                for (i in ringIndex until RING_SAMPLES) raf.writeShortLE(ring[i])
            }
            for (i in 0 until ringIndex) raf.writeShortLE(ring[i])
            raf
        } catch (_: Exception) {
            null
        }
    }

    private fun closeClip(
        raf: RandomAccessFile,
        startMs: Long,
        peak: Int,
    ) {
        try {
            val totalData = raf.length() - 44
            // El mínimo se mide sobre el ARCHIVO, no sobre "samples". Los 2 s de
            // pre-buffer ya están escritos antes de que el detector abra el clip, así que
            // comparar solo lo escrito después tiraba clips válidos de menos de 250 ms
            // (un ronquido corto) aunque el wav tuviera segundos de audio.
            if (totalData <= 0 || totalData < MIN_CLIP_BYTES) {
                raf.close()
                File(filesDir, "snore/clip_$startMs.wav").delete()
                return
            }
            raf.seek(4)
            raf.writeIntLE((36 + totalData).toInt())
            raf.seek(40)
            raf.writeIntLE(totalData.toInt())
            raf.close()
            val durationMs = totalData * 1000L / (SAMPLE_RATE * 2L)
            val peakDb = if (peak <= 0) -96 else (20.0 * log10(peak / 32767.0)).toInt().coerceIn(-96, 0)
            val dao = ReposoDatabase.get(applicationContext).snoreClips()
            serviceScope.launch {
                dao.insert(SnoreClip(startMs = startMs, durationMs = durationMs, file = "clip_$startMs.wav", peakDb = peakDb))
            }
        } catch (_: Exception) {
            // archivo corrupto: mejor dejarlo fuera de la BD; el cleanup lo borra.
        }
    }

    private fun failCapture() {
        // Se avisa en la notificación y se apaga el flag: antes el servicio se detenía
        // en silencio y la UI seguía diciendo "Grabando" aunque el micrófono estuviera
        // cerrado (por ejemplo, ocupado por otra app).
        running = false
        settings.snoreActive = false
        settings.snoreStartMs = 0L
        serviceScope.launch {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(
                NOTIF_ID + 1,
                NotificationCompat.Builder(this@SnoreService, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_snore_mic)
                    .setContentTitle("No se pudo usar el micrófono")
                    .setContentText("Otra app puede estar usándolo. La grabación se detuvo.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build(),
            )
            stopSelf()
        }
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Grabación de ronquidos", NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        const val ACTION_START = "io.github.reposo.action.START_SNORE"
        const val ACTION_STOP = "io.github.reposo.action.STOP_SNORE"
        private const val CHANNEL_ID = "snore"
        private const val NOTIF_ID = 27

        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val READ_SAMPLES = 2048 // 128 ms
        private const val RING_SAMPLES = SAMPLE_RATE * 2 // 2 s de pre-buffer

        /**
         * Umbrales de nivel (RMS 0..1), medidos sobre el micrófono del teléfono.
         *
         * Estaban en 0.06 / 0.02, que en la práctica solo capturaban toses o hablar
         * cerca del móvil. Un ronquido real a un metro da 0.02–0.06 de RMS, y el sonido
         * de la habitación de noche ronda 0.005–0.015. Bajarlos a 0.02 / 0.008 es lo que
         * separa "respiración y ronquido" de "silencio".
         */
        private const val LOUD_THRESHOLD = 0.02
        private const val QUIET_THRESHOLD = 0.008

        /**
         * 800 ms de sonido sostenido para abrir un clip dejaba fuera los ronquidos
         * entrecortados. 350 ms detecta un ronquido suelto sin dispararse con un golpe
         * de sábanas.
         */
        private const val TRIGGER_MS = 350L
        private const val QUIET_MS = 4_000L
        private const val MIN_CLIP_BYTES = SAMPLE_RATE * 2 / 4 // 250 ms de PCM 16 bits mono
        private const val MAX_SAMPLES = SAMPLE_RATE * 60 // 60 s por clip

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, SnoreService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, SnoreService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}

private fun RandomAccessFile.writeWavHeader(sampleRate: Int) {
    writeBytes("RIFF")
    writeIntLE(0) // riff size, se parchea
    writeBytes("WAVE")
    writeBytes("fmt ")
    writeIntLE(16)
    writeShortLE(1) // PCM
    writeShortLE(1) // mono
    writeIntLE(sampleRate)
    writeIntLE(sampleRate * 2) // byte rate
    writeShortLE(2) // block align
    writeShortLE(16) // bits
    writeBytes("data")
    writeIntLE(0) // data size, se parchea
}

private fun RandomAccessFile.writeShortLE(v: Short) {
    writeByte(v.toInt() and 0xFF)
    writeByte((v.toInt() shr 8) and 0xFF)
}

private fun RandomAccessFile.writeIntLE(v: Int) {
    writeByte(v and 0xFF)
    writeByte((v shr 8) and 0xFF)
    writeByte((v shr 16) and 0xFF)
    writeByte((v shr 24) and 0xFF)
}

private fun RandomAccessFile.writePcm(buf: ShortArray, n: Int) {
    for (i in 0 until n) writeShortLE(buf[i])
}
