package com.gymlog.app.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// PALETA BASE HUNTER (GLOBAL)
// Se mantienen por si se usan en Theme.kt u otros sitios generales
// ==============================================================================
val HunterBlack = Color(0xFF050910) // Fondo principal
val HunterDarkBlue = Color(0xFF0F1623) // Fondo tarjetas
val HunterSurface = Color(0xFF1A2233) // Superficies

// COLORES PRINCIPALES
val HunterPrimary = Color(0xFF2E8FFF) // Azul Eléctrico
val HunterPurple = Color(0xFFD500F9) // Morado Neón
val HunterCyan = Color(0xFF00E5FF) // Cian

// Acentos
val HunterSecondary = Color(0xFFFF3D00) // Naranja
val HunterTextPrimary = Color(0xFFFFFFFF)
val HunterTextSecondary = Color(0xFF94A3B8)

// Rangos
val RankE = Color(0xFFB0BEC5)
val RankD = Color(0xFF90CAF9)
val RankC = Color(0xFF4FC3F7)
val RankB = Color(0xFF2E8FFF)
val RankA = Color(0xFFD500F9)
val RankS = Color(0xFFFFFFFF)

// ==============================================================================
// COLORES ESPECÍFICOS POR PANTALLA / FUNCIONALIDAD (HARDCODEADOS)
// ==============================================================================

object ScreenColors {

    object Global {
        val CalendarBox = Color(0xFF54697E)
        val ErrorRed = Color(0xFFC43232)
    }

    object MainScreen {
        // Basado en 0xFF2E8FFF
        val NeonGlowStart = Color(0xFF2E8FFF).copy(alpha = 0.6f)
        val NeonGlowEnd = Color.Transparent
        // Basado en 0xFF2E8FFF
        val IconBorder = Color(0xFF2E8FFF).copy(alpha = 0.3f)
    }

    object CreateExercise {
        val ImageOverlay = Color.Black.copy(alpha = 0.4f)
        // Basado en 0xFF2E8FFF
        val IconTint = Color(0xFF2E8FFF).copy(alpha = 0.5f)
    }

    object CalendarDetail {
        // Basado en 0xFF2E8FFF
        val DaySelectedBorder = Color(0xFF2E8FFF)
        val DaySwapSourceBorder = Color(0xFFFFC107)
        // Basado en 0xFF2E8FFF (RankB)
        val DayCompletedBg = Color(0xFF2E8FFF).copy(alpha = 0.1f)
        // Basado en 0xFF2E8FFF (RankB)
        val DayCompletedIcon = Color(0xFF2E8FFF)
        // Basado en 0xFF94A3B8
        val DayEmptyDot = Color(0xFF94A3B8).copy(alpha = 0.2f)
        // Basado en 0xFF1A2233
        val MultiSelectBarBg = Color(0xFF1A2233)
    }

    object DaySlotDetail {
        // Basado en 0xFF00E5FF
        val StatusCompleted = Color(0xFF00E5FF)
        // Basado en 0xFFFF3D00
        val StatusPending = Color(0xFFFF3D00)
        // Basado en 0xFF2E8FFF
        val CardBorder = Color(0xFF2E8FFF).copy(alpha = 0.5f)
    }

    object TrainingMode {
        // Basado en 0xFFD500F9
        val TimerRunningText = Color(0xFFD500F9)
        val TimerStoppedText = Color(0xFFFFFFFF)
        // Basado en 0xFFFF3D00
        val TimerButtonStop = Color(0xFFFF3D00)
        // Basado en 0xFF1A2233
        val ProgressTrack = Color(0xFF1A2233)
        // Basado en 0xFFD500F9
        val ActiveSetBg = Color(0xFFD500F9).copy(alpha = 0.1f)
        // Basado en 0xFFD500F9
        val ActiveSetBorder = Color(0xFFD500F9)
    }

    object Timer {
        // Basado en 0xFF94A3B8
        val PickerTextDisabled = Color(0xFF94A3B8).copy(alpha = 0.3f)
        // Basado en 0xFF2E8FFF
        val PickerLabel = Color(0xFF2E8FFF)
    }

    object Backup {
        val ExportColorButton = Color(0x4B0053F9)
        val ExportIconBg = Color(0x4B0053F9)
        val ExportIconTint = Color(0xFF00A2F9)

        val ImportColorButton = Color(0x66FF3D00)
        val ImportIconBg = Color(0x66FF3D00)
        val ImportIconTint = Color(0xFFFF7700)
    }
}