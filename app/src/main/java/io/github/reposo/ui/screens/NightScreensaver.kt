package io.github.reposo.ui.screens

import android.app.Activity
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.reposo.player.Preset
import io.github.reposo.player.Presets
import io.github.reposo.ui.theme.ClayButton
import io.github.reposo.ui.theme.ReposoTheme
import io.github.reposo.ui.theme.ReposoThemeMode
import io.github.reposo.ui.theme.clay
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Paleta nocturna: el salvapantallas es siempre oscuro, aunque el tema sea claro.
// Las piezas usan la misma arcilla (relieve mate) que el resto de la app.
// ---------------------------------------------------------------------------
private val NightTop = Color(0xFF09070F)
private val NightMid = Color(0xFF151024)
private val NightBottom = Color(0xFF1E1735)
private val NightCard = Color(0xFF241E38)
private val NightAccent = Color(0xFFCFBCFF)
private val NightText = Color(0xFFEDE7FA)
private val NightMuted = Color(0xFF9A92B5)
private val NightOnAccent = Color(0xFF2A1B52)
private val NightWire = Color(0xFF3A3350)

/**
 * Salvapantallas nocturno.
 *
 * Aparece al empezar una noche y ocupa la pantalla completa: reloj, luna de arcilla,
 * alarma y las mezclas listas para sonar. Mantiene la pantalla encendida y esconde las
 * barras del sistema mientras está visible, para no despertar con el teléfono encendido.
 */
@Composable
fun NightScreensaver(
    activeMixTitle: String?,
    monitoring: Boolean,
    snoreActive: Boolean,
    alarmEnabled: Boolean,
    alarmHour: Int,
    alarmMinute: Int,
    onAlarmChange: (Int?, Int?) -> Unit,
    onPlayMix: (Preset) -> Unit,
    onExit: () -> Unit,
    mixes: List<Preset> = Presets.all,
) {
    val context = LocalContext.current

    // Un salvapantallas no debe apagarse solo: si la pantalla se apaga, no sirve.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Inmersivo: se ocultan las barras y se restauran al salir, conservando lo que hubiera.
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previousBehavior = controller?.systemBarsBehavior
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (previousBehavior != null) controller.systemBarsBehavior = previousBehavior
        }
    }

    BackHandler(onBack = onExit)

    // Reloj: se refresca justo al cambiar de minuto, sin despertar al dispositivo cada segundo.
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val hour12 = ((now.hour + 11) % 12) + 1
    val minute = now.minute
    val suffix = if (now.hour < 12) "a. m." else "p. m."

    Box(
        Modifier
            .fillMaxSize()
            .background(
                // Un solo degradado de borde a borde: sin él, el corte entre el bloque del
                // reloj y el carrusel se veía como una banda de otro tono.
                Brush.verticalGradient(
                    0f to NightTop,
                    0.35f to NightMid,
                    0.72f to NightBottom,
                    1f to NightTop,
                ),
            ),
    ) {
        StarField(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))

            AlarmPill(
                enabled = alarmEnabled,
                hour = alarmHour,
                minute = alarmMinute,
                onPick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> onAlarmChange(h, m) },
                        alarmHour,
                        alarmMinute,
                        true,
                    ).show()
                },
            )

            Spacer(Modifier.height(20.dp))

            ClayMoon()

            Spacer(Modifier.height(6.dp))

            Text(
                text = "$hour12:${minute.toString().padStart(2, '0')}",
                color = NightText,
                fontSize = 78.sp,
                fontWeight = FontWeight.Light,
                // Ancho fijo por dígito: sin esto el reloj baila al pasar de 1:59 a 2:00.
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 78.sp,
                    fontWeight = FontWeight.Light,
                    fontFeatureSettings = "tnum",
                ),
            )

            Text(
                text = suffix,
                color = NightMuted,
                fontSize = 14.sp,
                letterSpacing = 3.sp,
            )

            Spacer(Modifier.height(26.dp))

            Text(
                text = "AYUDA PARA DORMIR",
                color = NightMuted,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(12.dp))

            // El carrusel sale del padding para que las tarjetas se corten en el borde,
            // que es lo que invita a deslizar.
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            ) {
                items(mixes, key = { it.id }) { preset ->
                    NightMixCard(
                        preset = preset,
                        active = preset.title == activeMixTitle,
                        onPlay = { onPlayMix(preset) },
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            ClayButton(
                text = "Salir",
                onClick = onExit,
                modifier = Modifier.width(210.dp),
                containerColor = NightText,
                contentColor = NightOnAccent,
            )

            Spacer(Modifier.height(18.dp))

            MonitoringLabel(monitoring = monitoring, snoreActive = snoreActive)

            Spacer(Modifier.height(28.dp))
        }
    }
}

