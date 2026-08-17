package com.jasond.homeflix.ui.player

const val DEFAULT_SEEK_MS = 10_000L

fun seekStepMs(repeatCount: Int): Long = when {
    repeatCount >= 10 -> 30_000L
    repeatCount >= 4 -> 20_000L
    else -> DEFAULT_SEEK_MS
}

fun seekTarget(positionMs: Long, durationMs: Long, deltaMs: Long): Long {
    val upperBound = if (durationMs > 0) durationMs else Long.MAX_VALUE
    return (positionMs + deltaMs).coerceIn(0L, upperBound)
}
