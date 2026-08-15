package com.jasond.homeflix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.data.model.PlaybackProgress
import com.jasond.homeflix.ui.format.audioChannelLabel
import com.jasond.homeflix.ui.format.formatRuntime

@Composable
fun DetailsScreen(
    movie: Movie?, progress: PlaybackProgress?, onLoadProgress: () -> Unit,
    onPlayMovie: (Long, Boolean) -> Unit, onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    if (movie == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF09090C)), contentAlignment = Alignment.Center) {
            Text("Movie details unavailable")
        }
        return
    }
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(movie.id) { onLoadProgress(); playFocus.requestFocus() }
    Box(Modifier.fillMaxSize().background(Color(0xFF09090C))) {
        movie.backdropUrl?.let { AsyncImage(model = it, contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            listOf(Color(0xFA09090C), Color(0xD909090C), Color(0x2509090C)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color.Transparent, Color(0xEE09090C)))))
        Column(Modifier.fillMaxWidth(0.68f).align(Alignment.CenterStart).padding(start = 72.dp, top = 44.dp)) {
            Text(movie.title.uppercase(), fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
            val facts = listOfNotNull(movie.year?.toString(), movie.quality, formatRuntime(movie.durationSeconds))
            if (facts.isNotEmpty()) Text(facts.joinToString("  •  "), color = Color(0xFFE0E0E0),
                fontSize = 20.sp, modifier = Modifier.padding(top = 12.dp))
            Text(movie.description ?: "Description unavailable", fontSize = 20.sp, lineHeight = 28.sp,
                modifier = Modifier.padding(top = 24.dp))
            movie.genre?.let { Text(it, color = Color.LightGray, fontSize = 17.sp,
                modifier = Modifier.padding(top = 12.dp)) }
            val video = movie.videoCodec?.uppercase()
            val audio = listOfNotNull(movie.audioCodec?.uppercase(), audioChannelLabel(movie.audioChannels))
                .joinToString(" ").ifBlank { null }
            if (video != null || audio != null) Text(listOfNotNull(video?.let { "Video: $it" },
                audio?.let { "Audio: $it" }).joinToString("    "), color = Color.LightGray,
                fontSize = 16.sp, modifier = Modifier.padding(top = 18.dp))
            if (movie.subtitles.isNotEmpty()) Text("Subtitles: ${movie.subtitles.map { it.language }.distinct().joinToString()}",
                color = Color.LightGray, fontSize = 16.sp, modifier = Modifier.padding(top = 7.dp))
            if (progress?.canResume == true) Text("${progress.progressPercent.toInt()}% watched",
                color = Color.LightGray, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(top = 30.dp)) {
                Button(onClick = { onPlayMovie(movie.id, false) }, modifier = Modifier.focusRequester(playFocus)) {
                    Text(if (progress?.canResume == true) "▶  RESUME" else "▶  PLAY", fontWeight = FontWeight.Bold)
                }
                if (progress?.canResume == true) Button(onClick = { onPlayMovie(movie.id, true) }) { Text("START OVER") }
                Button(onClick = onBack) { Text("BACK") }
            }
        }
    }
}
