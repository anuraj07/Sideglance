package com.deep.sideglance.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = CreamBg,
    primaryContainer = SageLight,
    onPrimaryContainer = SageDark,
    secondary = Terracotta,
    onSecondary = CreamBg,
    secondaryContainer = TerracottaLight,
    onSecondaryContainer = Charcoal,
    background = CreamBg,
    onBackground = Charcoal,
    surface = SoftSand,
    onSurface = Charcoal,
    surfaceVariant = CardSand,
    onSurfaceVariant = Charcoal,
    outline = OutlineGrey
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkBg,
    primaryContainer = DarkSand,
    onPrimaryContainer = LightText,
    secondary = DarkTerracotta,
    onSecondary = DarkBg,
    secondaryContainer = DarkCard,
    onSecondaryContainer = LightText,
    background = DarkBg,
    onBackground = LightText,
    surface = DarkCard,
    onSurface = LightText,
    surfaceVariant = DarkSand,
    onSurfaceVariant = LightText,
    outline = SoftGrey
)

@Composable
fun SideGlanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}