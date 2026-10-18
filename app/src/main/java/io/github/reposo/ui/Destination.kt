package io.github.reposo.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.reposo.R

enum class Destination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Home("home", R.string.nav_home, Icons.Rounded.Home),
    Routine("routine", R.string.nav_routine, Icons.Rounded.Alarm),
    Sleep("sleep", R.string.nav_sleep, Icons.Rounded.Bedtime),
    Stats("stats", R.string.nav_stats, Icons.Rounded.BarChart),
    Settings("settings", R.string.nav_settings, Icons.Rounded.Settings),
}