/**
 * Cielo: estrellas que titilan a distinto ritmo y derivan muy despacio.
 *
 * Cada estrella tiene su propia fase y velocidad, así que no parpadean al unísono (que es
 * lo que delata una animación barata). La deriva es de pocos píxeles por minuto: se nota
 * si miras, pero no distrae a quien intenta dormirse.
 */
@Composable
private fun StarField(modifier: Modifier = Modifier) {
    val stars = remember {
        val random = Random(11)
        List(64) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.6f + random.nextFloat() * 1.7f,
                alpha = 0.22f + random.nextFloat() * 0.58f,
                // Periodo entre 2,6 y 7,4 s: sin dos estrellas iguales.
                periodMs = 2600 + random.nextInt(4800),
                // Desfase inicial para que ninguna arranque encendida a la vez.
                phase = random.nextFloat(),
                driftX = (random.nextFloat() - 0.5f) * 26f,
                driftY = (random.nextFloat() - 0.5f) * 20f,
            )
        }
    }

    // Un único reloj que avanza de forma continua; cada estrella lo usa con su periodo
    // y su fase. Una transition por estrella serían 64 animaciones compitiendo.
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        clock.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(60_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        )
    }

    Canvas(modifier) {
        val t = clock.value
        stars.forEach { star ->
            // Titileo: seno entre 0.35 y 1, con la fase propia de la estrella.
            val local = (t * 60_000f / star.periodMs + star.phase) % 1f
            val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(local * 2f * PI.toFloat()))

            val center = Offset(
                x = star.x * size.width + star.driftX.dp.toPx() * t,
                y = star.y * size.height + star.driftY.dp.toPx() * t,
            )
            val r = star.radius.dp.toPx()
            val a = star.alpha * twinkle

            // Halo solo en las más grandes: da profundidad sin coste perceptible.
            if (star.radius > 1.6f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = a * 0.22f), Color.Transparent),
                        center = center,
                        radius = r * 4f,
                    ),
                    radius = r * 4f,
                    center = center,
                )
            }
            drawCircle(
                color = Color.White.copy(alpha = a * 0.85f),
                radius = r,
                center = center,
            )
        }
    }
}

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val periodMs: Int,
    val phase: Float,
    val driftX: Float,
    val driftY: Float,
)

/**
 * Luna de arcilla: disco mate, cráteres, ojos cerrados y rubor.
 *
 * Tres animaciones encadenadas, todas muy lentas para no robar el sueño:
 * - respiración (escala) continua;
 * - el halo que la rodea late a otro ritmo, así el brillo no va pegado al volumen;
 * - cada cierto tiempo abre y cierra los ojos, un parpadeo de oveja dormida.
 */
@Composable
private fun ClayMoon() {
    val breath by rememberInfiniteTransition(label = "moonBreath").animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "moonBreath",
    )

    // Halo con su propio periodo (7,3 s) y desfasado: el brillo respira distinto que la masa.
    val halo by rememberInfiniteTransition(label = "moonHalo").animateFloat(
        initialValue = 0.72f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(7300, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "moonHaloPulse",
    )

    // Parpadeo: casi todo el tiempo dormida (1 = ojos cerrados). Cada 7 s se abre un
    // instante y vuelve a cerrarse con una curva suave.
    var eyelid by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(7000)
            animate(
                initialValue = 1f,
                targetValue = 0.12f,
                animationSpec = tween(420, easing = EaseInOut),
            ) { value, _ -> eyelid = value }
            animate(
                initialValue = 0.12f,
                targetValue = 1f,
                animationSpec = tween(620, easing = EaseInOut),
            ) { value, _ -> eyelid = value }
        }
    }

    Canvas(
        Modifier
            .size(146.dp)
            .graphicsLayer {
                scaleX = breath
                scaleY = breath
            },
    ) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Halo: da la sensación de que la luna ilumina la habitación.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NightAccent.copy(alpha = 0.20f * halo), Color.Transparent),
                center = center,
                radius = r * 2.05f * halo,
            ),
            radius = r * 2.05f * halo,
            center = center,
        )

        // Disco: luz arriba-izquierda, sombra abajo-derecha (misma lógica que clayLighting).
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFBF7FF), Color(0xFFDCD0F6), Color(0xFFBFAEE6)),
                center = Offset(center.x - r * 0.34f, center.y - r * 0.38f),
                radius = r * 1.65f,
            ),
            radius = r,
            center = center,
        )

        // Cráteres: hundidos, con el borde inferior iluminado.
        val craters = listOf(
            Triple(-0.30f, -0.22f, 0.155f),
            Triple(0.26f, -0.34f, 0.10f),
            Triple(0.34f, 0.16f, 0.075f),
        )
        craters.forEach { (dx, dy, cr) ->
            val c = Offset(center.x + r * dx, center.y + r * dy)
            drawCircle(Color(0x1A3B2A63), radius = r * cr, center = c)
            drawCircle(
                color = Color(0x33FFFFFF),
                radius = r * cr,
                center = c + Offset(0f, r * cr * 0.30f),
                style = Stroke(width = r * 0.05f),
            )
        }

        // Ojos: el arco se aplana al abrirse. Con eyelid = 1 queda la curva dormida de
        // siempre y con 0,12 una línea casi recta, que se lee como ojo abierto.
        val eyeY = center.y + r * 0.06f
        val eyeDx = r * 0.34f
        val eyeW = r * 0.36f
        val eyeH = r * 0.30f * eyelid
        listOf(-1f, 1f).forEach { side ->
            drawArc(
                color = Color(0xFF5B4A84),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x + side * eyeDx - eyeW / 2f, eyeY - eyeH / 2f),
                size = Size(eyeW, eyeH),
                style = Stroke(width = r * 0.062f, cap = StrokeCap.Round),
            )
        }

        // Rubor.
        listOf(-1f, 1f).forEach { side ->
            drawCircle(
                color = Color(0x40F2A8C4),
                radius = r * 0.15f,
                center = Offset(center.x + side * r * 0.44f, center.y + r * 0.30f),
            )
        }

        // Boquita: media curva.
        drawArc(
            color = Color(0xFF5B4A84),
            startAngle = 25f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(center.x - r * 0.11f, center.y + r * 0.18f),
            size = Size(r * 0.22f, r * 0.16f),
            style = Stroke(width = r * 0.055f, cap = StrokeCap.Round),
        )
    }
}

