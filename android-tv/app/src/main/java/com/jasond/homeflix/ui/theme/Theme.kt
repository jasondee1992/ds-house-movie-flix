package com.jasond.homeflix.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val colors = darkColorScheme(
    primary = HomeFlixColors.Brand,
    background = HomeFlixColors.Background,
    surface = HomeFlixColors.Surface,
    onPrimary = HomeFlixColors.TextPrimary,
    onBackground = HomeFlixColors.TextPrimary,
    onSurface = HomeFlixColors.TextPrimary,
)

@Composable
fun HomeFlixTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
