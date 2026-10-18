package io.github.reposo.ui.theme

enum class ReposoThemeMode(val storageValue: String, val label: String) {
    SYSTEM("system", "Sistema"),
    LIGHT("light", "Claro"),
    DARK("dark", "Oscuro"),
    AMOLED("amoled", "AMOLED");

    companion object {
        fun fromPreference(value: String?): ReposoThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}
