# Reglas de R8 para Sheepsleep.
#
# Room, Media3 y Navigation traen reglas de consumer en sus AAR; aquí solo se
# documenta lo que la app necesita por sí misma.

# El catálogo de sonidos se resuelve por nombre con Resources.getIdentifier(),
# así que R8 no puede ver la referencia. Los .wav se protegen con
# res/raw/keep.xml; esto evita que el código que los nombra se optimice mal.
-keepclassmembers class io.github.reposo.player.SoundCatalog {
    *;
}

# Los nombres de los campos de los data class exportados a JSON se usan tal cual.
-keepclassmembers class io.github.reposo.data.UpdateInfo { *; }
-keepclassmembers class io.github.reposo.sounds.RemoteSound { *; }

# WorkManager instancia los workers por reflexión desde el nombre de clase.
-keep class io.github.reposo.sounds.DownloadSoundWorker { <init>(...); }

# AndroidX Work mantiene sus propios receivers; se conserva el comportamiento
# por defecto del AAR.
