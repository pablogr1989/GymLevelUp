package com.gymlog.app.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// PALETA BASE HUNTER (GLOBAL)
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
// COLORES ESPECÍFICOS POR PANTALLA / FUNCIONALIDAD
// ==============================================================================

object ScreenColors {

    object Global {
        val CalendarBox = Color(0xFF54697E)
        val ErrorRed = Color(0xFFC43232)
    }

    object MainScreen {
        val NeonGlowStart = Color(0xFF2E8FFF).copy(alpha = 0.6f)
        val NeonGlowEnd = Color.Transparent
        val IconBorder = Color(0xFF2E8FFF).copy(alpha = 0.3f)
    }

    object CreateExercise {
        val ImageOverlay = Color.Black.copy(alpha = 0.4f)
        val IconTint = Color(0xFF2E8FFF).copy(alpha = 0.5f)
    }

    object CalendarDetail {
        val DaySelectedBorder = Color(0xFF2E8FFF)
        val DaySwapSourceBorder = Color(0xFFFFC107)
        val DayCompletedBg = Color(0xFF2E8FFF).copy(alpha = 0.1f)
        val DayCompletedIcon = Color(0xFF2E8FFF)
        val DayEmptyDot = Color(0xFF94A3B8).copy(alpha = 0.2f)
        val MultiSelectBarBg = Color(0xFF1A2233)
    }

    object DaySlotDetail {
        val StatusCompleted = Color(0xFF00E5FF)
        val StatusPending = Color(0xFFFF3D00)
        val CardBorder = Color(0xFF2E8FFF).copy(alpha = 0.5f)

        val InitTrainingColorButton = Color(0xFF2E8FFF)
    }

    object TrainingMode {
        // --- Top Bar ---
        val TitleTextRunning = Color(0xFF2E8FFF) // Azul Eléctrico
        val TitleTextDefault = Color(0xFFFFFFFF)
        val TopBarIcon = Color(0xFFFFFFFF)

        // --- Objective Card ---
        val ObjectiveTitle = Color(0xFF2E8FFF) // Azul Eléctrico
        val ObjectiveEmptyText = Color(0xFF94A3B8)
        val ObjectiveItemBgCurrent = Color(0xFF2E8FFF).copy(alpha = 0.1f)
        val ObjectiveItemBgDefault = Color.Transparent
        val ObjectiveDotCurrent = Color(0xFF2E8FFF)
        val ObjectiveDotDone = Color(0xFF00E5FF) // Cian (Completado)
        val ObjectiveDotPending = Color(0xFF94A3B8)
        val ObjectiveTextDone = Color(0xFF94A3B8)
        val ObjectiveTextDefault = Color(0xFFFFFFFF)
        val ObjectiveCheckIcon = Color(0xFF00E5FF)

        // --- Rest Config ---
        val RestConfigLabel = Color(0xFF94A3B8)
        val RestConfigValue = Color(0xFF2E8FFF) // Azul Eléctrico
        val RestConfigBtnBg = Color(0xFF1A2233)
        val RestConfigBtnContent = Color(0xFFFFFFFF)

        // --- Main Buttons ---
        val MainBtnActiveBg = Color(0xFF2E8FFF) // Azul Eléctrico
        val MainBtnDefaultBg = Color(0xFF00E5FF) // Cian Neón (Para diferenciar estado inicial)
        val MainBtnText = Color(0xFFFFFFFF)

        // --- Progress ---
        val ProgressLabel = Color(0xFF2E8FFF)
        val ProgressValue = Color(0xFFFFFFFF)
        val ProgressIndicator = Color(0xFF2E8FFF)
        val ProgressTrack = Color(0xFF1A2233)

        // --- Alarm Button ---
        val AlarmBtnGradientStart = Color(0xFF2E8FFF) // Azul Eléctrico
        val AlarmBtnGradientEnd = Color(0xFF00E5FF) // Cian
        val AlarmBtnContent = Color(0xFFFFFFFF)

