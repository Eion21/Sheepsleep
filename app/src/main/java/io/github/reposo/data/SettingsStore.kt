package io.github.reposo.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Ajustes de audio que sí surten efecto: se guardan en SharedPreferences para
 * que el servicio de reproducción los aplique aunque la UI ya no exista.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("reposo_preferences", Context.MODE_PRIVATE)

    /** Duración del fundido final del temporizador, en minutos. */
    var fadeOutMinutes: Int
        get() = prefs.getInt(KEY_FADE_OUT_MINUTES, DEFAULT_FADE_OUT_MINUTES).coerceIn(5, 60)
        set(value) {
            prefs.edit().putInt(KEY_FADE_OUT_MINUTES, value.coerceIn(5, 60)).apply()
        }

    /** Tope de volumen aplicado a todas las capas (0.2..1.0). */
    var nightVolumeLimit: Float
        get() = prefs.getFloat(KEY_NIGHT_VOLUME_LIMIT, DEFAULT_NIGHT_VOLUME_LIMIT).coerceIn(0.2f, 1f)
        set(value) {
            prefs.edit().putFloat(KEY_NIGHT_VOLUME_LIMIT, value.coerceIn(0.2f, 1f)).apply()
        }

    /** Última versión de actualización descartada por el usuario ("" = ninguna). */
    var ignoredUpdateTag: String        get() = prefs.getString(KEY_IGNORED_UPDATE_TAG, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_IGNORED_UPDATE_TAG, value).apply()
        }

    /** Alarma diaria habilitada. */
    var alarmEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALARM_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ALARM_ENABLED, value).apply()
        }

    var alarmHour: Int
        get() = prefs.getInt(KEY_ALARM_HOUR, 7).coerceIn(0, 23)
        set(value) {
            prefs.edit().putInt(KEY_ALARM_HOUR, value.coerceIn(0, 23)).apply()
        }

    var alarmMinute: Int
        get() = prefs.getInt(KEY_ALARM_MINUTE, 0).coerceIn(0, 59)
        set(value) {
            prefs.edit().putInt(KEY_ALARM_MINUTE, value.coerceIn(0, 59)).apply()
        }

    var alarmVibration: Boolean
        get() = prefs.getBoolean(KEY_ALARM_VIBRATION, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ALARM_VIBRATION, value).apply()
        }

    /** Grabación de ronquidos en curso (sobrevive recreación de la UI). */
    var snoreActive: Boolean        get() = prefs.getBoolean(KEY_SNORE_ACTIVE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SNORE_ACTIVE, value).apply()
        }

    /** Instante en que arrancó la grabación (0 = apagada). */
    var snoreStartMs: Long
        get() = prefs.getLong(KEY_SNORE_START_MS, 0L)
        set(value) {
            prefs.edit().putLong(KEY_SNORE_START_MS, value).apply()
        }

    /**
     * Días que se conservan los clips de ronquido y los buckets de movimiento.
     * Es el valor que la UI muestra y el que usa la limpieza periódica: antes el
     * slider era estado local y el borrado estaba fijo a 30 días.
     */
    var clipRetentionDays: Int
        get() = prefs.getInt(KEY_CLIP_RETENTION_DAYS, DEFAULT_CLIP_RETENTION_DAYS)
            .coerceIn(MIN_CLIP_RETENTION_DAYS, MAX_CLIP_RETENTION_DAYS)
        set(value) {
            prefs.edit()
                .putInt(KEY_CLIP_RETENTION_DAYS, value.coerceIn(MIN_CLIP_RETENTION_DAYS, MAX_CLIP_RETENTION_DAYS))
                .apply()
        }

    companion object {
        const val KEY_FADE_OUT_MINUTES = "fade_out_minutes"
        const val KEY_NIGHT_VOLUME_LIMIT = "night_volume_limit"
        const val KEY_IGNORED_UPDATE_TAG = "ignored_update_tag"
        const val KEY_ALARM_ENABLED = "alarm_enabled"
        const val KEY_ALARM_HOUR = "alarm_hour"
        const val KEY_ALARM_MINUTE = "alarm_minute"
        const val KEY_ALARM_VIBRATION = "alarm_vibration"
        const val KEY_SNORE_ACTIVE = "snore_active"
        const val KEY_SNORE_START_MS = "snore_start_ms"
        const val KEY_CLIP_RETENTION_DAYS = "clip_retention_days"

        /**
         * Días que se conservan los clips. Mínimo 1: un 0 borraría todo nada más
         * grabarse, que no es lo que espera nadie al mover el slider.
         */
        const val MIN_CLIP_RETENTION_DAYS = 1

        /** Tope alto para que un valor corrupto no desactive la limpieza para siempre. */
        const val MAX_CLIP_RETENTION_DAYS = 90

        /** Valor por defecto, usado también al limpiar si nunca se tocó el ajuste. */
        const val DEFAULT_CLIP_RETENTION_DAYS = 30

        const val DEFAULT_FADE_OUT_MINUTES = 15
        const val DEFAULT_NIGHT_VOLUME_LIMIT = 0.8f
    }
}
