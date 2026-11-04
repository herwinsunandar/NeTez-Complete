package com.herwin.netez.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SoftBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF252A40),
    onPrimaryContainer = SoftBluePrimary,
    secondary = SoftBlueSecondary,
    onSecondary = Color.White,
    tertiary = SoftBlueTertiary,
    background = PremiumNavy,
    surface = SoftDarkBg,
    surfaceVariant = Color(0xFF252A40),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = TextGray,
    outline = Color(0xFF333A52),
    outlineVariant = Color(0xFF2A3045),
    error = SoftTechDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SoftTechPrimary,
    onPrimary = Color.White,
    primaryContainer = SoftTechSurfaceVariant,
    onPrimaryContainer = SoftTechPrimary,
    secondary = SoftTechSecondary,
    onSecondary = Color.White,
    secondaryContainer = SoftTechSecondary.copy(alpha = 0.15f),
    onSecondaryContainer = SoftTechSecondary,
    tertiary = SoftTechTertiary,
    onTertiary = Color.White,
    background = SoftTechBackground,
    onBackground = SoftTechTextPrimary,
    surface = SoftTechSurface,
    onSurface = SoftTechTextPrimary,
    surfaceVariant = SoftTechSurfaceVariant,
    onSurfaceVariant = SoftTechTextSecondary,
    outline = SoftTechBorder,
    outlineVariant = SoftTechBorder.copy(alpha = 0.5f),
    error = SoftTechDanger,
    onError = Color.White
)

@Composable
fun NeTezTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
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
