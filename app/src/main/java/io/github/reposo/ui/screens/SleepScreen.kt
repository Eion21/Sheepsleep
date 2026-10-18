package io.github.reposo.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.reposo.R
import io.github.reposo.ui.LocalBottomBarInset
import io.github.reposo.ui.stretchOverscroll
import io.github.reposo.data.SleepSession
import io.github.reposo.ui.theme.ClayBackground
import io.github.reposo.ui.theme.clay
import io.github.reposo.ui.theme.claySwitchColors
import io.github.reposo.ui.theme.ReposoTheme
import io.github.reposo.ui.theme.ReposoThemeMode
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SleepScreen(
    active: SleepSession?,
    mixTitle: String?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenNightMode: () -> Unit = {},
    onTimerSelected: (Int?) -> Unit = {},
    alarmEnabled: Boolean = false,
    alarmHour: Int = 7,
    alarmMinute: Int = 0,
    onAlarmChange: (Int?, Int?) -> Unit = { _, _ -> },
    snoreActive: Boolean = false,
    snoreStartMs: Long = 0L,
    onSnoreToggle: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val sleeping = active != null
    var sheepCount by remember { mutableIntStateOf(0) }
    var selectedTimer by remember { mutableStateOf("Sin límite") }
    var soundEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(sleeping) {
        if (!sleeping) sheepCount = 0
    }

    val infiniteTransition = rememberInfiniteTransition(
        label = "breathing"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4000,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .stretchOverscroll()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(
                modifier = Modifier.height(35.dp)
            )

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (sleeping)
                            "Buenas noches"
                        else
                            "Listo para dormir",

                        color = colors.onSurface,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (sleeping)
                            "Relájate y deja que el sueño llegue"
                        else
                            "Prepara tu mente para descansar",

                        color = colors.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clay(RoundedCornerShape(50), colors.primaryContainer, elevation = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Nightlight,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(35.dp)
            )

            // ESTADO DE SUEÑO
            if (sleeping) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenNightMode)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Durmiendo...",
                        color = colors.primary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    // Vuelve al salvapantallas si se salió de él sin detener la noche.
                    Text(
                        text = "· volver al modo noche",
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                Text(
                    text = "Tu noche comienza aquí",
                    color = colors.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // BOTÓN PRINCIPAL (Circular click and ripple shape fixed with clip(CircleShape))
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(
                        if (sleeping)
                            breathingScale
                        else
                            1f
                    )
                    .border(
                        width = 10.dp,
                        color = colors.primary.copy(
                            alpha = 0.10f
                        ),
                        shape = CircleShape
                    )
                    .padding(10.dp)
                    .clip(CircleShape)
                    .clay(CircleShape, colors.primary, elevation = 12.dp)
                    .clickable {
                        if (sleeping) {
                            onStop()
                        } else {
                            onStart()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (sleeping) Icons.Rounded.Pause else Icons.Rounded.Nightlight,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(42.dp),
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = if (sleeping)
                            "Pausar"
                        else
                            "Empezar",

                        color = colors.onPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // OVEJITAS
            SheepCounter(
                sleeping = sleeping,
                sheepCount = sheepCount,
                onSheep = {
                    if (sleeping) {
                        sheepCount++
                    }
                }
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // SONIDO ACTUAL
            SoundSelector(
                soundName = mixTitle ?: "Marrón profundo",
                soundEnabled = soundEnabled,
                onToggle = {
                    soundEnabled = !soundEnabled
                }
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // TEMPORIZADOR
            TimerSelector(
                selectedTimer = selectedTimer,
                onSelected = {
                    selectedTimer = it
                    // "15 minutos" -> 15; "Sin límite" -> null (cancela)
                    onTimerSelected(it.removeSuffix(" minutos").toIntOrNull())
                }
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // ALARMA
            AlarmSelector(
                enabled = alarmEnabled,
                hour = alarmHour,
                minute = alarmMinute,
                onChange = onAlarmChange,
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // GRABACIÓN DE RONQUIDOS
            SnoreSelector(
                active = snoreActive,
                startMs = snoreStartMs,
                onToggle = onSnoreToggle,
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            if (sleeping) {
                Text(
                    text = when {
                        sheepCount == 0 ->
                            "Respira lentamente..."

                        sheepCount < 10 ->
                            "Las ovejitas están llegando 🐑"

                        sheepCount < 25 ->
                            "Muy bien... sigue relajándote"

                        else ->
                            "Dulces sueños 💤"
                    },

                    color = colors.onSurfaceVariant,
                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }

            Spacer(
                // El último bloque sube por encima de la barra flotante.
                modifier = Modifier.height(40.dp + LocalBottomBarInset.current)
            )
        }
    }
}

@Composable
fun SheepCounter(
    sleeping: Boolean,
    sheepCount: Int,
    onSheep: () -> Unit
) {
    var sleepingPose by remember { mutableIntStateOf(0) }
    var cardWidthPx by remember { mutableIntStateOf(0) }
    val jump = remember { Animatable(0f) }

    LaunchedEffect(sheepCount) {
        if (sheepCount > 0) {
            jump.snapTo(0f)
            jump.animateTo(1f, animationSpec = tween(650, easing = LinearEasing))
        }
    }

    val t = jump.value
    val arc = sin(PI.toFloat() * t)
    val jumping = sheepCount > 0 && t < 1f
    val numberScale = 1f + 0.35f * arc

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { cardWidthPx = it.width }
            .clay(RoundedCornerShape(20.dp), MaterialTheme.colorScheme.surface, elevation = 5.dp)
            .clickable(enabled = sleeping) {
                onSheep()
                sleepingPose = (sleepingPose + 1) % 2
            }
            .padding(
                horizontal = 18.dp,
                vertical = 14.dp
            ),

        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (sleepingPose == 0) R.drawable.ic_sheep_sleep_a
                    else R.drawable.ic_sheep_sleep_b,
                ),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )

            if (jumping) {
                Icon(
                    painter = painterResource(R.drawable.ic_sheep_jump),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .graphicsLayer {
                            translationX = cardWidthPx * 0.72f * (1f - 2f * t)
                            translationY = -cardWidthPx * 0.14f * arc
                            rotationZ = 18f * arc
                            scaleX = 1.1f
                            scaleY = 1.1f
                        },
                )
            }
        }

        Spacer(
            modifier = Modifier.width(14.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Contar ovejitas",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Text(
                text = if (sleeping)
                    "Toca cada vez que salte una"
                else
                    "Disponible al comenzar",

                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        if (sleeping) {
            Text(
                text = "$sheepCount",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    scaleX = numberScale
                    scaleY = numberScale
                },
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_sheep_sleep_a),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun SoundSelector(
    soundName: String,
    soundEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clay(RoundedCornerShape(18.dp), MaterialTheme.colorScheme.surface, elevation = 5.dp)
            .padding(16.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(
            modifier = Modifier.width(14.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = soundName,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Sonido relajante",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Switch(
            checked = soundEnabled,
            onCheckedChange = {
                onToggle()
            },
            colors = claySwitchColors()
        )
    }
}

@Composable
fun TimerSelector(
    selectedTimer: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clay(RoundedCornerShape(18.dp), MaterialTheme.colorScheme.surface, elevation = 5.dp)
                .clickable {
                    expanded = true
                }
                .padding(16.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Temporizador",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Text(
                    text = selectedTimer,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            listOf(
                "Sin límite",
                "15 minutos",
                "30 minutos",
                "45 minutos",
                "60 minutos",
                "90 minutos"
            ).forEach { timer ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = timer,
                            color = if (timer == selectedTimer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (timer == selectedTimer) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelected(timer)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AlarmSelector(
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onChange: (Int?, Int?) -> Unit,
) {
    val context = LocalContext.current

    fun showPicker() {
        TimePickerDialog(
            context,
            { _, pickedHour, pickedMinute -> onChange(pickedHour, pickedMinute) },
            hour,
            minute,
            true,
        ).show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clay(RoundedCornerShape(18.dp), MaterialTheme.colorScheme.surface, elevation = 5.dp)
            .clickable { showPicker() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Alarm,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Despertar",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Text(
                text = if (enabled) "%02d:%02d".format(hour, minute) else "Sin alarma",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = { on ->
                if (on) showPicker() else onChange(null, null)
            },
            colors = claySwitchColors(),
        )
    }
}

@Composable
fun SnoreSelector(
    active: Boolean,
    startMs: Long,
    onToggle: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active, startMs) {
        while (active) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
    }
    val elapsed = if (active && startMs > 0) (now - startMs).coerceAtLeast(0) else 0L
    val minutes = elapsed / 60_000
    val seconds = (elapsed % 60_000) / 1_000

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clay(RoundedCornerShape(18.dp), MaterialTheme.colorScheme.surface, elevation = 5.dp)
            .clickable { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Grabar ronquidos",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Text(
                text = if (active) "Grabando · se guarda solo lo que suena fuerte"
                       else "Escucha y guarda solo los ronquidos",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }

        if (active) {
            Text(
                text = "Detener · %02d:%02d".format(minutes, seconds),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
    }
}

@Preview(name = "Dormir · Claro", showBackground = true, backgroundColor = 0xFFF6F1FB)
@Composable
private fun SleepLightPreview() = ReposoTheme(mode = ReposoThemeMode.LIGHT) {
    SleepPreviewContent()
}

@Preview(name = "Dormir · Oscuro", showBackground = true, backgroundColor = 0xFF12101C)
@Composable
private fun SleepDarkPreview() = ReposoTheme(mode = ReposoThemeMode.DARK) {
    SleepPreviewContent()
}

@Composable
private fun SleepPreviewContent() {
    ClayBackground {
        SleepScreen(active = null, mixTitle = "Marrón profundo", onStart = {}, onStop = {})
    }
}
