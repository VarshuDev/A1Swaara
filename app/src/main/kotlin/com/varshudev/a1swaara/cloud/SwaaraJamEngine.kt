package com.varshudev.a1swaara.cloud

import android.content.Context
import android.util.Log
import com.varshudev.a1swaara.service.PlayerService
import com.varshudev.a1swaara.utils.playTrackWithService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.time.Instant

data class JamRoomState(
    val roomCode: String,
    val hostId: String,
    val hostName: String,
    val roomName: String,
    val currentSongId: String,
    val currentTitle: String,
    val currentArtist: String,
    val currentArtworkUrl: String?,
    val isPlaying: Boolean,
    val playbackPositionMs: Long,
    val playbackUpdatedAt: Long,
    val isPublic: Boolean,
    val allowDedications: Boolean,
    val activeListenersCount: Int,
    val isHost: Boolean = false
)

data class JamDedication(
    val id: Long,
    val roomCode: String,
    val senderName: String,
    val dedicatedTo: String,
    val songId: String,
    val songTitle: String,
    val message: String,
    val createdAt: Long
)

data class JamQueueItem(
    val id: Long,
    val roomCode: String,
    val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val addedByName: String,
    val votes: Int = 1
)

data class JamVibe(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderName: String,
    val vibeType: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class JamRoomSummary(
    val roomCode: String,
    val roomName: String,
    val hostName: String,
    val currentTitle: String?,
    val activeListeners: Int,
    val lastActiveMs: Long = System.currentTimeMillis()
)

/**
 * Real-time collaborative audio synchronization, democratic queueing, and song dedication engine.
 * Backed primarily by Firebase Firestore for zero-downtime resilient synchronization,
 * with best-effort secondary mirroring to Supabase.
 */
object SwaaraJamEngine {
    private const val TAG = "SwaaraJamEngine"
    private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val INACTIVITY_TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes TTL
    private val random = SecureRandom()

    private val _currentRoom = MutableStateFlow<JamRoomState?>(null)
    val currentRoom: StateFlow<JamRoomState?> = _currentRoom.asStateFlow()

    private val _activeDedications = MutableStateFlow<List<JamDedication>>(emptyList())
    val activeDedications: StateFlow<List<JamDedication>> = _activeDedications.asStateFlow()

    private val _activeQueue = MutableStateFlow<List<JamQueueItem>>(emptyList())
    val activeQueue: StateFlow<List<JamQueueItem>> = _activeQueue.asStateFlow()

    private val _activeVibes = MutableStateFlow<List<JamVibe>>(emptyList())
    val activeVibes: StateFlow<List<JamVibe>> = _activeVibes.asStateFlow()

    private var sessionAuth: SwaaraCloudAuth? = null
    private var syncJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.IO)

    private val localVotes = mutableMapOf<Long, Int>()

    // Helper extensions for flexible Map reading across formats
    private fun Map<String, Any?>.optStr(vararg keys: String, default: String = ""): String {
        for (k in keys) {
            val v = this[k]
            if (v is String && v.isNotBlank()) return v
        }
        return default
    }

    private fun Map<String, Any?>.optLong(vararg keys: String, default: Long = 0L): Long {
        for (k in keys) {
            val v = this[k]
            if (v is Number) return v.toLong()
            if (v is String) {
                val parsed = v.toLongOrNull()
                if (parsed != null) return parsed
                try {
                    return Instant.parse(v).toEpochMilli()
                } catch (_: Exception) {}
            }
        }
        return default
    }

    private fun Map<String, Any?>.optBool(vararg keys: String, default: Boolean = false): Boolean {
        for (k in keys) {
            val v = this[k]
            if (v is Boolean) return v
            if (v is String) return v.toBoolean()
        }
        return default
    }

    private fun Map<String, Any?>.optInt(vararg keys: String, default: Int = 1): Int {
        for (k in keys) {
            val v = this[k]
            if (v is Number) return v.toInt()
            if (v is String) v.toIntOrNull()?.let { return it }
        }
        return default
    }

    fun sendVibe(context: Context, vibeType: String) {
        val auth = SwaaraCloudAuth.getInstance(context)
        val room = _currentRoom.value ?: return
        val vibe = JamVibe(
            senderName = auth.displayName,
            vibeType = vibeType
        )
        val list = _activeVibes.value.toMutableList()
        list.add(vibe)
        if (list.size > 25) {
            list.removeAt(0)
        }
        _activeVibes.value = list

        engineScope.launch {
            try {
                // Update Firestore embedded vibes
                val serializedVibes = list.map { v ->
                    mapOf(
                        "id" to v.id,
                        "senderName" to v.senderName,
                        "vibeType" to v.vibeType,
                        "timestamp" to v.timestamp
                    )
                }
                SwaaraCloudConfig.setDocument("jam_rooms/${room.roomCode}", mapOf("vibes" to serializedVibes))

                // Best-effort Supabase insert
                val token = auth.validAccessToken()
                if (token != null) {
                    val payload = JSONObject().apply {
                        put("room_code", room.roomCode)
                        put("sender_name", auth.displayName)
                        put("dedicated_to", "Jam")
                        put("song_id", room.currentSongId.ifBlank { "vibe" })
                        put("song_title", room.currentTitle.ifBlank { "Session" })
                        put("message", "[VIBE:$vibeType]")
                        put("is_active", true)
                    }
                    runCatching {
                        SwaaraSupabaseClient.insert("jam_dedications", payload.toString(), token)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to broadcast vibe: ${e.message}")
            }
        }
    }

    fun upvoteQueueItem(itemId: Long) {
        val cur = localVotes[itemId] ?: 1
        localVotes[itemId] = cur + 1
        val updated = _activeQueue.value.map { item ->
            if (item.id == itemId) item.copy(votes = item.votes + 1) else item
        }.sortedByDescending { it.votes }
        _activeQueue.value = updated

        val room = _currentRoom.value ?: return
        engineScope.launch {
            try {
                val serializedQueue = serializeQueue(updated)
                SwaaraCloudConfig.setDocument("jam_rooms/${room.roomCode}", mapOf("queue" to serializedQueue))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist queue upvote: ${e.message}")
            }
        }
    }

    fun removeQueueItem(itemId: Long) {
        val room = _currentRoom.value ?: return
        if (!room.isHost) return
        val updated = _activeQueue.value.filter { it.id != itemId }
        _activeQueue.value = updated

        engineScope.launch {
            try {
                val serializedQueue = serializeQueue(updated)
                SwaaraCloudConfig.setDocument("jam_rooms/${room.roomCode}", mapOf("queue" to serializedQueue))
                val token = sessionAuth?.validAccessToken()
                if (token != null) {
                    runCatching {
                        SwaaraSupabaseClient.delete("jam_queue", "id=eq.$itemId", token)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete queue item: ${e.message}")
            }
        }
    }

    fun playQueueItemNow(context: Context, item: JamQueueItem) {
        val room = _currentRoom.value ?: return
        if (!room.isHost) return
        engineScope.launch {
            updatePlayback(
                songId = item.songId,
                title = item.title,
                artist = item.artist,
                artworkUrl = item.artworkUrl,
                isPlaying = true,
                positionMs = 0L
            )
            removeQueueItem(item.id)
            withContext(Dispatchers.Main) {
                playTrackWithService(
                    context = context,
                    binder = PlayerService.activeBinder.value,
                    videoId = item.songId,
                    title = item.title,
                    artist = item.artist,
                    thumbnailUrl = item.artworkUrl
                )
            }
        }
    }

    fun advanceToNextQueueItem(context: Context): Boolean {
        val room = _currentRoom.value ?: return false
        if (!room.isHost) return false
        val nextItem = _activeQueue.value.firstOrNull() ?: return false
        playQueueItemNow(context, nextItem)
        return true
    }

    fun generateRoomCode(): String {
        val sb = StringBuilder("JAM")
        for (i in 0 until 3) {
            sb.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)])
        }
        return sb.toString()
    }

    suspend fun createRoom(
        context: Context,
        roomName: String = "Swaara Live Session",
        isPublic: Boolean = true,
        allowDedications: Boolean = true,
        initialSongId: String = "",
        initialTitle: String = "",
        initialArtist: String = "",
        initialArtworkUrl: String? = null,
        isPlaying: Boolean = false,
        positionMs: Long = 0L
    ): String? = withContext(Dispatchers.IO) {
        val auth = SwaaraCloudAuth.getInstance(context)
        sessionAuth = auth

        val roomCode = generateRoomCode()
        val nowMs = System.currentTimeMillis()
        val cleanRoomName = roomName.trim().ifBlank { "Swaara Live Session" }

        val docData = mutableMapOf<String, Any?>(
            "roomCode" to roomCode,
            "room_code" to roomCode,
            "hostId" to auth.userId,
            "host_id" to auth.userId,
            "hostName" to auth.displayName,
            "host_name" to auth.displayName,
            "roomName" to cleanRoomName,
            "room_name" to cleanRoomName,
            "currentSongId" to initialSongId,
            "current_song_id" to initialSongId,
            "songId" to initialSongId,
            "currentTitle" to initialTitle,
            "current_title" to initialTitle,
            "title" to initialTitle,
            "currentArtist" to initialArtist,
            "current_artist" to initialArtist,
            "artist" to initialArtist,
            "currentArtworkUrl" to (initialArtworkUrl ?: ""),
            "current_artwork_url" to (initialArtworkUrl ?: ""),
            "artworkUrl" to (initialArtworkUrl ?: ""),
            "isPlaying" to isPlaying,
            "is_playing" to isPlaying,
            "playbackPositionMs" to positionMs,
            "playback_position_ms" to positionMs,
            "positionMs" to positionMs,
            "playbackUpdatedAt" to nowMs,
            "playback_updated_at" to nowMs,
            "updatedAt" to nowMs,
            "isPublic" to isPublic,
            "is_public" to isPublic,
            "allowDedications" to allowDedications,
            "allow_dedications" to allowDedications,
            "activeListenersCount" to 1,
            "active_listeners_count" to 1,
            "queue" to emptyList<Map<String, Any?>>(),
            "dedications" to emptyList<Map<String, Any?>>(),
            "vibes" to emptyList<Map<String, Any?>>()
        )

        val firestoreSuccess = SwaaraCloudConfig.setDocument("jam_rooms/$roomCode", docData)
        if (!firestoreSuccess) {
            Log.e(TAG, "Failed to create Jam room document in Firestore.")
            return@withContext null
        }

        // Secondary best-effort Supabase insert
        val token = auth.validAccessToken()
        if (token != null) {
            engineScope.launch {
                try {
                    val payload = JSONObject().apply {
                        put("room_code", roomCode)
                        put("host_id", auth.userId)
                        put("host_name", auth.displayName)
                        put("room_name", cleanRoomName)
                        put("current_song_id", initialSongId)
                        put("current_title", initialTitle)
                        put("current_artist", initialArtist)
                        put("current_artwork_url", initialArtworkUrl ?: "")
                        put("is_playing", isPlaying)
                        put("playback_position_ms", positionMs)
                        put("playback_updated_at", Instant.ofEpochMilli(nowMs).toString())
                        put("is_public", isPublic)
                        put("allow_dedications", allowDedications)
                        put("active_listeners_count", 1)
                    }
                    SwaaraSupabaseClient.insert("jam_rooms", payload.toString(), token)
                } catch (e: Exception) {
                    Log.w(TAG, "Secondary Supabase insert skipped: ${e.message}")
                }
            }
        }

        val state = JamRoomState(
            roomCode = roomCode,
            hostId = auth.userId,
            hostName = auth.displayName,
            roomName = cleanRoomName,
            currentSongId = initialSongId,
            currentTitle = initialTitle,
            currentArtist = initialArtist,
            currentArtworkUrl = initialArtworkUrl,
            isPlaying = isPlaying,
            playbackPositionMs = positionMs,
            playbackUpdatedAt = nowMs,
            isPublic = isPublic,
            allowDedications = allowDedications,
            activeListenersCount = 1,
            isHost = true
        )
        _currentRoom.value = state
        _activeQueue.value = emptyList()
        _activeDedications.value = emptyList()
        _activeVibes.value = emptyList()
        startSyncLoop(roomCode, isHost = true)
        roomCode
    }

    suspend fun joinRoom(context: Context, roomCode: String): Boolean = withContext(Dispatchers.IO) {
        val auth = SwaaraCloudAuth.getInstance(context)
        sessionAuth = auth

        var cleanCode = roomCode.trim().uppercase()
        if (!cleanCode.startsWith("JAM") && cleanCode.length == 3) {
            cleanCode = "JAM$cleanCode"
        }
        if (cleanCode.length < 4) return@withContext false

        // Fetch from Firestore
        var doc = SwaaraCloudConfig.getDocument("jam_rooms/$cleanCode")
        if (doc == null && cleanCode.startsWith("JAM")) {
            // Also try raw code in case it was stored without JAM prefix
            doc = SwaaraCloudConfig.getDocument("jam_rooms/${cleanCode.removePrefix("JAM")}")
        }
        if (doc == null) {
            Log.w(TAG, "Room $cleanCode not found in Firestore.")
            return@withContext false
        }

        val now = System.currentTimeMillis()
        val updatedAtMs = doc.optLong("playbackUpdatedAt", "playback_updated_at", "updatedAt", default = now)

        if (now - updatedAtMs > INACTIVITY_TIMEOUT_MS) {
            Log.i(TAG, "Room $cleanCode is inactive (>10 mins).")
            return@withContext false
        }

        val hostId = doc.optStr("hostId", "host_id")
        val isHost = (hostId.isNotBlank() && hostId == auth.userId)
        val currentListeners = doc.optInt("activeListenersCount", "active_listeners_count", default = 1)
        val newListenersCount = if (isHost) currentListeners else currentListeners + 1

        val artwork = doc.optStr("currentArtworkUrl", "current_artwork_url", "artworkUrl").ifBlank { null }
        val state = JamRoomState(
            roomCode = cleanCode,
            hostId = hostId.ifBlank { "Host" },
            hostName = doc.optStr("hostName", "host_name", default = "Host"),
            roomName = doc.optStr("roomName", "room_name", default = "Jam Room"),
            currentSongId = doc.optStr("currentSongId", "current_song_id", "songId"),
            currentTitle = doc.optStr("currentTitle", "current_title", "title"),
            currentArtist = doc.optStr("currentArtist", "current_artist", "artist"),
            currentArtworkUrl = artwork,
            isPlaying = doc.optBool("isPlaying", "is_playing", default = false),
            playbackPositionMs = doc.optLong("playbackPositionMs", "playback_position_ms", "positionMs", default = 0L),
            playbackUpdatedAt = updatedAtMs,
            isPublic = doc.optBool("isPublic", "is_public", default = true),
            allowDedications = doc.optBool("allowDedications", "allow_dedications", default = true),
            activeListenersCount = newListenersCount,
            isHost = isHost
        )

        // Parse embedded sub-collections
        parseEmbeddedCollections(doc, cleanCode)

        // Update listener presence in Firestore if joined as listener
        if (!isHost) {
            engineScope.launch {
                SwaaraCloudConfig.setDocument(
                    "jam_rooms/$cleanCode",
                    mapOf("activeListenersCount" to newListenersCount, "active_listeners_count" to newListenersCount)
                )
            }
        }

        _currentRoom.value = state
        startSyncLoop(cleanCode, isHost)
        true
    }

    suspend fun leaveRoom(context: Context) = withContext(Dispatchers.IO) {
        val room = _currentRoom.value ?: return@withContext
        syncJob?.cancel()
        syncJob = null

        if (room.isHost) {
            SwaaraCloudConfig.deleteDocument("jam_rooms/${room.roomCode}")
            val token = sessionAuth?.validAccessToken()
            if (token != null) {
                runCatching {
                    SwaaraSupabaseClient.delete("jam_rooms", "room_code=eq.${room.roomCode}", token)
                }
            }
        } else {
            val decremented = (room.activeListenersCount - 1).coerceAtLeast(1)
            SwaaraCloudConfig.setDocument(
                "jam_rooms/${room.roomCode}",
                mapOf("activeListenersCount" to decremented, "active_listeners_count" to decremented)
            )
        }
        sessionAuth = null

        _currentRoom.value = null
        _activeDedications.value = emptyList()
        _activeQueue.value = emptyList()
        _activeVibes.value = emptyList()
    }

    suspend fun updatePlayback(
        songId: String,
        title: String,
        artist: String,
        artworkUrl: String?,
        isPlaying: Boolean,
        positionMs: Long
    ) = withContext(Dispatchers.IO) {
        val room = _currentRoom.value ?: return@withContext
        if (!room.isHost) return@withContext

        val nowMs = System.currentTimeMillis()

        val updateMap = mapOf<String, Any?>(
            "currentSongId" to songId,
            "current_song_id" to songId,
            "songId" to songId,
            "currentTitle" to title,
            "current_title" to title,
            "title" to title,
            "currentArtist" to artist,
            "current_artist" to artist,
            "artist" to artist,
            "currentArtworkUrl" to (artworkUrl ?: ""),
            "current_artwork_url" to (artworkUrl ?: ""),
            "artworkUrl" to (artworkUrl ?: ""),
            "isPlaying" to isPlaying,
            "is_playing" to isPlaying,
            "playbackPositionMs" to positionMs,
            "playback_position_ms" to positionMs,
            "positionMs" to positionMs,
            "playbackUpdatedAt" to nowMs,
            "playback_updated_at" to nowMs,
            "updatedAt" to nowMs
        )

        SwaaraCloudConfig.setDocument("jam_rooms/${room.roomCode}", updateMap)

        // Best effort Supabase patch
        val token = sessionAuth?.validAccessToken()
        if (token != null) {
            engineScope.launch {
                try {
                    val payload = JSONObject().apply {
                        put("current_song_id", songId)
                        put("current_title", title)
                        put("current_artist", artist)
                        put("current_artwork_url", artworkUrl ?: "")
                        put("is_playing", isPlaying)
                        put("playback_position_ms", positionMs)
                        put("playback_updated_at", Instant.ofEpochMilli(nowMs).toString())
                    }
                    SwaaraSupabaseClient.patch("jam_rooms", payload.toString(), "room_code=eq.${room.roomCode}", token)
                } catch (e: Exception) {
                    Log.w(TAG, "Secondary Supabase playback patch skipped: ${e.message}")
                }
            }
        }

        _currentRoom.value = room.copy(
            currentSongId = songId,
            currentTitle = title,
            currentArtist = artist,
            currentArtworkUrl = artworkUrl,
            isPlaying = isPlaying,
            playbackPositionMs = positionMs,
            playbackUpdatedAt = nowMs
        )
    }

    suspend fun sendDedication(
        context: Context,
        dedicatedTo: String,
        songId: String,
        songTitle: String,
        message: String
    ): Boolean = withContext(Dispatchers.IO) {
        val room = _currentRoom.value ?: return@withContext false
        val auth = SwaaraCloudAuth.getInstance(context)

        val newDedication = JamDedication(
            id = System.currentTimeMillis(),
            roomCode = room.roomCode,
            senderName = auth.displayName,
            dedicatedTo = dedicatedTo.trim(),
            songId = songId,
            songTitle = songTitle,
            message = message.trim(),
            createdAt = System.currentTimeMillis()
        )

        val list = _activeDedications.value.toMutableList()
        list.add(0, newDedication)
        if (list.size > 20) {
            list.removeAt(list.size - 1)
        }
        _activeDedications.value = list

        val serialized = list.map { d ->
            mapOf(
                "id" to d.id,
                "roomCode" to d.roomCode,
                "senderName" to d.senderName,
                "dedicatedTo" to d.dedicatedTo,
                "songId" to d.songId,
                "songTitle" to d.songTitle,
                "message" to d.message,
                "createdAt" to d.createdAt
            )
        }
        val success = SwaaraCloudConfig.setDocument("jam_rooms/${room.roomCode}", mapOf("dedications" to serialized))

        // Best effort Supabase mirror
        val token = auth.validAccessToken()
        if (token != null) {
            engineScope.launch {
                try {
                    val payload = JSONObject().apply {
                        put("room_code", room.roomCode)
                        put("sender_name", auth.displayName)
                        put("dedicated_to", dedicatedTo.trim())
                        put("song_id", songId)
                        put("song_title", songTitle)
                        put("message", message.trim())
                        put("is_active", true)
                    }
                    SwaaraSupabaseClient.insert("jam_dedications", payload.toString(), token)
                } catch (e: Exception) {
                    Log.w(TAG, "Secondary Supabase dedication skipped: ${e.message}")
                }
            }
        }
        success
    }

    suspend fun addSongToQueue(
        context: Context,
        songId: String,
        title: String,
        artist: String,
        artworkUrl: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val room = _currentRoom.value ?: return@withContext false
        val auth = SwaaraCloudAuth.getInstance(context)

        val currentList = _activeQueue.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.songId == songId }
        if (existingIndex >= 0) {
            val item = currentList[existingIndex]
            currentList[existingIndex] = item.copy(votes = item.votes + 1)
        } else {
            currentList.add(
                JamQueueItem(
                    id = System.currentTimeMillis(),
                    roomCode = room.roomCode,
                    songId = songId,
                    title = title,
                    artist = artist,
                    artworkUrl = artworkUrl,
                    addedByName = auth.displayName,
                    votes = 1
                )
            )
        }
        val sorted = currentList.sortedByDescending { it.votes }
        _activeQueue.value = sorted

        val serialized = serializeQueue(sorted)
        val success = SwaaraCloudConfig.setDocument("jam_rooms/${room.roomCode}", mapOf("queue" to serialized))

        // Best effort Supabase mirror
        val token = auth.validAccessToken()
        if (token != null) {
            engineScope.launch {
                try {
                    val payload = JSONObject().apply {
                        put("room_code", room.roomCode)
                        put("song_id", songId)
                        put("title", title)
                        put("artist", artist)
                        put("artwork_url", artworkUrl ?: "")
                        put("added_by_name", auth.displayName)
                    }
                    SwaaraSupabaseClient.insert("jam_queue", payload.toString(), token)
                } catch (e: Exception) {
                    Log.w(TAG, "Secondary Supabase queue skipped: ${e.message}")
                }
            }
        }
        success
    }

    suspend fun listPublicRooms(): List<JamRoomSummary> = withContext(Dispatchers.IO) {
        try {
            val docs = SwaaraCloudConfig.listDocuments("jam_rooms", pageSize = 30)
            val now = System.currentTimeMillis()
            val list = mutableListOf<JamRoomSummary>()

            for ((docId, fields) in docs) {
                val isPublic = fields.optBool("isPublic", "is_public", default = true)
                if (!isPublic) continue

                val updatedAtMs = fields.optLong("playbackUpdatedAt", "playback_updated_at", "updatedAt", default = now)
                if (now - updatedAtMs > INACTIVITY_TIMEOUT_MS) continue

                val code = fields.optStr("roomCode", "room_code", default = docId)
                val roomName = fields.optStr("roomName", "room_name", default = "Jam Room")
                val hostName = fields.optStr("hostName", "host_name", default = "Host")
                val currentTitle = fields.optStr("currentTitle", "current_title", "title").ifBlank { null }
                val listeners = fields.optInt("activeListenersCount", "active_listeners_count", default = 1)

                list.add(
                    JamRoomSummary(
                        roomCode = code,
                        roomName = roomName,
                        hostName = hostName,
                        currentTitle = currentTitle,
                        activeListeners = listeners,
                        lastActiveMs = updatedAtMs
                    )
                )
            }
            list.sortedByDescending { it.lastActiveMs }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing public rooms from Firestore", e)
            emptyList()
        }
    }

    private fun serializeQueue(queue: List<JamQueueItem>): List<Map<String, Any?>> {
        return queue.map { q ->
            mapOf(
                "id" to q.id,
                "roomCode" to q.roomCode,
                "songId" to q.songId,
                "title" to q.title,
                "artist" to q.artist,
                "artworkUrl" to (q.artworkUrl ?: ""),
                "addedByName" to q.addedByName,
                "votes" to q.votes
            )
        }
    }

    private fun parseEmbeddedCollections(doc: Map<String, Any?>, roomCode: String) {
        // 1. Queue
        val queueRaw = doc["queue"] as? List<*>
        if (queueRaw != null) {
            val parsedQueue = mutableListOf<JamQueueItem>()
            for (item in queueRaw) {
                if (item is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    val m = item as Map<String, Any?>
                    val itemId = m.optLong("id", default = System.currentTimeMillis())
                    val votes = localVotes[itemId] ?: m.optInt("votes", default = 1)
                    parsedQueue.add(
                        JamQueueItem(
                            id = itemId,
                            roomCode = m.optStr("roomCode", "room_code", default = roomCode),
                            songId = m.optStr("songId", "song_id"),
                            title = m.optStr("title"),
                            artist = m.optStr("artist"),
                            artworkUrl = m.optStr("artworkUrl", "artwork_url").ifBlank { null },
                            addedByName = m.optStr("addedByName", "added_by_name", default = "Listener"),
                            votes = votes
                        )
                    )
                }
            }
            _activeQueue.value = parsedQueue.sortedByDescending { it.votes }
        }

        // 2. Dedications
        val dedRaw = doc["dedications"] as? List<*>
        if (dedRaw != null) {
            val parsedDedications = mutableListOf<JamDedication>()
            for (item in dedRaw) {
                if (item is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    val m = item as Map<String, Any?>
                    parsedDedications.add(
                        JamDedication(
                            id = m.optLong("id", default = System.currentTimeMillis()),
                            roomCode = m.optStr("roomCode", "room_code", default = roomCode),
                            senderName = m.optStr("senderName", "sender_name", default = "Friend"),
                            dedicatedTo = m.optStr("dedicatedTo", "dedicated_to", default = "Everyone"),
                            songId = m.optStr("songId", "song_id"),
                            songTitle = m.optStr("songTitle", "song_title"),
                            message = m.optStr("message"),
                            createdAt = m.optLong("createdAt", "created_at", default = System.currentTimeMillis())
                        )
                    )
                }
            }
            _activeDedications.value = parsedDedications
        }

        // 3. Vibes
        val vibesRaw = doc["vibes"] as? List<*>
        if (vibesRaw != null) {
            val parsedVibes = mutableListOf<JamVibe>()
            for (item in vibesRaw) {
                if (item is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    val m = item as Map<String, Any?>
                    parsedVibes.add(
                        JamVibe(
                            id = m.optStr("id", default = java.util.UUID.randomUUID().toString()),
                            senderName = m.optStr("senderName", "sender_name", default = "Listener"),
                            vibeType = m.optStr("vibeType", "vibe_type", default = "HEART"),
                            timestamp = m.optLong("timestamp", default = System.currentTimeMillis())
                        )
                    )
                }
            }
            _activeVibes.value = parsedVibes
        }
    }

    private fun startSyncLoop(roomCode: String, isHost: Boolean) {
        syncJob?.cancel()
        syncJob = engineScope.launch {
            var heartbeatCounter = 0
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()

                    if (isHost) {
                        // Host pulse: sends heartbeat every ~8 seconds to keep room active
                        heartbeatCounter++
                        if (heartbeatCounter >= 4) {
                            heartbeatCounter = 0
                            val pulseMap = mapOf<String, Any?>(
                                "playbackUpdatedAt" to now,
                                "playback_updated_at" to now,
                                "updatedAt" to now
                            )
                            SwaaraCloudConfig.setDocument("jam_rooms/$roomCode", pulseMap)
                        }

                        // Host reads incoming queue items, dedications, and vibes
                        val doc = SwaaraCloudConfig.getDocument("jam_rooms/$roomCode")
                        if (doc != null) {
                            parseEmbeddedCollections(doc, roomCode)
                            val listeners = doc.optInt("activeListenersCount", "active_listeners_count", default = 1)
                            _currentRoom.value = _currentRoom.value?.copy(activeListenersCount = listeners)
                        }
                    } else {
                        // Listener sync: fetch full room state from Firestore
                        val doc = SwaaraCloudConfig.getDocument("jam_rooms/$roomCode")
                        if (doc != null) {
                            val updatedAtMs = doc.optLong("playbackUpdatedAt", "playback_updated_at", "updatedAt", default = now)

                            // Expire if host has been inactive > 10 mins
                            if (now - updatedAtMs > INACTIVITY_TIMEOUT_MS) {
                                Log.i(TAG, "Room $roomCode timed out due to 10 minutes of inactivity.")
                                _currentRoom.value = null
                                break
                            }

                            val artwork = doc.optStr("currentArtworkUrl", "current_artwork_url", "artworkUrl").ifBlank { null }
                            _currentRoom.value = _currentRoom.value?.copy(
                                currentSongId = doc.optStr("currentSongId", "current_song_id", "songId"),
                                currentTitle = doc.optStr("currentTitle", "current_title", "title"),
                                currentArtist = doc.optStr("currentArtist", "current_artist", "artist"),
                                currentArtworkUrl = artwork,
                                isPlaying = doc.optBool("isPlaying", "is_playing", default = false),
                                playbackPositionMs = doc.optLong("playbackPositionMs", "playback_position_ms", "positionMs", default = 0L),
                                playbackUpdatedAt = updatedAtMs,
                                activeListenersCount = doc.optInt("activeListenersCount", "active_listeners_count", default = 1)
                            )
                            parseEmbeddedCollections(doc, roomCode)
                        } else {
                            // Document deleted by host -> room ended
                            Log.i(TAG, "Room $roomCode was ended by the host.")
                            _currentRoom.value = null
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Sync loop exception: ${e.message}")
                }
                delay(2000)
            }
        }
    }
}
