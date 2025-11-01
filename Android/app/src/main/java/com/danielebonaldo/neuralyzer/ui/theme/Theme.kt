package com.danielebonaldo.neuralyzer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Green,
    background = Black,
    surface = Gray,
    onPrimary = Black,
    onBackground = White,
    onSurface = White
)

@Composable
fun NeuralyzerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
