package com.inferra.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ExecutiveDarkColorScheme = darkColorScheme(
    primary = KleinBlueSecondary,
    onPrimary = CrispTextPrimary,
    primaryContainer = ElevatedSurfaceDark,
    onPrimaryContainer = CrispTextPrimary,
    secondary = AccentMuted,
    onSecondary = CrispTextPrimary,
    tertiary = SageGreenDark,
    background = ObsidianBg,
    onBackground = CrispTextPrimary,
    surface = SlateSurface,
    onSurface = CrispTextPrimary,
    surfaceVariant = ElevatedSurfaceDark,
    onSurfaceVariant = CrispTextSecondary,
    outline = BorderDark,
    outlineVariant = BorderDark,
)

private val ExecutiveLightColorScheme = lightColorScheme(
    primary = KleinBluePrimary,
    onPrimary = PorcelainSurface,
    primaryContainer = ElevatedSurfaceLight,
    onPrimaryContainer = CharcoalTextPrimary,
    secondary = AccentMuted,
    onSecondary = PorcelainSurface,
    tertiary = SageGreen,
    background = PaperWhiteBg,
    onBackground = CharcoalTextPrimary,
    surface = PorcelainSurface,
    onSurface = CharcoalTextPrimary,
    surfaceVariant = ElevatedSurfaceLight,
    onSurfaceVariant = SlateTextSecondary,
    outline = BorderLight,
    outlineVariant = BorderLight,
)

@Composable
fun InferraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ExecutiveDarkColorScheme else ExecutiveLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
