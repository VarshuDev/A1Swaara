package com.varshudev.a1swaara.notifications

import com.grack.nanojson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwaaraNotificationStoreTest {

    @Test
    fun `test notification type fromString parsing`() {
        assertEquals(SwaaraNotificationType.MUSIC_DROP, SwaaraNotificationType.fromString("music_drop"))
        assertEquals(SwaaraNotificationType.MUSIC_DROP, SwaaraNotificationType.fromString("DROP"))
        assertEquals(SwaaraNotificationType.MUSIC_DROP, SwaaraNotificationType.fromString("release"))

        assertEquals(SwaaraNotificationType.SMART_STASH, SwaaraNotificationType.fromString("smart_stash"))
        assertEquals(SwaaraNotificationType.SMART_STASH, SwaaraNotificationType.fromString("STASH"))

        assertEquals(SwaaraNotificationType.COMMUNITY, SwaaraNotificationType.fromString("community"))
        assertEquals(SwaaraNotificationType.COMMUNITY, SwaaraNotificationType.fromString("JAM"))

        assertEquals(SwaaraNotificationType.ANNOUNCEMENT, SwaaraNotificationType.fromString("announcement"))
        assertEquals(SwaaraNotificationType.ANNOUNCEMENT, SwaaraNotificationType.fromString("something_random"))
        assertEquals(SwaaraNotificationType.ANNOUNCEMENT, SwaaraNotificationType.fromString(null))
    }

    @Test
    fun `test SwaaraNotificationItem serialization and deserialization`() {
        val original = SwaaraNotificationItem(
            id = "test_item_123",
            title = "New Album Drop • Sanjith Hegde",
            message = "Check out the brand new acoustic EP streaming now in 320 KBPS lossless sound.",
            timestamp = 1700000000000L,
            type = SwaaraNotificationType.MUSIC_DROP,
            actionUrl = "https://github.com/VarshuDev/A1Swaara_apk",
            artworkUrl = "https://example.com/artwork.jpg",
            isRead = false,
            playQuery = "Sanjith Hegde Acoustic EP"
        )

        val jsonString = original.toJsonString()
        val restored = SwaaraNotificationItem.fromJsonString(jsonString)

        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.message, restored.message)
        assertEquals(original.timestamp, restored.timestamp)
        assertEquals(original.type, restored.type)
        assertEquals(original.actionUrl, restored.actionUrl)
        assertEquals(original.artworkUrl, restored.artworkUrl)
        assertEquals(original.isRead, restored.isRead)
        assertEquals(original.playQuery, restored.playQuery)
    }

    @Test
    fun `test SwaaraNotificationItem handles missing optional fields gracefully`() {
        val json = JsonObject().apply {
            put("id", "simple_notif")
            put("title", "Simple Notification")
            put("message", "A basic message")
            put("type", "ANNOUNCEMENT")
        }

        val item = SwaaraNotificationItem.fromJsonObject(json)
        assertEquals("simple_notif", item.id)
        assertEquals("Simple Notification", item.title)
        assertEquals("A basic message", item.message)
        assertEquals(SwaaraNotificationType.ANNOUNCEMENT, item.type)
        assertFalse(item.isRead)
        assertNull(item.actionUrl)
        assertNull(item.artworkUrl)
        assertNull(item.playQuery)
    }

    @Test
    fun `test SwaaraNotificationItem equality and read status mutation`() {
        val item = SwaaraNotificationItem(
            id = "notif_read_test",
            title = "Smart Stash Completed",
            message = "10 songs ready for offline listening",
            type = SwaaraNotificationType.SMART_STASH,
            isRead = false
        )

        assertFalse(item.isRead)
        val readItem = item.copy(isRead = true)
        assertTrue(readItem.isRead)
        assertEquals(item.id, readItem.id)
    }
}
