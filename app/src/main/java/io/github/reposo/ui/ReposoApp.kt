@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package io.github.reposo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import io.github.reposo.data.SettingsStore
import io.github.reposo.data.SleepViewModel
import io.github.reposo.monitor.SnoreService
import io.github.reposo.player.AlarmScheduler
import io.github.reposo.player.PlayerViewModel
import io.github.reposo.ui.screens.HomeScreen
import io.github.reposo.ui.screens.NightScreensaver
import io.github.reposo.ui.screens.MixerScreen
import io.github.reposo.ui.screens.RonquidosScreen
import io.github.reposo.ui.screens.RoutineScreen
import io.github.reposo.ui.screens.SettingsScreen
import io.github.reposo.ui.screens.SleepScreen
import io.github.reposo.ui.screens.SoundsScreen
import io.github.reposo.ui.screens.StatsScreen
import io.github.reposo.ui.theme.ClayBackground
import io.github.reposo.ui.theme.ClayCard
import io.github.reposo.ui.theme.ClayPillShape
import io.github.reposo.ui.theme.ReposoThemeMode

private const val MIXER_ROUTE = "mixer"
private const val SNORE_ROUTE = "snore"

/**
 * Alto de la barra flotante, publicado a las pantallas.
 *
 * La barra ya no reserva espacio (flota sobre el contenido), así que el final de cada
 * lista se lee debajo de ella. Las pantallas usan este valor como relleno inferior para
 * que el último elemento pueda subir por encima en vez de quedar tapado.
 */
internal val LocalBottomBarInset = staticCompositionLocalOf { 0.dp }

