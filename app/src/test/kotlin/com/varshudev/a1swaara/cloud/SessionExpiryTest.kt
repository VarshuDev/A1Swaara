package com.varshudev.a1swaara.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExpiryTest {
    @Test fun expiredAndMissingSessionsNeedRefresh() {
        assertTrue(SessionExpiry.needsRefresh(0L, 1_000_000L))
        assertTrue(SessionExpiry.needsRefresh(999_999L, 1_000_000L))
        assertTrue(SessionExpiry.needsRefresh(1_000_000L, 1_000_000L))
    }

    @Test fun refreshesAtTheSafetyMargin() {
        assertTrue(SessionExpiry.needsRefresh(1_060_000L, 1_000_000L))
        assertFalse(SessionExpiry.needsRefresh(1_060_001L, 1_000_000L))
    }
}
