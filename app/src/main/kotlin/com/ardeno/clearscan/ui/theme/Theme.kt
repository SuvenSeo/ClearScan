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
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = ClearScanSage,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = ClearScanWarm,
    onTertiary = Color.White,
    error = SystemRed,
    onError = Color.White,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = SystemBackground,
    onBackground = LabelPrimary,
    surface = SystemGroupedBackground,
    onSurface = LabelPrimary,
    surfaceVariant = SystemSecondaryGrouped,
    onSurfaceVariant = LabelSecondary,
    outline = Separator,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkSystemBlue,
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkClearScanSage,
    onSecondary = Color(0xFF1A354F),
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkClearScanWarm,
    onTertiary = Color(0xFF1A1A1A),
    error = DarkSystemRed,
    onError = Color.White,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkSystemBackground,
    onBackground = DarkLabelPrimary,
    surface = DarkGroupedBackground,
    onSurface = DarkLabelPrimary,
    surfaceVariant = DarkSecondaryGrouped,
    onSurfaceVariant = DarkLabelSecondary,
    outline = DarkSeparator,
    outlineVariant = DarkOutlineVariant
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
