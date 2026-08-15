package com.jasond.homeflix.ui.player

import com.jasond.homeflix.data.model.PlaybackProgress

fun safeResumePosition(positionMs: Long, savedDurationMs: Long, mediaDurationMs: Long? = null): Long {
    if (positionMs <= 0 || savedDurationMs <= 0 || positionMs >= savedDurationMs) return 0
    if (mediaDurationMs != null && (mediaDurationMs <= 0 || positionMs >= mediaDurationMs)) return 0
    return positionMs
}

fun initialPlaybackPosition(progress: PlaybackProgress?, startOver: Boolean): Long {
    if (startOver || progress == null || progress.completed) return 0
    return safeResumePosition(progress.positionMs, progress.durationMs)
}
