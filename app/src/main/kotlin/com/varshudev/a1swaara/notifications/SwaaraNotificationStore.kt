package com.varshudev.a1swaara.notifications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.grack.nanojson.JsonArray
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SwaaraNotificationType(val label: String) {
    MUSIC_DROP("MUSIC DROP"),
    SMART_STASH("SMART STASH"),
    COMMUNITY("COMMUNITY"),
    ANNOUNCEMENT("ANNOUNCEMENT");

    companion object {
        fun fromString(value: String?): SwaaraNotificationType {
            return when (value?.uppercase()) {
                "MUSIC_DROP", "DROP", "RELEASE" -> MUSIC_DROP
                "SMART_STASH", "STASH" -> SMART_STASH
                "COMMUNITY", "JAM" -> COMMUNITY
                else -> ANNOUNCEMENT
            }
        }
    }
}

data class SwaaraNotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: SwaaraNotificationType = SwaaraNotificationType.ANNOUNCEMENT,
    val actionUrl: String? = null,
    val artworkUrl: String? = null,
    val isRead: Boolean = false,
    val playQuery: String? = null
) {
    fun toJsonObject(): JsonObject = JsonObject().apply {
        put("id", id)
        put("title", title)
        put("message", message)
        put("timestamp", timestamp)
        put("type", type.name)
        put("actionUrl", actionUrl ?: "")
        put("artworkUrl", artworkUrl ?: "")
        put("isRead", isRead)
        put("playQuery", playQuery ?: "")
    }

    fun toJsonString(): String = JsonWriter.string(toJsonObject())

    companion object {
        fun fromJsonObject(json: JsonObject): SwaaraNotificationItem {
            return SwaaraNotificationItem(
                id = json.getString("id", "") ?: "",
                title = json.getString("title", "") ?: "",
                message = json.getString("message", "") ?: "",
                timestamp = json.getLong("timestamp", System.currentTimeMillis()),
                type = SwaaraNotificationType.fromString(json.getString("type", "")),
                actionUrl = json.getString("actionUrl", "").takeIf { !it.isNullOrBlank() },
                artworkUrl = json.getString("artworkUrl", "").takeIf { !it.isNullOrBlank() },
                isRead = json.getBoolean("isRead", false),
                playQuery = json.getString("playQuery", "").takeIf { !it.isNullOrBlank() }
            )
        }

        fun fromJsonString(str: String): SwaaraNotificationItem {
            val json = JsonParser.`object`().from(str)
            return fromJsonObject(json)
        }
    }
}

/**
 * Flagship Notification & Activity Center Store for A1 Swaara.
 * Maintains local persistence, read/unread states, real-time unread badges,
 * and category filtering matching top streaming apps.
 */
object SwaaraNotificationStore {
    private const val TAG = "SwaaraNotifStore"
    private const val PREFS_NAME = "a1swaara_notification_center_store"
    private const val KEY_NOTIFICATIONS = "key_notifications_json_v1"
    private const val KEY_SEEDED = "key_seeded_defaults"
    private const val MAX_ITEMS = 50

    private val mutex = Mutex()
    private val storeScope = CoroutineScope(Dispatchers.IO)

    private val _notifications = MutableStateFlow<List<SwaaraNotificationItem>>(emptyList())
    val notifications: StateFlow<List<SwaaraNotificationItem>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _openActivityCenter = MutableSharedFlow<Boolean>(replay = 0, extraBufferCapacity = 1)
    val openActivityCenter: SharedFlow<Boolean> = _openActivityCenter.asSharedFlow()

    private var isInitialized = false

    fun requestOpenActivityCenter() {
        _openActivityCenter.tryEmit(true)
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        storeScope.launch {
            loadFromDisk(context)
        }
    }

