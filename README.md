![Logo de Sheepsleep](Logo/sheep-sleep-1.png)

# Sheepsleep

**Monitoreo del sueño y relajación para Android. Gratis, de código abierto y 100 % local.**

Sin cuentas · Sin nube · Sin servicios de Google

# Sheepsleep

 

**Monitoreo del sueño y relajación para Android. Gratis, de código abierto y 100 % local.**

 

Sin cuentas · Sin nube · Sin servicios de Google

 

[Versión](https://img.shields.io/badge/versi%C3%B3n-1.0-blue) [Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white) [Licencia](https://img.shields.io/badge/licencia-MIT-green)

 &lt;/div&gt; 

---

 

## Contenido

 

- [Características](https://claude.ai/chat/873c0808-8652-4367-a078-1cae816408e4#caracter%C3%ADsticas)
- [Privacidad](https://claude.ai/chat/873c0808-8652-4367-a078-1cae816408e4#privacidad)
- [Capturas de pantalla](https://claude.ai/chat/873c0808-8652-4367-a078-1cae816408e4#capturas-de-pantalla)
- [Compilar](https://claude.ai/chat/873c0808-8652-4367-a078-1cae816408e4#compilar)
- [Sonidos](https://claude.ai/chat/873c0808-8652-4367-a078-1cae816408e4#sonidos)
- [Pruebas](https://claude.ai/chat/873c0808-8652-4367-a078-1cae816408e4#pruebas)
- [Publicar una versión](https://claude.ai/chat/873c0808-8652-4367-a078-1cae816408e4#publicar-una-versi%C3%B3n)
- [Tipografía](https://claude.ai/chat/873c0808-8652-4367-a078-1cae816408e4#tipograf%C3%ADa)
- [Contribuir](https://claude.ai/chat/873c0808-8652-4367-a078-1cae816408e4#contribuir)
- [Licencia](https://claude.ai/chat/873c0808-8652-4367-a078-1cae816408e4#licencia)

 

## Características

 

| Área | Qué ofrece |
| --- | --- |
| 🎧 **Mezclas** | Capas de ruido con volumen independiente (pestañas *Inicio*, *Rutina* y *Crear mezcla*). Cada capa usa su propio `ExoPlayer` dentro de un servicio en segundo plano con notificación. |
| 🌙 **Dormir** | Al empezar la noche se abre un salvapantallas con reloj, luna y tus mezclas. Mantiene la pantalla encendida y oculta las barras del sistema. |
| 🐑 **Ovejitas** | Contador de ovejitas para conciliar el sueño. |
| 📊 **Estadísticas** | Noches registradas, hipnograma y clips de ronquido, con historial guardado en Room. |
| 🎙️ **Ronquidos y movimiento** | Grabación de ronquidos y monitor de movimiento durante la noche. |
| ⏰ **Despertador** | Alarma integrada. |
| 📤 **Tus datos** | Exportación en CSV o JSON y borrado total desde Ajustes. |

 

## Privacidad

 

Todo se queda en tu dispositivo:

 

- Las sesiones, el movimiento y el audio de los ronquidos viven en el sandbox de la app.
- No se sube nada a ningún servidor.
- Puedes exportarlos (CSV/JSON) o borrarlos por completo en **Ajustes → *Tus datos de sueño***.

 

La única conexión externa es la comprobación de actualizaciones contra GitHub Releases (ver [Publicar una versión](https://claude.ai/chat/873c0808-8652-4367-a078-1cae816408e4#publicar-una-versi%C3%B3n)).

 

## Capturas de pantalla

 &lt;!-- Añade aquí capturas de Inicio, sesión de sueño y Estadísticas. Ejemplo: &lt;img src="docs/img/inicio.png" width="220"&gt; --&gt; 

*Próximamente.*

 

## Compilar

 

**Requisitos**

 

- Android Studio Ladybug o superior
- JDK 17
- `compileSdk 37` · `minSdk 26`

 

**Pasos**

 

1. Abre la carpeta del proyecto en Android Studio.
2. Ejecuta el módulo `app`, o desde la terminal:

 

```bash
./gradlew :app:assembleDebug     # APK de depuración
./gradlew :app:assembleRelease   # APK de release (sin firmar por defecto)

```

 

Para firmar el APK de release con tu propio keystore, sigue [docs/](https://claude.ai/chat/docs/FIRMA.md)[FIRMA.md](http://FIRMA.md). Si no existe [`keystore.properties`](http://keystore.properties), el build genera un APK sin firmar en lugar de fallar.

 

## Sonidos

 

Incluye ruido **blanco**, **rosa** y **marrón**, generados por código, por lo que no tienen licencia que atribuir.

 

### Añadir más sonidos

 

Copia el archivo a `app/src/main/res/raw/` con uno de estos nombres (en minúsculas):

 

`rain` · `waves` · `wind` · `fire` · `forest`

 

Formatos admitidos: `.ogg`, `.wav`, `.flac`.

 

Recomendaciones:

 

- Usa material CC0 o compatible con la licencia del proyecto, y anota la atribución.
- Para bucles sin cortes, usa archivos preparados para loop.

 

Los sonidos que importa el usuario se guardan en `filesDir/custom_audio`.

 

> [!NOTE] `res/raw/keep.xml` mantiene los `.wav` en el APK de release. El catálogo resuelve los sonidos por nombre con `getIdentifier()`, una referencia que R8 no puede ver.

 

## Pruebas

 

```bash
./gradlew :app:testDebugUnitTest

```

 

Son 33 pruebas centradas en lo que puede afectar al usuario:

 

| Archivo | Qué comprueba |
| --- | --- |
| `VersionComparatorTest` | Detección de versiones nuevas; un prelanzamiento (`1.0.0-beta`) no se ofrece sobre la estable. |
| `HashVerifiedDownloadTest` | Un SHA-256 incorrecto no publica el archivo ni deja restos. |
| `CatalogValidationTest` | Las entradas inválidas del catálogo (rutas, extensiones, hashes) se descartan. |
| `ClipRetentionTest` | La retención configurada se persiste y acota; el corte de limpieza la respeta. |
| `SoundCatalogTest` | Los sonidos sin archivo no aparecen en la lista y todo lo listado se puede reproducir. |

 

## Publicar una versión

 

`AppUpdateChecker` consulta GitHub Releases del repositorio [`Eion21/sheepsleep`](https://github.com/Eion21/sheepsleep).

 

1. Sube `versionCode` y `versionName`.
2. Compila y firma el APK de release.
3. Crea la release en GitHub con la etiqueta `v1.0.0` (usa la versión correspondiente).

 

## Tipografía

 

- **Nunito Sans**: cuerpo de texto
- **Varela Round**: títulos

 

Ambas están empaquetadas en `app/src/main/res/font/` y se distribuyen bajo SIL Open Font License 1.1 (ver [`OFL.txt`](https://claude.ai/chat/OFL.txt)).

 

## Contribuir

 

¡Las contribuciones son bienvenidas! Abre un *issue* para reportar errores o proponer ideas, o envía un *pull request*. Antes de hacerlo, ejecuta las pruebas:

 

```bash
./gradlew :app:testDebugUnitTest

```

 

## Licencia

 

Distribuido bajo licencia MIT. Consulta [LICENSE](https://claude.ai/chat/LICENSE).
