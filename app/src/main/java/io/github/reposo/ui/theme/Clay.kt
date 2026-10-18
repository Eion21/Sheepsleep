@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.reposo.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// 1) SUPERFICIE DE ARCILLA: sombra exterior + luz interior + borde grueso
// ---------------------------------------------------------------------------

val ClayShape = RoundedCornerShape(26.dp)
val ClayBigShape = RoundedCornerShape(36.dp)
val ClayPillShape = RoundedCornerShape(50)

/**
 * Colores de la iluminación. Se eligen cálidos en vez de blanco puro y negro puro:
 * la luz de una habitación no es neutra, y el gris neutro es lo que hace que una
 * superficie parezca plástico en lugar de masa.
 */
private val ClayHighlight = Color(0xFFFFF4E8)
private val ClayShade = Color(0xFF120C1E)

/**
 * Iluminación de plastilina.
 *
 * Tres capas, en este orden:
 * 1. Luz cálida desde arriba que se apaga hacia la mitad (la masa se ve iluminada).
 * 2. Sombra interior en el borde inferior: la pieza está apoyada, no flotando.
 * 3. Grano muy tenue: la plastilina no es un plástico liso, y sin textura el ojo la
 *    lee como un rectángulo de color.
 *
 * El blanco se mantiene bajo a propósito: en tema oscuro un velo fuerte lava la
 * tarjeta y la vuelve grisácea en vez de darle volumen.
 */
private fun DrawScope.clayLighting(softness: Float = 0.5f) {
    // 1) Luz superior en banda fija (36 dp), no proporcional a la altura: en una tarjeta
    //    alta, un degradado relativo iluminaba media tarjeta y dejaba los títulos sin
    //    contraste. La luz de una pieza de masa vive cerca del borde.
    val band = 36.dp.toPx().coerceAtMost(size.height * 0.5f)
    drawRect(
        Brush.verticalGradient(
            0f to ClayHighlight.copy(alpha = 0.22f * softness),
            1f to Color.Transparent,
            startY = 0f,
            endY = band,
        ),
    )

    // 2) Filo superior brillante: el reflejo que separa la masa del fondo.
    val rim = 2.dp.toPx()
    drawRect(
        Brush.verticalGradient(
            0f to ClayHighlight.copy(alpha = 0.42f * softness),
            1f to Color.Transparent,
            startY = 0f,
            endY = rim * 2.4f,
        ),
    )

    // 3) Sombra interior inferior: el canto de la masa, la parte que no recibe luz.
    val lip = (size.height * 0.12f).coerceIn(rim * 2f, 26.dp.toPx())
    drawRect(
        Brush.verticalGradient(
            0f to Color.Transparent,
            1f to ClayShade.copy(alpha = 0.34f * softness),
            startY = size.height - lip,
            endY = size.height,
        ),
    )

    // 4) Reflejo lateral tenue, el que hace que la pieza parezca redonda y no un plano.
    drawRect(
        Brush.horizontalGradient(
            0f to ClayHighlight.copy(alpha = 0.07f * softness),
            0.18f to Color.Transparent,
            0.82f to Color.Transparent,
            1f to ClayShade.copy(alpha = 0.13f * softness),
        ),
    )
}

/**
 * Volumen exterior: una masa apilada proyecta una sombra corta y densa, no un halo
 * difuso. `ambient` apenas se usa; casi todo el peso va en `spot`, más abajo.
 */
private fun Modifier.clayDepth(elevation: Dp, shape: Shape): Modifier =
    shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.55f),
        spotColor = Color.Black.copy(alpha = 0.75f),
    )

/**
 * Modificador base clay: borde redondeado, fondo plano e iluminación de masa.
 *
 * El degradado se dibuja bajo el contenido para no teñir el texto.
 */
