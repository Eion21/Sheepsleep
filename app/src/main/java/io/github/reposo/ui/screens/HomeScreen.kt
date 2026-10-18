package io.github.reposo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.reposo.ui.LocalBottomBarInset
import io.github.reposo.ui.stretchOverscroll
import io.github.reposo.R
import io.github.reposo.player.PlayerState
import io.github.reposo.player.Preset
import io.github.reposo.player.Presets
import io.github.reposo.player.SavedMix
import io.github.reposo.ui.components.AnimatedSheepCounterCard
import io.github.reposo.ui.theme.ClayBackground
import io.github.reposo.ui.theme.ClayButton
import io.github.reposo.ui.theme.ClayCard
import io.github.reposo.ui.theme.ClayChip
import io.github.reposo.ui.theme.clay
import io.github.reposo.ui.theme.ReposoTheme
import io.github.reposo.ui.theme.ReposoThemeMode

data class SleepSound(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val preset: Preset? = null
)

@Composable
fun HomeScreen(
    lastMix: SavedMix?,
    playerState: PlayerState,
    onResume: () -> Unit,
    onTogglePlay: () -> Unit,
    onPlayPreset: (Preset) -> Unit,
    onCreateMix: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var selectedGoal by remember { mutableStateOf(Presets.goals.first()) }

    val defaultSounds = listOf(
        SleepSound("Marrón profundo", "Ruido marrón", Icons.Rounded.GraphicEq, Presets.all.firstOrNull { idMatches(it.id, "brown") }),
        SleepSound("Marrón + rosa", "2 sonidos", Icons.Rounded.Water, Presets.all.firstOrNull { idMatches(it.id, "rain") }),
        SleepSound("Pradera nocturna", "Ovejas + naturaleza", Icons.Rounded.Forest, Presets.all.firstOrNull { idMatches(it.id, "forest") })
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.primary.copy(alpha = 0.04f), Color.Transparent),
                    )
                )
                .stretchOverscroll()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // HEADER
            Row(verticalAlignment = Alignment.CenterVertically) {
                // El logo se apoya en una pastilla de masa: el recorte de la oveja solo
                // flotando sobre el fondo se leía como una pegatina.
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clay(
                            RoundedCornerShape(22.dp),
                            colors.primaryContainer,
                            elevation = 7.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_sheep_logo),
                        contentDescription = null,
                        modifier = Modifier.size(46.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Buenas noches",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Descansa, mañana será un gran día ✦",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CONTINUAR
            if (lastMix != null) {
                Text(
                    text = "Continuar",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val isPlaying = playerState.isPlaying && (playerState.title == lastMix.title)
                ContinueCard(
                    lastMix = lastMix,
                    isPlaying = isPlaying,
                    onPlay = {
                        if (playerState.title == lastMix.title) {
                            onTogglePlay()
                        } else {
                            onResume()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // OBJETIVO DE SUEÑO
            Text(
                text = "Tu objetivo de sueño",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Presets.goals) { goal ->
                    ClayChip(
                        text = goal,
                        selected = selectedGoal == goal,
                        onClick = { selectedGoal = goal },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // OVEJITAS (Animated Counter Card)
            AnimatedSheepCounterCard(
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // RECOMENDADOS SEGÚN OBJETIVO
            Text(
                text = "Recomendado: $selectedGoal",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(defaultSounds) { sound ->
                    SoundCard(
                        sound = sound,
                        onClick = {
                            sound.preset?.let { onPlayPreset(it) }
                                ?: Presets.all.firstOrNull()?.let { onPlayPreset(it) }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CREAR MEZCLA
            ClayButton(
                onClick = onCreateMix,
                modifier = Modifier.fillMaxWidth(),
                text = "Crear tu propia mezcla",
                leading = { Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailing = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp)) },
            )

            // Aire suficiente para que el botón suba por encima de la barra flotante al
            // llegar al final. Sin esto queda medio tapado y parece cortado.
            Spacer(modifier = Modifier.height(LocalBottomBarInset.current + 12.dp))
        }
    }
}

private fun idMatches(id1: String, id2: String) = id1.equals(id2, ignoreCase = true)

@Composable
fun ContinueCard(
    lastMix: SavedMix?,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    ClayCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    lastMix?.title ?: "Mezcla guardada",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${lastMix?.layers?.size ?: 1} sonido(s)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }

            IconButton(
                onClick = onPlay
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
fun SoundCard(
    sound: SleepSound,
    onClick: () -> Unit
) {
    ClayCard(
        modifier = Modifier
            .width(190.dp)
            .height(150.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = sound.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = sound.name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = sound.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

@Preview(name = "Inicio · Claro", showBackground = true, backgroundColor = 0xFFF6F1FB)
@Composable
private fun HomeLightPreview() = ReposoTheme(mode = ReposoThemeMode.LIGHT) {
    HomePreviewContent()
}

@Preview(name = "Inicio · Oscuro", showBackground = true, backgroundColor = 0xFF12101C)
@Composable
private fun HomeDarkPreview() = ReposoTheme(mode = ReposoThemeMode.DARK) {
    HomePreviewContent()
}

@Composable
private fun HomePreviewContent() {
    ClayBackground {
        HomeScreen(
        lastMix = SavedMix("Mi mezcla", mapOf("brown" to 0.6f)),
        playerState = PlayerState(),
        onResume = {},
        onTogglePlay = {},
            onPlayPreset = {},
            onCreateMix = {}
        )
    }
}