    private suspend fun loadFromDisk(context: Context) = mutex.withLock {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val rawJson = prefs.getString(KEY_NOTIFICATIONS, null)

            val list = mutableListOf<SwaaraNotificationItem>()
            if (!rawJson.isNullOrBlank()) {
                runCatching {
                    val array = JsonParser.array().from(rawJson)
                    for (i in 0 until array.size) {
                        val obj = array.getObject(i) ?: continue
                        list.add(SwaaraNotificationItem.fromJsonObject(obj))
                    }
                }
            }

            val seeded = prefs.getBoolean(KEY_SEEDED, false)
            if (list.isEmpty() && !seeded) {
                val seedItems = listOf(
                    SwaaraNotificationItem(
                        id = "seed_studio_lossless",
                        title = "320 KBPS Studio Audio Engine Active",
                        message = "Welcome to A1 Swaara. Enjoy studio-grade lossless playback, personalized acoustic pacing, and dynamic parametric EQ.",
                        timestamp = System.currentTimeMillis(),
                        type = SwaaraNotificationType.ANNOUNCEMENT
                    ),
                    SwaaraNotificationItem(
                        id = "seed_smart_stash",
                        title = "Smart Stash Online",
                        message = "Autonomous offline caching is enabled. Your heavy-rotation tracks will cache on Wi-Fi so you never face silence.",
                        timestamp = System.currentTimeMillis() - 1000L * 60L * 30L,
                        type = SwaaraNotificationType.SMART_STASH
                    )
                )
                list.addAll(seedItems)
                persistListLocked(prefs, list)
                prefs.edit().putBoolean(KEY_SEEDED, true).apply()
            }

            updateStateFlows(list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load notifications from disk", e)
        }
    }

    fun addNotification(context: Context, item: SwaaraNotificationItem) {
        storeScope.launch {
            mutex.withLock {
                try {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val current = _notifications.value.toMutableList()

                    val existingIndex = current.indexOfFirst { it.id == item.id }
                    if (existingIndex >= 0) {
                        current[existingIndex] = item
                    } else {
                        current.add(0, item)
                    }

                    val trimmed = current.take(MAX_ITEMS)
                    persistListLocked(prefs, trimmed)
                    updateStateFlows(trimmed)
                    Log.d(TAG, "Notification stored: [${item.type}] ${item.title}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding notification", e)
                }
            }
        }
    }

    fun markAsRead(context: Context, id: String) {
        storeScope.launch {
            mutex.withLock {
                try {
                    val current = _notifications.value
                    val target = current.find { it.id == id } ?: return@withLock
                    if (target.isRead) return@withLock

                    val updated = current.map {
                        if (it.id == id) it.copy(isRead = true) else it
                    }
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    persistListLocked(prefs, updated)
                    updateStateFlows(updated)
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking notification as read", e)
                }
            }
        }
    }

    fun markAllAsRead(context: Context) {
        storeScope.launch {
            mutex.withLock {
                try {
                    val current = _notifications.value
                    if (current.all { it.isRead }) return@withLock

                    val updated = current.map { it.copy(isRead = true) }
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    persistListLocked(prefs, updated)
                    updateStateFlows(updated)
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking all as read", e)
                }
            }
        }
    }

    fun deleteNotification(context: Context, id: String) {
        storeScope.launch {
            mutex.withLock {
                try {
                    val current = _notifications.value
                    val updated = current.filterNot { it.id == id }
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    persistListLocked(prefs, updated)
                    updateStateFlows(updated)
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting notification", e)
                }
            }
        }
    }

    fun clearAll(context: Context) {
        storeScope.launch {
            mutex.withLock {
                try {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    persistListLocked(prefs, emptyList())
                    updateStateFlows(emptyList())
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing notifications", e)
                }
            }
        }
    }

    private fun persistListLocked(prefs: SharedPreferences, list: List<SwaaraNotificationItem>) {
        val array = JsonArray()
        list.forEach { array.add(it.toJsonObject()) }
        val raw = JsonWriter.string(array)
        prefs.edit().putString(KEY_NOTIFICATIONS, raw).apply()
    }

    private fun updateStateFlows(list: List<SwaaraNotificationItem>) {
        _notifications.value = list
        _unreadCount.value = list.count { !it.isRead }
    }
}
