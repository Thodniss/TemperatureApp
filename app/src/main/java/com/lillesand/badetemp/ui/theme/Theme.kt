package com.lillesand.badetemp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LillesandColorScheme = lightColorScheme(
    primary = LillesandBlue,
    onPrimary = LillesandWhite,
    primaryContainer = LillesandSky,
    onPrimaryContainer = LillesandBlueDark,
    secondary = LillesandOcean,
    onSecondary = LillesandWhite,
    secondaryContainer = LillesandSky,
    onSecondaryContainer = LillesandBlueDark,
    background = LillesandSkyLight,
    onBackground = LillesandText,
    surface = LillesandWhite,
    onSurface = LillesandText,
    surfaceVariant = LillesandSky,
    onSurfaceVariant = LillesandTextSecondary,
    outline = LillesandDivider,
    error = TempHot,
    onError = LillesandWhite
)

private val LillesandDarkColorScheme = darkColorScheme(
    primary = LillesandBlueLight,
    onPrimary = DarkBackground,
    primaryContainer = LillesandBlueDark,
    onPrimaryContainer = LillesandSky,
    secondary = LillesandOcean,
    onSecondary = DarkBackground,
    secondaryContainer = LillesandBlueDark,
    onSecondaryContainer = LillesandSky,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider,
    error = TempHot,
    onError = DarkBackground
)

@Composable
fun LillesandBadetempTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) LillesandDarkColorScheme else LillesandColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            if (darkTheme) {
                window.statusBarColor = DarkBackground.toArgb()
                window.navigationBarColor = DarkBackground.toArgb()
            } else {
                window.statusBarColor = LillesandBlueDark.toArgb()
                window.navigationBarColor = LillesandBlue.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LillesandTypography,
        content = content
    )
}
