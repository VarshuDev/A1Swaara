package com.varshudev.a1swaara.cloud

import android.content.Context
import android.util.Log
import com.varshudev.a1swaara.Database
import com.varshudev.a1swaara.models.Playlist
import com.varshudev.a1swaara.models.Song
import com.varshudev.a1swaara.models.SongPlaylistMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val favoritesCount: Int, val playlistsCount: Int, val message: String) : SyncStatus()
    data class Error(val errorMessage: String) : SyncStatus()
}

/**
 * High-performance account synchronization engine connecting Room SQLite database
 * with Supabase PostgreSQL (user_favorites, user_playlists, user_playlist_tracks).
 */
object SwaaraAccountSyncEngine {
    private const val TAG = "SwaaraAccountSync"

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val syncScope = CoroutineScope(Dispatchers.IO)

    fun triggerSync(context: Context) {
        syncScope.launch {
            sync(context)
        }
    }

    suspend fun sync(context: Context): SyncResult = withContext(Dispatchers.IO) {
        val auth = SwaaraCloudAuth.getInstance(context)
        if (!auth.isSignedIn) {
            val msg = "Sign in with Google to sync your playlists and favorites to the cloud."
            _syncStatus.value = SyncStatus.Error(msg)
            return@withContext SyncResult(success = false, message = msg)
        }

        _syncStatus.value = SyncStatus.Syncing
        val userId = auth.userId
        val token = auth.accessToken

        // Sync local offline listening metrics to Supabase
        try {
            SwaaraTopListenerManager.syncLocalStatsOnSignIn(context)
        } catch (e: Exception) {
            Log.w(TAG, "Notice during local stats sync: ${e.message}")
        }

        try {
            var totalSyncedFavs = 0
            var totalSyncedPlaylists = 0

            // 1. Sync Favorites (Two-Way Reconciliation)
            val localFavs = Database.favoritesByLikedAtDesc().first()
            val remoteFavsJson = SwaaraSupabaseClient.get("user_favorites", "user_id=eq.$userId", token)

            val remoteFavSongIds = mutableSetOf<String>()
            if (remoteFavsJson != null) {
                for (i in 0 until remoteFavsJson.length()) {
                    val item = remoteFavsJson.getJSONObject(i)
                    val songId = item.getString("song_id")
                    remoteFavSongIds.add(songId)

                    // If not present in local, restore into local Room
                    val title = item.optString("title", "Track")
                    val artist = item.optString("artist", "Unknown")
                    val artworkUrl = item.optString("artwork_url", null).takeIf { !it.isNullOrBlank() && it != "null" }

                    val song = Song(
                        id = songId,
                        title = title,
                        artistsText = artist,
                        durationText = null,
                        thumbnailUrl = artworkUrl,
                        likedAt = System.currentTimeMillis()
                    )
                    runCatching {
                        Database.insert(song)
                        Database.like(songId, System.currentTimeMillis())
                    }
                }
            }

            // Push local favorites missing from remote to Supabase
            val favsToUpload = localFavs.filter { it.id !in remoteFavSongIds }
            if (favsToUpload.isNotEmpty()) {
                val batchArray = JSONArray()
                for (fav in favsToUpload) {
                    val obj = JSONObject().apply {
                        put("user_id", userId)
                        put("song_id", fav.id)
                        put("title", fav.title)
                        put("artist", fav.artistsText ?: "Unknown")
                        put("artwork_url", fav.thumbnailUrl ?: "")
                    }
                    batchArray.put(obj)
                }
                SwaaraSupabaseClient.upsert("user_favorites", batchArray.toString(), "user_id,song_id", token)
            }
            totalSyncedFavs = localFavs.size + remoteFavSongIds.size

            // 2. Sync Playlists
            val localPreviews = Database.playlistPreviewsByNameAsc().first()
            val remotePlaylistsJson = SwaaraSupabaseClient.get("user_playlists", "user_id=eq.$userId", token)

            val remotePlaylistNames = mutableSetOf<String>()
            if (remotePlaylistsJson != null) {
                for (i in 0 until remotePlaylistsJson.length()) {
                    val pObj = remotePlaylistsJson.getJSONObject(i)
                    val pId = pObj.getString("id")
                    val pName = pObj.getString("name")
                    remotePlaylistNames.add(pName)

                    val existingMatch = localPreviews.firstOrNull { it.playlist.name == pName }
                    var targetLocalId = existingMatch?.playlist?.id ?: Database.insert(Playlist(name = pName))
                    if (targetLocalId <= 0) {
                        val currentPreviews = runCatching { Database.playlistPreviewsByNameAsc().first() }.getOrNull() ?: emptyList()
                        targetLocalId = currentPreviews.firstOrNull { it.playlist.name == pName }?.playlist?.id ?: -1L
                    }

                    // Fetch remote playlist tracks
                    val tracksJson = SwaaraSupabaseClient.get("user_playlist_tracks", "playlist_id=eq.$pId&order=position.asc", token)
                    if (tracksJson != null && targetLocalId > 0) {
                        for (t in 0 until tracksJson.length()) {
                            val track = tracksJson.getJSONObject(t)
                            val sId = track.getString("song_id")
                            val sTitle = track.optString("title", "Track")
                            val sArtist = track.optString("artist", "Unknown")
                            val sArtwork = track.optString("artwork_url", null).takeIf { !it.isNullOrBlank() && it != "null" }
                            val pos = track.optInt("position", t)

                            val song = Song(
                                id = sId,
                                title = sTitle,
                                artistsText = sArtist,
                                durationText = null,
                                thumbnailUrl = sArtwork
                            )
                            runCatching { Database.insert(song) }
                            runCatching {
                                Database.insert(
                                    SongPlaylistMap(
                                        songId = sId,
                                        playlistId = targetLocalId,
                                        position = pos
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Push local playlists missing in remote
            for (preview in localPreviews) {
                val plName = preview.playlist.name
                if (plName !in remotePlaylistNames) {
                    val plObj = JSONObject().apply {
                        put("user_id", userId)
                        put("name", plName)
                    }
                    val created = SwaaraSupabaseClient.upsert("user_playlists", JSONArray().put(plObj).toString(), null, token)
                    if (created) {
                        // Fetch the created playlist ID
                        val createdJson = SwaaraSupabaseClient.get("user_playlists", "user_id=eq.$userId&name=eq.$plName", token)
                        val newRemoteId = createdJson?.optJSONObject(0)?.optString("id")
                        if (!newRemoteId.isNullOrBlank()) {
                            val songs = Database.playlistSongs(preview.playlist.id).first()
                            val tracksArray = JSONArray()
                            songs.forEachIndexed { idx, s ->
                                tracksArray.put(
                                    JSONObject().apply {
                                        put("playlist_id", newRemoteId)
                                        put("song_id", s.id)
                                        put("title", s.title)
                                        put("artist", s.artistsText ?: "Unknown")
                                        put("artwork_url", s.thumbnailUrl ?: "")
                                        put("position", idx)
                                    }
                                )
                            }
                            if (tracksArray.length() > 0) {
                                SwaaraSupabaseClient.insert("user_playlist_tracks", tracksArray.toString(), token)
                            }
                        }
                    }
                }
            }
            totalSyncedPlaylists = localPreviews.size

            // 3. Update Profiles
            val profilePayload = JSONObject().apply {
                put("id", userId)
                put("display_name", auth.displayName)
                put("avatar_url", auth.avatarUrl ?: "")
                put("email", auth.userEmail ?: "")
            }
            SwaaraSupabaseClient.upsert("profiles", JSONArray().put(profilePayload).toString(), "id", token)

            // 4. Sync accumulated local listening stats to Supabase leaderboard
            runCatching {
                SwaaraTopListenerManager.syncLocalStatsOnSignIn(context)
            }.onFailure { Log.w(TAG, "Failed to sync local listening stats", it) }

            auth.lastSyncTimestamp = System.currentTimeMillis()
            val msg = "Synced $totalSyncedFavs favorites and $totalSyncedPlaylists playlists with Supabase Cloud."
            _syncStatus.value = SyncStatus.Success(totalSyncedFavs, totalSyncedPlaylists, msg)

            SyncResult(
                success = true,
                favoritesCount = totalSyncedFavs,
                playlistsCount = totalSyncedPlaylists,
                message = msg
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            val err = "Sync failed: ${e.localizedMessage ?: "Network error"}"
            _syncStatus.value = SyncStatus.Error(err)
            SyncResult(success = false, message = err)
        }
    }
}
