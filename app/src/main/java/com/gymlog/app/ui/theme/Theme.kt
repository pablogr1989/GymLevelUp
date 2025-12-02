package com.gymlog.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


object AppColors
{
    object Global
    {
        val CalendarBox = Color(0xFF54697E)
    }
}


// Definimos solo el esquema oscuro (Dark Scheme) ya que la estética Hunter es dark-only
private val HunterColorScheme = darkColorScheme(
    primary = HunterPrimary,
    onPrimary = Color.White,
    primaryContainer = HunterSurface,
    onPrimaryContainer = HunterPrimary,

    secondary = HunterSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3E2723), // Un tono rojizo oscuro para contenedores secundarios
    onSecondaryContainer = HunterSecondary,

    background = HunterBlack,
    onBackground = HunterTextPrimary,

    surface = HunterDarkBlue,
    onSurface = HunterTextPrimary,

    surfaceVariant = HunterSurface,
    onSurfaceVariant = HunterTextSecondary,

    outline = HunterPrimary.copy(alpha = 0.5f) // Bordes brillantes sutiles
)

@Composable
fun GymLogTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = HunterColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Barra de estado del mismo color que el fondo para inmersión total
            window.statusBarColor = HunterBlack.toArgb()
            window.navigationBarColor = HunterBlack.toArgb()

            // Iconos de la barra de estado claros (porque el fondo es oscuro)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Usaremos la tipografía por defecto por ahora, luego podemos meter fuentes custom
        content = content
    )
}
