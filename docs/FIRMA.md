# Firma de la app

El APK de release se firma con un keystore que **nunca** entra al repositorio.
Sin él, `assembleRelease` sigue funcionando pero genera un APK sin firmar.

## Crear el keystore (una sola vez)

Guárdalo fuera del proyecto, por ejemplo en `~/.android-keys/`:

```bash
mkdir -p ~/.android-keys
keytool -genkeypair -v \
  -keystore ~/.android-keys/sheepsleep.jks \
  -alias sheepsleep \
  -keyalg RSA -keysize 2048 -validity 10000
```

Usa una contraseña fuerte y anótala en tu gestor de contraseñas. **Si la pierdes,
no podrás publicar actualizaciones** para quien ya tenga instalada la app: Android
rechaza un APK firmado con otra clave.

## Configurar el build

Crea `keystore.properties` en la raíz del proyecto (ya está en `.gitignore`):

```properties
storeFile=/home/tu-usuario/.android-keys/sheepsleep.jks
storePassword=tu-contraseña
keyAlias=sheepsleep
keyPassword=tu-contraseña
```

`storeFile` admite ruta absoluta o relativa a la raíz del proyecto.

## Alternativa para CI

Si prefieres no crear el archivo, define variables de entorno:

| Variable | Contenido |
|---|---|
| `SHEEPSLEEP_STORE_FILE` | Ruta al `.jks` |
| `SHEEPSLEEP_STORE_PASSWORD` | Contraseña del keystore |
| `SHEEPSLEEP_KEY_ALIAS` | Alias de la clave |
| `SHEEPSLEEP_KEY_PASSWORD` | Contraseña de la clave |

En GitHub Actions, guárdalos como *repository secrets* y decodifica el keystore
desde un secret en base64 antes de compilar.

## Comprobar la firma

```bash
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Debe imprimir `Signer #1 certificate DN` con tus datos. Si el archivo se llama
`app-release-unsigned.apk`, no se encontró `keystore.properties` y el build salió
sin firmar.

## Publicar una versión

1. Sube `versionCode` y `versionName` en `app/build.gradle.kts`.
2. `./gradlew :app:assembleRelease`
3. Crea un tag `v1.0.0` y adjunta el APK a la release de GitHub.

El nombre del tag debe ser la versión con `v` delante: `AppUpdateChecker` lo lee
para avisar de actualizaciones dentro de la app.
