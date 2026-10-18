package io.github.reposo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import io.github.reposo.ui.LocalBottomBarInset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import io.github.reposo.data.SnoreClip
import io.github.reposo.ui.stretchOverscroll
import io.github.reposo.ui.theme.ClayBackground
import io.github.reposo.ui.theme.ClayCard
import io.github.reposo.ui.theme.ReposoTheme
import io.github.reposo.ui.theme.ReposoThemeMode
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RonquidosScreen(
    clips: List<SnoreClip>,
    onBack: () -> Unit,
    onDelete: (SnoreClip) -> Unit,
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var playingId by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    fun toggle(clip: SnoreClip) {
        if (playingId == clip.id) {
            player.pause()
            playingId = null
            return
        }
        val file = File(context.filesDir, "snore/${clip.file}")
        if (!file.exists()) return
        player.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
        player.prepare()
        player.play()
        playingId = clip.id
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .stretchOverscroll(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                "Ronquidos",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }

        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Aún no hay grabaciones",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Activa «Grabar ronquidos» en Dormir y vuelve aquí.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            LazyColumn(
                // El último elemento puede subir por encima de la barra flotante.
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + LocalBottomBarInset.current,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(clips, key = { it.id }) { clip ->
                    SnoreClipRow(
                        clip = clip,
                        playing = playingId == clip.id,
                        onToggle = { toggle(clip) },
                        onDelete = {
                            if (playingId == clip.id) {
                                player.pause()
                                playingId = null
                            }
                            onDelete(clip)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SnoreClipRow(
    clip: SnoreClip,
    playing: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    // popIn = false: es una fila de lista, la deformación se repetiría en cada recarga.
    ClayCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp, popIn = false) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = CLIP_TIME.format(Instant.ofEpochMilli(clip.startMs)),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Text(
                    text = "%02d:%02d · pico %d dB".format(
                        clip.durationMs / 60_000,
                        (clip.durationMs % 60_000) / 1_000,
                        clip.peakDb,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Borrar",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private val CLIP_TIME: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM · HH:mm").withZone(ZoneId.systemDefault())

@Preview(name = "Ronquidos · Claro", showBackground = true, backgroundColor = 0xFFF6F1FB)
@Composable
private fun SnoreLightPreview() = ReposoTheme(mode = ReposoThemeMode.LIGHT) {
    RonquidosScreen(
        clips = listOf(
            SnoreClip(id = 1, startMs = System.currentTimeMillis(), durationMs = 12_400, file = "clip_1.wav", peakDb = -14),
            SnoreClip(id = 2, startMs = System.currentTimeMillis() - 3_600_000, durationMs = 7_100, file = "clip_2.wav", peakDb = -21),
        ),
        onBack = {},
        onDelete = {},
    )
}

@Preview(name = "Ronquidos · Oscuro", showBackground = true, backgroundColor = 0xFF12101C)
@Composable
private fun SnoreDarkPreview() = ReposoTheme(mode = ReposoThemeMode.DARK) {
    RonquidosScreen(clips = emptyList(), onBack = {}, onDelete = {})
}