/** Píldora de alarma: muestra la hora si está puesta, o invita a definirla. */
@Composable
private fun AlarmPill(
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onPick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clay(RoundedCornerShape(50), NightCard, elevation = 9.dp, softness = 0.55f)
            .clickable(onClick = onPick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Alarm,
            contentDescription = null,
            tint = NightAccent,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (enabled) "%02d:%02d".format(hour, minute) else "Alarma inteligente",
                color = NightText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (enabled) "Despertar programado" else "Toca para elegir la hora",
                color = NightMuted,
                fontSize = 12.sp,
            )
        }
    }
}

/** Tarjeta de mezcla: la luna de fondo le da profundidad sin competir con el texto. */
@Composable
private fun NightMixCard(
    preset: Preset,
    active: Boolean,
    onPlay: () -> Unit,
) {
    Box(
        Modifier
            .width(216.dp)
            .height(158.dp)
            .clay(RoundedCornerShape(26.dp), NightCard, elevation = 10.dp, softness = 0.6f)
            .clip(RoundedCornerShape(26.dp)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NightAccent.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.30f, size.height * 0.15f),
                    radius = size.width * 0.9f,
                ),
                radius = size.width * 0.9f,
                center = Offset(size.width * 0.30f, size.height * 0.15f),
            )
        }

        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Text(
                text = preset.title,
                color = NightText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = "${preset.layers.size} elemento${if (preset.layers.size == 1) "" else "s"}",
                color = NightMuted,
                fontSize = 12.sp,
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clay(CircleShape, NightAccent, elevation = 6.dp, softness = 0.7f)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Reproducir ${preset.title}",
                        tint = NightOnAccent,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (active) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = NightAccent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Sonando",
                            color = NightAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        // Punto hundido: remate visual del carrusel, como la ranura del slider.
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NightWire),
                        )
                    }
                }
            }
        }
    }
}

/** Estado del monitor: punto que late cuando hay una noche en curso. */
@Composable
private fun MonitoringLabel(monitoring: Boolean, snoreActive: Boolean) {
    val pulse by rememberInfiniteTransition(label = "monitorPulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "monitorPulse",
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (monitoring) {
            Box(
                Modifier
                    .size(7.dp)
                    .graphicsLayer { alpha = pulse }
                    .clip(CircleShape)
                    .background(NightAccent),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = when {
                snoreActive -> "Grabando ronquidos…"
                monitoring -> "Monitoreando…"
                else -> "Sin sesión activa"
            },
            color = NightMuted,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp,
        )
    }
}

@Preview(name = "Salvapantallas", showBackground = true, backgroundColor = 0xFF09070F, heightDp = 900)
@Composable
private fun NightScreensaverPreview() = ReposoTheme(mode = ReposoThemeMode.DARK) {
    NightScreensaver(
        activeMixTitle = "Marrón profundo",
        monitoring = true,
        snoreActive = false,
        alarmEnabled = true,
        alarmHour = 7,
        alarmMinute = 0,
        onAlarmChange = { _, _ -> },
        onPlayMix = {},
        onExit = {},
    )
}
