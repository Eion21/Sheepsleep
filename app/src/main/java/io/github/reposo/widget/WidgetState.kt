package io.github.reposo.widget

import android.content.Context

/**
 * Estado que pinta el widget media. Lo escribe [io.github.reposo.player.MixEngine]
 * (la fuente de verdad), para que el widget se actualice aunque la UI no exista.
 */
object WidgetState {

    private const val PREFS = "widget_prefs"
    private const val KEY_TITLE = "title"
    private const val KEY_PLAYING = "playing"

    fun save(context: Context, title: String?, playing: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TITLE, title)
            .putBoolean(KEY_PLAYING, playing)
            .apply()
    }

    fun savePlaying(context: Context, playing: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PLAYING, playing)
            .apply()
    }

    fun title(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TITLE, null)

    fun isPlaying(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PLAYING, false)
}
