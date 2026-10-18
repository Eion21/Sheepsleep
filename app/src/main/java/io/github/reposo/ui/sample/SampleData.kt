package io.github.reposo.ui.sample

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** El orden del enum es el orden de las filas en el gráfico de fases. */
enum class Phase(val label: String, val color: Color) {
    Awake("Despierto", Color(0xFFFFB74D)),
    Rem("REM", Color(0xFF4FC3F7)),
    Light("Ligero", Color(0xFF7986CB)),
    Deep("Profundo", Color(0xFF3949AB)),
}

enum class RecordingType(val label: String, val icon: ImageVector) {
    Talk("Conversación", Icons.Rounded.RecordVoiceOver),
    Snore("Ronquido", Icons.Rounded.Air),
}

data class Recording(val time: String, val type: RecordingType, val seconds: Int)
data class Track(val title: String, val subtitle: String, val goal: String)

/** Datos falsos solo para prototipar la UI. Se reemplazan por Room más adelante. */
object SampleData {
    const val STEP_MIN = 20

    val goals = listOf("Duerme más rápido", "Duerme mejor", "Relájate")

    val tracks = listOf(
        Track("Lluvia suave", "Mezcla · 45 min", "Duerme más rápido"),
        Track("Escaneo corporal", "Meditación · 12 min", "Duerme más rápido"),
        Track("Olas nocturnas", "Mezcla · 60 min", "Duerme mejor"),
        Track("Respiración 4-7-8", "Meditación · 8 min", "Duerme mejor"),
        Track("Bosque en calma", "Mezcla · 30 min", "Relájate"),
        Track("Liberar tensión", "Meditación · 15 min", "Relájate"),
    )
    val meditations = tracks.filter { it.subtitle.startsWith("Meditación") }
    val mixes = tracks.filter { it.subtitle.startsWith("Mezcla") }

    val saved = listOf("Mi mezcla 1", "Lluvia + fogata", "Siesta rápida")

    /** Una noche: 19 muestras de 20 min = 6h 20min en cama, 5h 40min dormido. */
    val night = listOf(
        Phase.Light, Phase.Light, Phase.Deep, Phase.Deep, Phase.Light,
        Phase.Rem, Phase.Light, Phase.Deep, Phase.Light, Phase.Rem,
        Phase.Awake, Phase.Light, Phase.Deep, Phase.Light, Phase.Rem,
        Phase.Light, Phase.Rem, Phase.Light, Phase.Awake,
    )

    val recordings = listOf(
        Recording("01:42", RecordingType.Snore, 38),
        Recording("02:15", RecordingType.Talk, 12),
        Recording("03:58", RecordingType.Snore, 54),
        Recording("05:07", RecordingType.Talk, 9),
    )
}
