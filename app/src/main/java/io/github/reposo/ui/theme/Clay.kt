@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.reposo.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// 1) SUPERFICIE DE ARCILLA: sombra exterior + luz interior + borde grueso
// ---------------------------------------------------------------------------

val ClayShape = RoundedCornerShape(20.dp)
val ClayBigShape = RoundedCornerShape(28.dp)
val ClayPillShape = RoundedCornerShape(50)

/**
 * Iluminación: luz difusa desde arriba-izquierda, sombra suave hacia abajo-derecha.
 *
 * El blanco se mantiene bajo (0.13) y con un recorrido corto (0.38): así se lee como un
 * brillo de borde que infla la pieza, en vez de un velo que lava toda la tarjeta y la
 * vuelve grisácea contra el fondo oscuro.
 */
private fun DrawScope.clayLighting(softness: Float = 0.5f) {
    val diagonal = Offset(size.width, size.height)
    drawRect(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.13f * softness), Color.Transparent),
            start = Offset.Zero,
            end = Offset(size.width * 0.38f, size.height * 0.38f),
        ),
    )
    drawRect(
        Brush.linearGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.10f * softness)),
            start = Offset(size.width * 0.45f, size.height * 0.45f),
            end = diagonal,
        ),
    )
}

/**
 * Modificador base clay: borde redondeado, fondo plano, sombras interior
 * (dibujadas bajo el contenido para no teñir el texto).
 */
fun Modifier.clay(
    shape: Shape = ClayShape,
    color: Color,
    elevation: Dp = 6.dp,
    softness: Float = 1f,
): Modifier = this
    .shadow(elevation, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.45f))
    .clip(shape)
    .background(color)
    .drawWithContent {
        clayLighting(softness)
        drawContent()
    }

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    shape: Shape = ClayShape,
    padding: Dp = 16.dp,
    elevation: Dp = 6.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clay(shape, containerColor, elevation)
            .then(
                if (onClick != null) {
                    val interaction = remember { MutableInteractionSource() }
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(padding),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// 2) BOTÓN MULLIDO: se aplana al pulsar (feedback clay)
// ---------------------------------------------------------------------------

@Composable
fun ClayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
    shape: Shape = ClayPillShape,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0f else 1f, tween(200), label = "clayPress")
    val color = if (enabled) containerColor else containerColor.copy(alpha = 0.38f)
    val contentAlpha = if (enabled) 1f else 0.5f

    // Row y no Box: en un Box el icono guía, el texto y el icono final se apilan
    // en el mismo centro y quedan superpuestos (el icono se veía encima de las letras).
    Row(
        modifier
            .graphicsLayer {
                scaleX = 0.97f + 0.03f * press
                scaleY = 0.95f + 0.05f * press
            }
            .clay(
                shape,
                color,
                elevation = (2.dp + 6.dp * press),
                softness = 0.6f + 0.4f * press,
            )
            .then(
                if (enabled) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 28.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Los iconos heredan el color del contenido. Antes caían en el LocalContentColor
        // del fondo (onBackground), que sobre el lavanda del botón daba poco contraste.
        CompositionLocalProvider(LocalContentColor provides contentColor.copy(alpha = contentAlpha)) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = contentColor.copy(alpha = contentAlpha),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
fun ClayOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ClayPillShape,
) {
    val colorScheme = MaterialTheme.colorScheme
    ClayButton(
        text,
        onClick,
        modifier,
        containerColor = colorScheme.surfaceVariant,
        contentColor = colorScheme.onSurfaceVariant,
        shape = shape,
    )
}

/** Chip clay: seleccionado = acento y menos relieve; sin seleccionar = superficie mate. */
@Composable
fun ClayChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clay(
                ClayPillShape,
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                elevation = if (selected) 5.dp else 3.dp,
                softness = if (selected) 1f else 0.6f,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ---------------------------------------------------------------------------
// 3) FONDO: masa de arcilla con manchas suaves
// ---------------------------------------------------------------------------

@Composable
fun ClayBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val dark = colorScheme.background.luminance() < 0.5f
    Box(
        modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .drawWithContent {
                drawContent()
                val r = size.width * 0.85f
                if (dark) {
                    val a = Offset(size.width * 0.88f, size.height * 0.1f)
                    drawCircle(Brush.radialGradient(listOf(Color(0x146A52C8), Color.Transparent), a, r), r, a)
                    val b = Offset(size.width * 0.08f, size.height * 0.88f)
                    drawCircle(Brush.radialGradient(listOf(Color(0x0D9E5C74), Color.Transparent), b, r), r, b)
                } else {
                    val a = Offset(size.width * 0.9f, size.height * 0.06f)
                    drawCircle(Brush.radialGradient(listOf(Color(0x14D9CBFF), Color.Transparent), a, r), r, a)
                    val b = Offset(size.width * 0.02f, size.height * 0.9f)
                    drawCircle(Brush.radialGradient(listOf(Color(0x11FFD9E4), Color.Transparent), b, r), r, b)
                }
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// 4) SLIDER CLAY: surco hundido + píldora elevada + bola de arcilla
// ---------------------------------------------------------------------------

/** Superficie hundida: sombra arriba, luz abajo (efecto ranura). */
fun Modifier.groove(shape: Shape = ClayPillShape, color: Color): Modifier = this
    .clip(shape)
    .background(color)
    .drawBehind {
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color.Black.copy(alpha = 0.22f),
                    Color.Transparent,
                    // 0.18 y no 0.32: la ranura debe leerse hundida, no brillante.
                    Color.White.copy(alpha = 0.18f),
                ),
            ),
        )
    }
    .border(1.5.dp, Color.Black.copy(alpha = 0.14f), shape)

@Composable
private fun ClayThumb(interactionSource: MutableInteractionSource, enabled: Boolean) {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 1.18f else 1f, tween(150), label = "clayThumb")
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(30.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clay(
                CircleShape,
                if (enabled) colors.tertiary else colors.surfaceVariant,
                elevation = if (pressed) 2.dp else 6.dp,
                softness = if (pressed) 1.3f else 1f,
            ),
    )
}

@Composable
private fun ClayTrack(state: SliderState, enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    val range = state.valueRange.endInclusive - state.valueRange.start
    val frac = if (range > 0f) {
        ((state.value - state.valueRange.start) / range).coerceIn(0f, 1f)
    } else {
        0f
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp)
            .height(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .groove(
                    ClayPillShape,
                    if (enabled) colors.surfaceVariant else colors.surfaceVariant.copy(alpha = 0.5f),
                ),
        )
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(frac)
                .clay(
                    ClayPillShape,
                    if (enabled) colors.primary else colors.primary.copy(alpha = 0.4f),
                    elevation = 4.dp,
                    softness = 0.9f,
                ),
        )
    }
}

