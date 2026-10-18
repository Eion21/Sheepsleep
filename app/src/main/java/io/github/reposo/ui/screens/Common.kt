package io.github.reposo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.reposo.player.Preset
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val PREVIEW_BG = 0xFFF6F1FB

fun formatMinutes(m: Int) = if (m >= 60) "${m / 60}h ${m % 60}min" else "$m min"

fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes >= 60 -> formatMinutes(minutes.toInt())
        minutes > 0 -> "$minutes min"
        else -> "${ms / 1000} s"
    }
}

fun formatClock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

fun formatDateTime(ms: Long): String =
    DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        content()
    }
}

@Composable
fun PresetRow(preset: Preset, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(preset.title) },
        supportingContent = { Text(preset.subtitle) },
        leadingContent = { Icon(Icons.Rounded.PlayCircle, contentDescription = "Reproducir") },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
