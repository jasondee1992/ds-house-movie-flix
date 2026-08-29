package com.jasond.homeflix.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.ui.HomeUiState
import com.jasond.homeflix.ui.HomeViewModel
import com.jasond.homeflix.ui.components.LoadingPosterCard
import com.jasond.homeflix.ui.components.MovieRow
import com.jasond.homeflix.ui.components.SidebarNavigation
import com.jasond.homeflix.ui.components.TvIcon
import com.jasond.homeflix.ui.components.TvVectorIcon
import com.jasond.homeflix.ui.format.formatRuntime
import com.jasond.homeflix.ui.theme.HomeFlixColors
import com.jasond.homeflix.ui.theme.TvMotion
import com.jasond.homeflix.ui.theme.TvSpacing
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onMovieSelected: (Long) -> Unit,
    onPlayMovie: (Long) -> Unit,
    myListIds: Set<Long>,
    onToggleMyList: (Long) -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contentFocus = remember { FocusRequester() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        CinematicBackground()
        when (val current = state) {
            HomeUiState.Loading -> HomeLoading()
            is HomeUiState.Error -> HomeError(viewModel::loadMovies)
            is HomeUiState.Success -> if (current.movies.isEmpty()) HomeEmpty() else HomeLibrary(
                movies = current.movies,
                continueWatching = current.continueWatching.associate { item ->
                    item.movie.id to (item.progress.progressPercent / 100.0).toFloat()
                },
                heroHeight = maxHeight * .95f,
                contentFocus = contentFocus,
                onMovieSelected = onMovieSelected,
                onPlayMovie = onPlayMovie,
                myListIds = myListIds,
                onToggleMyList = onToggleMyList,
            )
        }
        SidebarNavigation("home", onNavigate, onExitToContent = { contentFocus.requestFocus() })
    }
}

@Composable
private fun HomeLibrary(
    movies: List<Movie>,
    continueWatching: Map<Long, Float>,
    heroHeight: Dp,
    contentFocus: FocusRequester,
    onMovieSelected: (Long) -> Unit,
    onPlayMovie: (Long) -> Unit,
    myListIds: Set<Long>,
    onToggleMyList: (Long) -> Unit,
) {
    val recent = remember(movies) { movies.sortedByDescending { it.dateAdded } }
    val categories = remember(movies) { moviesByCategory(movies) }
    val continueMovies = remember(movies, continueWatching) {
        continueWatching.keys.mapNotNull { id -> movies.firstOrNull { it.id == id } }
    }
    var focusedCandidate by rememberSaveable { mutableLongStateOf(recent.first().id) }
    var featuredId by rememberSaveable { mutableLongStateOf(recent.first().id) }
    val featured = movies.firstOrNull { it.id == featuredId } ?: recent.first()
    val context = LocalContext.current
    LaunchedEffect(focusedCandidate) {
        delay(TvMotion.FocusSettleMillis)
        featuredId = focusedCandidate
    }
    LaunchedEffect(featuredId, movies) {
        val index = movies.indexOfFirst { it.id == featuredId }.coerceAtLeast(0)
        movies.subList(index, minOf(index + 3, movies.size)).forEach { candidate ->
            (candidate.backdropUrl ?: candidate.posterUrl)?.let { url ->
                context.imageLoader.enqueue(ImageRequest.Builder(context).data(url).size(1280, 720).build())
            }
        }
    }
    val onFocused: (Movie) -> Unit = { focusedCandidate = it.id }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 64.dp),
    ) {
        item { DynamicHero(featured, heroHeight, contentFocus, onMovieSelected, onPlayMovie,
            featured.id in myListIds, onToggleMyList) }
        if (continueMovies.isNotEmpty()) item {
            MovieRow("Continue Watching", continueMovies, onMovieSelected,
                Modifier.padding(start = TvSpacing.SidebarClearance), continueWatching, onFocused)
        }
        item { MovieRow("Recently Added", recent.take(14), onMovieSelected,
            Modifier.padding(start = TvSpacing.SidebarClearance), onMovieFocused = onFocused) }
        categories.forEach { (category, items) ->
            item(key = "category-$category") {
                MovieRow(category, items, onMovieSelected, Modifier.padding(start = TvSpacing.SidebarClearance),
                    onMovieFocused = onFocused)
            }
        }
    }
}

@Composable
private fun DynamicHero(
    movie: Movie,
    height: Dp,
    playFocus: FocusRequester,
    onMoreInfo: (Long) -> Unit,
    onPlay: (Long) -> Unit,
    isInMyList: Boolean,
    onToggleMyList: (Long) -> Unit,
) {
    var heroFocused by remember { mutableStateOf(false) }
    AnimatedContent(
        targetState = movie,
        transitionSpec = {
            fadeIn(tween(TvMotion.HeroCrossfadeMillis)) togetherWith
                fadeOut(tween(TvMotion.HeroCrossfadeMillis))
        },
        label = "featured movie",
    ) { featuredMovie ->
        HeroBanner(featuredMovie, height, playFocus, { heroFocused = it }, onMoreInfo, onPlay,
            isInMyList, onToggleMyList)
    }
}

