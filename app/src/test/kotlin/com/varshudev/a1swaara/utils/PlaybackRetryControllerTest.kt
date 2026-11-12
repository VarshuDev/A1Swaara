package com.varshudev.a1swaara.utils

import androidx.media3.common.PlaybackException
import com.varshudev.a1swaara.service.UnplayableException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import java.net.UnknownHostException

class PlaybackRetryControllerTest {

    @Test
    fun `recoverable network errors retry with configured delays until exhausted`() {
        val controller = PlaybackRetryController(longArrayOf(500L, 1500L))
        val networkError = PlaybackException(
            "Network failed",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )

        val first = controller.nextPlanOrNull("song1", networkError)
        val second = controller.nextPlanOrNull("song1", networkError)
        val exhausted = controller.nextPlanOrNull("song1", networkError)

        assertEquals(500L, first?.delayMs)
        assertEquals(1500L, second?.delayMs)
        assertNull(exhausted)

        controller.reset("song1")
        assertEquals(500L, controller.nextPlanOrNull("song1", networkError)?.delayMs)
    }

    @Test
    fun `dns and unplayable errors are recoverable while content unavailable is not`() {
        val dnsError = PlaybackException(
            "DNS failed",
            UnknownHostException("music.youtube.com"),
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED
        )
        assertTrue(dnsError.isDnsResolutionError())
        assertTrue(dnsError.isRecoverablePlaybackError())

        val unplayable = UnplayableException()
        assertTrue(unplayable.isRecoverablePlaybackError())

        val contentUnavailable = PlaybackException(
            "Removed",
            ContentNotAvailableException("Deleted"),
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED
        )
        assertFalse(contentUnavailable.isRecoverablePlaybackError())

        val controller = PlaybackRetryController(longArrayOf(300L))
        assertNull(controller.nextPlanOrNull("song2", contentUnavailable))
        assertNotNull(controller.nextPlanOrNull("song2", dnsError))
    }
}