fun Modifier.clay(
    shape: Shape = ClayShape,
    color: Color,
    elevation: Dp = 6.dp,
    softness: Float = 1f,
    grain: Float = 1f,
): Modifier = this
    .clayDepth(elevation, shape)
    .clip(shape)
    .background(color)
    .drawWithContent {
        clayLighting(softness)
        drawContent()
        // El grano va por encima pero con alfa muy bajo: se percibe como textura, no
        // como suciedad, y no toca el color del texto.
        clayGrain(grain)
    }

/**
 * Textura de plastilina.
 *
 * Un único rectángulo relleno con un `BitmapShader` tileado: la GPU repite un patrón de
 * 64×64 px. Dibujar la retícula punto a punto costaría miles de llamadas por fotograma
 * y arrastraría los fps de las listas.
 */
private class GrainTexture(density: Float) {
    val shader: android.graphics.BitmapShader

    init {
        // Ruido por píxel, no manchas. Las versiones anteriores dibujaban círculos
        // grandes y el ojo los leía como burbujas o como ruido de televisor. La textura
        // de plastilina es microscópica: a tamaño real solo quita el aspecto de plástico
        // liso y hace que la superficie parezca mate.
        val tile = (96 * density).toInt().coerceAtLeast(32)
        val pixels = IntArray(tile * tile)

        var seed = 20260118
        fun next(): Int {
            seed = seed * 1103515245 + 12345
            return (seed ushr 16) and 0xFF
        }

        // Cada píxel oscila alrededor del gris medio con muy poco margen, y se aplica
        // con alfa bajo: un grano de baja amplitud que no se distingue punto a punto.
        for (i in pixels.indices) {
            val v = next()
            pixels[i] = if (v > 128) {
                android.graphics.Color.argb(10, 255, 248, 240)
            } else {
                android.graphics.Color.argb(10, 14, 9, 24)
            }
        }

        val bitmap = android.graphics.Bitmap.createBitmap(
            tile, tile, android.graphics.Bitmap.Config.ARGB_8888,
        )
        bitmap.setPixels(pixels, 0, tile, 0, 0, tile, tile)

        shader = android.graphics.BitmapShader(
            bitmap,
            android.graphics.Shader.TileMode.REPEAT,
            android.graphics.Shader.TileMode.REPEAT,
        )
    }
}

private val grainCache = mutableMapOf<Int, GrainTexture>()

