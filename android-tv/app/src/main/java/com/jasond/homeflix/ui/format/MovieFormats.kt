package com.jasond.homeflix.ui.format

import java.util.Locale

fun formatRuntime(seconds: Long?): String? {
    if (seconds == null || seconds <= 0) return null
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun formatBytes(bytes: Long?): String? {
    if (bytes == null || bytes < 0) return null
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble(); var unit = -1
    while (value >= 1024 && unit < units.lastIndex) { value /= 1024; unit++ }
    return String.format(Locale.US, "%.1f %s", value, units[unit])
}

fun audioChannelLabel(channels: Int?): String? = when (channels) {
    1 -> "Mono"; 2 -> "Stereo"; 6 -> "5.1"; 8 -> "7.1"; null -> null; else -> "$channels ch"
}
