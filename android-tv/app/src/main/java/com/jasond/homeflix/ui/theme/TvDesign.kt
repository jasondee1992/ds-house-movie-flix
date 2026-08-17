package com.jasond.homeflix.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object HomeFlixColors {
    val Brand = Color(0xFFE50914)
    val Background = Color(0xFF050506)
    val BackgroundLight = Color(0xFF111114)
    val BackgroundDark = Color(0xFF010102)
    val Surface = Color(0xFF17171A)
    val SurfaceRaised = Color(0xFF25252A)
    val TextPrimary = Color(0xFFF7F7F8)
    val TextSecondary = Color(0xFFB8B8BE)
    val Success = Color(0xFF46D369)
}

object TvSpacing {
    val ScreenHorizontal = 58.dp
    val SidebarClearance = 58.dp
    val TopSafe = 34.dp
    val BottomSafe = 42.dp
    val NavigationHeight = 64.dp
    val RowGap = 34.dp
    val CardGap = 18.dp
}

object TvMotion {
    const val FocusMillis = 180
    const val ScreenMillis = 220
    const val HeroCrossfadeMillis = 380
    const val FocusSettleMillis = 140L
    const val HeroRotationMillis = 10_000L
}

object TvDimensions {
    val CardRadius = 6.dp
    val FocusBorder = 1.5.dp
    const val FocusScale = 1.07f
    val ControlSize = 54.dp
    val PrimaryControlSize = 64.dp
    const val OverlayOpacity = .86f
}