        // --- HUD Card General ---
        val HudCardBg = Color(0xFF1A2233)
        val HudCardBorder = Color(0xFF2E8FFF).copy(alpha = 0.5f)
        val HudTitle = Color(0xFFFFFFFF)
        val HudMuscleGroup = Color(0xFF00E5FF) // Cian para resaltar sobre el título
        val HudImageBorder = Color(0xFF2E8FFF).copy(alpha = 0.3f)
        val HudDivider = Color(0xFF2E8FFF).copy(alpha = 0.2f)

        // --- Active Set Row ---
        val ActiveSetBg = Color(0xFF2E8FFF).copy(alpha = 0.1f)
        val ActiveSetBorder = Color(0xFF2E8FFF)
        val ActiveSetLabel = Color(0xFF2E8FFF)
        val ActiveSetValue = Color(0xFFFFFFFF)
        val ActiveSetSubLabel = Color(0xFF94A3B8)

        // --- Other Sets (Inactive) ---
        val OtherSetsBg = Color(0xFF050910)
        val OtherSetsBorder = Color(0xFF2E8FFF).copy(alpha = 0.2f)
        val OtherSetsLabel = Color(0xFF94A3B8)
        val OtherSetsValue = Color(0xFF94A3B8)

        // --- Notes Input ---
        val NoteInputLabel = Color(0xFF94A3B8)
        val NoteInputFocusedBorder = Color(0xFF2E8FFF)
        val NoteInputUnfocusedBorder = Color(0xFF2E8FFF).copy(alpha = 0.3f)
        val NoteInputCursor = Color(0xFF2E8FFF)

        // --- Control Panel (Series) ---
        val ControlPanelBg = Color(0xFF050910)
        val ControlPanelBorder = Color(0xFF2E8FFF).copy(alpha = 0.2f)
        val ControlPanelLabel = Color(0xFF94A3B8)
        val ControlPanelValue = Color(0xFFFFFFFF)
        val ControlPanelBtnRunningBg = Color(0xFF1A2233)
        val ControlPanelBtnStoppedBg = Color(0xFF2E8FFF) // Azul Eléctrico
        val ControlPanelBtnText = Color(0xFFFFFFFF)

        // --- Timer Section ---
        // Cambiado de marrón/rojizo a un azul muy oscuro para mantener la atmósfera fría
        val TimerSectionBg = Color(0xFF0F1623).copy(alpha = 0.5f)
        val TimerDescLabel = Color(0xFF00E5FF) // Cian
        val TimerAdjustBtnBg = Color(0xFF1A2233)
        val TimerAdjustBtnContent = Color(0xFFFFFFFF)
        val TimerRunningText = Color(0xFF2E8FFF) // Azul Eléctrico
        val TimerStoppedText = Color(0xFFFFFFFF)
        val TimerControlBtnBg = Color(0xFF1A2233)
        val TimerControlBtnIcon = Color(0xFFFFFFFF)
        val TimerButtonStop = Color(0xFF9559BD) // Mantenemos Naranja para STOP/Peligro
        val StopAlarmBtnBg = Color(0xFF8867B4)
        val StopAlarmBtnContent = Color(0xFFFFFFFF)

        // --- Next Button ---
        val NextBtnBg = Color(0xFF2E8FFF) // Azul Eléctrico
        val NextBtnText = Color(0xFFFFFFFF)

        // --- Exercise Image Placeholder ---
        val ExImageBg = Color(0xFF050910)
        val ExImageBorder = Color(0xFF2E8FFF).copy(alpha = 0.5f)
        val ExImageGlowStart = Color(0xFF2E8FFF).copy(alpha = 0.5f)
        val ExImageGlowEnd = Color.Transparent
        val ExImageIcon = Color(0xFF2E8FFF)
    }

    object Timer {
        val PickerTextDisabled = Color(0xFF94A3B8).copy(alpha = 0.3f)
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


