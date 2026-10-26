package io.github.reposo.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.github.reposo.ui.LocalBottomBarInset
import androidx.compose.ui.unit.dp
import io.github.reposo.data.SleepSession
import io.github.reposo.data.SnoreClip
import io.github.reposo.data.MovementBucket
import io.github.reposo.data.SleepAnalysis
import io.github.reposo.ui.stretchOverscroll
import io.github.reposo.ui.theme.ClayBackground
import io.github.reposo.ui.theme.ClayCard
import io.github.reposo.ui.theme.ReposoTheme
import io.github.reposo.ui.theme.ReposoThemeMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(
    sessions: List<SleepSession>,
    snoreClips: List<SnoreClip> = emptyList(),
    onOpenSnore: () -> Unit = {},
    movementBuckets: List<MovementBucket> = emptyList(),
    sleepGoalMinutes: Int = 480,
) {
    val done = sessions.filter { it.endMs != null }
    val dayClips = snoreClips.count { it.startMs >= System.currentTimeMillis() - 24L * 3600_000 }

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text("Estadísticas", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) }

            item {
                SnoreSummaryCard(
                    clipCount = dayClips,
                    lastClipMs = snoreClips.firstOrNull()?.startMs,
                    onClick = onOpenSnore,
                )
            }

            if (done.isEmpty()) {
                item {
                    InfoCard(
                        "Aún no hay datos",
                        "Cuando termines tu primera noche desde la pestaña Dormir, aparecerá aquí.",
                    )
                }
            } else {
                val last = done.first()
                item { Text("Última noche · ${formatDateTime(last.startMs)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground) }
                item {
                    // El puntaje sustituye a los "—" que había: antes las tarjetas de
                    // "Dormido" y "Calidad" eran relleno sin datos. El remember vive
                    // dentro del item: el builder del LazyColumn no es contexto composable.
                    val lastReport = remember(last, movementBuckets, sleepGoalMinutes) {
                        SleepAnalysis.analyze(last.startMs, last.endMs!!, movementBuckets, sleepGoalMinutes)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(formatDuration(last.endMs!! - last.startMs), "En la cama", Modifier.weight(1f))
                        StatCard("${lastReport.score}", "Puntaje", Modifier.weight(1f))
                        StatCard(styleLabel(lastReport.style), "Estilo", Modifier.weight(1f))
                    }
                }
                item {
                    BarChartCard(done)
                }
                item {
                    SleepHeatmapCard(done)
                }
                item { SleepPhasesCard(done, movementBuckets) }
                item { RestlessMomentsCard(done, movementBuckets) }
                item { Text("Historial", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground) }
                items(done, key = { it.id }) { s ->
                    ClayCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                        ListItem(
                            headlineContent = { Text(formatDateTime(s.startMs), color = MaterialTheme.colorScheme.onSurface) },
                            supportingContent = { Text("En la cama: ${formatDuration(s.endMs!! - s.startMs)}", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarChartCard(sessions: List<SleepSession>) {
    // El DAO devuelve las sesiones de más nueva a más vieja. Aquí se necesitan las 7
    // ÚLTIMAS (no las 7 primeras del listado) y en orden ascendente, para que el eje
    // temporal vaya de izquierda (viejo) a derecha (nuevo) como se lee un gráfico.
    val recent = sessions.take(7).reversed()
    if (recent.isEmpty()) return

    val durations = recent.map { (it.endMs ?: System.currentTimeMillis()) - it.startMs }
    val maxDuration = durations.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val avgDuration = durations.average().toLong()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val animProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "barsGrowth"
    )

    ClayCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Actividad reciente", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("Promedio diario: ${formatDuration(avgDuration)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                recent.forEach { session ->
                    val duration = (session.endMs ?: System.currentTimeMillis()) - session.startMs
                    val fraction = ((duration.toFloat() / maxDuration.toFloat()) * animProgress).coerceIn(0.02f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        // El valor se fija arriba del todo de la columna: así la barra no
                        // puede empujarlo fuera de su sitio por alta que sea.
                        Text(
                            text = formatDuration(duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        // La barra ocupa el espacio que sobra, descontando los dos textos.
                        // Antes era fillMaxHeight(fraction) sobre los 150 dp completos, así
                        // que la barra más alta desbordaba la tarjeta y tapaba el día.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(24.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            // Fecha corta, no solo el día de la semana: dos noches del
                            // mismo día mostraban ambas "Sun", que no distingue nada.
                            text = DateTimeFormatter.ofPattern("d MMM").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(session.startMs)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepHeatmapCard(sessions: List<SleepSession>) {
    val zone = ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zone)
    val sessionMap = remember(sessions) { sessions.associateBy { formatter.format(Instant.ofEpochMilli(it.startMs)) } }

    val today = Instant.now().atZone(zone).toLocalDate()
    val startDate = today.minusDays(84) // 12 weeks

    val days = remember(sessions) {
        val list = mutableListOf<Triple<String, Long, Int>>()
        var current = startDate
        while (!current.isAfter(today)) {
            val iso = current.toString()
            val session = sessionMap[iso]
            val duration = session?.let { (it.endMs!! - it.startMs) } ?: 0L
            val level = when {
                duration >= 8 * 3600_000L -> 4
                duration >= 6 * 3600_000L -> 3
                duration >= 4 * 3600_000L -> 2
                duration > 0 -> 1
                else -> 0
            }
            list.add(Triple(iso, duration, level))
            current = current.plusDays(1)
        }
        list
    }

    val totalHours = sessions.sumOf { (it.endMs!! - it.startMs) } / 3600_000L
    var selectedDayInfo by remember { mutableStateOf<String?>(null) }

    ClayCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Calendario de sueño", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("$totalHours hrs en total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val weeks = days.chunked(7)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(weeks.size) { weekIndex ->
                    val weekDays = weeks[weekIndex]
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        weekDays.forEach { (date, duration, level) ->
                            val color = when (level) {
                                4 -> MaterialTheme.colorScheme.primary
                                3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(color)
                                    .clickable {
                                        val hours = duration / 3600_000.toFloat()
                                        selectedDayInfo = if (duration > 0) "$date: %.1f horas de sueño".format(hours) else "$date: Sin registro"
                                    }
                            )
                        }
                    }
                }
            }

            selectedDayInfo?.let { info ->
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Menos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                listOf(0, 1, 2, 3, 4).forEach { lvl ->
                    val color = when (lvl) {
                        4 -> MaterialTheme.colorScheme.primary
                        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text("Más", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private const val PHASE_BIN_MS = 600_000L // 10 min

private fun sessionWithData(
    sessions: List<SleepSession>,
    buckets: List<MovementBucket>,
): SleepSession? = sessions.firstOrNull { s ->
    val end = s.endMs ?: return@firstOrNull false
    buckets.any { it.startMs in s.startMs..end }
}

private fun bucketsOf(session: SleepSession, buckets: List<MovementBucket>): List<MovementBucket> {
    val end = session.endMs ?: return emptyList()
    return buckets.filter { it.startMs in session.startMs..end }
}

/** Rachas score 3 ≥30 s → (inicio, duración). Top 5 por duración. */
private fun restlessMoments(sessionBuckets: List<MovementBucket>): List<Pair<Long, Long>> {
    val moments = mutableListOf<Pair<Long, Long>>()
    var runStart = 0L
    var runBuckets = 0
    fun close() {
        if (runBuckets > 0) {
            val durationMs = runBuckets * 10_000L
            if (durationMs >= 30_000) moments.add(runStart to durationMs)
            runBuckets = 0
        }
    }
    sessionBuckets.forEach { b ->
        if (b.score >= 3) {
            if (runBuckets == 0) runStart = b.startMs
            runBuckets++
        } else {
            close()
        }
    }
    close()
    return moments.sortedByDescending { it.second }.take(5)
}

@Composable
private fun phaseColor(score: Int): Color = when (score) {
    0 -> MaterialTheme.colorScheme.primary
    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    2 -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun SleepPhasesCard(sessions: List<SleepSession>, buckets: List<MovementBucket>) {
    val session = remember(sessions, buckets) { sessionWithData(sessions, buckets) }

    ClayCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Fases de sueño", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

            if (session == null) {
                Text(
                    "Empieza una noche para ver tus fases.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            val inSession = remember(session, buckets) { bucketsOf(session, buckets) }
            val bins = remember(inSession, session) {
                inSession
                    .groupBy { (it.startMs - session.startMs) / PHASE_BIN_MS }
                    .toSortedMap()
                    .map { (_, list) -> list.map { it.score }.average().toInt().coerceIn(0, 3) }
            }
            val quietPct = if (inSession.isEmpty()) 0
            else inSession.count { it.score == 0 } * 100 / inSession.size
            val moves = remember(inSession) {
                inSession.zip(inSession.drop(1)).count { (a, b) -> b.score == 3 && a.score < 3 }
            }
            val zone = remember { ZoneId.systemDefault() }
            val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm").withZone(zone) }

            if (bins.isEmpty()) {
                Text(
                    "Sin datos de movimiento en esta noche.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
            ) {
                bins.forEach { score ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 1.dp)
                            .background(phaseColor(score), RoundedCornerShape(3.dp)),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    timeFmt.format(Instant.ofEpochMilli(session.startMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    timeFmt.format(Instant.ofEpochMilli(session.endMs!!)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(
                    "Reposo" to 0,
                    "Ligero" to 1,
                    "Medio" to 2,
                    "Inquieto" to 3,
                ).forEach { (label, score) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .background(phaseColor(score), RoundedCornerShape(2.dp)),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                "%d%% quieto · %d movimiento%s".format(quietPct, moves, if (moves == 1) "" else "s"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RestlessMomentsCard(sessions: List<SleepSession>, buckets: List<MovementBucket>) {
    val session = remember(sessions, buckets) { sessionWithData(sessions, buckets) } ?: return
    val moments = remember(session, buckets) { restlessMoments(bucketsOf(session, buckets)) }

    ClayCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Momentos inquietos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

            if (moments.isEmpty()) {
                Text(
                    "Noche tranquila: sin movimientos largos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()) }
                moments.forEach { (startMs, durationMs) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            timeFmt.format(Instant.ofEpochMilli(startMs)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "%d s".format(durationMs / 1000),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnoreSummaryCard(
    clipCount: Int,
    lastClipMs: Long?,
    onClick: () -> Unit,
) {
    ClayCard(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ronquidos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = when {
                        clipCount == 0 -> "Sin clips en las últimas 24 h"
                        else -> "$clipCount clip${if (clipCount == 1) "" else "s"} · últimas 24 h" +
                            (lastClipMs?.let { " · último ${formatDateTime(it)}" } ?: "")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    ClayCard(modifier, padding = 12.dp) {
        Column {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    ClayCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(name = "Estadísticas · Claro", showBackground = true, backgroundColor = 0xFFF6F1FB)
@Composable
private fun StatsEmptyLightPreview() = ReposoTheme(mode = ReposoThemeMode.LIGHT) {
    ClayBackground { StatsScreen(emptyList()) }
}

@Preview(name = "Estadísticas · Oscuro", showBackground = true, backgroundColor = 0xFF12101C)
@Composable
private fun StatsEmptyDarkPreview() = ReposoTheme(mode = ReposoThemeMode.DARK) {
    ClayBackground { StatsScreen(emptyList()) }
}
