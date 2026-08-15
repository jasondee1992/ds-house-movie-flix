package com.jasond.homeflix.ui.player

import com.jasond.homeflix.data.model.PlaybackProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPositionTest {
    private fun progress(position: Long = 42_000, duration: Long = 100_000, completed: Boolean = false) =
        PlaybackProgress(2, position, duration, position * 100.0 / duration, completed, null)

    @Test fun `valid incomplete progress resumes`() {
        assertEquals(42_000, initialPlaybackPosition(progress(), startOver = false))
        assertTrue(progress().canResume)
    }

    @Test fun `start over ignores saved progress`() {
        assertEquals(0, initialPlaybackPosition(progress(), startOver = true))
    }

    @Test fun `completed progress starts at beginning`() {
        assertEquals(0, initialPlaybackPosition(progress(completed = true), startOver = false))
        assertFalse(progress(completed = true).canResume)
    }

    @Test fun `invalid positions are clamped to beginning`() {
        assertEquals(0, safeResumePosition(-1, 100_000))
        assertEquals(0, safeResumePosition(100_000, 100_000))
        assertEquals(0, safeResumePosition(90_000, 100_000, mediaDurationMs = 80_000))
    }
}
