package com.ardeno.clearscan.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ClearScanColorScheme: ColorScheme = lightColorScheme(
    primary = Navy,
    onPrimary = White,
    secondary = Sage,
    onSecondary = White,
    tertiary = Amber,
    onTertiary = Ink,
    error = Coral,
    background = Paper,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Cloud,
    onSurfaceVariant = Ink
)

@Composable
fun ClearScanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClearScanColorScheme,
        typography = Typography(),
        content = content
    )
}
