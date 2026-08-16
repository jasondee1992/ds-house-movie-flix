package com.jasond.homeflix.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val colors = darkColorScheme(
    primary = Color(0xFFE50914),
    background = Color(0xFF090909),
    surface = Color(0xFF181818),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun HomeFlixTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
