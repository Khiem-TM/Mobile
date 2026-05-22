package com.vitalai.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Teal500,
    onPrimary = Color.White,
    primaryContainer = Mint700,
    onPrimaryContainer = Mint100,
    secondary = Mint500,
    onSecondary = Color.White,
    secondaryContainer = Mint900,
    onSecondaryContainer = Mint100,
    tertiary = MacroCarbs,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = AmberContainer,
    background = GreenDark,
    onBackground = GreenLight,
    surface = GreenDark,
    onSurface = GreenLight,
    surfaceVariant = Mint700,
    onSurfaceVariant = Mint200,
    outline = Mint700,
    error = Color(0xFFFF6B6B),
    errorContainer = ErrorContainerDark,
    onError = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary = Teal500,
    onPrimary = Color.White,
    primaryContainer = Mint50,
    onPrimaryContainer = Mint700,
    secondary = GreenMid,
    onSecondary = Color.White,
    secondaryContainer = Ink100,
    onSecondaryContainer = Ink700,
    tertiary = MacroCarbs,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = AppBackground,
    onBackground = TextBody,
    surface = CardBackground,
    onSurface = TextBody,
    surfaceVariant = Mint50,
    onSurfaceVariant = TextMuted,
    outline = BorderColor,
    error = MacroProtein,
    errorContainer = ErrorContainerLight,
    onError = Color.White,
)

@Composable
fun VitalAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
