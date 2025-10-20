package com.sumi.flowplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.runtime.SideEffect

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

private val DarkColorScheme = darkColorScheme(
    primary = Purple500,
    onPrimary = LightText,
    secondary = Teal200,
    onSecondary = LightText,
    background = DarkBg,
    onBackground = LightText,
    surface = DarkBg,
    onSurface = LightText
)

@Composable
fun MusicAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val systemUiController = rememberSystemUiController()

    if (darkTheme) {
        SideEffect {
            // 상태바 색상 (배경과 동일하게)
            systemUiController.setStatusBarColor(
                color = colorScheme.background,
                darkIcons = false
            )
            // 하단 내비게이션 바 색상 (배경과 동일하게)
            systemUiController.setNavigationBarColor(
                color = colorScheme.background,
                darkIcons = false
            )
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}