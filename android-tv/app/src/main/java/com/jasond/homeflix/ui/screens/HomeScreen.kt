package com.jasond.homeflix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import coil.compose.*
import com.jasond.homeflix.data.model.ContinueWatchingItem
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.ui.*
import com.jasond.homeflix.ui.format.formatRuntime

private val NetflixRed = Color(0xFFE50914)
private val CanvasBlack = Color(0xFF070708)

@Composable
fun HomeScreen(onMovieSelected: (Long) -> Unit, viewModel: HomeViewModel) {
    val connection by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF19090B), Color(0xFF0E0E11), CanvasBlack, Color.Black),
            ),
        ),
    ) {
        // Low-contrast ambient light keeps the library cinematic without
        // competing with poster artwork or white text on a television.
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(Color(0x45E50914), Color(0x120F0F12), Color.Transparent),
                    center = Offset.Zero,
                    radius = 920f,
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(Color(0x243D1820), Color.Transparent),
                    center = Offset(1920f, 700f),
                    radius = 1100f,
                ),
            ),
        )
        when (val current = state) {
            HomeUiState.Loading -> MessageState("Loading your library…")
            is HomeUiState.Error -> ErrorState(current.message, viewModel::loadMovies)
            is HomeUiState.Success -> if (current.movies.isEmpty()) EmptyState() else MovieLibrary(
                current.movies, current.continueWatching, searchQuery, onMovieSelected,
            )
        }
        Header(connection, searchQuery, onSearchChanged = { searchQuery = it })
    }
}