@Composable
private fun HeroBanner(
    movie: Movie,
    height: Dp,
    playFocus: FocusRequester,
    onHeroFocused: (Boolean) -> Unit,
    onMoreInfo: (Long) -> Unit,
    onPlay: (Long) -> Unit,
    isInMyList: Boolean,
    onToggleMyList: (Long) -> Unit,
) {
    val image = movie.backdropUrl ?: movie.posterUrl
    val displayTitle = remember(movie.title) {
        movie.title.replace(Regex("""\s*\((?:19|20)\d{2}\)\s*$"""), "").trim()
    }
    Box(Modifier.fillMaxWidth().height(height).background(HomeFlixColors.Background)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(image).crossfade(120).build(),
            contentDescription = "${movie.title} featured backdrop",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(Color(0x33000000)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            listOf(Color(0xFA070708), Color(0xD6070708), Color(0x52070708), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color(0x38070708), Color.Transparent, HomeFlixColors.Background), startY = 20f)))
        Column(
            Modifier.align(Alignment.TopStart).padding(start = 96.dp, top = 82.dp).widthIn(max = 620.dp),
        ) {
        Text("DS CINEMA FEATURED", color = HomeFlixColors.Brand, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            Text(displayTitle, color = Color.White, fontSize = 46.sp, lineHeight = 49.sp, fontWeight = FontWeight.Black,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 13.dp))
            val facts = listOfNotNull(
                movie.year?.toString(), movie.quality, formatRuntime(movie.durationSeconds),
                movie.genre?.substringBefore(',')?.trim(),
            )
            if (facts.isNotEmpty()) Text(facts.joinToString("   |   "), color = HomeFlixColors.TextSecondary,
                fontSize = 17.sp, modifier = Modifier.padding(top = 17.dp))
            Text(movie.description ?: "Featured from your personal HomeFlix library.",
                color = HomeFlixColors.TextPrimary, fontSize = 18.sp, lineHeight = 26.sp,
                maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 18.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 25.dp).onFocusChanged { onHeroFocused(it.hasFocus) },
            ) {
                Button(
                    onClick = { onPlay(movie.id) },
                    modifier = Modifier.focusRequester(playFocus),
                    colors = ButtonDefaults.colors(containerColor = Color.White, contentColor = Color.Black),
                ) {
                    TvVectorIcon(TvIcon.PLAY, Modifier.size(20.dp), Color.Black)
                    Text("PLAY", fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 9.dp, end = 8.dp))
                }
                Button(
                    onClick = { onToggleMyList(movie.id) },
                    colors = ButtonDefaults.colors(containerColor = Color(0xD9343439), contentColor = Color.White),
                ) {
                    TvVectorIcon(TvIcon.BOOKMARK, Modifier.size(20.dp),
                        if (isInMyList) HomeFlixColors.Brand else Color.White)
                    Text(if (isInMyList) "IN MY LIST" else "MY LIST", fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 9.dp, end = 7.dp))
                }
                Button(
                    onClick = { onMoreInfo(movie.id) },
                    colors = ButtonDefaults.colors(containerColor = Color(0xD94A4A50), contentColor = Color.White),
                ) {
                    TvVectorIcon(TvIcon.INFO, Modifier.size(20.dp))
                    Text("MORE INFO", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 9.dp, end = 7.dp))
                }
            }
        }
    }
}

internal fun moviesByCategory(movies: List<Movie>): Map<String, List<Movie>> = movies
    .flatMap { movie ->
        val genres = movie.genre?.split(',')?.map(String::trim)?.filter(String::isNotBlank).orEmpty()
        (genres.ifEmpty { listOf("More to Explore") }).map { category -> category to movie }
    }
    .groupBy({ it.first }, { it.second })
    .mapValues { (_, values) -> values.distinctBy { it.id }.sortedBy { it.title.lowercase() } }
    .toSortedMap(String.CASE_INSENSITIVE_ORDER)

@Composable
private fun CinematicBackground() {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(HomeFlixColors.BackgroundLight, HomeFlixColors.Background, HomeFlixColors.BackgroundDark))))
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(
        listOf(Color(0x3DE50914), Color.Transparent), Offset.Zero, 980f)))
}

@Composable
private fun HomeLoading() {
    Column(Modifier.fillMaxSize().padding(start = TvSpacing.SidebarClearance, top = 100.dp)) {
        Box(Modifier.width(430.dp).height(44.dp).background(HomeFlixColors.SurfaceRaised, RoundedCornerShape(6.dp)))
        Box(Modifier.padding(top = 18.dp).width(610.dp).height(130.dp)
            .background(HomeFlixColors.Surface.copy(alpha = .8f), RoundedCornerShape(8.dp)))
        Text("Preparing your library", fontSize = 23.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 76.dp, bottom = 18.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(TvSpacing.CardGap)) {
            items(7) { LoadingPosterCard() }
        }
    }
}

@Composable
private fun HomeError(retry: () -> Unit) = CenteredState(
    title = "Unable to load movies",
    message = "Check that your HomeFlix server is available, then try again.",
    action = "RETRY",
    onAction = retry,
)

@Composable
private fun HomeEmpty() = CenteredState(
    title = "Your library is ready for movies",
    message = "Add movies to a configured media folder and scan the library.",
)

@Composable
fun CenteredState(title: String, message: String, action: String? = null, onAction: () -> Unit = {}) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(58.dp).background(HomeFlixColors.Brand, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) { Text("DS", fontSize = 23.sp, fontWeight = FontWeight.Black) }
            Text(title, color = HomeFlixColors.TextPrimary, fontSize = 29.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 22.dp))
            Text(message, fontSize = 17.sp, color = HomeFlixColors.TextSecondary,
                modifier = Modifier.padding(top = 10.dp).widthIn(max = 560.dp))
            action?.let { Button(onClick = onAction, modifier = Modifier.padding(top = 25.dp)) { Text(it) } }
        }
    }
}
