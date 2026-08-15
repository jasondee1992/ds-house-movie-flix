package com.jasond.homeflix.ui.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.data.model.Subtitle

fun buildMediaItem(movie: Movie): MediaItem {
    val subtitles = movie.subtitles.mapNotNull(::subtitleConfiguration)
    return MediaItem.Builder()
        .setUri(movie.streamUrl)
        .setMediaId(movie.id.toString())
        .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(movie.title).build())
        .setSubtitleConfigurations(subtitles)
        .build()
}

private fun subtitleConfiguration(subtitle: Subtitle): MediaItem.SubtitleConfiguration? {
    val mimeType = when (subtitle.format.lowercase()) {
        "srt" -> MimeTypes.APPLICATION_SUBRIP
        "vtt" -> MimeTypes.TEXT_VTT
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        else -> return null
    }
    return MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitle.url))
        .setMimeType(mimeType)
        .setLabel(subtitle.language)
        .setId(subtitle.id.toString())
        .setSelectionFlags(if (subtitle.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
        .build()
}
