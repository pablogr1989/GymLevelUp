# GymLog - Aplicación de Gestión de Entrenamientos

## Descripción
GymLog es una aplicación Android nativa para gestionar entrenamientos de gimnasio. Permite registrar ejercicios, series, repeticiones y peso, mantener un historial detallado y organizar los ejercicios por grupos musculares.

## Características principales

- ✅ **Gestión de ejercicios** por grupos musculares (Piernas, Espalda, Torso, Bíceps, Tríceps, Hombros)
- ✅ **Registro de estadísticas** (series × repeticiones × peso)
- ✅ **Historial completo** de cada ejercicio con timestamps
- ✅ **Búsqueda y filtrado** de ejercicios
- ✅ **Imágenes opcionales** para cada ejercicio
- ✅ **Almacenamiento 100% local** (sin servidor)
- ✅ **Interfaz moderna** con Material Design 3
- ✅ **Soporte para modo oscuro**
- ✅ **Datos prepoblados** con ejercicios de ejemplo

## Tecnologías utilizadas

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose
- **Base de datos:** Room (SQLite)
- **Arquitectura:** MVVM con Repository Pattern
- **Inyección de dependencias:** Hilt
- **Navegación:** Navigation Compose
- **Gestión de imágenes:** Coil

## Requisitos del sistema

- Android Studio Hedgehog (2023.1.1) o superior
- SDK de Android 34
- Gradle 8.2.0
- Kotlin 1.9.22
- Dispositivo/Emulador con Android 8.0 (API 26) o superior

## Instrucciones de compilación

### 1. Configurar el entorno

1. Instala Android Studio desde [https://developer.android.com/studio](https://developer.android.com/studio)
2. Abre Android Studio y configura el SDK de Android 34

### 2. Clonar/Descargar el proyecto

```bash
# Si tienes el proyecto en un repositorio
git clone [URL_DEL_REPOSITORIO]

# O simplemente copia la carpeta GymLog a tu directorio de proyectos
```

### 3. Abrir el proyecto en Android Studio

1. Abre Android Studio
2. Selecciona "Open" y navega hasta la carpeta GymLog
3. Espera a que se sincronicen las dependencias (puede tomar varios minutos la primera vez)

### 4. Configurar el SDK local

1. Si aparece un error sobre el SDK, ve a `File > Project Structure > SDK Location`
2. Configura la ruta de tu Android SDK
3. O edita el archivo `local.properties` y actualiza la línea:
   ```
   sdk.dir=/ruta/a/tu/Android/Sdk
   ```

### 5. Compilar y ejecutar

#### Opción A: Usando Android Studio

1. Conecta un dispositivo Android con depuración USB habilitada o configura un emulador
2. Haz clic en el botón "Run" (▶️) o presiona Shift+F10
3. Selecciona el dispositivo destino
4. La aplicación se compilará e instalará automáticamente

#### Opción B: Usando la línea de comandos

```bash
# En la raíz del proyecto GymLog

# Para compilar un APK de debug
./gradlew assembleDebug

# El APK se generará en:
# app/build/outputs/apk/debug/app-debug.apk

# Para instalar directamente en un dispositivo conectado
./gradlew installDebug
```

### 6. Generar APK de release (opcional)

1. En Android Studio: `Build > Generate Signed Bundle/APK`
2. Selecciona APK
3. Crea o selecciona un keystore
4. Completa la información de firma
5. Selecciona "release" como build variant
6. El APK firmado se generará en `app/release/`

## Estructura del proyecto

```
GymLog/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/gymlog/app/
│   │       │   ├── data/           # Capa de datos
│   │       │   │   ├── local/      # Room DB, DAOs, Entities
│   │       │   │   └── repository/ # Implementación del repositorio
│   │       │   ├── domain/         # Capa de dominio
│   │       │   │   ├── model/      # Modelos de dominio
│   │       │   │   └── repository/ # Interfaces del repositorio
│   │       │   ├── di/             # Módulos de Hilt
│   │       │   ├── ui/             # Capa de presentación
│   │       │   │   ├── screens/    # Pantallas (Compose)
│   │       │   │   ├── navigation/ # Navegación
│   │       │   │   └── theme/      # Tema y estilos
│   │       │   └── MainActivity.kt # Actividad principal
│   │       └── res/                # Recursos
│   └── build.gradle.kts           # Configuración del módulo
├── build.gradle.kts                # Configuración del proyecto
└── settings.gradle.kts             # Configuración de Gradle
```

## Uso de la aplicación

### Pantalla principal
- Muestra ejercicios agrupados por músculo
- Los grupos son expandibles/colapsables
- Cada ejercicio muestra series × repeticiones y peso actual
- Botón "+" para crear nuevos ejercicios
- Búsqueda por nombre
- Filtrado por grupo muscular

### Crear ejercicio
- Nombre obligatorio
- Descripción opcional
- Grupo muscular obligatorio
- Imagen opcional desde galería
- Valores iniciales opcionales (se crea entrada en historial)

### Detalle del ejercicio
- Vista de información del ejercicio
- Edición de series, repeticiones y peso
- Al guardar se actualiza el ejercicio y se añade al historial
- Lista de historial ordenada por fecha (más reciente primero)
- Posibilidad de eliminar entradas del historial

## Solución de problemas comunes

### Error: SDK location not found
- Configura la ruta del SDK en `local.properties`

### Error: Gradle sync failed
- Verifica tu conexión a internet
- Limpia y reconstruye: `Build > Clean Project`, luego `Build > Rebuild Project`

### La aplicación no se instala
- Habilita "Orígenes desconocidos" en los ajustes del dispositivo
- Verifica que el dispositivo tenga Android 8.0 o superior

### Base de datos vacía
- La aplicación incluye datos de ejemplo que se cargan automáticamente
- Si no aparecen, desinstala y vuelve a instalar la app

## Funcionalidades adicionales implementadas

- ✅ Validación de formularios
- ✅ Animaciones suaves en las transiciones
- ✅ Snackbars para confirmaciones
- ✅ Diálogos de confirmación para acciones destructivas
- ✅ Soporte para imágenes con placeholder
- ✅ Formato de fechas legible en español
- ✅ Teclados numéricos para campos apropiados
- ✅ Estados de carga con indicadores visuales

## Licencia

Este proyecto es de código abierto con fines educativos.

## Contacto

Para preguntas o sugerencias sobre el proyecto, puedes contactar al desarrollador.

---

**Nota:** Esta aplicación almacena todos los datos localmente en el dispositivo. No hay sincronización en la nube ni backup automático. Se recomienda hacer copias de seguridad periódicas desde los ajustes del sistema Android.
