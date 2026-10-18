package io.github.reposo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.reposo.R
import io.github.reposo.ui.theme.clay
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class SheepPhase { IDLE, ARRIVING, JUMPING, RESTING }

/**
 * Un toque = una oveja.
 *
 * El toque inicia el ciclo y el resto se reproduce solo: la oveja aparece dormida a la
 * derecha, salta en parábola (oveja 2) hacia la izquierda, se duerme al caer y se
 * desvanece. Antes cada fase esperaba un toque distinto, así que una sola oveja visible
 * costaba 3 toques y el contador sumaba 3. Ahora los toques dados mientras la animación
 * corre se ignoran, de modo que el número nunca supera a las ovejas que se han visto.
 */
@Composable
fun AnimatedSheepCounterCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String = "Toca para contar ovejitas"
) {
    var count by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(SheepPhase.IDLE) }

    // El ciclo avanza solo: aparecer, saltar, dormirse y desvanecerse no piden más toques.
    LaunchedEffect(phase) {
        when (phase) {
            SheepPhase.ARRIVING -> {
                delay(700)
                phase = SheepPhase.JUMPING
            }
            SheepPhase.RESTING -> {
                delay(650)
                phase = SheepPhase.IDLE
            }
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clay(RoundedCornerShape(22.dp), MaterialTheme.colorScheme.surface, elevation = 6.dp)
            .clickable(enabled = enabled) {
                // Solo cuenta el toque que arranca un ciclo; el resto ya es animación.
                if (phase == SheepPhase.IDLE) {
                    count++
                    phase = SheepPhase.ARRIVING
                }
            }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Contar ovejitas",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        // Singular/plural: con 1 el texto decía "1 ovejitas".
                        text = when (count) {
                            0 -> subtitle
                            1 -> "1 ovejita"
                            else -> "$count ovejitas"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                AnimatedContent(
                    targetState = count,
                    transitionSpec = {
                        (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                    },
                    label = "sheepCount",
                ) { n ->
                    Text(
                        "$n",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            SheepStage(
                phase = phase,
                onJumpDone = { phase = SheepPhase.RESTING },
            )
        }
    }
}

/**
 * Escenario de la oveja. Se aísla en su propio composable porque `AnimatedVisibility`
 * tiene sobrecargas para ColumnScope y BoxScope: dentro de un Box anidado en la Column
 * la resolución se vuelve ambigua y el compilador rechaza la llamada implícita.
 */
@Composable
private fun SheepStage(phase: SheepPhase, onJumpDone: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(120.dp)) {
        Fence(Modifier.align(Alignment.BottomCenter))
        AnimatedVisibility(
            visible = phase != SheepPhase.IDLE,
            enter = fadeIn(tween(240)) + scaleIn(initialScale = 0.8f, animationSpec = tween(240)),
            exit = fadeOut(tween(220)) + scaleOut(targetScale = 0.8f, animationSpec = tween(220)),
        ) {
            when (phase) {
                SheepPhase.ARRIVING ->
                    SleepingSheep(R.drawable.oveja1, Modifier.align(Alignment.BottomEnd))
                SheepPhase.JUMPING ->
                    JumpingSheep(onDone = onJumpDone)
                // RESTING y el IDLE del desvanecido: la oveja ya dormida a la izquierda.
                else ->
                    SleepingSheep(R.drawable.oveja3, Modifier.align(Alignment.BottomStart))
            }
        }
    }
}

@Composable
private fun Fence(modifier: Modifier = Modifier) {
    val wood = Color(0xFF8D6E63)
    Canvas(modifier.size(width = 64.dp, height = 48.dp)) {
        val thickness = 6.dp.toPx()
        listOf(0.1f, 0.5f, 0.9f).forEach { fx ->
            drawRect(wood, Offset(size.width * fx - thickness / 2, 0f), Size(thickness, size.height))
        }
        listOf(0.25f, 0.65f).forEach { fy ->
            drawRect(wood, Offset(0f, size.height * fy), Size(size.width, thickness * 0.7f))
        }
    }
}

@Composable
private fun SleepingSheep(res: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = modifier.size(40.dp),
    )
}

@Composable
private fun JumpingSheep(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 1500, easing = LinearEasing))
        onDone()
    }

    val density = LocalDensity.current
    val jumpPx = with(density) { 70.dp.toPx() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value * density.density
        Image(
            painter = painterResource(R.drawable.oveja2),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(40.dp)
                .graphicsLayer {
                    val p = progress.value
                    translationX = -(w - size.width) * p
                    val arc = sin(p * PI).toFloat()
                    translationY = -arc * jumpPx
                    rotationZ = -14f * cos(p * PI).toFloat()
                },
        )
    }
}
