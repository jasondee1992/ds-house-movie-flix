package com.jasond.homeflix.data.remote

import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.POST

data class HealthResponse(val status: String, val service: String)
data class ScanResponse(val status: String)

data class MovieDto(
    val id: Long,
    val title: String,
    val year: Int?,
    val duration_seconds: Long?,
    val description: String?,
    val genre: String?,
    val poster_url: String?,
    val backdrop_url: String? = null,
    val stream_url: String = "/api/movies/$id/stream",
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

data class SubtitleDto(
    val id: Long, val language: String, val format: String, val is_default: Boolean,
    val url: String = "",
)

data class ProgressDto(
    val movie_id: Long,
    val position_ms: Long,
    val duration_ms: Long,
    val progress_percent: Double,
    val completed: Boolean,
    val last_watched_at: String?,
)

data class ProgressUpdateDto(val position_ms: Long, val duration_ms: Long)

data class ContinueWatchingDto(
    val movie: MovieDto,
    val position_ms: Long,
    val duration_ms: Long,
    val progress_percent: Double,
    val last_watched_at: String?,
)

interface ApiService {
    @GET("api/health")
    suspend fun health(): HealthResponse

    @GET("api/movies")
    suspend fun movies(): List<MovieDto>

    @POST("api/library/scan")
    suspend fun scanLibrary(): ScanResponse

    @GET("api/movies/{id}")
    suspend fun movie(@Path("id") id: Long): MovieDto

    @GET("api/movies/{id}/progress")
    suspend fun progress(@Path("id") id: Long): ProgressDto

    @PUT("api/movies/{id}/progress")
    suspend fun updateProgress(@Path("id") id: Long, @Body update: ProgressUpdateDto): ProgressDto

    @DELETE("api/movies/{id}/progress")
    suspend fun clearProgress(@Path("id") id: Long)

    @GET("api/continue-watching")
    suspend fun continueWatching(): List<ContinueWatchingDto>
}
