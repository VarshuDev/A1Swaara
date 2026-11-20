package com.varshudev.a1swaara.cloud

import android.content.Context
import android.os.Build
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
import java.util.UUID

data class SwaaraDevice(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String, // "android", "windows", "web"
    val lastSeenMs: Long,
    val currentSongId: String?,
    val currentTitle: String?,
    val currentArtist: String?,
    val isPlaying: Boolean,
    val isThisDevice: Boolean = false
)

data class SwaaraConnectTransferCommand(
    val targetDeviceId: String,
    val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val positionMs: Long,
    val isPlaying: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Sovereign Cross-Device Handoff & Remote Control Engine for A1 Swaara.
 * Allows instant, zero-latency playback transfer between Android, Windows PC, and Web.
 */
object SwaaraConnectEngine {
    private const val TAG = "SwaaraConnectEngine"
    private const val PREFS_NAME = "a1swaara_connect_prefs"
    private const val KEY_DEVICE_ID = "connect_device_id"

    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    private var localDeviceId: String = ""
    private var localDeviceName: String = ""

    private val _availableDevices = MutableStateFlow<List<SwaaraDevice>>(emptyList())
    val availableDevices: StateFlow<List<SwaaraDevice>> = _availableDevices.asStateFlow()

    private val _activeRemoteDevice = MutableStateFlow<SwaaraDevice?>(null)
    val activeRemoteDevice: StateFlow<SwaaraDevice?> = _activeRemoteDevice.asStateFlow()

    fun initialize(context: Context) {
        if (localDeviceId.isNotBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = "swr-dev-" + UUID.randomUUID().toString().take(12)
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        localDeviceId = id
        localDeviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        Log.i(TAG, "Initialized SwaaraConnect on $localDeviceName (ID: $localDeviceId)")
    }

    fun startSession(context: Context, binder: PlayerService.Binder?) {
        initialize(context)
        if (syncJob?.isActive == true) return

        syncJob = scope.launch {
            while (isActive) {
                runCatching {
                    publishLocalPresence(binder)
                    pollIncomingCommands(context, binder)
                }.onFailure {
                    Log.w(TAG, "Connect sync tick failed", it)
                }
                delay(8000L) // 8s heartbeat
            }
        }
    }

    private suspend fun publishLocalPresence(binder: PlayerService.Binder?) {
        val snapshot = withContext(Dispatchers.Main) {
            val player = binder?.player
            val currentItem = player?.currentMediaItem
            val metadata = currentItem?.mediaMetadata
            mapOf<String, Any?>(
                "deviceId" to localDeviceId,
                "deviceName" to localDeviceName,
                "deviceType" to "android",
                "lastSeenMs" to System.currentTimeMillis(),
                "currentSongId" to (currentItem?.mediaId ?: ""),
                "currentTitle" to (metadata?.title?.toString() ?: ""),
                "currentArtist" to (metadata?.artist?.toString() ?: ""),
                "artworkUrl" to (metadata?.artworkUri?.toString() ?: ""),
                "positionMs" to (player?.currentPosition ?: 0L),
                "isPlaying" to (player?.isPlaying ?: false)
            )
        }

        withContext(Dispatchers.IO) {
            SwaaraCloudConfig.setDocument("connect_devices/$localDeviceId", snapshot)
        }
    }

    suspend fun refreshDevices() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val found = mutableListOf<SwaaraDevice>()
        val seenIds = mutableSetOf<String>()

        // Dynamically discover all active devices registered in connect_devices
        val listed = SwaaraCloudConfig.listDocuments("connect_devices", pageSize = 25)
        for ((id, doc) in listed) {
            if (id == localDeviceId || !seenIds.add(id)) continue
            val lastSeen = (doc["lastSeenMs"] as? Number)?.toLong() ?: 0L
            if (now - lastSeen < 90_000L) {
                found.add(
                    SwaaraDevice(
                        deviceId = id,
                        deviceName = doc["deviceName"] as? String ?: "Swaara Device",
                        deviceType = doc["deviceType"] as? String ?: "windows",
                        lastSeenMs = lastSeen,
                        currentSongId = doc["currentSongId"] as? String,
                        currentTitle = doc["currentTitle"] as? String,
                        currentArtist = doc["currentArtist"] as? String,
                        isPlaying = (doc["isPlaying"] as? Boolean) ?: false,
                        isThisDevice = false
                    )
                )
            }
        }

        // Also check well-known desktop & web device IDs if collection listing returned empty
        if (found.isEmpty()) {
            val knownIds = listOf("swaara-pc-desktop", "swaara-web-player")
            for (id in knownIds) {
                if (id == localDeviceId || !seenIds.add(id)) continue
                val doc = SwaaraCloudConfig.getDocument("connect_devices/$id") ?: continue
                val lastSeen = (doc["lastSeenMs"] as? Number)?.toLong() ?: 0L
                if (now - lastSeen < 90_000L) {
                    found.add(
                        SwaaraDevice(
                            deviceId = id,
                            deviceName = doc["deviceName"] as? String ?: "Swaara PC",
                            deviceType = doc["deviceType"] as? String ?: "windows",
                            lastSeenMs = lastSeen,
                            currentSongId = doc["currentSongId"] as? String,
                            currentTitle = doc["currentTitle"] as? String,
                            currentArtist = doc["currentArtist"] as? String,
                            isPlaying = (doc["isPlaying"] as? Boolean) ?: false,
                            isThisDevice = false
                        )
                    )
                }
            }
        }

        _availableDevices.value = found.sortedByDescending { it.lastSeenMs }
    }

    suspend fun transferPlaybackToDevice(
        targetDevice: SwaaraDevice,
        binder: PlayerService.Binder?
    ): Boolean {
        val commandData = withContext(Dispatchers.Main) {
            val player = binder?.player ?: return@withContext null
            val currentItem = player.currentMediaItem ?: return@withContext null
            val metadata = currentItem.mediaMetadata
            mapOf<String, Any?>(
                "command" to "TRANSFER_PLAY",
                "songId" to currentItem.mediaId,
                "title" to (metadata.title?.toString() ?: ""),
                "artist" to (metadata.artist?.toString() ?: ""),
                "artworkUrl" to (metadata.artworkUri?.toString() ?: ""),
                "positionMs" to player.currentPosition,
                "isPlaying" to true,
                "fromDeviceId" to localDeviceId,
                "timestamp" to System.currentTimeMillis()
            )
        } ?: return false

        val success = withContext(Dispatchers.IO) {
            SwaaraCloudConfig.setDocument("connect_commands/${targetDevice.deviceId}", commandData)
        }
        if (success) {
            withContext(Dispatchers.Main) {
                binder?.player?.pause()
            }
            _activeRemoteDevice.value = targetDevice
            Log.i(TAG, "Transferred playback to ${targetDevice.deviceName}")
        }
        return success
    }

    suspend fun sendRemoteCommand(
        targetDeviceId: String,
        command: String,
        positionMs: Long = 0L
    ): Boolean = withContext(Dispatchers.IO) {
        val commandData = mapOf<String, Any?>(
            "command" to command,
            "positionMs" to positionMs,
            "fromDeviceId" to localDeviceId,
            "timestamp" to System.currentTimeMillis()
        )
        SwaaraCloudConfig.setDocument("connect_commands/$targetDeviceId", commandData)
    }

    private suspend fun pollIncomingCommands(context: Context, binder: PlayerService.Binder?) = withContext(Dispatchers.IO) {
        val cmdDoc = SwaaraCloudConfig.getDocument("connect_commands/$localDeviceId") ?: return@withContext
        val timestamp = (cmdDoc["timestamp"] as? Number)?.toLong() ?: 0L
        val now = System.currentTimeMillis()
        if (now - timestamp > 20_000L) return@withContext // Ignore stale commands (>20s)

        val command = cmdDoc["command"] as? String ?: return@withContext
        if (command == "ACK") return@withContext

        val positionMs = (cmdDoc["positionMs"] as? Number)?.toLong() ?: 0L

        // Acknowledge immediately to prevent re-execution
        SwaaraCloudConfig.setDocument("connect_commands/$localDeviceId", mapOf("command" to "ACK", "timestamp" to now))

        when (command) {
            "TRANSFER_PLAY" -> {
                val songId = cmdDoc["songId"] as? String ?: return@withContext
                Log.i(TAG, "Received transfer command for song $songId at $positionMs ms")
                withContext(Dispatchers.Main) {
                    playTrackWithService(
                        context = context,
                        binder = binder,
                        videoId = songId,
                        title = cmdDoc["title"] as? String ?: "Swaara Track",
                        artist = cmdDoc["artist"] as? String ?: "",
                        thumbnailUrl = cmdDoc["artworkUrl"] as? String,
                        onStarted = {
                            if (positionMs > 1500L) {
                                scope.launch {
                                    delay(900L)
                                    withContext(Dispatchers.Main) {
                                        (binder ?: PlayerService.activeBinder.value)?.player?.seekTo(positionMs)
                                    }
                                }
                            }
                        }
                    )
                }
            }
            "REMOTE_PLAY" -> withContext(Dispatchers.Main) { binder?.player?.play() }
            "REMOTE_PAUSE" -> withContext(Dispatchers.Main) { binder?.player?.pause() }
            "REMOTE_NEXT" -> withContext(Dispatchers.Main) { binder?.player?.seekToNext() }
            "REMOTE_PREV" -> withContext(Dispatchers.Main) { binder?.player?.seekToPrevious() }
            "REMOTE_SEEK" -> withContext(Dispatchers.Main) { binder?.player?.seekTo(positionMs) }
        }
    }

    fun stopSession() {
        syncJob?.cancel()
        syncJob = null
    }
}