@Composable
fun ClaySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        thumb = { ClayThumb(interactionSource, enabled) },
        track = { state -> ClayTrack(state, enabled) },
    )
}

// ---------------------------------------------------------------------------
// 5) COLORES DE CONTROLES M3 TEÑIDOS DE CLAY
// ---------------------------------------------------------------------------

@Composable
fun claySwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
)

// ---------------------------------------------------------------------------
// 6) VISTA PREVIA
// ---------------------------------------------------------------------------

@Preview(name = "Arcilla · Claro", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ClayPreviewLight() = ReposoTheme(mode = ReposoThemeMode.LIGHT) {
    ClayPreviewContent()
}

@Preview(name = "Arcilla · Oscuro", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ClayPreviewDark() = ReposoTheme(mode = ReposoThemeMode.DARK) {
    ClayPreviewContent()
}

@Composable
private fun ClayPreviewContent() {
    ClayBackground {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ClayCard(Modifier.fillMaxWidth()) {
                Text("Ruido marrón profundo", style = MaterialTheme.typography.titleMedium)
            }
            ClayCard(Modifier.fillMaxWidth()) {
                Text(
                    "7 h 32 min",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                var sliderValue by remember { mutableFloatStateOf(0.6f) }
                ClaySlider(sliderValue, { sliderValue = it }, Modifier.fillMaxWidth())
            }
            ClayButton("Empezar sesión", onClick = {})
            ClayOutlineButton("Personalizar", onClick = {})
        }
    }
}
