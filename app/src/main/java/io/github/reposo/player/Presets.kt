package io.github.reposo.player

/** Mezclas listas para usar, hechas solo con los sonidos incluidos en la app. */
data class Preset(
    val id: String,
    val title: String,
    val subtitle: String,
    val goal: String,
    val layers: Map<String, Float>,
)

object Presets {
    val goals = listOf("Duerme más rápido", "Duerme mejor", "Relájate")

    val all = listOf(
        Preset("brown_deep", "Marrón profundo", "Ruido marrón", "Duerme más rápido", mapOf("brown" to 0.8f)),
        Preset("brown_pink", "Marrón + rosa", "2 sonidos", "Duerme más rápido", mapOf("brown" to 0.6f, "pink" to 0.4f)),
        Preset("pink_soft", "Rosa suave", "Ruido rosa", "Duerme mejor", mapOf("pink" to 0.7f)),
        Preset("white_steady", "Blanco constante", "Ruido blanco", "Duerme mejor", mapOf("white" to 0.5f)),
        Preset("breeze", "Brisa", "2 sonidos", "Relájate", mapOf("pink" to 0.5f, "white" to 0.3f)),
        Preset("warm_low", "Grave y cálido", "2 sonidos", "Relájate", mapOf("brown" to 1.0f, "pink" to 0.2f)),
    )
}
