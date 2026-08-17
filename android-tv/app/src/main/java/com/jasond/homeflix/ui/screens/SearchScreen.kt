package com.jasond.homeflix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.ui.HomeUiState
import com.jasond.homeflix.ui.HomeViewModel
import com.jasond.homeflix.ui.components.MoviePosterCard
import com.jasond.homeflix.ui.components.SidebarNavigation
import com.jasond.homeflix.ui.theme.HomeFlixColors
import com.jasond.homeflix.ui.theme.TvSpacing

@Composable
fun SearchScreen(
    viewModel: HomeViewModel,
    onMovieSelected: (Long) -> Unit,
    onNavigate: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val movies = (state as? HomeUiState.Success)?.movies.orEmpty()
    var query by rememberSaveable { mutableStateOf("") }
    val contentFocus = remember { FocusRequester() }
    val results = remember(movies, query) {
        val needle = query.trim()
        if (needle.isBlank()) emptyList() else movies.filter { movie ->
            movie.title.contains(needle, true) ||
                movie.genre?.contains(needle, true) == true ||
                movie.year?.toString()?.contains(needle) == true
        }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(HomeFlixColors.BackgroundLight, HomeFlixColors.Background, HomeFlixColors.BackgroundDark)))) {
        Column(Modifier.fillMaxSize().padding(start = 104.dp, top = 52.dp, end = 48.dp)) {
            Text("Search HomeFlix", fontSize = 34.sp, fontWeight = FontWeight.Black)
            SearchField(query, { query = it }, contentFocus)
            when {
                query.isBlank() -> SearchPrompt()
                results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No matches found", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("Try a title, genre, or year.", color = HomeFlixColors.TextSecondary,
                            fontSize = 17.sp, modifier = Modifier.padding(top = 9.dp))
                    }
                }
                else -> {
                    Text("${results.size} result${if (results.size == 1) "" else "s"}",
                        color = HomeFlixColors.TextSecondary, fontSize = 16.sp,
                        modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(166.dp),
                        horizontalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 52.dp),
                    ) {
                        items(results, key = { it.id }) { movie ->
                            MoviePosterCard(movie, { onMovieSelected(movie.id) })
                        }
                    }
                }
            }
        }
        SidebarNavigation("search", onNavigate, onExitToContent = { contentFocus.requestFocus() })
    }
}

@Composable
private fun SearchField(value: String, onValueChanged: (String) -> Unit, requester: FocusRequester) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    fun leaveInput() {
        keyboard?.hide()
        if (!focusManager.moveFocus(FocusDirection.Down)) focusManager.clearFocus(force = true)
    }
    BackHandler(enabled = focused, onBack = ::leaveInput)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        requester.requestFocus()
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChanged,
        singleLine = true,
        textStyle = TextStyle(color = HomeFlixColors.TextPrimary, fontSize = 22.sp),
        cursorBrush = SolidColor(HomeFlixColors.Brand),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { leaveInput() }),
        modifier = Modifier.fillMaxWidth(.62f).padding(top = 16.dp).height(58.dp)
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                    leaveInput(); true
                } else false
            }
            .background(Color(0xE625252B), RoundedCornerShape(7.dp))
            .border(2.dp, if (focused) Color.White else Color(0xFF5A5A62), RoundedCornerShape(7.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        decorationBox = { input ->
            Box { if (value.isBlank()) Text("Search movies, genres, or years",
                color = HomeFlixColors.TextSecondary, fontSize = 20.sp); input() }
        },
    )
}

@Composable
private fun SearchPrompt() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Find something to watch", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Search your personal library using the TV keyboard.", color = HomeFlixColors.TextSecondary,
                fontSize = 17.sp, modifier = Modifier.padding(top = 9.dp))
        }
    }
}

@Composable
fun LibraryGridScreen(
    route: String,
    title: String,
    movies: List<Movie>,
    onMovieSelected: (Long) -> Unit,
    onNavigate: (String) -> Unit,
) {
    val contentFocus = remember { FocusRequester() }
    Box(Modifier.fillMaxSize().background(HomeFlixColors.Background)) {
        Column(Modifier.fillMaxSize().padding(start = 104.dp, top = 52.dp, end = 48.dp)) {
            Text(title, fontSize = 34.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 26.dp))
            if (movies.isEmpty()) CenteredState("Nothing here yet", "This section will use your existing library data.")
            else LazyVerticalGrid(
                columns = GridCells.Adaptive(166.dp),
                horizontalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                contentPadding = PaddingValues(bottom = 52.dp),
            ) {
                itemsIndexed(movies, key = { _, movie -> movie.id }) { index, movie ->
                    MoviePosterCard(
                        movie,
                        { onMovieSelected(movie.id) },
                        modifier = if (index == 0) Modifier.focusRequester(contentFocus) else Modifier,
                    )
                }
            }
        }
        SidebarNavigation(route, onNavigate, onExitToContent = { contentFocus.requestFocus() })
    }
}
