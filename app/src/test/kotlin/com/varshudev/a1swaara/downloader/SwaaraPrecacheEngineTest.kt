package com.varshudev.a1swaara.downloader

import org.junit.Assert.assertEquals
import org.junit.Test

class SwaaraPrecacheEngineTest {

    @Test
    fun `selects up to three upcoming remote tracks and skips local or blank ids`() {
        val queue = listOf(
            "track0",
            "local:123",
            "",
            "remote1",
            null,
            "remote2",
            "remote3"
        )

        assertEquals(
            listOf("remote1"),
            SwaaraPrecacheEngine.selectUpcomingRemoteTrackIds(queue, currentIndex = 0, maxLookahead = 3)
        )
        assertEquals(
            listOf("remote1", "remote2"),
            SwaaraPrecacheEngine.selectUpcomingRemoteTrackIds(queue, currentIndex = 2, maxLookahead = 3)
        )
    }

    @Test
    fun `returns empty list when queue too small or index at end`() {
        assertEquals(
            emptyList<String>(),
            SwaaraPrecacheEngine.selectUpcomingRemoteTrackIds(listOf("onlyOne"), currentIndex = 0)
        )
        assertEquals(
            emptyList<String>(),
            SwaaraPrecacheEngine.selectUpcomingRemoteTrackIds(listOf("a", "b"), currentIndex = 1)
        )
        assertEquals(
            emptyList<String>(),
            SwaaraPrecacheEngine.selectUpcomingRemoteTrackIds(listOf("a", "b"), currentIndex = -1)
        )
    }
}
