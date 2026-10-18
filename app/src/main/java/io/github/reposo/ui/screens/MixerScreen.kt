package io.github.reposo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.reposo.player.SoundCatalog
import io.github.reposo.sounds.SoundLibrary
import io.github.reposo.ui.LocalBottomBarInset
import io.github.reposo.ui.stretchOverscroll
import io.github.reposo.ui.theme.ClayBackground
import io.github.reposo.ui.theme.ClayButton
import io.github.reposo.ui.theme.ClayCard
import io.github.reposo.ui.theme.ClaySlider
import io.github.reposo.ui.theme.ReposoTheme
import io.github.reposo.ui.theme.ReposoThemeMode

@Composable
fun MixerScreen(
    onBack: () -> Unit,
    onPlay: (Map<String, Float>) -> Unit,
    onVolume: (String, Float) -> Unit,
    onSave: (String, Map<String, Float>) -> Unit = { _, _ -> },
    onMoreSounds: () -> Unit = {},
) {
    val context = LocalContext.current
    val sounds = remember { mutableStateListOf(*SoundLibrary.available(context).toTypedArray()) }
    val volumes = remember { mutableStateMapOf<String, Float>() }
    val active = volumes.filterValues { it > 0.01f }

    var showSaveDialog by remember { mutableStateOf(false) }
    var mixName by remember { mutableStateOf("Mi mezcla") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val name = it.lastPathSegment?.substringAfterLast('/') ?: "Sonido propio"
            SoundCatalog.addCustomSound(context, name, it)
            sounds.clear()
            sounds.addAll(SoundLibrary.available(context))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                // Los botones de acción suben por encima de la barra flotante.
                .padding(bottom = 16.dp + LocalBottomBarInset.current),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground) }
                Text("Crear tu propia mezcla", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            }

            ClayButton(
                text = "Añadir sonido propio",
                onClick = { launcher.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                leading = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
            )

            if (sounds.isEmpty()) {
                Text(
                    "No hay sonidos disponibles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(Modifier.weight(1f).stretchOverscroll()) {
                items(sounds, key = { it.id }) { s ->
                    val v = volumes[s.id] ?: 0f
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = 16.dp,
                    ) {
                        Column {
                            Row {
                                Text(s.label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                                Text("${(v * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            ClaySlider(
                                value = v,
                                onValueChange = { volumes[s.id] = it; onVolume(s.id, it) },
                            )
                        }
                    }
                }
            }
            TextButton(onClick = onMoreSounds) {
                Text("Descargar más sonidos")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClayButton(
                    text = "Guardar mezcla",
                    onClick = { showSaveDialog = true },
                    enabled = active.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                ClayButton(
                    text = "Reproducir mezcla",
                    onClick = { onPlay(active) },
                    enabled = active.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar mezcla") },
            text = {
                OutlinedTextField(
                    value = mixName,
                    onValueChange = { mixName = it },
                    singleLine = true,
                    label = { Text("Nombre") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = mixName.ifBlank { "Mi mezcla" }
                    onSave(name, active.toMap())
                    showSaveDialog = false
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Preview(name = "Mezclador · Claro", showBackground = true, backgroundColor = 0xFFF6F1FB)
@Composable
private fun MixerLightPreview() = ReposoTheme(mode = ReposoThemeMode.LIGHT) {
    MixerPreviewContent()
}

@Preview(name = "Mezclador · Oscuro", showBackground = true, backgroundColor = 0xFF12101C)
@Composable
private fun MixerDarkPreview() = ReposoTheme(mode = ReposoThemeMode.DARK) {
    MixerPreviewContent()
}

@Composable
private fun MixerPreviewContent() {
    ClayBackground { MixerScreen(onBack = {}, onPlay = {}, onVolume = { _, _ -> }, onMoreSounds = {}) }
}
