package com.sumi.jamplay.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

private val LightColors = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    primary = LightProgressActive,
    secondary = LightThumb
)

private val DarkColors = darkColorScheme(
    background = JamPlayBackground,
    surface = JamPlaySurface,
    onBackground = JamPlayOnBackground,
    onSurface = JamPlayOnSurface,

    primary = Color.White,
    onPrimary = JamPlayPurple,
    secondary = JamPlayPurpleLight,
    onSecondary = Color.White,

    primaryContainer = JamPlaySurface,
    onPrimaryContainer = Color.White,
    surfaceVariant = JamPlaySurface,
    onSurfaceVariant = JamPlayOnSurface.copy(alpha = 0.7f),
)

@Composable
fun JamPlayTheme(content: @Composable () -> Unit) {

    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content
    )
}