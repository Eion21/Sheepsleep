package io.github.reposo.player

import android.os.Bundle
import androidx.media3.session.SessionCommand

/** Comandos personalizados entre la UI (MediaController) y el servicio. */
object PlaybackCommands {
    val SET_MIX = SessionCommand("io.github.reposo.SET_MIX", Bundle.EMPTY)
    val SET_VOLUME = SessionCommand("io.github.reposo.SET_VOLUME", Bundle.EMPTY)
    val SET_SLEEP_TIMER = SessionCommand("io.github.reposo.SET_SLEEP_TIMER", Bundle.EMPTY)
    val SET_VOLUME_CAP = SessionCommand("io.github.reposo.SET_VOLUME_CAP", Bundle.EMPTY)

    const val KEY_TITLE = "title"
    const val KEY_IDS = "ids"
    const val KEY_VOLUMES = "volumes"
    const val KEY_ID = "id"
    const val KEY_VOLUME = "volume"
    const val KEY_MINUTES = "minutes"
    const val KEY_CAP = "cap"
    const val KEY_FADE_MS = "fade_ms"
}
