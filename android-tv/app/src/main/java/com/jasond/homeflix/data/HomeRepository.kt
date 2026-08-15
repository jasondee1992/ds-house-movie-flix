package com.jasond.homeflix.data

import com.jasond.homeflix.data.remote.ApiService
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.data.remote.toDomain
import com.jasond.homeflix.data.model.ContinueWatchingItem
import com.jasond.homeflix.data.model.PlaybackProgress
import com.jasond.homeflix.data.remote.ProgressUpdateDto

class HomeRepository(private val api: ApiService) {
    suspend fun isServerConnected(): Boolean = runCatching {
        val response = api.health()
        response.status == "ok" && response.service == "homeflix"
    }.getOrDefault(false)

    suspend fun getMovies(): Result<List<Movie>> = runCatching {
        api.movies().map { it.toDomain() }
    }

    suspend fun getProgress(movieId: Long): Result<PlaybackProgress> = runCatching {
        api.progress(movieId).toDomain()
    }

    suspend fun saveProgress(movieId: Long, positionMs: Long, durationMs: Long): Result<PlaybackProgress> =
        runCatching { api.updateProgress(movieId, ProgressUpdateDto(positionMs, durationMs)).toDomain() }

    suspend fun clearProgress(movieId: Long): Result<Unit> = runCatching { api.clearProgress(movieId) }

    suspend fun getContinueWatching(): Result<List<ContinueWatchingItem>> = runCatching {
        api.continueWatching().map { it.toDomain() }
    }
}
