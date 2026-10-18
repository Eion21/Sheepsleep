package io.github.reposo

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import io.github.reposo.ui.ReposoApp
import io.github.reposo.ui.theme.ReposoTheme
import io.github.reposo.ui.theme.ReposoThemeMode

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* la reproducción funciona igual */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val preferences = getSharedPreferences("reposo_preferences", MODE_PRIVATE)
        val initialThemeMode = ReposoThemeMode.fromPreference(preferences.getString("theme_mode", null))
        setContent {
            var themeMode by remember { mutableStateOf(initialThemeMode) }
            val systemIsDark = isSystemInDarkTheme()
            val appIsDark = when (themeMode) {
                ReposoThemeMode.SYSTEM -> systemIsDark
                ReposoThemeMode.LIGHT -> false
                ReposoThemeMode.DARK, ReposoThemeMode.AMOLED -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !appIsDark
                    isAppearanceLightNavigationBars = !appIsDark
                }
            }
            ReposoTheme(mode = themeMode) {
                ReposoApp(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        preferences.edit().putString("theme_mode", mode.storageValue).apply()
                    },
                )
            }
        }
    }
}
