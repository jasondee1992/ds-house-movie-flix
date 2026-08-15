package com.jasond.homeflix.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import coil.compose.*
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.ui.*
import com.jasond.homeflix.ui.format.formatRuntime

@Composable
fun HomeScreen(onMovieSelected: (Long) -> Unit, viewModel: HomeViewModel) {
    val connection by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Color(0xFF08080B))) {
        Header(connection)
        when (val current = state) {
            HomeUiState.Loading -> MessageState("Loading your library…")
            is HomeUiState.Error -> ErrorState(current.message, viewModel::loadMovies)
            is HomeUiState.Success -> if (current.movies.isEmpty()) EmptyState() else MovieLibrary(current.movies, onMovieSelected)
        }
    }
}

@Composable
private fun Header(connection: ConnectionStatus) {
    Row(Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("HomeFlix", color = Color(0xFFE50914), fontWeight = FontWeight.Bold, fontSize = 30.sp)
        Spacer(Modifier.weight(1f))
        Text(if (connection == ConnectionStatus.CONNECTED) "● Connected" else "Server unavailable", color = if (connection == ConnectionStatus.CONNECTED) Color(0xFF72D572) else Color.LightGray, fontSize = 14.sp)
        Spacer(Modifier.width(28.dp)); Text("Search", fontSize = 18.sp); Spacer(Modifier.width(24.dp)); Text("Settings", fontSize = 18.sp)
    }
}

@Composable
private fun MovieLibrary(movies: List<Movie>, onSelected: (Long) -> Unit) {
    val recent = remember(movies) { movies.sortedByDescending { it.dateAdded } }
    val all = remember(movies) { movies.sortedBy { it.title.lowercase() } }
    val hero = recent.first()
    val focus = remember { FocusRequester() }
    LaunchedEffect(hero.id) { focus.requestFocus() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { Hero(hero, focus) { onSelected(hero.id) } }
        item { MovieRow("Recently Added", recent.take(12), onSelected) }
        item { Spacer(Modifier.height(18.dp)); MovieRow("All Movies", all, onSelected) }
    }
}

@Composable
private fun Hero(movie: Movie, focus: FocusRequester, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(390.dp).background(Color(0xFF15151C))) {
        if (movie.backdropUrl != null) AsyncImage(model = movie.backdropUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF808080B), Color(0xB008080B), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF08080B)))))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 64.dp).fillMaxWidth(0.58f)) {
            Text(movie.title.uppercase(), fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
            val facts = listOfNotNull(movie.year?.toString(), movie.quality, formatRuntime(movie.durationSeconds))
            if (facts.isNotEmpty()) Text(facts.joinToString("     "), color = Color(0xFFE0E0E0), fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
            Text(movie.description ?: "A movie from your HomeFlix library", color = Color(0xFFD4D4D4), fontSize = 18.sp, modifier = Modifier.padding(top = 18.dp))
            Button(onClick = onClick, modifier = Modifier.padding(top = 22.dp).focusRequester(focus)) { Text("▶  View Details", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MovieRow(title: String, movies: List<Movie>, onSelected: (Long) -> Unit) {
    Column { Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 48.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp), contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp)) {
            itemsIndexed(movies, key = { _, item -> item.id }) { _, movie -> MovieCard(movie) { onSelected(movie.id) } }
        }
    }
}

@Composable
private fun MovieCard(movie: Movie, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "movie focus")
    Column(Modifier.width(150.dp).padding(vertical = 5.dp)) {
        Card(onClick = onClick, modifier = Modifier.size(150.dp, 225.dp).scale(scale).onFocusChanged { focused = it.isFocused }.then(if (focused) Modifier.border(3.dp, Color.White, RoundedCornerShape(9.dp)) else Modifier), colors = CardDefaults.colors(containerColor = Color(0xFF282830)), shape = CardDefaults.shape(RoundedCornerShape(9.dp))) { Poster(movie.posterUrl, movie.title) }
        Text(movie.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 8.dp))
        movie.year?.let { Text(it.toString(), color = Color.LightGray, fontSize = 13.sp) }
    }
}

@Composable
fun Poster(url: String?, description: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(model = url, contentDescription = "$description poster", contentScale = ContentScale.Crop, modifier = modifier.fillMaxSize()) {
        if (painter.state is coil.compose.AsyncImagePainter.State.Success) SubcomposeAsyncImageContent() else Box(Modifier.fillMaxSize().background(Color(0xFF25252D)), contentAlignment = Alignment.Center) { Text("H", color = Color(0xFFE50914), fontSize = 52.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun MessageState(message: String) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message, fontSize = 24.sp) }
@Composable private fun EmptyState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Your HomeFlix library is empty", fontSize = 26.sp) }
@Composable private fun ErrorState(message: String, retry: () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(message, fontSize = 26.sp); Button(retry, Modifier.padding(top = 20.dp)) { Text("Retry") } } }
