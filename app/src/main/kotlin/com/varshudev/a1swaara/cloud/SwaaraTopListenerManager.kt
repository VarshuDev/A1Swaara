package com.varshudev.a1swaara.cloud

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class ArtistTopListener(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val totalSeconds: Long,
    val playCount: Int,
    val badge: String,
    val artistName: String = ""
)

data class GlobalTopListener(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val totalHours: Double,
    val totalTracks: Long,
    val streakDays: Int
)

/**
 * Manages user listening analytics, top listener leaderboards, and artist fan tier badges.
 * Built with high-performance batch profile resolution, resilient URL encoding, and local caching.
 */
object SwaaraTopListenerManager {
    private const val TAG = "SwaaraTopListener"
    private const val PREFS_LOCAL_STATS = "swaara_local_listen_stats"

    @Volatile
    private var cachedGlobalLeaders: List<GlobalTopListener> = emptyList()
    @Volatile
    private var lastGlobalFetchTime: Long = 0L

    private const val CACHE_EXPIRY_MS = 60_000L // 1 minute fresh cache

    /**
     * Batch-fetches all user profiles in a single query to eliminate N sequential HTTP roundtrips.
     */
    private suspend fun fetchProfilesMap(): Map<String, Pair<String, String?>> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, Pair<String, String?>>()
        try {
            val profilesJson = SwaaraSupabaseClient.get("profiles", "select=id,display_name,avatar_url")
            if (profilesJson != null) {
                for (i in 0 until profilesJson.length()) {
                    val p = profilesJson.getJSONObject(i)
                    val id = p.optString("id")
                    if (id.isNotBlank()) {
                        val name = p.optString("display_name").takeIf { it.isNotBlank() && it != "null" } ?: "Audiophile"
                        val avatar = p.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }
                        map[id] = Pair(name, avatar)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to batch-fetch profiles", e)
        }
        map
    }

    /**
     * Records local listening stats when user is not signed in to Supabase.
     */
    private fun recordLocalListen(context: Context, artistName: String, listenSeconds: Long) {
        try {
            val sp = context.getSharedPreferences(PREFS_LOCAL_STATS, Context.MODE_PRIVATE)
            val totalSec = sp.getLong("local_total_seconds", 0L) + listenSeconds
            val totalTracks = sp.getLong("local_total_tracks", 0L) + 1L

            val artistKey = "artist_" + artistName.trim().lowercase().replace("[^a-zA-Z0-9]".toRegex(), "_")
            val artistSec = sp.getLong("${artistKey}_sec", 0L) + listenSeconds
            val artistPlays = sp.getInt("${artistKey}_plays", 0) + 1

            sp.edit()
                .putLong("local_total_seconds", totalSec)
                .putLong("local_total_tracks", totalTracks)
                .putLong("${artistKey}_sec", artistSec)
                .putInt("${artistKey}_plays", artistPlays)
                .putString("${artistKey}_raw_name", artistName.trim())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record local listen stats", e)
        }
    }

    /**
     * Reports valid playback duration to update artist and global stats on Supabase.
     */
    suspend fun recordListen(
        context: Context,
        artistName: String,
        listenSeconds: Long
    ) = withContext(Dispatchers.IO) {
        if (artistName.isBlank() || listenSeconds < 5) return@withContext
        val auth = SwaaraCloudAuth.getInstance(context)
        val userId = auth.userId
        val token = auth.accessToken

        // Always record locally so hours accumulate even before signing in
        recordLocalListen(context, artistName, listenSeconds)

        // Supabase requires authenticated JWT due to Row-Level Security
        if (token.isNullOrBlank()) {
            return@withContext
        }

        try {
            val trimmedArtist = artistName.trim()
            val encodedArtist = URLEncoder.encode(trimmedArtist, "UTF-8").replace("+", "%20")

            // 1. Fetch current artist stat for user
            val query = "user_id=eq.$userId&artist_name=eq.$encodedArtist"
            val existing = SwaaraSupabaseClient.get("user_artist_stats", query, token)
            var currentSec = 0L
            var currentPlays = 0

            if (existing != null && existing.length() > 0) {
                val row = existing.getJSONObject(0)
                currentSec = row.optLong("total_listen_seconds", 0L)
                currentPlays = row.optInt("play_count", 0)
            }

            val newSec = currentSec + listenSeconds
            val newPlays = currentPlays + 1

            val artistStatPayload = JSONObject().apply {
                put("user_id", userId)
                put("artist_name", trimmedArtist)
                put("total_listen_seconds", newSec)
                put("play_count", newPlays)
            }

            SwaaraSupabaseClient.upsert(
                "user_artist_stats",
                JSONArray().put(artistStatPayload).toString(),
                "user_id,artist_name",
                token
            )

            // 2. Fetch current global stat for user
            val globalExisting = SwaaraSupabaseClient.get("global_listening_stats", "user_id=eq.$userId", token)
            var gSec = 0L
            var gTracks = 0L

            if (globalExisting != null && globalExisting.length() > 0) {
                val gRow = globalExisting.getJSONObject(0)
                gSec = gRow.optLong("total_seconds", 0L)
                gTracks = gRow.optLong("total_tracks_played", 0L)
            }

            val globalPayload = JSONObject().apply {
                put("user_id", userId)
                put("total_seconds", gSec + listenSeconds)
                put("total_tracks_played", gTracks + 1L)
                put("last_listened_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date()))
            }

            SwaaraSupabaseClient.upsert(
                "global_listening_stats",
                JSONArray().put(globalPayload).toString(),
                "user_id",
                token
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record listen stats for $artistName", e)
        }
    }

    /**
     * Syncs accumulated local playback stats to Supabase once user logs in with Google.
     */
    suspend fun syncLocalStatsOnSignIn(context: Context) = withContext(Dispatchers.IO) {
        val auth = SwaaraCloudAuth.getInstance(context)
        if (!auth.isSignedIn) return@withContext
        val token = auth.accessToken ?: return@withContext
        val userId = auth.userId

        try {
            val sp = context.getSharedPreferences(PREFS_LOCAL_STATS, Context.MODE_PRIVATE)
            val localSec = sp.getLong("local_total_seconds", 0L)
            val localTracks = sp.getLong("local_total_tracks", 0L)
            if (localSec <= 0L) return@withContext

            val existing = SwaaraSupabaseClient.get("global_listening_stats", "user_id=eq.$userId", token)
            var remSec = 0L
            var remTracks = 0L
            if (existing != null && existing.length() > 0) {
                val r = existing.getJSONObject(0)
                remSec = r.optLong("total_seconds", 0L)
                remTracks = r.optLong("total_tracks_played", 0L)
            }

            val mergedSec = maxOf(remSec, remSec + localSec)
            val mergedTracks = maxOf(remTracks, remTracks + localTracks)

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("total_seconds", mergedSec)
                put("total_tracks_played", mergedTracks)
            }
            SwaaraSupabaseClient.upsert(
                "global_listening_stats",
                JSONArray().put(payload).toString(),
                "user_id",
                token
            )
            // Clear local accumulation after successful sync
            sp.edit().clear().apply()
            Log.i(TAG, "Successfully synced local listening stats to Supabase for $userId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync local stats to Supabase", e)
        }
    }

    /**
     * Retrieves top listeners for an artist, or top listeners across all artists if artistName is blank.
     */
    suspend fun getArtistLeaderboard(
        artistName: String,
        limit: Int = 25
    ): List<ArtistTopListener> = withContext(Dispatchers.IO) {
        try {
            val trimmed = artistName.trim()
            val query = if (trimmed.isBlank()) {
                "order=total_listen_seconds.desc&limit=$limit"
            } else {
                val encoded = URLEncoder.encode(trimmed, "UTF-8").replace("+", "%20")
                "artist_name=ilike.*$encoded*&order=total_listen_seconds.desc&limit=$limit"
            }

            var results = SwaaraSupabaseClient.get("user_artist_stats", query)

            // If fuzzy search with full name yielded 0, try the primary artist token
            if ((results == null || results.length() == 0) && trimmed.contains(" ")) {
                val primaryToken = trimmed.split(" ", ",", "&", "-")
                    .map { it.trim() }
                    .firstOrNull { it.length >= 3 }

                if (primaryToken != null) {
                    val fallbackEncoded = URLEncoder.encode(primaryToken, "UTF-8").replace("+", "%20")
                    results = SwaaraSupabaseClient.get(
                        "user_artist_stats",
                        "artist_name=ilike.*$fallbackEncoded*&order=total_listen_seconds.desc&limit=$limit"
                    )
                }
            }

            // Fallback to top artists if no results found for query
            if (results == null || results.length() == 0) {
                results = SwaaraSupabaseClient.get("user_artist_stats", "order=total_listen_seconds.desc&limit=$limit")
            }

            if (results == null || results.length() == 0) return@withContext emptyList()

            val profilesMap = fetchProfilesMap()
            val list = mutableListOf<ArtistTopListener>()

            for (i in 0 until results.length()) {
                val row = results.getJSONObject(i)
                val uId = row.optString("user_id")
                val totalSec = row.optLong("total_listen_seconds", 0L)
                val count = row.optInt("play_count", 0)
                val rowArtist = row.optString("artist_name", trimmed)

                val profile = profilesMap[uId]
                val name = profile?.first ?: "Listener ${uId.take(4)}"
                val avatar = profile?.second

                val rank = i + 1
                val badge = when (rank) {
                    1 -> "CROWN LISTENER"
                    in 2..3 -> "SUPERFAN (TOP 1%)"
                    in 4..10 -> "CORE FAN"
                    else -> "REGULAR"
                }

                list.add(
                    ArtistTopListener(
                        rank = rank,
                        userId = uId,
                        displayName = name,
                        avatarUrl = avatar,
                        totalSeconds = totalSec,
                        playCount = count,
                        badge = badge,
                        artistName = rowArtist
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching artist leaderboard", e)
            emptyList()
        }
    }

    /**
     * Retrieves the global top listeners across the entire community in a fast single batch.
     */
    suspend fun getGlobalLeaderboard(
        limit: Int = 25,
        forceRefresh: Boolean = false
    ): List<GlobalTopListener> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedGlobalLeaders.isNotEmpty() && (now - lastGlobalFetchTime) < CACHE_EXPIRY_MS) {
            return@withContext cachedGlobalLeaders
        }

        try {
            val query = "order=total_seconds.desc&limit=$limit"
            val results = SwaaraSupabaseClient.get("global_listening_stats", query)
            if (results == null || results.length() == 0) {
                return@withContext cachedGlobalLeaders
            }

            val profilesMap = fetchProfilesMap()
            val list = mutableListOf<GlobalTopListener>()

            for (i in 0 until results.length()) {
                val row = results.getJSONObject(i)
                val uId = row.optString("user_id")
                val totalSec = row.optLong("total_seconds", 0L)
                val totalTracks = row.optLong("total_tracks_played", 0L)
                val streak = row.optInt("current_streak_days", 1)

                val profile = profilesMap[uId]
                val name = profile?.first ?: "Audiophile ${uId.take(4)}"
                val avatar = profile?.second

                val hours = (totalSec / 3600.0)

                list.add(
                    GlobalTopListener(
                        rank = i + 1,
                        userId = uId,
                        displayName = name,
                        avatarUrl = avatar,
                        totalHours = Math.round(hours * 10.0) / 10.0,
                        totalTracks = totalTracks,
                        streakDays = streak
                    )
                )
            }

            if (list.isNotEmpty()) {
                cachedGlobalLeaders = list
                lastGlobalFetchTime = now
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching global leaderboard", e)
            cachedGlobalLeaders
        }
    }
}
