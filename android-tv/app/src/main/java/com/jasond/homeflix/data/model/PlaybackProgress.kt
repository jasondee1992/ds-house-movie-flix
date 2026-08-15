package com.jasond.homeflix.data.model

data class PlaybackProgress(
    val movieId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val progressPercent: Double,
    val completed: Boolean,
    val lastWatchedAt: String?,
) {
    val canResume: Boolean get() = positionMs > 0 && durationMs > 0 && !completed
}

data class ContinueWatchingItem(
    val movie: Movie,
    val progress: PlaybackProgress,
)
