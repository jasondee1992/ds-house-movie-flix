package com.jasond.homeflix.data.remote

import retrofit2.http.GET

data class HealthResponse(val status: String, val service: String)

data class MovieDto(
    val id: Long,
    val title: String,
    val year: Int?,
    val duration_seconds: Long?,
    val description: String?,
    val genre: String?,
    val poster_url: String?,
    val backdrop_url: String? = null,
    val file_extension: String?,
    val file_size: Long?,
    val date_added: String?,
    val video_width: Int? = null,
    val video_height: Int? = null,
    val quality: String? = null,
    val video_codec: String? = null,
    val audio_codec: String? = null,
    val audio_channels: Int? = null,
    val subtitle_count: Int = 0,
    val subtitles: List<SubtitleDto> = emptyList(),
)

data class SubtitleDto(val id: Long, val language: String, val format: String, val is_default: Boolean)

interface ApiService {
    @GET("api/health")
    suspend fun health(): HealthResponse

    @GET("api/movies")
    suspend fun movies(): List<MovieDto>

    @GET("api/movies/{id}")
    suspend fun movie(@retrofit2.http.Path("id") id: Long): MovieDto
}
