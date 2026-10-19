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

        // Presets con los sonidos de naturaleza. Se mezclan a volúmenes moderados:
        // a tope, la lluvia tapa la fogata y se pierde la sensación de capas.
        Preset("rain_window", "Lluvia en la ventana", "Lluvia", "Duerme más rápido", mapOf("rain" to 0.7f)),
        Preset("rain_fire", "Lluvia y fogata", "2 sonidos", "Duerme más rápido", mapOf("rain" to 0.55f, "fire" to 0.35f)),
        Preset("ocean", "Olas del mar", "Olas", "Duerme mejor", mapOf("waves" to 0.75f)),
        Preset("seaside_night", "Noche en la costa", "2 sonidos", "Duerme mejor", mapOf("waves" to 0.55f, "wind" to 0.3f)),
        Preset("forest_night", "Bosque de noche", "Grillos y aire", "Relájate", mapOf("forest" to 0.6f, "wind" to 0.25f)),
        Preset("cabin", "Cabaña con chimenea", "2 sonidos", "Relájate", mapOf("fire" to 0.6f, "wind" to 0.3f)),
    )
}
