package com.vitalai.ui.theme

import android.app.Activity
import android.graphics.Color.TRANSPARENT
import android.os.Build
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
    primary = Mint500,
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
    primary = Mint500,
    onPrimary = Color.White,
    primaryContainer = Mint50,
    onPrimaryContainer = Mint700,
    secondary = Mint600,
    onSecondary = Color.White,
    secondaryContainer = AppSurface2,
    onSecondaryContainer = Ink700,
    tertiary = MacroCarbs,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = AppBackground,
    onBackground = Ink900,
    surface = AppSurface,
    onSurface = Ink900,
    surfaceVariant = AppSurface2,
    onSurfaceVariant = TextMuted,
    outline = AppLine,
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
            window.statusBarColor = TRANSPARENT
            window.navigationBarColor = TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
