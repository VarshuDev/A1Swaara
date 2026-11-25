package com.varshudev.a1swaara.cloud

import android.content.Context
import android.util.Log
import com.varshudev.a1swaara.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Calendar

data class UserListeningStats(
    val totalHours: Double,
    val totalTracks: Long,
    val streakDays: Int
)

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
     * Records local listening stats immediately on-device and calculates streaks.
     */
    private fun recordLocalListen(context: Context, artistName: String, listenSeconds: Long) {
        try {
            val sp = context.getSharedPreferences(PREFS_LOCAL_STATS, Context.MODE_PRIVATE)
            val totalSec = sp.getLong("local_total_seconds", 0L) + listenSeconds
            val totalTracks = sp.getLong("local_total_tracks", 0L) + 1L

            val now = System.currentTimeMillis()
            val lastListenTime = sp.getLong("last_listen_ms", 0L)
            val currentStreak = sp.getInt("current_streak_days", 1)

            val newStreak = if (lastListenTime == 0L) {
                1
            } else {
                val calNow = Calendar.getInstance().apply { timeInMillis = now }
                val calLast = Calendar.getInstance().apply { timeInMillis = lastListenTime }
                val dayNow = calNow.get(Calendar.DAY_OF_YEAR)
                val yearNow = calNow.get(Calendar.YEAR)
                val dayLast = calLast.get(Calendar.DAY_OF_YEAR)
                val yearLast = calLast.get(Calendar.YEAR)

                if (yearNow == yearLast && dayNow == dayLast) {
                    currentStreak
                } else if (yearNow == yearLast && dayNow == dayLast + 1) {
                    currentStreak + 1
                } else if (yearNow == yearLast + 1 && dayNow == 1 && dayLast >= 365) {
                    currentStreak + 1
                } else {
                    1
                }
            }

            val artistKey = "artist_" + artistName.trim().lowercase().replace("[^a-zA-Z0-9]".toRegex(), "_")
            val artistSec = sp.getLong("${artistKey}_sec", 0L) + listenSeconds
            val artistPlays = sp.getInt("${artistKey}_plays", 0) + 1

            sp.edit()
                .putLong("local_total_seconds", totalSec)
                .putLong("local_total_tracks", totalTracks)
                .putInt("current_streak_days", newStreak)
                .putLong("last_listen_ms", now)
                .putLong("${artistKey}_sec", artistSec)
                .putInt("${artistKey}_plays", artistPlays)
                .putString("${artistKey}_raw_name", artistName.trim())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record local listen stats", e)
        }
    }

    /**
     * Fast synchronous local stats snapshot for instant UI rendering without blocking.
     */
    fun getLocalUserStats(context: Context): UserListeningStats {
        return try {
            val sp = context.getSharedPreferences(PREFS_LOCAL_STATS, Context.MODE_PRIVATE)
            val localSec = sp.getLong("local_total_seconds", 0L)
            var localTracks = sp.getLong("local_total_tracks", 0L)
            val streak = sp.getInt("current_streak_days", 1)

            if (localTracks == 0L) {
                try {
                    val count = Database.eventsCountNow().toLong()
                    if (count > 0L) localTracks = count
                } catch (_: Exception) {}
            }

            val hours = Math.round((localSec / 3600.0) * 10.0) / 10.0
            UserListeningStats(hours, localTracks, streak)
        } catch (e: Exception) {
            UserListeningStats(0.0, 0L, 1)
        }
    }

    /**
     * Reconciles local stats with Room Database and Firebase Firestore, with Supabase fallback.
     */
    suspend fun getUserStats(context: Context): UserListeningStats = withContext(Dispatchers.IO) {
        val auth = SwaaraCloudAuth.getInstance(context)
        val userId = auth.userId
        val token = auth.validAccessToken()
        val sp = context.getSharedPreferences(PREFS_LOCAL_STATS, Context.MODE_PRIVATE)

        var bestSec = sp.getLong("local_total_seconds", 0L)
        var bestTracks = sp.getLong("local_total_tracks", 0L)
        var bestStreak = sp.getInt("current_streak_days", 1)

        // 1. Reconcile with Room Database events if room has recorded more track events
        try {
            val count = Database.eventsCountNow().toLong()
            if (count > bestTracks) {
                bestTracks = count
            }
        } catch (_: Exception) {}

        // 2. Fetch from Firebase Firestore (Always accessible backend)
        try {
            val firestoreDoc = SwaaraCloudConfig.getDocument("user_stats/$userId")
            if (firestoreDoc != null) {
                val cSec = (firestoreDoc["totalSeconds"] as? Number)?.toLong() ?: 0L
                val cTracks = (firestoreDoc["totalTracks"] as? Number)?.toLong() ?: 0L
                val cStreak = (firestoreDoc["streakDays"] as? Number)?.toInt() ?: 1

                if (cSec > bestSec) bestSec = cSec
                if (cTracks > bestTracks) bestTracks = cTracks
                if (cStreak > bestStreak) bestStreak = cStreak
            }

            // Push newest stats to Firestore if local has higher metrics
            if (bestSec > 0L || bestTracks > 0L) {
                val cloudData = mapOf(
                    "userId" to userId,
                    "displayName" to auth.displayName,
                    "avatarUrl" to (auth.avatarUrl ?: ""),
                    "totalSeconds" to bestSec,
                    "totalTracks" to bestTracks,
                    "streakDays" to bestStreak,
                    "lastListenedAt" to System.currentTimeMillis()
                )
                SwaaraCloudConfig.setDocument("user_stats/$userId", cloudData)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed syncing with Firestore user_stats", e)
        }

        // 3. Supabase sync if token available and reachable
        if (!token.isNullOrBlank()) {
            try {
                val supabaseStats = SwaaraSupabaseClient.get("global_listening_stats", "user_id=eq.$userId", token)
                if (supabaseStats != null && supabaseStats.length() > 0) {
                    val row = supabaseStats.getJSONObject(0)
                    val sSec = row.optLong("total_seconds", 0L)
                    val sTracks = row.optLong("total_tracks_played", 0L)
                    val sStreak = row.optInt("current_streak_days", 1)

                    if (sSec > bestSec) bestSec = sSec
                    if (sTracks > bestTracks) bestTracks = sTracks
                    if (sStreak > bestStreak) bestStreak = sStreak
                }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase global stats query skipped (paused/offline): ${e.message}")
            }
        }

        // Save consolidated numbers back to local SharedPreferences
        sp.edit()
            .putLong("local_total_seconds", bestSec)
            .putLong("local_total_tracks", bestTracks)
            .putInt("current_streak_days", bestStreak)
            .apply()

        val hours = Math.round((bestSec / 3600.0) * 10.0) / 10.0
        UserListeningStats(hours, bestTracks, bestStreak)
    }

    /**
     * Reports valid playback duration to update artist and global stats across local and cloud targets.
     */
    suspend fun recordListen(
        context: Context,
        artistName: String,
        listenSeconds: Long
    ) = withContext(Dispatchers.IO) {
        if (artistName.isBlank() || listenSeconds < 5) return@withContext
        val auth = SwaaraCloudAuth.getInstance(context)
        val userId = auth.userId
        val token = auth.validAccessToken()

        // 1. Always record locally so hours accumulate immediately
        recordLocalListen(context, artistName, listenSeconds)

        // 2. Sync to Firebase Firestore so live stats and leaderboard stay updated
        try {
            val sp = context.getSharedPreferences(PREFS_LOCAL_STATS, Context.MODE_PRIVATE)
            val curSec = sp.getLong("local_total_seconds", 0L)
            val curTracks = sp.getLong("local_total_tracks", 0L)
            val curStreak = sp.getInt("current_streak_days", 1)

            val firestorePayload = mapOf(
                "userId" to userId,
                "displayName" to auth.displayName,
                "avatarUrl" to (auth.avatarUrl ?: ""),
                "totalSeconds" to curSec,
                "totalTracks" to curTracks,
                "streakDays" to curStreak,
                "lastListenedAt" to System.currentTimeMillis()
            )
            SwaaraCloudConfig.setDocument("user_stats/$userId", firestorePayload)
        } catch (e: Exception) {
            Log.w(TAG, "Failed updating Firestore user_stats in recordListen", e)
        }

        // 3. Supabase requires authenticated JWT due to Row-Level Security
        if (token.isNullOrBlank()) {
            return@withContext
        }

        try {
            val trimmedArtist = artistName.trim()
            val encodedArtist = URLEncoder.encode(trimmedArtist, "UTF-8").replace("+", "%20")

            // 1. Fetch current artist stat for user
            val query = "user_id=eq.$userId&artist_name=eq.$encodedArtist"
            val existing = SwaaraSupabaseClient.get("user_artist_stats", query, token) ?: return@withContext
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
            val globalExisting = SwaaraSupabaseClient.get("global_listening_stats", "user_id=eq.$userId", token) ?: return@withContext
            var gSec = 0L
            var gTracks = 0L

            if (globalExisting.length() > 0) {
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
                put("last_listened_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date()))
            }
            SwaaraSupabaseClient.upsert(
                "global_listening_stats",
                JSONArray().put(payload).toString(),
                "user_id",
                token
            )

            // 2. Sync all accumulated artist listening stats
            val allEntries = sp.all
            val artistKeys = allEntries.keys
                .filter { it.startsWith("artist_") && it.endsWith("_raw_name") }
                .map { it.removeSuffix("_raw_name") }

            for (key in artistKeys) {
                val rawArtistName = sp.getString("${key}_raw_name", "") ?: ""
                val localArtistSec = sp.getLong("${key}_sec", 0L)
                val localArtistPlays = sp.getInt("${key}_plays", 0)

                if (rawArtistName.isNotBlank() && localArtistSec > 0L) {
                    val encodedArtist = URLEncoder.encode(rawArtistName, "UTF-8").replace("+", "%20")
                    val existingArtist = SwaaraSupabaseClient.get(
                        "user_artist_stats",
                        "user_id=eq.$userId&artist_name=eq.$encodedArtist",
                        token
                    )
                    var remArtistSec = 0L
                    var remArtistPlays = 0
                    if (existingArtist != null && existingArtist.length() > 0) {
                        val row = existingArtist.getJSONObject(0)
                        remArtistSec = row.optLong("total_listen_seconds", 0L)
                        remArtistPlays = row.optInt("play_count", 0)
                    }

                    val artistPayload = JSONObject().apply {
                        put("user_id", userId)
                        put("artist_name", rawArtistName)
                        put("total_listen_seconds", remArtistSec + localArtistSec)
                        put("play_count", remArtistPlays + localArtistPlays)
                    }

                    SwaaraSupabaseClient.upsert(
                        "user_artist_stats",
                        JSONArray().put(artistPayload).toString(),
                        "user_id,artist_name",
                        token
                    )
                }
            }

            // Clear local accumulation after successful sync
            sp.edit().clear().apply()
            Log.i(TAG, "Successfully synced local global and artist listening stats to Supabase for $userId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync local stats to Supabase", e)
        }
    }

    /**
     * Retrieves top listeners for an artist, or top listeners across all artists if artistName is blank.
     * Incorporates Supabase query with automatic resilient fallback to Firestore community trending data.
     */
    suspend fun getArtistLeaderboard(
        artistName: String,
        limit: Int = 25
    ): List<ArtistTopListener> = withContext(Dispatchers.IO) {
        val trimmed = artistName.trim()
        try {
            val query = if (trimmed.isBlank()) {
                "order=total_listen_seconds.desc&limit=$limit"
            } else {
                val encoded = URLEncoder.encode(trimmed, "UTF-8").replace("+", "%20")
                "artist_name=ilike.*$encoded*&order=total_listen_seconds.desc&limit=$limit"
            }

            var results: JSONArray? = null
            try {
                results = SwaaraSupabaseClient.get("user_artist_stats", query)

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
            } catch (e: Exception) {
                Log.w(TAG, "Supabase artist leaderboard skipped (falling back to Firestore): ${e.message}")
            }

            if (results != null && results.length() > 0) {
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
                return@withContext list
            }

            // Resilient Fallback: Construct artist leaderboard from Firestore community trending
            val trendingDocs = SwaaraCloudConfig.listDocuments("community_trending", pageSize = 50)
            val filtered = trendingDocs.mapNotNull { (_, fields) ->
                val artist = fields["artist"] as? String ?: return@mapNotNull null
                val plays = (fields["playCount"] as? Number)?.toInt() ?: 1
                val artwork = fields["artworkUrl"] as? String
                Triple(artist, plays, artwork)
            }.filter { triple ->
                trimmed.isBlank() || triple.first.contains(trimmed, ignoreCase = true)
            }.groupBy { it.first }
            .map { (artist, playsList) ->
                val totalPlays = playsList.sumOf { it.second }
                val art = playsList.firstOrNull { !it.third.isNullOrBlank() }?.third
                ArtistTopListener(
                    rank = 0,
                    userId = "artist_${artist.hashCode()}",
                    displayName = artist,
                    avatarUrl = art,
                    totalSeconds = totalPlays * 210L,
                    playCount = totalPlays,
                    badge = "TOP FAN",
                    artistName = artist
                )
            }.sortedByDescending { it.playCount }
            .take(limit)

            filtered.mapIndexed { idx, it ->
                val rank = idx + 1
                val badge = when (rank) {
                    1 -> "CROWN LISTENER"
                    in 2..3 -> "SUPERFAN (TOP 1%)"
                    in 4..10 -> "CORE FAN"
                    else -> "REGULAR"
                }
                it.copy(rank = rank, badge = badge)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching artist leaderboard", e)
            emptyList()
        }
    }

    /**
     * Retrieves the global top listeners across the entire community in a fast single batch.
     * Tries Supabase first, automatically falling back to Firestore user telemetry if paused/offline.
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
            var results: JSONArray? = null
            try {
                results = SwaaraSupabaseClient.get("global_listening_stats", query)
            } catch (e: Exception) {
                Log.w(TAG, "Supabase global leaderboard query failed (falling back to Firestore): ${e.message}")
            }

            val list = mutableListOf<GlobalTopListener>()

            if (results != null && results.length() > 0) {
                val profilesMap = fetchProfilesMap()

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
            } else {
                // Resilient Fallback: Read community user stats from Firestore!
                val firestoreDocs = SwaaraCloudConfig.listDocuments("user_stats", pageSize = limit)
                val mapped = firestoreDocs.mapNotNull { (id, fields) ->
                    val uId = fields["userId"] as? String ?: id
                    val name = fields["displayName"] as? String ?: "Swaara Listener"
                    val avatar = fields["avatarUrl"] as? String
                    val sec = (fields["totalSeconds"] as? Number)?.toLong() ?: 0L
                    val tracks = (fields["totalTracks"] as? Number)?.toLong() ?: 0L
                    val streak = (fields["streakDays"] as? Number)?.toInt() ?: 1
                    val hours = Math.round((sec / 3600.0) * 10.0) / 10.0
                    if (sec > 0L || tracks > 0L) {
                        GlobalTopListener(0, uId, name, avatar, hours, tracks, streak)
                    } else null
                }.sortedByDescending { it.totalHours }

                mapped.forEachIndexed { idx, item ->
                    list.add(item.copy(rank = idx + 1))
                }
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