private fun DrawScope.clayGrain(strength: Float) {
    if (strength <= 0f) return
    val densityKey = density.toInt().coerceAtLeast(1)
    val texture = grainCache.getOrPut(densityKey) { GrainTexture(density) }
    // Muy bajo: la textura se intuye, no se ve. Si se nota el patrón, deja de ser masa.
    val alpha = (0.85f * strength).coerceIn(0f, 1f)

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            shader = texture.shader
            this.alpha = (alpha * 255).toInt()
        }
        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
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
    // Al pulsar la masa se aplasta: baja el volumen (elevación), se comprime en vertical
    // y se ensancha un poco. Antes solo cambiaba la sombra y no se notaba el toque.
    val press by animateFloatAsState(if (pressed) 0f else 1f, tween(180), label = "clayPress")
    val color = if (enabled) containerColor else containerColor.copy(alpha = 0.38f)
    val contentAlpha = if (enabled) 1f else 0.5f

    // Row y no Box: en un Box el icono guía, el texto y el icono final se apilan
    // en el mismo centro y quedan superpuestos (el icono se veía encima de las letras).
    Row(
        modifier
            .graphicsLayer {
                scaleX = 0.985f + 0.018f * press
                scaleY = 0.93f + 0.07f * press
                translationY = 3.dp.toPx() * (1f - press)
            }
            .clay(
                shape,
                color,
                elevation = (1.dp + 8.dp * press),
                softness = 0.55f + 0.55f * press,
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
// 3) FONDO: mesa de trabajo con luz central y viñeta
// ---------------------------------------------------------------------------

/**
 * Fondo de la app.
 *
 * Un color plano hace que las piezas parezcan pegatinas. Aquí hay una luz tenue que
 * viene de arriba a la izquierda y una viñeta en los bordes: las tarjetas se leen como
 * masas apoyadas sobre una superficie, no flotando en el vacío.
 */
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
                    // Luz de lámpara: cálida y muy baja, arriba a la izquierda.
                    val lamp = Offset(size.width * 0.22f, -size.height * 0.08f)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0x1FFFF0DC), Color.Transparent), lamp, r * 1.3f,
                        ),
                        r * 1.3f, lamp,
                    )
                    // Manchas de color lejanas: evitan que el fondo sea un negro uniforme.
                    val a = Offset(size.width * 0.92f, size.height * 0.18f)
                    drawCircle(Brush.radialGradient(listOf(Color(0x1A6A52C8), Color.Transparent), a, r), r, a)
                    val b = Offset(size.width * 0.05f, size.height * 0.92f)
                    drawCircle(Brush.radialGradient(listOf(Color(0x149E5C74), Color.Transparent), b, r), r, b)
                    // Viñeta: hunde las esquinas y concentra la mirada en el centro.
                    drawRect(
                        Brush.radialGradient(
                            0.45f to Color.Transparent,
                            1f to Color(0x66000000),
                            center = Offset(size.width / 2f, size.height * 0.42f),
                            radius = size.maxDimension * 0.78f,
                        ),
                    )
                } else {
                    val lamp = Offset(size.width * 0.25f, -size.height * 0.05f)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0x33FFFFFF), Color.Transparent), lamp, r * 1.2f,
                        ),
                        r * 1.2f, lamp,
                    )
                    val a = Offset(size.width * 0.9f, size.height * 0.06f)
                    drawCircle(Brush.radialGradient(listOf(Color(0x14D9CBFF), Color.Transparent), a, r), r, a)
                    val b = Offset(size.width * 0.02f, size.height * 0.9f)
                    drawCircle(Brush.radialGradient(listOf(Color(0x11FFD9E4), Color.Transparent), b, r), r, b)
                    drawRect(
                        Brush.radialGradient(
                            0.5f to Color.Transparent,
                            1f to Color(0x14000000),
                            center = Offset(size.width / 2f, size.height * 0.42f),
                            radius = size.maxDimension * 0.8f,
                        ),
                    )
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

/**
 * Indica si el usuario tiene el dedo sobre el control.
 *
 * `collectIsPressedAsState()` no sirve aquí: el `Slider` de Material3 emite
 * `DragInteraction` al arrastrar, no `PressInteraction`. Escuchar solo el press dejaba
 * el efecto de hundimiento muerto justo en el gesto que importa.
 */
@Composable
private fun rememberIsTouching(interactionSource: MutableInteractionSource): Boolean {
    var touching by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press,
                is DragInteraction.Start -> touching = true

                is PressInteraction.Release,
                is PressInteraction.Cancel,
                is DragInteraction.Stop,
                is DragInteraction.Cancel -> touching = false
            }
        }
    }
    return touching
}

/**
 * Superficie hundida: sombra arriba, luz abajo.
 *
 * Es el inverso del relieve de las piezas: aquí la luz entra *dentro* del surco, así
 * que el borde superior queda oscuro y el inferior recibe el reflejo. Sin esa inversión
 * el carril se lee como una barra pintada y no como un canal.
 */
fun Modifier.groove(shape: Shape = ClayPillShape, color: Color): Modifier = this
    .clip(shape)
    .background(color)
    .drawBehind {
        drawRect(
            Brush.verticalGradient(
                0f to ClayShade.copy(alpha = 0.55f),
                0.35f to Color.Transparent,
                1f to ClayHighlight.copy(alpha = 0.38f),
            ),
        )
    }
    .border(1.dp, ClayShade.copy(alpha = 0.28f), shape)

