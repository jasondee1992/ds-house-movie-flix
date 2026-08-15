package com.jasond.homeflix.ui

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jasond.homeflix.data.HomeRepository
import com.jasond.homeflix.data.remote.ApiClient
import com.jasond.homeflix.ui.player.initialPlaybackPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerProgressState {
    data object Loading : PlayerProgressState
    data class Ready(val initialPositionMs: Long) : PlayerProgressState
}

class PlayerViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val repository = HomeRepository(ApiClient.service)
    private val movieId: Long = checkNotNull(savedStateHandle["movieId"])
    private val startOver: Boolean = savedStateHandle["startOver"] ?: false
    private val _state = MutableStateFlow<PlayerProgressState>(PlayerProgressState.Loading)
    val state: StateFlow<PlayerProgressState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (startOver) {
                repository.saveProgress(movieId, 0, 0)
                _state.value = PlayerProgressState.Ready(0)
            } else {
                val progress = repository.getProgress(movieId).getOrNull()
                val position = initialPlaybackPosition(progress, startOver = false)
                _state.value = PlayerProgressState.Ready(position)
            }
        }
    }

    fun save(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0) return
        viewModelScope.launch {
            repository.saveProgress(movieId, positionMs.coerceAtLeast(0), durationMs)
                .onFailure { Log.d("HomeFlixProgress", "Progress save failed; will retry", it) }
        }
    }

    fun saveThen(positionMs: Long, durationMs: Long, finished: () -> Unit) {
        viewModelScope.launch {
            if (durationMs > 0) repository.saveProgress(movieId, positionMs.coerceAtLeast(0), durationMs)
                .onFailure { Log.d("HomeFlixProgress", "Final progress save failed", it) }
            finished()
        }
    }
}
