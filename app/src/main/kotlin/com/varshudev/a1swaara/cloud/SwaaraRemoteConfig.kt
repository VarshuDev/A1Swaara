package com.varshudev.a1swaara.cloud

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SwaaraAnnouncement(
    val id: String,
    val title: String,
    val message: String,
    val actionUrl: String? = null,
    val isDismissible: Boolean = true
)

data class SwaaraFestiveVibe(
    val id: String,
    val enabled: Boolean = true,
    val festivalName: String = "",
    val title: String = "",
    val message: String = "",
    val bannerImageUrl: String? = null,
    val accentColorHex: String = "#FFD700",
    val secondaryColorHex: String = "#FF2DAA",
    val actionText: String = "Celebrate Now",
    val actionUrl: String? = null,
    val badgeText: String = "FESTIVE SPECIAL",
    val isDismissible: Boolean = true
)

data class RemoteConfigState(
    val pipedInstances: List<String> = listOf("https://pipedapi.ducks.party"),
    val streamProxyMode: String = "direct",
    val announcement: SwaaraAnnouncement? = null,
    val festiveVibe: SwaaraFestiveVibe? = null,
    val minRequiredVersion: String = "1.0.0",
    val recommendedBitrate: String = "320kbps"
)

/**
 * Manages cloud-driven remote configuration, dynamic streaming proxies, broadcast announcements, and festive vibes.
 */
object SwaaraRemoteConfig {
    private const val TAG = "SwaaraRemoteConfig"
    private const val PREFS_NAME = "a1swaara_remote_config_prefs"
    private const val KEY_DISMISSED_ANNOUNCEMENT_ID = "dismissed_announcement_id"
    private const val KEY_DISMISSED_FESTIVE_ID = "dismissed_festive_id"

    private val _configState = MutableStateFlow(RemoteConfigState())
    val configState: StateFlow<RemoteConfigState> = _configState.asStateFlow()

    suspend fun fetchAndApply(context: Context): RemoteConfigState = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val doc = SwaaraCloudConfig.getDocument("remote_config/app")
            var instances = listOf("https://pipedapi.ducks.party")
            var streamMode = "direct"
            var minVer = "1.0.0"
            var bitrate = "320kbps"
            var announcement: SwaaraAnnouncement? = null

            if (doc != null) {
                @Suppress("UNCHECKED_CAST")
                instances = (doc["activePipedInstances"] as? List<String>)?.filter { it.isNotBlank() }
                    ?: listOf("https://pipedapi.ducks.party")
                streamMode = doc["streamProxyMode"] as? String ?: "direct"
                minVer = doc["minRequiredVersion"] as? String ?: "1.0.0"
                bitrate = doc["recommendedBitrate"] as? String ?: "320kbps"

                val annEnabled = doc["announcementEnabled"] as? Boolean ?: false
                val annId = doc["announcementId"] as? String ?: ""
                val annTitle = doc["announcementTitle"] as? String ?: ""
                val annMsg = doc["announcementMessage"] as? String ?: ""
                val annUrl = doc["announcementActionUrl"] as? String

                val dismissedId = prefs.getString(KEY_DISMISSED_ANNOUNCEMENT_ID, "")
                announcement = if (annEnabled && annId.isNotBlank() && annId != dismissedId) {
                    SwaaraAnnouncement(
                        id = annId,
                        title = annTitle,
                        message = annMsg,
                        actionUrl = annUrl
                    )
                } else null
            }

            // Check sovereign festive broadcast document (notifications/festive_broadcast)
            var festiveVibe: SwaaraFestiveVibe? = null
            try {
                val festiveDoc = SwaaraCloudConfig.getDocument("notifications/festive_broadcast")
                if (festiveDoc != null) {
                    val festiveEnabled = festiveDoc["enabled"] as? Boolean ?: false
                    val festiveId = festiveDoc["id"] as? String ?: ""
                    val dismissedFestiveId = prefs.getString(KEY_DISMISSED_FESTIVE_ID, "")

                    if (festiveEnabled && festiveId.isNotBlank() && festiveId != dismissedFestiveId) {
                        festiveVibe = SwaaraFestiveVibe(
                            id = festiveId,
                            enabled = true,
                            festivalName = festiveDoc["festivalName"] as? String ?: "Festive Special",
                            title = festiveDoc["title"] as? String ?: "Festive Celebration",
                            message = festiveDoc["message"] as? String ?: "Experience festival melodies in studio-grade acoustics",
                            bannerImageUrl = (festiveDoc["bannerImageUrl"] as? String)?.takeIf { it.isNotBlank() },
                            accentColorHex = (festiveDoc["accentColorHex"] as? String)?.takeIf { it.isNotBlank() } ?: "#FFD700",
                            secondaryColorHex = (festiveDoc["secondaryColorHex"] as? String)?.takeIf { it.isNotBlank() } ?: "#FF2DAA",
                            actionText = (festiveDoc["actionText"] as? String)?.takeIf { it.isNotBlank() } ?: "Celebrate Now",
                            actionUrl = (festiveDoc["actionUrl"] as? String)?.takeIf { it.isNotBlank() },
                            badgeText = (festiveDoc["badgeText"] as? String)?.takeIf { it.isNotBlank() } ?: "FESTIVE SPECIAL",
                            isDismissible = festiveDoc["isDismissible"] as? Boolean ?: true
                        )
                        Log.i(TAG, "Loaded active festive broadcast: [${festiveVibe.id}] ${festiveVibe.title}")
                    }
                }
            } catch (fe: Exception) {
                Log.w(TAG, "Could not load festive broadcast document", fe)
            }

            val newState = RemoteConfigState(
                pipedInstances = instances,
                streamProxyMode = streamMode,
                announcement = announcement,
                festiveVibe = festiveVibe,
                minRequiredVersion = minVer,
                recommendedBitrate = bitrate
            )
            _configState.value = newState
            return@withContext newState
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote config", e)
        }
        _configState.value
    }

    fun dismissAnnouncement(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISMISSED_ANNOUNCEMENT_ID, id).apply()
        _configState.value = _configState.value.copy(announcement = null)
    }

    fun dismissFestiveVibe(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISMISSED_FESTIVE_ID, id).apply()
        _configState.value = _configState.value.copy(festiveVibe = null)
    }
}
