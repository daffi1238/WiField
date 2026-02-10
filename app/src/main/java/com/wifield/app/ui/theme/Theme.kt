package com.wifield.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = WiFieldPrimary,
    onPrimary = DarkOnBackground,
    primaryContainer = WiFieldPrimaryVariant,
    secondary = WiFieldSecondary,
    onSecondary = DarkOnBackground,
    secondaryContainer = WiFieldSecondaryVariant,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = SignalCritical
)

private val LightColorScheme = lightColorScheme(
    primary = WiFieldPrimary,
    onPrimary = LightSurface,
    primaryContainer = WiFieldPrimary.copy(alpha = 0.12f),
    secondary = WiFieldSecondary,
    onSecondary = LightSurface,
    secondaryContainer = WiFieldSecondary.copy(alpha = 0.12f),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    error = SignalCritical
)

@Composable
fun WiFieldTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