@Composable
private fun Header(connection: ConnectionStatus, searchQuery: String, onSearchChanged: (String) -> Unit) {
    var searchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun leaveSearch() {
        keyboard?.hide()
        if (!focusManager.moveFocus(FocusDirection.Down)) {
            focusManager.clearFocus(force = true)
        }
    }
    BackHandler(enabled = searchFocused, onBack = ::leaveSearch)
    Row(
        Modifier.fillMaxWidth().height(76.dp)
            .background(Brush.verticalGradient(listOf(Color(0xE6090909), Color.Transparent)))
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("HOMEFLIX", color = NetflixRed, fontWeight = FontWeight.Black, fontSize = 27.sp,
            letterSpacing = (-1).sp)
        Spacer(Modifier.width(36.dp))
        Text("Home", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(24.dp))
        Text("Movies", fontSize = 16.sp, color = Color(0xFFD2D2D2))
        Spacer(Modifier.width(24.dp))
        Text("My List", fontSize = 16.sp, color = Color(0xFFD2D2D2))
        Spacer(Modifier.weight(1f))
        BasicTextField(
            value = searchQuery, onValueChange = onSearchChanged, singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { leaveSearch() }),
            modifier = Modifier.width(if (searchFocused || searchQuery.isNotBlank()) 260.dp else 190.dp).height(40.dp)
                .onFocusChanged { searchFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                        leaveSearch()
                        true
                    } else {
                        false
                    }
                }
                .background(Color(0xCC111111), RoundedCornerShape(3.dp))
                .border(1.dp, if (searchFocused) Color.White else Color(0xFF777777), RoundedCornerShape(3.dp))
                .padding(horizontal = 13.dp, vertical = 9.dp),
            decorationBox = { input -> Box { if (searchQuery.isBlank()) Text("⌕  Search", color = Color(0xFFBDBDBD), fontSize = 16.sp); input() } },
        )
        Spacer(Modifier.width(20.dp))
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(
            if (connection == ConnectionStatus.CONNECTED) Color(0xFF46D369) else Color(0xFF777777)))
        Spacer(Modifier.width(8.dp))
        Text(if (connection == ConnectionStatus.CONNECTED) "Online" else "Offline", fontSize = 13.sp, color = Color.LightGray)
        Spacer(Modifier.width(20.dp))
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(5.dp)).background(NetflixRed), contentAlignment = Alignment.Center) {
            Text("H", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MovieLibrary(movies: List<Movie>, continueWatching: List<ContinueWatchingItem>, searchQuery: String, onSelected: (Long) -> Unit) {
    val recent = remember(movies) { movies.sortedByDescending { it.dateAdded } }
    val all = remember(movies) { movies.sortedBy { it.title.lowercase() } }
    val categories = remember(movies) { moviesByCategory(movies) }
    val matches = remember(movies, searchQuery) { if (searchQuery.isBlank()) emptyList() else all.filter {
        it.title.contains(searchQuery.trim(), true) || it.genre?.contains(searchQuery.trim(), true) == true || it.year?.toString()?.contains(searchQuery.trim()) == true
    } }
    if (searchQuery.isNotBlank()) {
        LazyColumn(Modifier.fillMaxSize().padding(top = 88.dp), contentPadding = PaddingValues(bottom = 36.dp)) {
            if (matches.isEmpty()) item { MessageState("No titles found for “${searchQuery.trim()}”") }
            else item { MovieRow("Search Results", matches, onSelected, 0) }
        }; return
    }
    val hero = recent.first(); val focus = remember { FocusRequester() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item { Hero(hero, focus) { onSelected(hero.id) } }
        if (continueWatching.isNotEmpty()) item { ContinueWatchingRow(continueWatching, onSelected) }
        item { MovieRow("Recently Added", recent.take(12), onSelected, 80) }
        categories.forEach { (category, categoryMovies) ->
            item { MovieRow(category, categoryMovies, onSelected, 130) }
        }
        item { MovieRow("All Movies", all, onSelected, 180) }
    }
}

@Composable
private fun Hero(movie: Movie, focus: FocusRequester, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(movie.id) {
        visible = true
        // Wait until AnimatedVisibility has mounted the focused button.
        withFrameNanos { }
        withFrameNanos { }
        focus.requestFocus()
    }
    Box(Modifier.fillMaxWidth().height(500.dp).background(Color(0xFF181818))) {
        (movie.backdropUrl ?: movie.posterUrl)?.let {
            AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF090909), Color(0xDA090909), Color(0x22090909), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x56090909), Color.Transparent, CanvasBlack), startY = 0f)))
        AnimatedVisibility(visible, enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 10 }, modifier = Modifier.align(Alignment.CenterStart)) {
            Column(Modifier.padding(start = 58.dp, top = 42.dp).fillMaxWidth(0.52f)) {
                Text("#1 IN YOUR LIBRARY TODAY", color = Color(0xFFD8D8D8), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text(movie.title.uppercase(), fontSize = 48.sp, lineHeight = 50.sp, fontWeight = FontWeight.Black, maxLines = 2, modifier = Modifier.padding(top = 12.dp))
                val facts = listOfNotNull(movie.year?.toString(), movie.quality, formatRuntime(movie.durationSeconds))
                if (facts.isNotEmpty()) Text(facts.joinToString("   •   "), color = Color(0xFFE5E5E5), fontSize = 17.sp, modifier = Modifier.padding(top = 16.dp))
                Text(movie.description ?: "Featured from your personal HomeFlix library.", color = Color(0xFFE1E1E1), fontSize = 18.sp, lineHeight = 25.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 22.dp)) {
                    Button(onClick, Modifier.focusRequester(focus), colors = ButtonDefaults.colors(containerColor = Color.White, contentColor = Color.Black)) { Text("▶  Play", fontWeight = FontWeight.Bold) }
                    Button(onClick, colors = ButtonDefaults.colors(containerColor = Color(0xB35B5B5B), contentColor = Color.White)) { Text("ⓘ  More Info", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingRow(items: List<ContinueWatchingItem>, onSelected: (Long) -> Unit) = Column {
    RowTitle("Continue Watching for Home")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 48.dp, vertical = 14.dp)) {
        itemsIndexed(items, key = { _, it -> it.movie.id }) { index, item -> MovieCard(item.movie, index, (item.progress.progressPercent / 100.0).toFloat()) { onSelected(item.movie.id) } }
    }
}

@Composable
private fun MovieRow(title: String, movies: List<Movie>, onSelected: (Long) -> Unit, revealDelay: Int) = Column(Modifier.padding(bottom = 10.dp)) {
    var visible by remember { mutableStateOf(false) }; LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(visible, enter = fadeIn(tween(450, revealDelay)) + slideInVertically(tween(450, revealDelay)) { it / 4 }) {
        Column { RowTitle(title); LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 48.dp, vertical = 14.dp)) {
            itemsIndexed(movies, key = { _, it -> it.id }) { index, movie -> MovieCard(movie, index, onClick = { onSelected(movie.id) }) }
        } }
    }
}

@Composable
private fun RowTitle(title: String) = Row(
    modifier = Modifier.padding(horizontal = 48.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Box(
        Modifier.width(4.dp).height(24.dp)
            .background(NetflixRed, RoundedCornerShape(2.dp)),
    )
    Spacer(Modifier.width(10.dp))
    Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF5F5F5))
}

