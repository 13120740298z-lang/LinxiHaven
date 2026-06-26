package com.testimony.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Primary Colors - Calming Teal
val Primary = Color(0xFF00838F)
val PrimaryVariant = Color(0xFF006064)
val OnPrimary = Color.White

// Secondary Colors - Soft Purple
val Secondary = Color(0xFF7E57C2)
val SecondaryVariant = Color(0xFF5E35B1)
val OnSecondary = Color.White

// Background Colors
val Background = Color(0xFFFAFAFA)
val Surface = Color.White
val OnBackground = Color(0xFF1C1B1F)
val OnSurface = Color(0xFF1C1B1F)

// Utility Colors
val Gray = Color(0xFF6B7280)
val SurfaceVariant = Color(0xFFF3F4F6)

// Risk Level Colors
val RiskGreen = Color(0xFF4CAF50)
val RiskYellow = Color(0xFFFFC107)
val RiskRed = Color(0xFFF44336)

// Calculator Theme Colors
val CalculatorBackground = Color(0xFFE0E0E0)
val CalculatorButton = Color.White
val CalculatorOperator = Color(0xFFFF9800)
val CalculatorDisplay = Color(0xFF212121)

// Dark Theme Colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnBackground = Color(0xFFE0E0E0)
val DarkOnSurface = Color(0xFFE0E0E0)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

@Composable
fun TestimonyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
