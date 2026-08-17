package com.jasond.homeflix.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.jasond.homeflix.R
import com.jasond.homeflix.ui.theme.HomeFlixColors
import com.jasond.homeflix.ui.theme.TvMotion
import com.jasond.homeflix.ui.theme.TvSpacing

data class NavigationItem(val route: String, val icon: TvIcon, val label: String)

val HomeFlixNavigationItems = listOf(
    NavigationItem("home", TvIcon.HOME, "Home"),
    NavigationItem("movies", TvIcon.MOVIES, "Movies"),
    NavigationItem("my-list", TvIcon.BOOKMARK, "My List"),
    NavigationItem("search", TvIcon.SEARCH, "Search"),
    NavigationItem("settings", TvIcon.SETTINGS, "Settings"),
)

/** Kept under the original function name so screen call sites remain source compatible. */
@Composable
fun SidebarNavigation(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    onExitToContent: () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().height(TvSpacing.NavigationHeight).zIndex(20f)
            .background(Brush.verticalGradient(listOf(Color(0xF2050506), Color(0xC9050506), Color.Transparent)))
            .padding(start = TvSpacing.ScreenHorizontal, end = TvSpacing.ScreenHorizontal, top = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(painterResource(R.drawable.ds_app_icon), "DS Cinema", Modifier.size(42.dp))
        Spacer(Modifier.width(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            HomeFlixNavigationItems.forEach { item ->
                NavigationButton(item, item.route == selectedRoute, onNavigate, onExitToContent)
            }
        }
    }
}

@Composable
private fun NavigationButton(
    item: NavigationItem,
    selected: Boolean,
    onNavigate: (String) -> Unit,
    onExitToContent: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var initialFocusRequested by rememberSaveable(item.route) { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    LaunchedEffect(selected) {
        if (selected && !initialFocusRequested) {
            withFrameNanos { }
            requester.requestFocus()
            initialFocusRequested = true
        }
    }
    val width by animateDpAsState(if (focused) 126.dp else 48.dp, tween(TvMotion.FocusMillis), label = "nav width")
    Box(contentAlignment = Alignment.BottomCenter) {
        Button(
            onClick = { onNavigate(item.route) },
            modifier = Modifier.width(width).height(44.dp)
                .then(if (selected) Modifier.focusRequester(requester) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.DirectionDown && event.type == KeyEventType.KeyDown) {
                        onExitToContent(); true
                    } else false
                },
            colors = ButtonDefaults.colors(
                containerColor = if (focused) Color(0xD9343439) else Color.Transparent,
                contentColor = Color.White,
                focusedContainerColor = Color(0xEE343439),
                focusedContentColor = Color.White,
            ),
            shape = ButtonDefaults.shape(shape = RoundedCornerShape(5.dp)),
            contentPadding = PaddingValues(horizontal = 11.dp),
        ) {
            TvVectorIcon(item.icon, Modifier.size(23.dp), if (selected) HomeFlixColors.Brand else Color.White)
            AnimatedVisibility(
                visible = focused,
                enter = fadeIn(tween(TvMotion.FocusMillis)),
                exit = fadeOut(tween(120)),
            ) {
                Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, modifier = Modifier.padding(start = 9.dp))
            }
        }
        if (selected) Box(Modifier.padding(bottom = 1.dp).width(if (focused) 44.dp else 20.dp).height(2.dp)
            .background(HomeFlixColors.Brand, RoundedCornerShape(1.dp)))
    }
}
