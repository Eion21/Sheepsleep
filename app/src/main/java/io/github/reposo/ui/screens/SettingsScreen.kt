package io.github.reposo.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import io.github.reposo.ui.LocalBottomBarInset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.reposo.BuildConfig
import io.github.reposo.data.AppUpdateChecker
import io.github.reposo.data.SettingsStore
import io.github.reposo.data.SleepExporter
import io.github.reposo.data.SleepSession
import io.github.reposo.data.SnoreClip
import io.github.reposo.data.UpdateInfo
import io.github.reposo.player.MixStore
import io.github.reposo.player.SavedMix
import io.github.reposo.ui.stretchOverscroll
import io.github.reposo.ui.theme.ClayBackground
import io.github.reposo.ui.theme.ClayButton
import io.github.reposo.ui.theme.ClayCard
import io.github.reposo.ui.theme.ClayOutlineButton
import io.github.reposo.ui.theme.ClayPillShape
import io.github.reposo.ui.theme.ClaySlider
import io.github.reposo.ui.theme.claySwitchColors
import io.github.reposo.ui.theme.ReposoThemeMode
import io.github.reposo.ui.theme.ReposoTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    themeMode: ReposoThemeMode,
    onThemeModeChange: (ReposoThemeMode) -> Unit,
    onVolumeCapChange: (Float) -> Unit = {},
    onPlayMix: (SavedMix) -> Unit = {},
    sessions: List<SleepSession> = emptyList(),
    snoreClips: List<SnoreClip> = emptyList(),
    onClearAllData: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val mixStore = remember { MixStore(context) }
    // Confirmación del borrado: es irreversible, así que nunca se ejecuta a un toque.
    var confirmClear by remember { mutableStateOf(false) }

    var fadeOutMinutes by remember { mutableIntStateOf(settings.fadeOutMinutes) }
    var nightVolumeLimit by remember { mutableFloatStateOf(settings.nightVolumeLimit) }
    var alarmVibration by remember { mutableStateOf(settings.alarmVibration) }
    // Retención real: se lee y se guarda en SettingsStore, que es quien la aplica.
    var clipRetentionDays by remember { mutableIntStateOf(settings.clipRetentionDays) }

    // Actualizaciones: check silencioso de GitHub Releases al abrir la pantalla.
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val info = AppUpdateChecker.check()
        if (info != null && info.version != settings.ignoredUpdateTag) {
            update = info
        }
    }

    var savedMixes by remember { mutableStateOf(mixStore.loadMixes()) }

    var mixToRename by remember { mutableStateOf<SavedMix?>(null) }
    var newMixName by remember { mutableStateOf("") }

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
        item {
            Text("Configuración", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("Personaliza tu experiencia de descanso", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // 1. MIS MEZCLAS
        item {
            SettingSectionHeader("Mis mezclas guardadas")
        }
        if (savedMixes.isEmpty()) {
            item {
                InfoCard("Sin mezclas guardadas", "Tus mezclas personalizadas aparecerán aquí.")
            }
        } else {
            items(savedMixes, key = { it.title }) { mix ->
                ClayCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayMix(mix) },
                    padding = 16.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mix.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${mix.layers.size} sonido(s)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                        IconButton(onClick = {
                            mixToRename = mix
                            newMixName = mix.title
                        }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Renombrar", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            mixStore.deleteMix(mix.title)
                            savedMixes = mixStore.loadMixes()
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // 2. RENDIMIENTO
        item {
            SettingSectionHeader("Rendimiento del sistema")
        }
        item {
            ClayCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Optimización de batería", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Evita que Android cierre el monitor de sueño durante la noche.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)

                    // Estado real, consultado al sistema: antes el botón no decía nada y
                    // no había forma de saber si la exclusión había surtido efecto.
                    val powerManager = remember {
                        context.getSystemService(android.content.Context.POWER_SERVICE)
                            as android.os.PowerManager
                    }
                    var ignoringBattery by remember {
                        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
                    }
                    // Al volver de los ajustes del sistema se refresca el estado.
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                ignoringBattery =
                                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    Text(
                        text = if (ignoringBattery) "Ya está excluida" else "Aún sujeta a optimización",
                        color = if (ignoringBattery) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    ClayButton(
                        text = if (ignoringBattery) "Revisar ajuste" else "Excluir de optimización",
                        onClick = { requestIgnoreBatteryOptimizations(context) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // 3. AUDIO
        item {
            SettingSectionHeader("Audio")
        }
        item {
            ClayCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Temporizador de apagado con fundido gradual", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Duración del fundido: $fadeOutMinutes minutos", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    ClaySlider(
                        value = fadeOutMinutes.toFloat(),
                        onValueChange = {
                            fadeOutMinutes = it.toInt()
                            settings.fadeOutMinutes = fadeOutMinutes
                        },
                        valueRange = 5f..60f,
                        steps = 10,
                    )

                    Spacer(Modifier.height(4.dp))
                    Text("Límite de volumen nocturno (${(nightVolumeLimit * 100).toInt()}%)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    ClaySlider(
                        value = nightVolumeLimit,
                        onValueChange = {
                            nightVolumeLimit = it
                            settings.nightVolumeLimit = it
                            onVolumeCapChange(it)
                        },
                        valueRange = 0.2f..1f,
                    )
                }
            }
        }

        // 4. MONITOR Y GRABACIONES
        item {
            SettingSectionHeader("Monitor y grabaciones")
        }
        item {
            ClayCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Antes había aquí un switch de "monitor de movimiento", otro de
                    // "grabación de ronquidos" y un slider de sensibilidad que nadie
                    // leía. Se controlan donde se usan: Dormir y Ajustes → alarma.
                    Text("Retención de grabaciones", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(
                        "Conserva los clips de ronquido durante: $clipRetentionDays días",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    ClaySlider(
                        value = clipRetentionDays.toFloat(),
                        onValueChange = {
                            clipRetentionDays = it.toInt()
                            settings.clipRetentionDays = clipRetentionDays
                        },
                        valueRange = SettingsStore.MIN_CLIP_RETENTION_DAYS.toFloat()..
                            SettingsStore.MAX_CLIP_RETENTION_DAYS.toFloat(),
                        steps = SettingsStore.MAX_CLIP_RETENTION_DAYS - SettingsStore.MIN_CLIP_RETENTION_DAYS - 1,
                    )
                    Text(
                        "Los clips y el registro de movimiento más antiguos se borran solos. " +
                            "El audio nunca sale del dispositivo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 5. ALARMA
        item {
            SettingSectionHeader("Alarma")
        }
        item {
            ClayCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // El sonido de alarma y el "despertar suave" eran estado local sin
                    // efecto: la alarma siempre usa el mismo tono y la ventana de despertar
                    // no está implementada. Se muestran como información, no como control.
                    Text("Aviso de alarma", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(
                        "La alarma suena con un tono suave y aumenta poco a poco. " +
                            "Se programa en Dormir → Despertar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vibración", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = alarmVibration,
                            onCheckedChange = {
                                alarmVibration = it
                                settings.alarmVibration = it
                            },
                            colors = claySwitchColors()
                        )
                    }
                }
            }
        }

        // 6. APARIENCIA
        item {
            SettingSectionHeader("Apariencia")
        }
        item {
            ClayCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tema visual", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReposoThemeMode.entries.chunked(2).forEach { modes ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                modes.forEach { mode ->
                                    FilterChip(
                                        selected = themeMode == mode,
                                        onClick = { onThemeModeChange(mode) },
                                        shape = ClayPillShape,
                                        label = { Text(mode.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. DATOS Y PRIVACIDAD
        item {
            SettingSectionHeader("Datos y privacidad")
        }
        item {
            ClayCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tus datos de sueño", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val uri = SleepExporter.exportCsv(context, sessions)
                            share(context, uri, "text/csv")
                        }) {
                            Text("Exportar CSV", color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton(onClick = {
                            val uri = SleepExporter.exportJson(context, sessions, snoreClips)
                            share(context, uri, "application/json")
                        }) {
                            Text("Exportar JSON", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    TextButton(onClick = { confirmClear = true }) {
                        Text("Borrar todos los datos", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // 8. ACERCA DE
        item {
            SettingSectionHeader("Acerca de")
        }
        item {
            ClayCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sheepsleep ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Licencia MIT · Código abierto", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    TextButton(onClick = {
                        AppUpdateChecker.openReleasePage(context, "https://github.com/${AppUpdateChecker.REPO}")
                    }) {
                        Text("Ver código en GitHub", color = MaterialTheme.colorScheme.primary)
                    }

                    update?.let { info ->
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        Text(
                            "Versión nueva disponible: v${info.version}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        info.notes?.let { notes ->
                            Text(
                                notes,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ClayButton(
                                text = if (downloading) "Descargando…" else "Actualizar",
                                enabled = !downloading,
                                onClick = {
                                    if (downloading) return@ClayButton
                                    downloading = true
                                    scope.launch {
                                        val apk = info.apkUrl?.let { AppUpdateChecker.downloadApk(context, it) }
                                        downloading = false
                                        update = null
                                        if (apk != null) {
                                            AppUpdateChecker.openInstaller(context, apk)
                                        } else {
                                            AppUpdateChecker.openReleasePage(context, info.releaseUrl)
                                        }
                                    }
                                },
                            )
                            ClayOutlineButton(
                                text = "Esta versión no",
                                onClick = {
                                    settings.ignoredUpdateTag = info.version
                                    update = null
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    mixToRename?.let { mix ->
        AlertDialog(
            onDismissRequest = { mixToRename = null },
            title = { Text("Renombrar mezcla") },
            text = {
                OutlinedTextField(
                    value = newMixName,
                    onValueChange = { newMixName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newMixName.isNotBlank()) {
                        mixStore.renameMix(mix.title, newMixName)
                        savedMixes = mixStore.loadMixes()
                    }
                    mixToRename = null
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mixToRename = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("¿Borrar todos los datos?") },
            text = {
                Text(
                    "Se eliminarán las noches registradas, el movimiento y las grabaciones de " +
                        "ronquidos de este dispositivo. No se puede deshacer.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAllData?.invoke()
                }) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancelar") }
            },
        )
    }
}

/** Abre el selector del sistema para compartir el archivo exportado. */
private fun share(context: android.content.Context, uri: android.net.Uri, mime: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = mime
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        android.content.Intent.createChooser(intent, "Compartir datos de Sheepsleep"),
    )
}

/**
 * Pide al sistema que excluya la app de la optimización de batería.
 *
 * Cadena de intentos, porque el diálogo directo no siempre está disponible:
 * 1. El diálogo de la app (ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS). Es el más
 *    claro para el usuario y el único que permite concederlo en un toque.
 * 2. Si el fabricante lo bloquea, la lista de apps excluidas, donde el usuario puede
 *    añadirla a mano.
 * 3. Si tampoco, los ajustes de batería de la app.
 *
 * Antes esto era un único intent con `catch (_: Exception) {}`: si fallaba, el botón
 * no hacía nada y no había ni rastro del motivo.
 */
private fun requestIgnoreBatteryOptimizations(context: android.content.Context) {
    val pkg = Uri.parse("package:${context.packageName}")
    val attempts = listOf(
        android.content.Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(pkg),
        android.content.Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(pkg),
    )
    for (intent in attempts) {
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Se prueba el siguiente; el último (detalles de la app) siempre existe.
        }
    }
}

@Composable
private fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
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

@Preview(name = "Ajustes · Claro", showBackground = true, backgroundColor = 0xFFF6F1FB)
@Composable
private fun SettingsLightPreview() = ReposoTheme(mode = ReposoThemeMode.LIGHT) {
    ClayBackground {
    SettingsScreen(themeMode = ReposoThemeMode.LIGHT, onThemeModeChange = {})
    }
}

@Preview(name = "Ajustes · Oscuro", showBackground = true, backgroundColor = 0xFF12101C)
@Composable
private fun SettingsDarkPreview() = ReposoTheme(mode = ReposoThemeMode.DARK) {
    ClayBackground {
    SettingsScreen(themeMode = ReposoThemeMode.DARK, onThemeModeChange = {})
    }
}