@Composable
private fun MovieCard(movie: Movie, index: Int, progress: Float? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.09f else 1f, tween(180), label = "card scale")
    val elevation by animateDpAsState(if (focused) 12.dp else 0.dp, tween(180), label = "card lift")
    Column(Modifier.width(158.dp).scale(scale).padding(vertical = 5.dp)) {
        Card(onClick, Modifier.fillMaxWidth().height(237.dp).onFocusChanged { focused = it.isFocused }
            .then(if (focused) Modifier.border(2.dp, Color.White, RoundedCornerShape(5.dp)) else Modifier),
            colors = CardDefaults.colors(containerColor = Color(0xFF242424)), shape = CardDefaults.shape(RoundedCornerShape(5.dp))) {
            Box(Modifier.fillMaxSize()) {
                Poster(movie.posterUrl, movie.title)
                Box(Modifier.align(Alignment.TopStart).padding(8.dp).background(Color(0xDDE50914), RoundedCornerShape(2.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) { Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Black) }
                progress?.let { Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(5.dp).background(Color(0xFF555555))) { Box(Modifier.fillMaxHeight().fillMaxWidth(it.coerceIn(0f, 1f)).background(NetflixRed)) } }
            }
        }
        Text(movie.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(top = 8.dp).alpha(if (focused) 1f else .88f))
        if (focused) Text(listOfNotNull(movie.year?.toString(), movie.quality).joinToString("  •  "), color = Color(0xFFB3B3B3), fontSize = 12.sp)
        Spacer(Modifier.height(elevation / 4))
    }
}

internal fun moviesByCategory(movies: List<Movie>): Map<String, List<Movie>> = movies
    .flatMap { movie ->
        movie.genre
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.ifEmpty { listOf("More to Explore") }
            .orEmpty()
            .ifEmpty { listOf("More to Explore") }
            .map { category -> category to movie }
    }
    .groupBy({ it.first }, { it.second })
    .mapValues { (_, categoryMovies) -> categoryMovies.sortedBy { it.title.lowercase() } }
    .toSortedMap(String.CASE_INSENSITIVE_ORDER)

@Composable private fun Backdrop(url: String?, description: String) = SubcomposeAsyncImage(url, "$description artwork", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) {
    if (painter.state is AsyncImagePainter.State.Success) SubcomposeAsyncImageContent() else Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF333333), Color(0xFF171717)))), contentAlignment = Alignment.Center) { Text("H", color = NetflixRed, fontSize = 42.sp, fontWeight = FontWeight.Black) }
}

@Composable fun Poster(url: String?, description: String, modifier: Modifier = Modifier) = SubcomposeAsyncImage(url, "$description poster", modifier.fillMaxSize(), contentScale = ContentScale.Crop) { if (painter.state is AsyncImagePainter.State.Success) SubcomposeAsyncImageContent() else Box(Modifier.fillMaxSize().background(Color(0xFF252525)), contentAlignment = Alignment.Center) { Text("H", color = NetflixRed, fontSize = 52.sp, fontWeight = FontWeight.Black) } }
@Composable private fun MessageState(message: String) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message, fontSize = 23.sp) }
@Composable private fun EmptyState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Your HomeFlix library is empty", fontSize = 25.sp) }
@Composable private fun ErrorState(message: String, retry: () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(message, fontSize = 24.sp); Button(retry, Modifier.padding(top = 20.dp)) { Text("Try Again") } } }
