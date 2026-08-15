package com.jasond.homeflix.data.model

data class Subtitle(val id: Long, val language: String, val format: String, val isDefault: Boolean)

data class Movie(
    val id: Long, val title: String, val year: Int?, val durationSeconds: Long?,
    val description: String?, val genre: String?, val posterUrl: String?, val backdropUrl: String?,
    val fileExtension: String?, val fileSize: Long?, val dateAdded: String?,
    val videoWidth: Int?, val videoHeight: Int?, val quality: String?,
    val videoCodec: String?, val audioCodec: String?, val audioChannels: Int?,
    val subtitleCount: Int, val subtitles: List<Subtitle>,
)
