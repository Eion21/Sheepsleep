package io.github.reposo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.reposo.sounds.DownloadState
import io.github.reposo.sounds.SoundsViewModel
import io.github.reposo.ui.LocalBottomBarInset

@Composable
fun SoundsScreen(onBack: () -> Unit, vm: SoundsViewModel = viewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp, start = 16.dp, end = 16.dp),
        // El último sonido sube por encima de la barra flotante.
        contentPadding = PaddingValues(bottom = 16.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver") }
                Text("Biblioteca de sonidos", style = MaterialTheme.typography.headlineSmall)
            }
        }
        if (loading && items.isEmpty()) {
            item { CircularProgressIndicator(Modifier.padding(16.dp)) }
        }
        if (offline) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Sin conexión con el catálogo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = vm::refresh) { Text("Reintentar") }
                }
            }
        }
        items(items, key = { it.sound.id }) { item ->
            val s = item.sound
            ListItem(
                headlineContent = { Text(s.title) },
                // La autoría y la licencia se muestran siempre: es la atribución que piden las licencias CC-BY.
                supportingContent = {
                    Text("${s.category} · ${"%.1f".format(s.sizeBytes / 1_000_000.0)} MB · ${s.license} · ${s.author}")
                },
                trailingContent = {
                    when (item.state) {
                        DownloadState.Remote -> IconButton(onClick = { vm.download(s) }) {
                            Icon(Icons.Rounded.Download, contentDescription = "Descargar")
                        }
                        DownloadState.Downloading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        DownloadState.Ready -> IconButton(onClick = { vm.delete(s) }) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Descargado. Toca para eliminar")
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}
