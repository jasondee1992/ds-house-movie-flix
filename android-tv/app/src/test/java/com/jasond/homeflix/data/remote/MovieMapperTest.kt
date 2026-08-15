package com.jasond.homeflix.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

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
        ).toDomain()

        assertEquals(7L, movie.id)
        assertEquals(10_140L, movie.durationSeconds)
        assertEquals(".mkv", movie.fileExtension)
    }
}
