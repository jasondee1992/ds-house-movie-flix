package com.jasond.homeflix.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class TvSeekTest {
    @Test fun `seek accelerates for held remote key`() {
        assertEquals(10_000L, seekStepMs(0))
        assertEquals(20_000L, seekStepMs(5))
        assertEquals(30_000L, seekStepMs(12))
    }

    @Test fun `seek target is clamped to playable timeline`() {
        assertEquals(0L, seekTarget(4_000, 100_000, -10_000))
        assertEquals(100_000L, seekTarget(95_000, 100_000, 10_000))
        assertEquals(40_000L, seekTarget(30_000, 100_000, 10_000))
    }
}
