package com.jasond.homeflix.data

import com.jasond.homeflix.data.remote.ApiService
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.data.remote.toDomain

class HomeRepository(private val api: ApiService) {
    suspend fun isServerConnected(): Boolean = runCatching {
        val response = api.health()
        response.status == "ok" && response.service == "homeflix"
    }.getOrDefault(false)

    suspend fun getMovies(): Result<List<Movie>> = runCatching {
        api.movies().map { it.toDomain() }
    }
}
