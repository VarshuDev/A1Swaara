package com.varshudev.a1swaara.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwaaraUpdateManagerTest {

    @Test
    fun `equal versions return false even with prefixes or suffixes`() {
        assertFalse(SwaaraUpdateManager.isRemoteVersionNewer("1.0.1", "1.0.1"))
        assertFalse(SwaaraUpdateManager.isRemoteVersionNewer("1.0.1-http3", "v1.0.1"))
        assertFalse(SwaaraUpdateManager.isRemoteVersionNewer(" v1.0.1 ", "1.0.1-http3"))
    }

    @Test
    fun `patch minor and major bumps return true`() {
        assertTrue(SwaaraUpdateManager.isRemoteVersionNewer("1.0.1", "1.0.2"))
        assertTrue(SwaaraUpdateManager.isRemoteVersionNewer("1.0.9", "1.1.0"))
        assertTrue(SwaaraUpdateManager.isRemoteVersionNewer("1.9.9", "2.0.0"))
        assertTrue(SwaaraUpdateManager.isRemoteVersionNewer("1.0", "1.0.1"))
        assertTrue(SwaaraUpdateManager.isRemoteVersionNewer("1.0.1-http3", "v1.0.2"))
    }

    @Test
    fun `older remote versions return false`() {
        assertFalse(SwaaraUpdateManager.isRemoteVersionNewer("1.0.2", "1.0.1"))
        assertFalse(SwaaraUpdateManager.isRemoteVersionNewer("1.2.0", "1.1.9"))
        assertFalse(SwaaraUpdateManager.isRemoteVersionNewer("2.0.0", "1.99.99"))
        assertFalse(SwaaraUpdateManager.isRemoteVersionNewer("1.0.1", "1.0"))
    }
}
