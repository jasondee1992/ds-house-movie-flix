package com.jasond.homeflix.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jasond.homeflix.data.HomeRepository
import com.jasond.homeflix.data.MyListStore
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.data.model.ContinueWatchingItem
import com.jasond.homeflix.data.model.PlaybackProgress
import com.jasond.homeflix.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

enum class ConnectionStatus { CHECKING, CONNECTED, UNAVAILABLE }

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val movies: List<Movie>, val continueWatching: List<ContinueWatchingItem>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: HomeRepository = HomeRepository(ApiClient.service),
    private val myListStore: MyListStore? = null,
) : ViewModel() {
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CHECKING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _progress = MutableStateFlow<Map<Long, PlaybackProgress>>(emptyMap())
    val progress: StateFlow<Map<Long, PlaybackProgress>> = _progress.asStateFlow()
    private val _myListIds = MutableStateFlow(myListStore?.load().orEmpty())
    val myListIds: StateFlow<Set<Long>> = _myListIds.asStateFlow()

    init {
        loadMovies()
        viewModelScope.launch {
            while (true) {
                delay(LIBRARY_REFRESH_INTERVAL_MS)
                refreshMovies(showLoading = false)
            }
        }
    }

    fun loadMovies() = refreshMovies(showLoading = true, scanBefore = false)

    fun refreshAfterPlayback() = refreshMovies(showLoading = false, scanBefore = false)

    private fun refreshMovies(showLoading: Boolean, scanBefore: Boolean = true) {
        if (showLoading) {
            _uiState.value = HomeUiState.Loading
            _connectionStatus.value = ConnectionStatus.CHECKING
        }
        viewModelScope.launch {
            if (scanBefore) repository.scanLibrary()
            val moviesRequest = async { repository.getMovies() }
            val continueRequest = async { repository.getContinueWatching() }
            val moviesResult = moviesRequest.await()
            val continueResult = continueRequest.await()
            moviesResult.onSuccess { movies ->
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    val items = continueResult.getOrDefault(emptyList())
                    _progress.value = items.associate { it.movie.id to it.progress }
                    _uiState.value = HomeUiState.Success(movies, items)
                    if (showLoading) viewModelScope.launch {
                        repository.scanLibrary()
                        refreshMovies(showLoading = false, scanBefore = false)
                    }
                }
                .onFailure {
                    _connectionStatus.value = ConnectionStatus.UNAVAILABLE
                    if (showLoading || _uiState.value !is HomeUiState.Success) {
                        _uiState.value = HomeUiState.Error("HomeFlix server unavailable")
                    }
                }
        }
    }

    private companion object {
        const val LIBRARY_REFRESH_INTERVAL_MS = 300_000L
    }

    fun movieById(id: Long): Movie? = (uiState.value as? HomeUiState.Success)
        ?.movies
        ?.firstOrNull { it.id == id }

    fun relatedMovies(id: Long, limit: Int = 12): List<Movie> {
        val movies = (uiState.value as? HomeUiState.Success)?.movies.orEmpty()
        val selected = movies.firstOrNull { it.id == id } ?: return emptyList()
        val genres = selected.genre?.split(',')?.map { it.trim().lowercase() }?.toSet().orEmpty()
        return movies.asSequence()
            .filter { it.id != id }
            .map { candidate ->
                val candidateGenres = candidate.genre?.split(',')?.map { it.trim().lowercase() }?.toSet().orEmpty()
                candidate to candidateGenres.intersect(genres).size
            }
            .filter { (_, score) -> score > 0 }
            .sortedWith(compareByDescending<Pair<Movie, Int>> { it.second }.thenBy { it.first.title.lowercase() })
            .take(limit)
            .map { it.first }
            .toList()
    }

    fun loadProgress(movieId: Long) {
        viewModelScope.launch {
            repository.getProgress(movieId).onSuccess { value ->
                _progress.value = _progress.value + (movieId to value)
            }
        }
    }

    fun progressFor(movieId: Long): PlaybackProgress? = _progress.value[movieId]

    fun toggleMyList(movieId: Long) {
        val updated = if (movieId in _myListIds.value) _myListIds.value - movieId else _myListIds.value + movieId
        _myListIds.value = updated
        myListStore?.save(updated)
    }
}
