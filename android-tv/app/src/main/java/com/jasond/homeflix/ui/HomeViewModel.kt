package com.jasond.homeflix.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jasond.homeflix.data.HomeRepository
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.data.model.ContinueWatchingItem
import com.jasond.homeflix.data.model.PlaybackProgress
import com.jasond.homeflix.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionStatus { CHECKING, CONNECTED, UNAVAILABLE }

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val movies: List<Movie>, val continueWatching: List<ContinueWatchingItem>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: HomeRepository = HomeRepository(ApiClient.service),
) : ViewModel() {
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CHECKING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _progress = MutableStateFlow<Map<Long, PlaybackProgress>>(emptyMap())
    val progress: StateFlow<Map<Long, PlaybackProgress>> = _progress.asStateFlow()

    init { loadMovies() }

    fun loadMovies() {
        _uiState.value = HomeUiState.Loading
        _connectionStatus.value = ConnectionStatus.CHECKING
        viewModelScope.launch {
            val moviesResult = repository.getMovies()
            val continueResult = repository.getContinueWatching()
            moviesResult.onSuccess { movies ->
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    val items = continueResult.getOrDefault(emptyList())
                    _progress.value = items.associate { it.movie.id to it.progress }
                    _uiState.value = HomeUiState.Success(movies, items)
                }
                .onFailure {
                    _connectionStatus.value = ConnectionStatus.UNAVAILABLE
                    _uiState.value = HomeUiState.Error("HomeFlix server unavailable")
                }
        }
    }

    fun movieById(id: Long): Movie? = (uiState.value as? HomeUiState.Success)
        ?.movies
        ?.firstOrNull { it.id == id }

    fun loadProgress(movieId: Long) {
        viewModelScope.launch {
            repository.getProgress(movieId).onSuccess { value ->
                _progress.value = _progress.value + (movieId to value)
            }
        }
    }

    fun progressFor(movieId: Long): PlaybackProgress? = _progress.value[movieId]
}
