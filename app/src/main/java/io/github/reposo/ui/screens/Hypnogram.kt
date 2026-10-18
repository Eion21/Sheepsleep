package io.github.reposo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.reposo.data.Phase

/** Una barra por muestra, en la fila de su fase (Despierto arriba, Profundo abajo). Se usará con el monitor de sueño. */
@Composable
fun Hypnogram(samples: List<Phase>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier) {
        val rowH = size.height / Phase.entries.size
        val left = 72.dp.toPx()
        val barW = (size.width - left) / samples.size.coerceAtLeast(1)

        Phase.entries.forEachIndexed { i, p ->
            val layout = measurer.measure(p.label, TextStyle(fontSize = 11.sp, color = labelColor))
            drawText(layout, topLeft = Offset(0f, i * rowH + (rowH - layout.size.height) / 2))
        }
        samples.forEachIndexed { i, p ->
            drawRoundRect(
                color = p.color,
                topLeft = Offset(left + i * barW + 1.dp.toPx(), p.ordinal * rowH + rowH * 0.15f),
                size = Size(barW - 2.dp.toPx(), rowH * 0.7f),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
    }
}
