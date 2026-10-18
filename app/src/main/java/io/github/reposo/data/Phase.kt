package io.github.reposo.data

import androidx.compose.ui.graphics.Color

/** El orden del enum es el orden de las filas en el gráfico de fases. */
enum class Phase(val label: String, val color: Color) {
    Awake("Despierto", Color(0xFFFFB74D)),
    Rem("REM", Color(0xFF4FC3F7)),
    Light("Ligero", Color(0xFF7986CB)),
    Deep("Profundo", Color(0xFF3949AB)),
}
