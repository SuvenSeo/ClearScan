package com.ardeno.clearscan.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = SystemBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EBFF),
    onPrimaryContainer = ClearScanNavy,
    secondary = ClearScanSage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5F0EA),
    onSecondaryContainer = ClearScanNavy,
    tertiary = SystemOrange,
    onTertiary = Color.White,
    error = SystemRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E3),
    onErrorContainer = SystemRed,
    background = SystemBackground,
    onBackground = LabelPrimary,
    surface = SystemGroupedBackground,
    onSurface = LabelPrimary,
    surfaceVariant = SystemSecondaryGrouped,
    onSurfaceVariant = LabelSecondary,
    outline = Separator,
    outlineVariant = Color(0x293C3C43)
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkSystemBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A3A5C),
    onPrimaryContainer = DarkSystemBlue,
    secondary = ClearScanSage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A3D32),
    onSecondaryContainer = Color(0xFFB8D4C4),
    tertiary = SystemOrange,
    onTertiary = Color.Black,
    error = DarkSystemRed,
    onError = Color.White,
    errorContainer = Color(0xFF3D1F1F),
    onErrorContainer = DarkSystemRed,
    background = DarkSystemBackground,
    onBackground = DarkLabelPrimary,
    surface = DarkGroupedBackground,
    onSurface = DarkLabelPrimary,
    surfaceVariant = DarkSecondaryGrouped,
    onSurfaceVariant = DarkLabelSecondary,
    outline = DarkSeparator,
    outlineVariant = Color(0x99545458)
)

@Composable
fun ClearScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = clearScanTypography(),
        shapes = ClearScanShapes,
        content = content
    )
}
