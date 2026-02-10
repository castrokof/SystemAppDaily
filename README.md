# SystemAppDaily

App Android para tomar lecturas diarias de macromedidores y enviar al sistema principal.

## Funcionalidades

- **Login**: Autenticación contra la API existente (`loginMovil1`)
- **Lista de macromedidores**: Muestra los macros asignados al usuario con estado de lecturas del día
- **Toma de lectura**: Permite ingresar valor del medidor + mínimo 2 fotos obligatorias
- **Límite diario**: Máximo 2 lecturas por día por macro, ampliable por autorización desde la web
- **Almacenamiento local**: Las lecturas se guardan localmente si no hay conexión (Room DB)
- **Cámara integrada**: Captura de fotos con CameraX

## Arquitectura

- **Lenguaje**: Kotlin
- **Patrón**: MVVM (Model-View-ViewModel)
- **Red**: Retrofit + OkHttp
- **BD Local**: Room
- **Cámara**: CameraX
- **UI**: Material Design 3 con ViewBinding

## Configuración

1. Abrir en Android Studio
2. La URL base de la API se configura en `app/build.gradle` → `buildConfigField "API_BASE_URL"`
3. Ajustar los campos de `LoginResponse.kt` según la respuesta real de tu API `loginMovil1`

## API

La app usa un endpoint existente (login) y 3 nuevos que debes crear en tu Laravel.
Ver **[ENDPOINTS_LARAVEL.md](ENDPOINTS_LARAVEL.md)** para la documentación completa de endpoints y tablas de BD sugeridas.

## Estructura del proyecto

```
app/src/main/java/com/systemapp/daily/
├── SystemAppDaily.kt          # Application class
├── data/
│   ├── api/
│   │   ├── ApiService.kt      # Definición de endpoints Retrofit
│   │   └── RetrofitClient.kt  # Singleton HTTP client
│   ├── local/
│   │   ├── AppDatabase.kt     # Room database
│   │   └── LecturaDao.kt      # Data Access Object
│   ├── model/
│   │   ├── Lectura.kt         # Modelo de lectura
│   │   ├── LoginRequest.kt    # Request de login
│   │   ├── LoginResponse.kt   # Response de login
│   │   └── Macro.kt           # Modelo de macromedidor
│   └── repository/
│       ├── AuthRepository.kt
│       ├── LecturaRepository.kt
│       └── MacroRepository.kt
├── ui/
│   ├── home/
│   │   ├── HomeActivity.kt    # Lista de macromedidores
│   │   ├── HomeViewModel.kt
│   │   └── MacroAdapter.kt
│   ├── lectura/
│   │   ├── CameraActivity.kt  # Captura de fotos CameraX
│   │   ├── FotoAdapter.kt
│   │   ├── LecturaActivity.kt # Toma de lectura
│   │   └── LecturaViewModel.kt
│   └── login/
│       ├── LoginActivity.kt
│       └── LoginViewModel.kt
└── utils/
    ├── Constants.kt
    ├── NetworkResult.kt
    └── SessionManager.kt
```
