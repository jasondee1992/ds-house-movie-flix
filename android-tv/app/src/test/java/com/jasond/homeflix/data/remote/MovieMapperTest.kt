package com.jasond.homeflix.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertFalse

class MovieMapperTest {
    @Test
    fun `relative poster URL resolves against configured API host`() {
        val result = resolveApiUrl("/api/movies/12/poster")
        assertEquals("http://10.0.2.2:8000/api/movies/12/poster", result)
    }

    @Test
    fun `dto maps snake case transport fields into domain model`() {
        val movie = MovieDto(
            id = 7,
            title = "Interstellar",
            year = 2014,
            duration_seconds = 10_140,
            description = null,
            genre = null,
            poster_url = null,
            file_extension = ".mkv",
            file_size = 42,
            date_added = "2026-08-15T12:00:00Z",
            stream_url = "/api/movies/7/stream",
            subtitles = listOf(SubtitleDto(31, "English", "srt", false, "/api/movies/7/subtitles/31")),
        ).toDomain()

        assertEquals(7L, movie.id)
        assertEquals(10_140L, movie.durationSeconds)
        assertEquals(".mkv", movie.fileExtension)
        assertEquals("http://10.0.2.2:8000/api/movies/7/stream", movie.streamUrl)
        assertEquals("http://10.0.2.2:8000/api/movies/7/subtitles/31", movie.subtitles.single().url)
    }

    @Test
    fun `continue watching transport maps movie and progress`() {
        val dto = MovieDto(3, "Movie", 2026, 100, null, null, null,
            file_extension = ".mkv", file_size = 10, date_added = null)
        val item = ContinueWatchingDto(dto, 40_000, 100_000, 40.0, "2026-08-15T12:00:00Z").toDomain()
        assertEquals(3L, item.movie.id)
        assertEquals(40_000L, item.progress.positionMs)
        assertEquals(40.0, item.progress.progressPercent, 0.0)
        assertFalse(item.progress.completed)
    }
}
