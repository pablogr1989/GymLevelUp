# GymLevelUp (Hunter Edition) - Gestor de Entrenamiento Gamificado

## ⚔️ Descripción
GymLevelUp no es solo un registro de gimnasio; es tu HUD de combate personal. Esta aplicación Android nativa transforma la gestión de entrenamientos en una experiencia gamificada con estética "Hunter" (inspirada en Solo Leveling). Diseñada para funcionar 100% offline, ofrece una interfaz oscura de alto contraste, herramientas de planificación avanzada y un sistema de seguimiento de variantes (Sets) para llevar tu progreso al siguiente nivel.

## ✨ Características Principales

### 🎨 Experiencia de Usuario "Hunter UI"
- **Interfaz Inmersiva:** Diseño *Dark Mode* profundo con acentos en Azul Eléctrico y Morado Neón.
- **Componentes Tácticos:** Tarjetas con efectos de brillo, tipografía futurista y paneles de control tipo videojuego.
- **Feedback Visual:** Barras de progreso de misión, temporizadores digitales gigantes y diálogos de sistema.

### 🏋️ Gestión de Combate (Entrenamiento)
- **Modo HUD:** Pantalla de entrenamiento optimizada con temporizador integrado, control de descanso dinámico y visualización clara del set activo.
- **Sistema de Variantes (Sets):** Configura múltiples variantes para un mismo ejercicio (ej. "Fuerza 5x5" vs "Hipertrofia 4x12") y selecciónalas según el día.
- **Historial Vinculado:** Cada registro de entrenamiento se guarda asociado a la variante específica utilizada.

### 📅 Planificación Estratégica
- **Calendario Dinámico:** Vista mensual para organizar tu rutina.
- **Sistema "Swap":** Reordena tu semana manteniendo pulsado un día y tocando otro para intercambiar sus contenidos al instante.
- **Inventario de Ejercicios:** Asigna ejercicios y variantes específicas a cada *DaySlot* (slot del día).

### 🛡️ Seguridad y Datos
- **Offline First:** Todos los datos viven en tu dispositivo (Room Database).
- **Backup & Restore:** Sistema de copias de seguridad en JSON con **migración automática** de versiones anteriores a la nueva estructura de datos.
- **Persistencia de Imágenes:** Las fotos de los ejercicios se guardan internamente para evitar pérdidas si borras la galería.

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin 100%
- **UI:** Jetpack Compose (Material 3 altamente personalizado)
- **Arquitectura:** MVVM + Clean Architecture
- **Inyección de Dependencias:** Dagger Hilt
- **Base de Datos:** Room (SQLite) con Relaciones 1:N
- **Asincronía:** Coroutines & Kotlin Flows
- **Gestión de Estado:** Patrón *UiState* (Single Source of Truth)
- **Imágenes:** Coil
- **Serialización:** Kotlinx Serialization

## 📱 Estructura del Proyecto

```text
GymLog/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/gymlog/app/
│   │       │   ├── data/           # Capa de Datos (Room, Repositories, Backup)
│   │       │   │   ├── local/      # Entities (Exercise, Set, DaySlot...), DAOs
│   │       │   │   └── repository/ # Implementación de repositorios
│   │       │   ├── domain/         # Capa de Dominio (Modelos puros, Interfaces)
│   │       │   ├── di/             # Módulos Hilt
│   │       │   ├── ui/             # Capa de Presentación (Compose)
│   │       │   │   ├── screens/    # Pantallas (Main, Detail, Training, Calendar...)
│   │       │   │   ├── theme/      # Sistema de Diseño Hunter (Color, Type, Components)
│   │       │   │   └── navigation/ # Grafo de navegación
│   │       │   └── util/           # Helpers (Validación, Imágenes, Constantes)
│   │       └── res/                # Recursos (Iconos vectoriales, Strings)