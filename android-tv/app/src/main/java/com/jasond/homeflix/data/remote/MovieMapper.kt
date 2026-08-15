package com.jasond.homeflix.data.remote

import com.jasond.homeflix.BuildConfig
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.data.model.Subtitle
import java.net.URI

fun MovieDto.toDomain(): Movie = Movie(
    id = id,
    title = title,
    year = year,
    durationSeconds = duration_seconds,
    description = description,
    genre = genre,
    posterUrl = poster_url?.let(::resolveApiUrl),
    backdropUrl = backdrop_url?.let(::resolveApiUrl),
    fileExtension = file_extension,
    fileSize = file_size,
    dateAdded = date_added,
    videoWidth = video_width,
    videoHeight = video_height,
    quality = quality,
    videoCodec = video_codec,
    audioCodec = audio_codec,
    audioChannels = audio_channels,
    subtitleCount = subtitle_count,
    subtitles = subtitles.map { Subtitle(it.id, it.language, it.format, it.is_default) },
)

internal fun resolveApiUrl(url: String): String = runCatching {
    val parsed = URI(url)
    if (parsed.isAbsolute) parsed.toString() else URI(BuildConfig.API_BASE_URL).resolve(parsed).toString()
}.getOrDefault(url)
