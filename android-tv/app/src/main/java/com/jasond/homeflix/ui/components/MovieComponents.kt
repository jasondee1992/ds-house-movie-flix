package com.jasond.homeflix.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.ui.theme.HomeFlixColors
import com.jasond.homeflix.ui.theme.TvMotion
import com.jasond.homeflix.ui.theme.TvSpacing

@Composable
fun MoviePosterCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    onFocused: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.08f else 1f,
        animationSpec = tween(TvMotion.FocusMillis),
        label = "poster focus",
    )
    Column(
        modifier.width(166.dp).zIndex(if (focused) 1f else 0f).scale(scale),
        horizontalAlignment = Alignment.Start,
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused?.invoke()
                }
                .then(if (focused) Modifier.border(3.dp, Color.White, RoundedCornerShape(7.dp)) else Modifier),
            colors = CardDefaults.colors(containerColor = HomeFlixColors.Surface),
            shape = CardDefaults.shape(RoundedCornerShape(7.dp)),
        ) {
            Box(Modifier.fillMaxSize()) {
                PosterImage(movie.posterUrl, movie.title)
                Box(
                    Modifier.fillMaxWidth().height(54.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9000000)))),
                )
                progress?.let { value ->
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(6.dp)
                            .background(Color(0xFF55555B)),
                    ) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth(value.coerceIn(0f, 1f))
                                .background(HomeFlixColors.Brand),
                        )
                    }
                }
            }
        }
        Text(
            movie.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 15.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
            color = if (focused) HomeFlixColors.TextPrimary else HomeFlixColors.TextSecondary,
            modifier = Modifier.padding(top = 10.dp).alpha(if (focused) 1f else .92f),
        )
        if (focused) {
            Text(
                listOfNotNull(movie.year?.toString(), movie.quality).joinToString("  |  "),
                color = HomeFlixColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MovieRow(
    title: String,
    movies: List<Movie>,
    onMovieSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    progressByMovie: Map<Long, Float> = emptyMap(),
) {
    Column(modifier.padding(bottom = TvSpacing.RowGap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(25.dp).background(HomeFlixColors.Brand, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(11.dp))
            Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = HomeFlixColors.TextPrimary)
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).focusRestorer().focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
            contentPadding = PaddingValues(end = 72.dp, top = 8.dp, bottom = 16.dp),
        ) {
            itemsIndexed(movies, key = { _, movie -> movie.id }) { _, movie ->
                MoviePosterCard(
                    movie = movie,
                    progress = progressByMovie[movie.id],
                    onClick = { onMovieSelected(movie.id) },
                )
            }
        }
    }
}

@Composable
fun PosterImage(url: String?, title: String, modifier: Modifier = Modifier) {
    val request = ImageRequest.Builder(LocalContext.current).data(url).crossfade(250).build()
    SubcomposeAsyncImage(
        model = request,
        contentDescription = "$title poster",
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)),
        contentScale = ContentScale.Crop,
    ) {
        if (painter.state is AsyncImagePainter.State.Success) SubcomposeAsyncImageContent()
        else ArtworkPlaceholder(title.take(1).uppercase())
    }
}

@Composable
fun LoadingPosterCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val glow by transition.animateFloat(
        initialValue = .35f,
        targetValue = .72f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeleton glow",
    )
    Box(
        modifier.width(166.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(7.dp))
            .background(HomeFlixColors.SurfaceRaised.copy(alpha = glow)),
    )
}

@Composable
private fun ArtworkPlaceholder(letter: String) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(HomeFlixColors.SurfaceRaised, HomeFlixColors.Background)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter.ifBlank { "H" }, color = HomeFlixColors.Brand, fontSize = 52.sp, fontWeight = FontWeight.Black)
    }
}
