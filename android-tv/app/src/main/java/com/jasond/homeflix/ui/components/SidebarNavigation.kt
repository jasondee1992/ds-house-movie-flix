package com.jasond.homeflix.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.jasond.homeflix.ui.theme.HomeFlixColors
import com.jasond.homeflix.ui.theme.TvMotion

data class SidebarItem(val route: String, val glyph: String, val label: String)

val HomeFlixSidebarItems = listOf(
    SidebarItem("home", "H", "Home"),
    SidebarItem("search", "S", "Search"),
    SidebarItem("movies", "M", "Movies"),
    SidebarItem("my-list", "+", "My List"),
    SidebarItem("settings", "*", "Settings"),
)

@Composable
fun SidebarNavigation(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(if (expanded) 238.dp else 76.dp, tween(TvMotion.FocusMillis), label = "sidebar")
    BackHandler(enabled = expanded) { expanded = false }
    Column(
        modifier.width(width).fillMaxHeight().zIndex(10f)
            .onFocusChanged { expanded = it.hasFocus }
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xF2070708), Color(0xE6070708), Color(0x00070708)),
                ),
            )
            .padding(start = 12.dp, end = if (expanded) 22.dp else 12.dp, top = 34.dp, bottom = 30.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(48.dp)) {
            Box(
                Modifier.size(42.dp).background(HomeFlixColors.Brand, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("H", fontSize = 24.sp, fontWeight = FontWeight.Black) }
            if (expanded) Text("HOMEFLIX", fontWeight = FontWeight.Black, fontSize = 19.sp,
                modifier = Modifier.padding(start = 12.dp))
        }
        Spacer(Modifier.weight(1f))
        HomeFlixSidebarItems.forEach { item ->
            val selected = item.route == selectedRoute
            Button(
                onClick = { onNavigate(item.route) },
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(vertical = 2.dp),
                colors = ButtonDefaults.colors(
                    containerColor = if (selected) Color(0xCC34343A) else Color.Transparent,
                    contentColor = HomeFlixColors.TextPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Box(Modifier.width(30.dp), contentAlignment = Alignment.CenterStart) {
                    Text(item.glyph, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = if (selected) HomeFlixColors.Brand else HomeFlixColors.TextPrimary)
                }
                if (expanded) Text(item.label, fontSize = 17.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 10.dp))
            }
        }
        Spacer(Modifier.weight(1.15f))
        if (expanded) Text("Personal streaming", fontSize = 12.sp, color = HomeFlixColors.TextSecondary,
            modifier = Modifier.padding(start = 12.dp))
    }
}