@Composable
fun ReposoApp(
    themeMode: ReposoThemeMode,
    onThemeModeChange: (ReposoThemeMode) -> Unit,
    playerVm: PlayerViewModel = viewModel(),
    sleepVm: SleepViewModel = viewModel(),
) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val player by playerVm.state.collectAsStateWithLifecycle()
    val lastMix by playerVm.lastMix.collectAsStateWithLifecycle()
    val sessions by sleepVm.sessions.collectAsStateWithLifecycle()
    val snoreClips by sleepVm.snoreClips.collectAsStateWithLifecycle()
    val movementBuckets by sleepVm.movementBuckets.collectAsStateWithLifecycle()
    val active by sleepVm.active.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val density = LocalDensity.current
    // Alto de la barra flotante; se mide en el sitio y se publica a las pantallas.
    var bottomBarInset by remember { mutableStateOf(0.dp) }
    val settingsStore = remember { SettingsStore(context) }
    var alarmEnabled by remember { mutableStateOf(settingsStore.alarmEnabled) }
    var alarmHour by remember { mutableStateOf(settingsStore.alarmHour) }
    var alarmMinute by remember { mutableStateOf(settingsStore.alarmMinute) }

    var snoreActive by remember { mutableStateOf(settingsStore.snoreActive) }
    var snoreStartMs by remember { mutableStateOf(settingsStore.snoreStartMs) }

    // Salvapantallas nocturno: se abre al empezar la noche y se cierra con "Salir"
    // sin detener la sesión (el sueño sigue registrándose de fondo).
    var nightMode by remember { mutableStateOf(false) }

    fun startSnore() {
        if (settingsStore.snoreStartMs == 0L) {
            settingsStore.snoreStartMs = System.currentTimeMillis()
        }
        SnoreService.start(context)
        snoreActive = true
        snoreStartMs = settingsStore.snoreStartMs
    }

    fun hasMicPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val snorePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startSnore()
    }

    /**
     * Arranca la noche y, si falta, pide el micrófono.
     *
     * El permiso se pedía solo al tocar "Grabar ronquidos", así que quien empezaba la
     * noche desde el botón grande nunca lo veía y la grabación quedaba silenciosamente
     * desactivada. Ahora se pide en el momento en que tiene sentido: al empezar.
     */
    fun startNight() {
        sleepVm.start(lastMix?.title)
        playerVm.playLast()
        nightMode = true
        if (!snoreActive && !hasMicPermission()) {
            snorePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // Si el servicio se detiene solo (micrófono fallido), sincronizar el estado.
    LaunchedEffect(snoreActive) {
        while (snoreActive) {
            delay(3_000)
            if (!settingsStore.snoreActive) {
                snoreActive = false
                snoreStartMs = 0
            } else {
                snoreStartMs = settingsStore.snoreStartMs
            }
        }
    }

    // Las alarmas sobreviven al proceso, pero no a un reinicio: reprogramar al abrir.
    remember {
        AlarmScheduler.rescheduleIfEnabled(context)
        true
    }

    LaunchedEffect(active) {
        if (active == null) nightMode = false
    }
    LaunchedEffect(route) {
        // Al navegar fuera de Dormir el overlay pierde sentido.
        if (route != Destination.Sleep.route) nightMode = false
    }

    // El fondo envuelve todo. Sin Scaffold: su bottomBar reservaba una franja opaca y el
    // contenido se cortaba en seco contra ese borde (el "cuadro" que recortaba tarjetas y
    // botones). Ahora la barra flota encima y el contenido pasa por detrás.
    ClayBackground {
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null,
                LocalBottomBarInset provides bottomBarInset,
            ) {
                NavHost(
                    nav,
                    startDestination = Destination.Home.route,
                    modifier = Modifier.fillMaxSize().statusBarsPadding(),
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { fadeOut(animationSpec = tween(300)) },
                ) {
                    composable(Destination.Home.route) {
                        HomeScreen(
                            lastMix = lastMix,
                            playerState = player,
                            onResume = playerVm::playLast,
                            onTogglePlay = playerVm::togglePlay,
                            onPlayPreset = { playerVm.playMix(it.title, it.layers) },
                            onCreateMix = { nav.navigate(MIXER_ROUTE) },
                        )
                    }
                    composable(Destination.Routine.route) {
                        RoutineScreen(onPlayPreset = { playerVm.playMix(it.title, it.layers) })
                    }
                    composable(Destination.Sleep.route) {
                        SleepScreen(
                            active = active,
                            mixTitle = lastMix?.title,
                            onStart = { startNight() },
                            onStop = {
                                sleepVm.stop()
                                playerVm.pause()
                            },
                            onOpenNightMode = { nightMode = true },
                            onTimerSelected = playerVm::setSleepTimer,
                            alarmEnabled = alarmEnabled,
                            alarmHour = alarmHour,
                            alarmMinute = alarmMinute,
                            onAlarmChange = { h, m ->
                                if (h == null || m == null) {
                                    alarmEnabled = false
                                    settingsStore.alarmEnabled = false
                                    AlarmScheduler.cancel(context)
                                } else {
                                    alarmEnabled = true
                                    alarmHour = h
                                    alarmMinute = m
                                    settingsStore.alarmEnabled = true
                                    settingsStore.alarmHour = h
                                    settingsStore.alarmMinute = m
                                    AlarmScheduler.schedule(context, h, m)
                                }
                            },
                            snoreActive = snoreActive,
                            snoreStartMs = snoreStartMs,
                            onSnoreToggle = {
                                if (snoreActive) {
                                    SnoreService.stop(context)
                                    snoreActive = false
                                    snoreStartMs = 0
                                    settingsStore.snoreActive = false
                                    settingsStore.snoreStartMs = 0
                                } else if (android.Manifest.permission.RECORD_AUDIO.let {
                                        androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                                        android.content.pm.PackageManager.PERMISSION_GRANTED
                                    }
                                ) {
                                    startSnore()
                                } else {
                                    snorePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                        )
                    }
                    composable(Destination.Stats.route) {
                        LaunchedEffect(Unit) { sleepVm.cleanupSnore() }
                        StatsScreen(
                            sessions = sessions,
                            snoreClips = snoreClips,
                            onOpenSnore = { nav.navigate(SNORE_ROUTE) },
                            movementBuckets = movementBuckets,
                        )
                    }
                    composable(Destination.Settings.route) {
                        SettingsScreen(
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            onVolumeCapChange = playerVm::setVolumeCap,
                            onPlayMix = { playerVm.playMix(it.title, it.layers) },
                            sessions = sessions,
                            snoreClips = snoreClips,
                            onClearAllData = { sleepVm.clearAllData() },
                        )
                    }
                    composable(MIXER_ROUTE) {
                        MixerScreen(
                            onBack = { nav.popBackStack() },
                            onPlay = { playerVm.playMix("Mi mezcla", it) },
                            onVolume = playerVm::setVolume,
                            onSave = { name, layers -> playerVm.saveMix(name, layers) },
                            onMoreSounds = { nav.navigate("sounds") },
                        )
                    }
                    composable("sounds") {
                        SoundsScreen(onBack = { nav.popBackStack() })
                    }
                    composable(SNORE_ROUTE) {
                        RonquidosScreen(
                            clips = snoreClips,
                            onBack = { nav.popBackStack() },
                            onDelete = sleepVm::deleteSnore,
                        )
                        LaunchedEffect(Unit) { sleepVm.cleanupSnore() }
                    }
                }
            }

            // Barra flotante: no reserva espacio, se apoya sobre el contenido. El degradado
            // de abajo arriba hace que lo que pasa por detrás se difumine en vez de
            // cortarse contra un borde recto.
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomBarInset = with(density) { it.height.toDp() } }
                    .background(
                        Brush.verticalGradient(
                            // Tres paradas: transparente arriba del todo, velo suficiente donde
                            // empieza el mini player y opaco al pie, para que el texto de la
                            // barra nunca compita con lo que pasa por detrás.
                            0f to Color.Transparent,
                            0.30f to MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                            0.62f to MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
                            1f to MaterialTheme.colorScheme.background,
                        ),
                    )
                    .navigationBarsPadding(),
            ) {
                AnimatedVisibility(
                    visible = player.title != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    MiniPlayer(player, onToggle = playerVm::togglePlay)
                }
                BottomNavigationBar(
                    currentRoute = route,
                    onNavigate = { targetRoute ->
                        nav.navigate(targetRoute) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        // Encima de todo, incluida la barra inferior: es un salvapantallas, no una pantalla.
        AnimatedVisibility(
            visible = nightMode && active != null,
            enter = fadeIn(animationSpec = tween(450)),
            exit = fadeOut(animationSpec = tween(350)),
        ) {
            NightScreensaver(
                activeMixTitle = player.title ?: lastMix?.title,
                monitoring = active != null,
                snoreActive = snoreActive,
                alarmEnabled = alarmEnabled,
                alarmHour = alarmHour,
                alarmMinute = alarmMinute,
                onAlarmChange = { h, m ->
                    if (h == null || m == null) {
                        alarmEnabled = false
                        settingsStore.alarmEnabled = false
                        AlarmScheduler.cancel(context)
                    } else {
                        alarmEnabled = true
                        alarmHour = h
                        alarmMinute = m
                        settingsStore.alarmEnabled = true
                        settingsStore.alarmHour = h
                        settingsStore.alarmMinute = m
                        AlarmScheduler.schedule(context, h, m)
                    }
                },
                onPlayMix = { playerVm.playMix(it.title, it.layers) },
                onExit = { nightMode = false },
            )
        }
    }
}

@Composable
fun BottomNavigationBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val destinations = Destination.entries

    ClayCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = ClayPillShape,
        padding = 6.dp,
        elevation = 8.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val itemWidth = maxWidth / destinations.size
            val density = LocalDensity.current
            var rowHeight by remember { mutableStateOf(0.dp) }
            val selectedIndex = destinations.indexOfFirst { it.route == currentRoute }

            val pillX by animateDpAsState(
                targetValue = if (selectedIndex >= 0) itemWidth * selectedIndex else 0.dp,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
                label = "pillX",
            )
            val pillAlpha by animateFloatAsState(
                targetValue = if (selectedIndex >= 0) 1f else 0f,
                label = "pillAlpha",
            )

            Box(
                Modifier
                    .offset(x = pillX)
                    .width(itemWidth)
                    .height(rowHeight)
                    .graphicsLayer { alpha = pillAlpha }
                    .background(colorScheme.primaryContainer, ClayPillShape),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        rowHeight = with(density) { size.height.toDp() }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                destinations.forEach { d ->
                    val selected = currentRoute == d.route
                    val tint by animateColorAsState(
                        targetValue = if (selected) {
                            colorScheme.onPrimaryContainer
                        } else {
                            colorScheme.onSurfaceVariant
                        },
                        label = "tint",
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(ClayPillShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onNavigate(d.route) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            d.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(d.label),
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                        )
                    }
                }
            }
        }
    }
}
