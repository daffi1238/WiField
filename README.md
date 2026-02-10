# WiField

Aplicacion Android de site survey WiFi para ingenieros de redes. Permite escanear, diagnosticar y documentar el estado de redes inalambricas en diferentes ubicaciones.

## Funcionalidades

### Escaneo pasivo
- Lista de puntos de acceso con RSSI, canal, banda, ancho de canal y seguridad
- Agrupacion por SSID
- Graficas de ocupacion de canales (2.4 GHz / 5 GHz)
- Auto-refresh cada 32s (respeta throttling de Android)

### Diagnostico activo
- Speed test (descarga y subida)
- Ping, latencia, jitter y packet loss
- Monitorizacion en tiempo real con graficas de RSSI y latencia
- Informacion de conexion (BSSID, link speed, frecuencia)

### Analisis de alertas
- Senal debil
- Canal saturado
- Overlap de canales (2.4 GHz)
- Seguridad debil (Open/WEP/WPS)
- Ancho de canal suboptimo
- Densidad excesiva de APs

### Gestion de datos
- Proyectos con multiples snapshots por ubicacion
- Comparador de snapshots lado a lado
- Persistencia local con Room (SQLite)

## Requisitos

- Android 9+ (API 28)
- Permisos de ubicacion (necesario para escaneo WiFi en Android)
- Conexion WiFi (para diagnostico activo)

## Instalacion rapida

El APK precompilado esta disponible en la carpeta `release/`:

```bash
adb install release/WiField-debug.apk
```

## Compilacion desde codigo fuente

### Prerequisitos

- **Java 17+** (probado con OpenJDK 21)
- **Android SDK** con:
  - Platform SDK 36
  - Build Tools 36.0.0
- No se necesita Android Studio, se compila desde terminal

### Configurar el SDK

Crear o editar `local.properties` en la raiz del proyecto:

```properties
sdk.dir=/ruta/a/tu/Android/Sdk
```

Ejemplo en Linux:
```properties
sdk.dir=/home/usuario/Android/Sdk
```

Ejemplo en Windows:
```properties
sdk.dir=C\:\\Users\\usuario\\AppData\\Local\\Android\\Sdk
```

### Compilar

```bash
# APK debug
./gradlew assembleDebug

# El APK se genera en:
# app/build/outputs/apk/debug/app-debug.apk
```

### Instalar en dispositivo

Conectar el telefono con depuracion USB activada:

```bash
# Verificar que el dispositivo esta conectado
adb devices

# Instalar
adb install app/build/outputs/apk/debug/app-debug.apk

# O directamente con Gradle
./gradlew installDebug
```

### Permisos post-instalacion

La app solicita permisos al abrirla. Si no aparece el dialogo, se pueden conceder manualmente:

```bash
adb shell pm grant com.wifield.app android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.wifield.app android.permission.ACCESS_COARSE_LOCATION
```

## Stack tecnico

| Componente | Tecnologia |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Base de datos | Room (SQLite) |
| Arquitectura | MVVM + Clean Architecture |
| Build | Gradle 9.1 + AGP 9.0 |
| Min SDK | 28 (Android 9) |
| Target SDK | 36 |

## Estructura del proyecto

```
app/src/main/java/com/wifield/app/
├── MainActivity.kt
├── WiFieldApplication.kt
├── data/
│   ├── local/
│   │   ├── WiFieldDatabase.kt
│   │   ├── entity/          # ProjectEntity, SnapshotEntity, AccessPointEntity, ActiveTestResultEntity
│   │   └── dao/             # DAOs con Flow reactivo
│   └── repository/          # ProjectRepository, SnapshotRepository
├── domain/
│   ├── analyzer/            # AlertAnalyzer (6 categorias de alerta)
│   └── model/               # WifiModels, Alert, SignalQuality
├── service/
│   ├── WifiScanner.kt       # Escaneo WiFi via WifiManager
│   ├── SpeedTestService.kt  # Test de velocidad HTTP
│   └── PingService.kt       # Latencia, jitter, packet loss
└── ui/
    ├── navigation/           # Rutas de navegacion
    ├── screens/              # 6 pantallas (Home, Scanner, ActiveDiag, Project, SnapshotDetail, Comparator)
    ├── components/           # Componentes reutilizables (SignalGauge, ChannelChart, AlertCard, etc.)
    ├── theme/                # Material 3 dark theme
    └── permission/           # Manejo de permisos runtime
```

## Licencia

Uso privado.
