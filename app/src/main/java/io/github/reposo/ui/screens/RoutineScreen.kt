package io.github.reposo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import io.github.reposo.ui.LocalBottomBarInset
import androidx.compose.ui.unit.dp
import io.github.reposo.player.Preset
import io.github.reposo.player.Presets
import io.github.reposo.ui.stretchOverscroll
import io.github.reposo.ui.theme.ClayBackground
import io.github.reposo.ui.theme.ClayCard
import io.github.reposo.ui.theme.claySwitchColors
import io.github.reposo.ui.theme.ReposoTheme
import io.github.reposo.ui.theme.ReposoThemeMode

@Composable
fun RoutineScreen(onPlayPreset: (Preset) -> Unit) {
    var monitor by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.stretchOverscroll(),
            // El último elemento puede subir por encima de la barra flotante.
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp + LocalBottomBarInset.current,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { Text("Tu rutina", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) }

            item {
                ClayCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                    TimeRow(Icons.Rounded.Bedtime, "Recordatorio de dormir", "22:30")
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    TimeRow(Icons.Rounded.Alarm, "Despertador", "06:30")
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ListItem(
                        headlineContent = { Text("Monitor de sueño", color = MaterialTheme.colorScheme.onSurface) },
                        supportingContent = { Text("Se activa al iniciar la rutina", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Rounded.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = {
                            Switch(
                                checked = monitor,
                                onCheckedChange = { monitor = it },
                                colors = claySwitchColors()
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }

            item {
                Section("Afloja la tensión") {
                    Text(
                        "Aún no hay meditaciones. Esta sección usará audios guiados que se añadirán más adelante.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Section("Sueño") {
                    Presets.all.forEach { p ->
                        ClayCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                            PresetRow(p, onClick = { onPlayPreset(p) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeRow(icon: ImageVector, title: String, time: String) {
    ListItem(
        headlineContent = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Text(time, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Preview(name = "Rutina · Claro", showBackground = true, backgroundColor = 0xFFF6F1FB)
@Composable
private fun RoutineLightPreview() = ReposoTheme(mode = ReposoThemeMode.LIGHT) {
    ClayBackground { RoutineScreen(onPlayPreset = {}) }
}

@Preview(name = "Rutina · Oscuro", showBackground = true, backgroundColor = 0xFF12101C)
@Composable
private fun RoutineDarkPreview() = ReposoTheme(mode = ReposoThemeMode.DARK) {
    ClayBackground { RoutineScreen(onPlayPreset = {}) }
}
