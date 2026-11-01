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
 * Implements 10-minute inactivity auto-purging and lockstep host-to-listener synchronization.
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

    private var syncJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.IO)

    private val localVotes = mutableMapOf<Long, Int>()

    fun sendVibe(context: Context, vibeType: String) {
        val auth = SwaaraCloudAuth.getInstance(context)
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

        val room = _currentRoom.value
        if (room != null) {
            engineScope.launch {
                try {
                    val payload = JSONObject().apply {
                        put("room_code", room.roomCode)
                        put("sender_name", auth.displayName)
                        put("dedicated_to", "Jam")
                        put("song_id", room.currentSongId.ifBlank { "vibe" })
                        put("song_title", room.currentTitle.ifBlank { "Session" })
                        put("message", "[VIBE:$vibeType]")
                        put("is_active", true)
                    }
                    SwaaraSupabaseClient.insert("jam_dedications", payload.toString(), auth.accessToken)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to broadcast vibe: ${e.message}")
                }
            }
        }
    }

    fun upvoteQueueItem(itemId: Long) {
        val cur = localVotes[itemId] ?: 1
        localVotes[itemId] = cur + 1
        _activeQueue.value = _activeQueue.value.map { item ->
            if (item.id == itemId) item.copy(votes = item.votes + 1) else item
        }.sortedByDescending { it.votes }
    }

    fun removeQueueItem(itemId: Long) {
        _activeQueue.value = _activeQueue.value.filter { it.id != itemId }
        engineScope.launch {
            try {
                SwaaraSupabaseClient.delete("jam_queue", "id=eq.$itemId")
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

    /**
     * Purges rooms where no activity or heartbeat has been reported for 10 minutes.
     */
    suspend fun purgeInactiveRooms() = withContext(Dispatchers.IO) {
        try {
            val cutoffIso = Instant.ofEpochMilli(System.currentTimeMillis() - INACTIVITY_TIMEOUT_MS).toString()
            SwaaraSupabaseClient.delete("jam_rooms", "playback_updated_at=lt.$cutoffIso")
        } catch (e: Exception) {
            Log.w(TAG, "Error purging inactive rooms: ${e.message}")
        }
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
        if (!auth.isSignedIn) {
            Log.w(TAG, "Google Sign-In is required to host a Jam session.")
            return@withContext null
        }

        val roomCode = generateRoomCode()
        val nowMs = System.currentTimeMillis()
        val nowIso = Instant.ofEpochMilli(nowMs).toString()

        val payload = JSONObject().apply {
            put("room_code", roomCode)
            put("host_id", auth.userId)
            put("host_name", auth.displayName)
            put("room_name", roomName.trim().ifBlank { "Swaara Live Session" })
            put("current_song_id", initialSongId)
            put("current_title", initialTitle)
            put("current_artist", initialArtist)
            put("current_artwork_url", initialArtworkUrl ?: "")
            put("is_playing", isPlaying)
            put("playback_position_ms", positionMs)
            put("playback_updated_at", nowIso)
            put("is_public", isPublic)
            put("allow_dedications", allowDedications)
            put("active_listeners_count", 1)
        }

        val success = SwaaraSupabaseClient.insert("jam_rooms", payload.toString(), auth.accessToken)
        if (success) {
            val state = JamRoomState(
                roomCode = roomCode,
                hostId = auth.userId,
                hostName = auth.displayName,
                roomName = roomName.trim().ifBlank { "Swaara Live Session" },
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
            startSyncLoop(roomCode, isHost = true)
            roomCode
        } else {
            null
        }
    }

    suspend fun joinRoom(context: Context, roomCode: String): Boolean = withContext(Dispatchers.IO) {
        val auth = SwaaraCloudAuth.getInstance(context)
        if (!auth.isSignedIn) {
            Log.w(TAG, "Google Sign-In is required to join a Jam session.")
            return@withContext false
        }

        val cleanCode = roomCode.trim().uppercase()
        val json = SwaaraSupabaseClient.get("jam_rooms", "room_code=eq.$cleanCode")
        if (json == null || json.length() == 0) return@withContext false

        val row = json.getJSONObject(0)
        val hostId = row.optString("host_id")
        val isHost = (hostId == auth.userId)

        // Validate 10-minute inactivity TTL
        val updatedAtStr = row.optString("playback_updated_at")
        val updatedAtMs = try {
            Instant.parse(updatedAtStr).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        if (System.currentTimeMillis() - updatedAtMs > INACTIVITY_TIMEOUT_MS) {
            Log.i(TAG, "Room $cleanCode is inactive (>10 mins). Purging.")
            SwaaraSupabaseClient.delete("jam_rooms", "room_code=eq.$cleanCode")
            return@withContext false
        }

        val state = JamRoomState(
            roomCode = cleanCode,
            hostId = hostId,
            hostName = row.optString("host_name", "Host"),
            roomName = row.optString("room_name", "Jam Room"),
            currentSongId = row.optString("current_song_id"),
            currentTitle = row.optString("current_title"),
            currentArtist = row.optString("current_artist"),
            currentArtworkUrl = if (row.has("current_artwork_url") && !row.isNull("current_artwork_url")) row.getString("current_artwork_url") else null,
            isPlaying = row.optBoolean("is_playing", false),
            playbackPositionMs = row.optLong("playback_position_ms", 0L),
            playbackUpdatedAt = updatedAtMs,
            isPublic = row.optBoolean("is_public", true),
            allowDedications = row.optBoolean("allow_dedications", true),
            activeListenersCount = row.optInt("active_listeners_count", 1) + (if (isHost) 0 else 1),
            isHost = isHost
        )

        if (!isHost) {
            val updatePayload = JSONObject().apply {
                put("active_listeners_count", state.activeListenersCount)
            }
            SwaaraSupabaseClient.patch("jam_rooms", updatePayload.toString(), "room_code=eq.$cleanCode")
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
            SwaaraSupabaseClient.delete("jam_rooms", "room_code=eq.${room.roomCode}")
        } else {
            val newCount = (room.activeListenersCount - 1).coerceAtLeast(1)
            val updatePayload = JSONObject().apply {
                put("active_listeners_count", newCount)
            }
            SwaaraSupabaseClient.patch("jam_rooms", updatePayload.toString(), "room_code=eq.${room.roomCode}")
        }

        _currentRoom.value = null
        _activeDedications.value = emptyList()
        _activeQueue.value = emptyList()
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
        val nowIso = Instant.ofEpochMilli(nowMs).toString()

        val payload = JSONObject().apply {
            put("current_song_id", songId)
            put("current_title", title)
            put("current_artist", artist)
            put("current_artwork_url", artworkUrl ?: "")
            put("is_playing", isPlaying)
            put("playback_position_ms", positionMs)
            put("playback_updated_at", nowIso)
        }

        SwaaraSupabaseClient.patch("jam_rooms", payload.toString(), "room_code=eq.${room.roomCode}")

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

        val payload = JSONObject().apply {
            put("room_code", room.roomCode)
            put("sender_name", auth.displayName)
            put("dedicated_to", dedicatedTo.trim())
            put("song_id", songId)
            put("song_title", songTitle)
            put("message", message.trim())
            put("is_active", true)
        }

        val success = SwaaraSupabaseClient.insert("jam_dedications", payload.toString(), auth.accessToken)
        if (success) {
            refreshDedications(room.roomCode)
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

        val payload = JSONObject().apply {
            put("room_code", room.roomCode)
            put("song_id", songId)
            put("title", title)
            put("artist", artist)
            put("artwork_url", artworkUrl ?: "")
            put("added_by_name", auth.displayName)
        }

        val success = SwaaraSupabaseClient.insert("jam_queue", payload.toString(), auth.accessToken)
        if (success) {
            refreshQueue(room.roomCode)
        }
        success
    }

    suspend fun listPublicRooms(): List<JamRoomSummary> = withContext(Dispatchers.IO) {
        try {
            purgeInactiveRooms()

            val json = SwaaraSupabaseClient.get("jam_rooms", "is_public=eq.true&order=created_at.desc&limit=25")
                ?: return@withContext emptyList()

            val list = mutableListOf<JamRoomSummary>()
            val now = System.currentTimeMillis()

            for (i in 0 until json.length()) {
                val row = json.getJSONObject(i)
                val updatedAtStr = row.optString("playback_updated_at")
                val updatedAtMs = try {
                    Instant.parse(updatedAtStr).toEpochMilli()
                } catch (_: Exception) {
                    now
                }

                // Discard inactive rooms older than 10 minutes
                if (now - updatedAtMs <= INACTIVITY_TIMEOUT_MS) {
                    list.add(
                        JamRoomSummary(
                            roomCode = row.getString("room_code"),
                            roomName = row.optString("room_name", "Jam Room"),
                            hostName = row.optString("host_name", "Host"),
                            currentTitle = if (row.has("current_title") && !row.isNull("current_title")) row.getString("current_title") else null,
                            activeListeners = row.optInt("active_listeners_count", 1),
                            lastActiveMs = updatedAtMs
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error listing public rooms", e)
            emptyList()
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
                        // Host heartbeat every ~12.5 seconds (every 5 iterations of 2500ms delay)
                        heartbeatCounter++
                        if (heartbeatCounter >= 5) {
                            heartbeatCounter = 0
                            val pulsePayload = JSONObject().apply {
                                put("playback_updated_at", Instant.now().toString())
                            }
                            SwaaraSupabaseClient.patch("jam_rooms", pulsePayload.toString(), "room_code=eq.$roomCode")
                        }
                    } else {
                        // Listener sync
                        val json = SwaaraSupabaseClient.get("jam_rooms", "room_code=eq.$roomCode")
                        if (json != null && json.length() > 0) {
                            val row = json.getJSONObject(0)
                            val updatedAtStr = row.optString("playback_updated_at")
                            val updatedAtMs = try {
                                Instant.parse(updatedAtStr).toEpochMilli()
                            } catch (_: Exception) {
                                now
                            }

                            // If room has been silent for > 10 minutes, session expired
                            if (now - updatedAtMs > INACTIVITY_TIMEOUT_MS) {
                                Log.i(TAG, "Room $roomCode timed out due to 10 minutes of inactivity.")
                                _currentRoom.value = null
                                break
                            }

                            _currentRoom.value = _currentRoom.value?.copy(
                                currentSongId = row.optString("current_song_id"),
                                currentTitle = row.optString("current_title"),
                                currentArtist = row.optString("current_artist"),
                                currentArtworkUrl = if (row.has("current_artwork_url") && !row.isNull("current_artwork_url")) row.getString("current_artwork_url") else null,
                                isPlaying = row.optBoolean("is_playing", false),
                                playbackPositionMs = row.optLong("playback_position_ms", 0L),
                                playbackUpdatedAt = updatedAtMs,
                                activeListenersCount = row.optInt("active_listeners_count", 1)
                            )
                        } else {
                            // Room was ended by host
                            _currentRoom.value = null
                            break
                        }
                    }

                    // Refresh Dedications & Queue every pulse
                    refreshDedications(roomCode)
                    refreshQueue(roomCode)
                } catch (e: Exception) {
                    Log.w(TAG, "Sync loop exception: ${e.message}")
                }
                delay(2500)
            }
        }
    }

    private suspend fun refreshDedications(roomCode: String) {
        val dedJson = SwaaraSupabaseClient.get(
            "jam_dedications",
            "room_code=eq.$roomCode&is_active=eq.true&order=created_at.desc&limit=15"
        ) ?: return

        val dedications = mutableListOf<JamDedication>()
        val vibesToAdd = mutableListOf<JamVibe>()

        for (i in 0 until dedJson.length()) {
            val d = dedJson.getJSONObject(i)
            val msg = d.getString("message")
            if (msg.startsWith("[VIBE:") && msg.endsWith("]")) {
                val vType = msg.substring(6, msg.length - 1)
                val vSender = d.getString("sender_name")
                val vId = d.optString("id", java.util.UUID.randomUUID().toString())
                val vTime = d.optLong("created_at", System.currentTimeMillis())
                vibesToAdd.add(JamVibe(id = vId, senderName = vSender, vibeType = vType, timestamp = vTime))
            } else {
                dedications.add(
                    JamDedication(
                        id = d.optLong("id", System.currentTimeMillis()),
                        roomCode = roomCode,
                        senderName = d.getString("sender_name"),
                        dedicatedTo = d.getString("dedicated_to"),
                        songId = d.getString("song_id"),
                        songTitle = d.getString("song_title"),
                        message = msg,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
        _activeDedications.value = dedications

        if (vibesToAdd.isNotEmpty()) {
            val currentVibes = _activeVibes.value.toMutableList()
            var changed = false
            for (newVibe in vibesToAdd) {
                if (currentVibes.none { it.id == newVibe.id }) {
                    currentVibes.add(newVibe)
                    changed = true
                }
            }
            if (changed) {
                while (currentVibes.size > 25) {
                    currentVibes.removeAt(0)
                }
                _activeVibes.value = currentVibes
            }
        }
    }

    private suspend fun refreshQueue(roomCode: String) {
        val qJson = SwaaraSupabaseClient.get(
            "jam_queue",
            "room_code=eq.$roomCode&order=created_at.asc&limit=30"
        ) ?: return

        val queue = mutableListOf<JamQueueItem>()
        for (i in 0 until qJson.length()) {
            val q = qJson.getJSONObject(i)
            val itemId = q.optLong("id", (i + 1).toLong())
            val votes = localVotes[itemId] ?: q.optInt("votes", 1)
            queue.add(
                JamQueueItem(
                    id = itemId,
                    roomCode = roomCode,
                    songId = q.getString("song_id"),
                    title = q.getString("title"),
                    artist = q.getString("artist"),
                    artworkUrl = if (q.has("artwork_url") && !q.isNull("artwork_url")) q.getString("artwork_url") else null,
                    addedByName = q.getString("added_by_name"),
                    votes = votes
                )
            )
        }
        _activeQueue.value = queue.sortedByDescending { it.votes }
    }
}
