<div align="center">

<img src="Logo/sheep-sleep-1.png" width="140" alt="Sheepsleep logo">

# Sheepsleep

**Sleep and relaxation for Android. Free, open source, 100 % local.**

[![Versión](https://img.shields.io/badge/versi%C3%B3n-1.2.0-blue?style=flat-square)](https://github.com/Eion21/Sheepsleep/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Licencia](https://img.shields.io/badge/licencia-MIT-green?style=flat-square)](LICENSE)
[![Tests](https://img.shields.io/badge/tests-46%20passing-brightgreen?style=flat-square)](#pruebas)

[Español](#español) · [English](#english)

</div>

---

## Español

**Monitoreo del sueño y relajación para Android. Gratis, de código abierto y 100 % local.**

Sin cuentas · Sin nube · Sin servicios de Google · Sin anuncios

### Contenido

- [Características](#características)
- [Privacidad](#privacidad)
- [Capturas de pantalla](#capturas-de-pantalla)
- [Compilar](#compilar)
- [Sonidos](#sonidos)
- [Pruebas](#pruebas)
- [Publicar una versión](#publicar-una-versión)
- [Tipografía](#tipografía)
- [Contribuir](#contribuir)
- [Licencia](#licencia)

### Características

| Área | Qué ofrece |
| --- | --- |
| 🎧 **Mezclas** | Ocho sonidos en capas con volumen independiente. Cada capa usa su propio `ExoPlayer` dentro de un servicio en segundo plano con notificación. |
| 🌙 **Dormir** | Salvapantallas nocturno con reloj, luna animada y tus mezclas. Mantiene la pantalla encendida y oculta las barras del sistema. |
| 🐑 **Ovejitas** | Contador para conciliar el sueño: la oveja salta de derecha a izquierda de la tarjeta y el número se anima al sumar. |
| 📊 **Estadísticas** | Noches registradas, actividad reciente, calendario de sueño de 12 semanas e hipnograma por fases. |
| 🎙️ **Ronquidos** | Grabación activada por volumen: guarda solo lo que suena fuerte, no toda la noche. |
| 🛏️ **Movimiento** | Acelerómetro con buckets de 10 s para estimar fases y momentos inquietos. |
| ⏰ **Despertador** | Alarma exacta con reintento tras reinicio. |
| 📤 **Tus datos** | Exportación en CSV o JSON y borrado total desde Ajustes. |

**Diseño**: tema propio *claymorphism* — superficies mates con luz cálida, grano de
plastilina, controles que se hunden al pulsarlos y rebote al soltar. Temas claro,
oscuro y AMOLED.

### Privacidad

Todo se queda en tu dispositivo:

- Las sesiones, el movimiento y el audio de los ronquidos viven en el sandbox de la app.
- No se sube nada a ningún servidor.
- Puedes exportarlos (CSV/JSON) o borrarlos por completo en **Ajustes → Tus datos de sueño**.

La única conexión externa es la comprobación de actualizaciones contra GitHub Releases.

### Capturas de pantalla

| Inicio | Modo noche | Estadísticas |
| :---: | :---: | :---: |
| <img src="docs/img/inicio.jpg" width="240" alt="Pantalla de inicio"> | <img src="docs/img/modo-noche.jpg" width="240" alt="Salvapantallas nocturno"> | <img src="docs/img/estadisticas.jpg" width="240" alt="Pantalla de estadísticas"> |
| Mezclas, objetivo de sueño y ovejitas | Reloj, luna y tus mezclas | Actividad, calendario y fases |

### Compilar

**Requisitos**: Android Studio Ladybug o superior, JDK 17, `compileSdk 37`, `minSdk 26`.

```bash
./gradlew :app:assembleDebug     # APK de depuración
./gradlew :app:assembleRelease   # APK de release (sin firmar si no hay keystore)
```

Para firmar el APK de release con tu propio keystore, sigue [docs/FIRMA.md](docs/FIRMA.md).
Si no existe `keystore.properties`, el build genera un APK sin firmar en lugar de fallar.

### Sonidos

Ocho sonidos, todos sintetizados por código con `tools/make_sounds.py`:

| Categoría | Sonidos |
| --- | --- |
| **Ruido** | Blanco · Rosa · Marrón |
| **Naturaleza** | Lluvia · Olas · Viento · Bosque de noche |
| **Hogar** | Fogata |

Sintetizarlos en lugar de descargarlos evita cualquier problema de licencia: son
originales y se distribuyen con la licencia del proyecto. Se regeneran con:

```bash
python3 tools/make_sounds.py     # escribe WAV en app/src/main/res/raw/
```

El script cierra cada pieza en un bucle sin cortes (fundido cruzado entre el final
y el principio) y normaliza a ~-20 dBFS para que se puedan mezclar varias capas sin
saturar. Los WAV se convierten a OGG para el APK:

```bash
for f in app/src/main/res/raw/*.wav; do
  ffmpeg -y -i "$f" -c:a libvorbis -q:a 3 "${f%.wav}.ogg"
done
rm app/src/main/res/raw/*.wav
```

**Añadir sonidos propios**: desde la app (*Crear mezcla* → *Añadir sonido propio*,
se guarda en `filesDir/custom_audio`) o dejando el archivo en `res/raw/` y
declarándolo en `SoundCatalog`.

> [!NOTE]
> `res/raw/keep.xml` mantiene los `.ogg` en el APK de release. El catálogo resuelve
> los sonidos por nombre con `getIdentifier()`, una referencia que R8 no puede ver.

Para descargar más hay un catálogo remoto opcional (`tools/make_catalog.py`),
validado con SHA-256 antes de publicar cada archivo.

### Pruebas

```bash
./gradlew :app:testDebugUnitTest
```

46 pruebas centradas en lo que puede afectar al usuario:

| Archivo | Qué comprueba |
| --- | --- |
| `VersionComparatorTest` | Detección de versiones nuevas; un prelanzamiento (`1.0.0-beta`) no se ofrece sobre la estable. |
| `HashVerifiedDownloadTest` | Un SHA-256 incorrecto no publica el archivo ni deja restos. |
| `CatalogValidationTest` | Las entradas inválidas del catálogo (rutas, extensiones, hashes) se descartan. |
| `ClipRetentionTest` | La retención configurada se persiste y acota; el corte de limpieza la respeta. |
| `SoundCatalogTest` | Los sonidos sin archivo no aparecen en la lista y todo lo listado se puede reproducir. |
| `SnoreDetectorTest` | El silencio no dispara grabaciones, un pico aislado tampoco y un sonido sostenido abre y cierra el clip. |

### Publicar una versión

`AppUpdateChecker` consulta GitHub Releases de [Eion21/Sheepsleep](https://github.com/Eion21/Sheepsleep).

1. Sube `versionCode` y `versionName` en `app/build.gradle.kts`.
2. Compila y firma el APK de release.
3. Crea la release en GitHub con la etiqueta `vX.Y.Z` y adjunta el APK.

### Tipografía

- **Nunito Sans**: cuerpo de texto
- **Varela Round**: títulos

Ambas empaquetadas en `app/src/main/res/font/` bajo SIL Open Font License 1.1 (ver [OFL.txt](OFL.txt)).

### Contribuir

¡Las contribuciones son bienvenidas! Abre un *issue* para reportar errores o proponer
ideas, o envía un *pull request*. Antes de enviarlo, ejecuta las pruebas:

```bash
./gradlew :app:testDebugUnitTest
```

### Licencia

MIT — ver [LICENSE](LICENSE).

### Temas del repositorio

Para que el proyecto se encuentre en GitHub, los temas recomendados son:

```
android  kotlin  jetpack-compose  sleep  white-noise  sleep-tracker
snore-detection  alarm  offline-first  privacy  material-you
claymorphism  open-source  no-ads
```

Se configuran en **Settings → Topics** del repositorio (interfaz web, sin token).

### Descripción del repositorio

La frase que GitHub muestra en los resultados de búsqueda y en las tarjetas:

> Sleep and relaxation for Android — layered sound mixer, night screensaver,
> snore detection and sleep stats. 100 % local, no accounts, no ads.

---

## English

**Sleep and relaxation for Android. Free, open source, 100 % local.**

No accounts · No cloud · No Google services · No ads

Sheepsleep is a sleep companion that runs entirely on your phone. It mixes
background sounds, tracks your night, and shows what happened while you slept —
without sending a single byte to a server.

### Features

| Area | What it does |
| --- | --- |
| 🎧 **Sound mixer** | Eight layered sounds with independent volume. Each layer runs on its own `ExoPlayer` inside a foreground service, so audio keeps playing with the screen off. |
| 🌙 **Night mode** | A screensaver with a clock, an animated moon and your mixes. Keeps the screen on and hides the system bars. |
| 🐑 **Sheep counter** | Count sheep to fall asleep: a sheep hops across the card and the number animates on each tap. |
| 📊 **Statistics** | Logged nights, recent activity, a 12-week sleep calendar and a hypnogram built from movement. |
| 🎙️ **Snore detection** | Volume-triggered recording: only loud passages are saved, not the whole night. |
| 🛏️ **Movement tracking** | Accelerometer sampled into 10-second buckets to estimate sleep phases and restless moments. |
| ⏰ **Alarm** | Exact alarm that survives reboots. |
| 📤 **Your data** | Export to CSV or JSON, or delete everything from Settings. |

### Design

A custom *claymorphism* theme: matte surfaces with warm light, a subtle plasticine
grain, and controls that press in and spring back. Light, dark and AMOLED themes.

### Privacy

Everything stays on your device:

- Sleep sessions, movement data and snore audio live inside the app sandbox.
- Nothing is uploaded anywhere.
- Export them (CSV/JSON) or wipe them completely in **Settings → Your sleep data**.

The only outbound request is the update check against GitHub Releases.

### Build

**Requirements**: Android Studio Ladybug or newer, JDK 17, `compileSdk 37`, `minSdk 26`.

```bash
./gradlew :app:assembleDebug     # debug APK
./gradlew :app:assembleRelease   # release APK (unsigned without a keystore)
```

See [docs/FIRMA.md](docs/FIRMA.md) for release signing. Without
`keystore.properties` the build produces an unsigned APK instead of failing.

### Sounds

All eight sounds are synthesized in code by `tools/make_sounds.py` — white, pink
and brown noise, rain, ocean waves, wind, night forest and a crackling fireplace.
Generating them avoids any licensing question: they are original and ship under the
project license. The script loops each piece seamlessly with a crossfade at the seam
and normalizes to about −20 dBFS so layers can be mixed without clipping.

### Tests

```bash
./gradlew :app:testDebugUnitTest
```

46 unit tests covering version comparison, download integrity (SHA-256), catalog
validation, clip retention, sound availability and snore detection.

### License

MIT — see [LICENSE](LICENSE).

---

<div align="center">

Made for people who need quiet to sleep.

[⬆ Back to top](#sheepsleep)

</div>
