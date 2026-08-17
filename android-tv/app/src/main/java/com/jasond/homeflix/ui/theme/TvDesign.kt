package com.jasond.homeflix.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object HomeFlixColors {
    val Brand = Color(0xFFE50914)
    val Background = Color(0xFF030B1B)
    val BackgroundLight = Color(0xFF07162B)
    val BackgroundDark = Color(0xFF010510)
    val Surface = Color(0xFF101A2A)
    val SurfaceRaised = Color(0xFF1C293C)
    val TextPrimary = Color(0xFFF7F7F8)
    val TextSecondary = Color(0xFFB8B8BE)
    val Success = Color(0xFF46D369)
}

object TvSpacing {
    val ScreenHorizontal = 56.dp
    val SidebarClearance = 84.dp
    val RowGap = 28.dp
    val CardGap = 16.dp
}

object TvMotion {
    const val FocusMillis = 210
    const val ScreenMillis = 280
    const val HeroCrossfadeMillis = 700
    const val HeroRotationMillis = 10_000L
}
