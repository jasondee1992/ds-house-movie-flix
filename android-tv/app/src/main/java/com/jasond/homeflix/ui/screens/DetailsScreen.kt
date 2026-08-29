package com.jasond.homeflix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.data.model.PlaybackProgress
import com.jasond.homeflix.ui.components.MovieRow
import com.jasond.homeflix.ui.components.TvIcon
import com.jasond.homeflix.ui.components.TvVectorIcon
import com.jasond.homeflix.ui.format.audioChannelLabel
import com.jasond.homeflix.ui.format.formatRuntime
import com.jasond.homeflix.ui.theme.HomeFlixColors
import com.jasond.homeflix.ui.theme.TvMotion

@Composable
fun DetailsScreen(
    movie: Movie?,
    progress: PlaybackProgress?,
    relatedMovies: List<Movie>,
    isInMyList: Boolean,
    onToggleMyList: () -> Unit,
    onLoadProgress: () -> Unit,
    onPlayMovie: (Long, Boolean) -> Unit,
    onMovieSelected: (Long) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    if (movie == null) {
        Box(Modifier.fillMaxSize().background(HomeFlixColors.Background)) {
            CenteredState("Movie unavailable", "Return to the library and choose another title.", "BACK", onBack)
        }
        return
    }
    val playFocus = remember { FocusRequester() }
    var visible by remember(movie.id) { mutableStateOf(false) }
    LaunchedEffect(movie.id) {
        onLoadProgress()
        visible = true
        withFrameNanos { }
        withFrameNanos { }
        playFocus.requestFocus()
    }
    LazyColumn(Modifier.fillMaxSize().background(HomeFlixColors.Background)) {
        item {
            Box(Modifier.fillMaxWidth().height(700.dp)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(movie.backdropUrl ?: movie.posterUrl).crossfade(120).build(),
                    contentDescription = "${movie.title} backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Color(0x33000000)))
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                    listOf(Color(0xFC070708), Color(0xD8070708), Color(0x62070708), Color.Transparent))))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x55070708), HomeFlixColors.Background))))
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(180)) + slideInHorizontally(tween(180)) { -it / 16 },
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    DetailsHeader(movie, progress, playFocus, isInMyList, onToggleMyList, onPlayMovie, onBack)
                }
            }
        }
        if (relatedMovies.isNotEmpty()) item {
            MovieRow(
                title = "More Like This",
                movies = relatedMovies,
                onMovieSelected = onMovieSelected,
                modifier = Modifier.padding(start = 72.dp, top = 8.dp),
            )
        }
        item { Spacer(Modifier.height(42.dp)) }
    }
}

@Composable
private fun DetailsHeader(
    movie: Movie,
    progress: PlaybackProgress?,
    playFocus: FocusRequester,
    isInMyList: Boolean,
    onToggleMyList: () -> Unit,
    onPlayMovie: (Long, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(.6f).padding(start = 72.dp, top = 52.dp)) {
        Text("DS CINEMA", color = HomeFlixColors.Brand, fontSize = 14.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
        Text(movie.title, color = Color.White, fontSize = 52.sp, lineHeight = 55.sp, fontWeight = FontWeight.Black,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
        MetadataRow(movie)
        Text(
            movie.description ?: "No description is available for this title.",
            fontSize = 19.sp,
            lineHeight = 27.sp,
            color = HomeFlixColors.TextPrimary,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 20.dp),
        )
        movie.genre?.let { genres ->
            Row(Modifier.padding(top = 15.dp)) {
                Text("Genres  ", color = HomeFlixColors.TextSecondary, fontSize = 16.sp)
                Text(genres, color = HomeFlixColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        val technical = listOfNotNull(
            movie.videoCodec?.uppercase(),
            listOfNotNull(movie.audioCodec?.uppercase(), audioChannelLabel(movie.audioChannels))
                .joinToString(" ").ifBlank { null },
            movie.subtitles.takeIf { it.isNotEmpty() }?.let { "${it.size} subtitle${if (it.size == 1) "" else "s"}" },
        )
        if (technical.isNotEmpty()) Text(technical.joinToString("   |   "), color = HomeFlixColors.TextSecondary,
            fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
        if (progress?.canResume == true) {
            val minutesLeft = ((progress.durationMs - progress.positionMs).coerceAtLeast(0) / 60_000)
            Text(if (minutesLeft > 0) "$minutesLeft min remaining" else "${progress.progressPercent.toInt()}% watched",
                color = HomeFlixColors.TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 27.dp)) {
            Button(
                onClick = { onPlayMovie(movie.id, false) },
                modifier = Modifier.focusRequester(playFocus),
                colors = ButtonDefaults.colors(containerColor = Color.White, contentColor = Color.Black),
            ) {
                TvVectorIcon(TvIcon.PLAY, Modifier.size(20.dp), Color.Black)
                Text(if (progress?.canResume == true) "RESUME" else "PLAY", fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 9.dp))
            }
            if (progress?.canResume == true) Button(onClick = { onPlayMovie(movie.id, true) }) { Text("START OVER") }
            Button(onClick = onToggleMyList, colors = ButtonDefaults.colors(containerColor = Color(0xD947474D))) {
                TvVectorIcon(TvIcon.BOOKMARK, Modifier.size(20.dp),
                    if (isInMyList) HomeFlixColors.Brand else Color.White)
                Text(if (isInMyList) "IN MY LIST" else "MY LIST", modifier = Modifier.padding(start = 8.dp))
            }
            Button(onClick = onBack, colors = ButtonDefaults.colors(containerColor = Color(0xD947474D))) {
                TvVectorIcon(TvIcon.BACK, Modifier.size(20.dp))
                Text("BACK", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun MetadataRow(movie: Movie) {
    val facts = listOfNotNull(movie.year?.toString(), movie.quality, formatRuntime(movie.durationSeconds))
    if (facts.isNotEmpty()) Text(
        facts.joinToString("   |   "),
        color = HomeFlixColors.TextSecondary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 15.dp),
    )
}