/**
 * Variante del surco con profundidad variable.
 *
 * [depth] va de 0 (al ras) a 1 (pozo). Al arrastrar el pulgar el carril se hunde un
 * poco más: la luz superior se densa y el labio inferior se ilumina, como cuando
 * presionas plastilina con el dedo. Es lo que separa un slider pintado de uno que
 * parece tener volumen.
 */
private fun Modifier.groove(
    shape: Shape,
    color: Color,
    depth: Float,
): Modifier = this
    .clip(shape)
    .background(color)
    .drawBehind {
        val d = depth.coerceIn(0f, 1f)
        // Pozo interior: oscurece desde el borde superior hacia el centro.
        drawRect(
            Brush.verticalGradient(
                0f to ClayShade.copy(alpha = 0.45f + 0.30f * d),
                0.30f to ClayShade.copy(alpha = 0.10f + 0.14f * d),
                0.72f to Color.Transparent,
                1f to ClayHighlight.copy(alpha = 0.30f + 0.34f * d),
            ),
        )
        // Labio inferior: el canto de masa que queda por delante del canal.
        if (d > 0.01f) {
            val lip = (size.height * 0.34f) * d
            drawRect(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to ClayHighlight.copy(alpha = 0.42f * d),
                    startY = size.height - lip,
                    endY = size.height,
                ),
            )
        }
    }
    .border(1.dp, ClayShade.copy(alpha = 0.28f + 0.22f * depth), shape)

/**
 * Pulgar de arcilla.
 *
 * Al pulsarlo se aplasta contra el carril —baja, se ensancha y pierde altura— en vez
 * de solo crecer. Es el gesto de presionar una bola de masa, y es lo que da la
 * sensación de que hay algo blando debajo.
 */
@Composable
private fun ClayThumb(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    pressed: Boolean,
    progress: Float,
) {
    val colors = MaterialTheme.colorScheme
    // Muelle corto: el aplastado debe sentirse inmediato, no flotar.
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "thumbPress",
    )
    val lift by animateFloatAsState(
        targetValue = if (pressed) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "thumbLift",
    )

    Box(
        Modifier
            .size(30.dp)
            .graphicsLayer {
                // Se ensancha al aplastarse: misma masa, más base.
                scaleX = 1f + 0.16f * press
                scaleY = 1f - 0.10f * press
                translationY = 1.5.dp.toPx() * press
            }
            .clay(
                CircleShape,
                if (enabled) colors.tertiary else colors.surfaceVariant,
                // La elevación cae al presionar: la bola se apoya en el canal.
                elevation = (2.dp + 5.dp * lift),
                softness = 0.9f + 0.5f * press,
            ),
    )
}

@Composable
private fun ClayTrack(state: SliderState, enabled: Boolean, pressing: Boolean) {
    val colors = MaterialTheme.colorScheme
    val range = state.valueRange.endInclusive - state.valueRange.start
    val frac = if (range > 0f) {
        ((state.value - state.valueRange.start) / range).coerceIn(0f, 1f)
    } else {
        0f
    }
    // El surco se hunde al arrastrar y vuelve a su sitio al soltar.
    val depth by animateFloatAsState(
        targetValue = if (pressing) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "grooveDepth",
    )

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
                    depth,
                ),
        )
        // Relleno: al presionar se hunde también, así que pierde relieve en vez de
        // quedar como una pegatina encima del canal.
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(frac)
                .clay(
                    ClayPillShape,
                    if (enabled) colors.primary else colors.primary.copy(alpha = 0.4f),
                    elevation = (1.dp + 3.dp * (1f - depth)),
                    softness = 0.75f + 0.2f * depth,
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
    // Press + Drag: el slider emite arrastre, no pulsación.
    val pressed = rememberIsTouching(interactionSource)
    val range = valueRange.endInclusive - valueRange.start
    val progress = if (range > 0f) ((value - valueRange.start) / range).coerceIn(0f, 1f) else 0f

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        thumb = { ClayThumb(interactionSource, enabled, pressed, progress) },
        track = { state -> ClayTrack(state, enabled, pressed) },
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
