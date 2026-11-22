package com.varshudev.a1swaara.cloud

import android.content.Context
import android.util.Log
import com.varshudev.a1swaara.Database
import com.varshudev.a1swaara.models.Playlist
import com.varshudev.a1swaara.models.Song
import com.varshudev.a1swaara.models.SongPlaylistMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
    private val syncMutex = Mutex()

    fun triggerSync(context: Context) {
        syncScope.launch {
            sync(context.applicationContext)
        }
    }

    suspend fun sync(context: Context): SyncResult = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val auth = SwaaraCloudAuth.getInstance(context)
            if (!auth.isSignedIn) {
                val msg = "Sign in with Google to sync your playlists and favorites to the cloud."
                _syncStatus.value = SyncStatus.Error(msg)
                return@withContext SyncResult(success = false, message = msg)
            }

            _syncStatus.value = SyncStatus.Syncing
            val userId = auth.userId
            val token = auth.validAccessToken() ?: run {
                val msg = "Your cloud session could not be refreshed. Check your connection or sign in again."
                _syncStatus.value = SyncStatus.Error(msg)
                return@withContext SyncResult(success = false, message = msg)
            }

            // Offline listening stats are not uploaded here: the legacy read/modify/write
            // flow is not idempotent and can clear unsent stats on a partial failure.
            try {
                var totalSyncedFavs = 0
                var totalSyncedPlaylists = 0

                // 1. Sync Favorites (Two-Way Reconciliation)
                val localFavs = Database.favoritesByLikedAtDesc().first()
                val remoteFavsJson = SwaaraSupabaseClient.get("user_favorites", "user_id=eq.$userId", token)
                    ?: error("Unable to fetch cloud favorites")

                val localFavIds = localFavs.map { it.id }.toSet()
                val remoteFavSongIds = mutableSetOf<String>()
                if (remoteFavsJson != null) {
                    for (i in 0 until remoteFavsJson.length()) {
                        val item = remoteFavsJson.getJSONObject(i)
                        val songId = item.getString("song_id")
                        remoteFavSongIds.add(songId)

                        // If not present in local, restore into local Room
                        val title = item.optString("title", "Track")
                        val artist = item.optString("artist", "Unknown")
                        val artworkUrl = item.optString("artwork_url").takeIf { it.isNotBlank() && it != "null" }

                        val song = Song(
                            id = songId,
                            title = title,
                            artistsText = artist,
                            durationText = null,
                            thumbnailUrl = artworkUrl,
                            likedAt = System.currentTimeMillis()
                        )
                        if (songId !in localFavIds) {
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
                    check(SwaaraSupabaseClient.upsert("user_favorites", batchArray.toString(), "user_id,song_id", token)) {
                        "Unable to upload favorites"
                    }
                }
                totalSyncedFavs = (localFavIds + remoteFavSongIds).size

                // 2. Sync Playlists
                val localPreviews = Database.playlistPreviewsByNameAsc().first()
                val remotePlaylistsJson = SwaaraSupabaseClient.get("user_playlists", "user_id=eq.$userId", token)
                    ?: error("Unable to fetch cloud playlists")
                check(localPreviews.groupBy { it.playlist.name }.values.none { it.size > 1 }) {
                    "Duplicate local playlist names cannot be safely synced"
                }

                val remotePlaylistNames = mutableSetOf<String>()
                for (i in 0 until remotePlaylistsJson.length()) {
                    check(remotePlaylistNames.add(remotePlaylistsJson.getJSONObject(i).getString("name"))) {
                        "Duplicate cloud playlist names cannot be safely synced"
                    }
                }
                if (remotePlaylistsJson != null) {
                    for (i in 0 until remotePlaylistsJson.length()) {
                        val pObj = remotePlaylistsJson.getJSONObject(i)
                        val pId = pObj.getString("id")
                        val pName = pObj.getString("name")

                        val existingMatch = localPreviews.firstOrNull { it.playlist.name == pName }
                        var targetLocalId = existingMatch?.playlist?.id ?: Database.insert(Playlist(name = pName))
                        if (targetLocalId <= 0) {
                            val currentPreviews = Database.playlistPreviewsByNameAsc().first()
                            targetLocalId = currentPreviews.firstOrNull { it.playlist.name == pName }?.playlist?.id ?: -1L
                        }

                        check(targetLocalId > 0) { "Unable to create local playlist" }
                        // Fetch remote playlist tracks
                        val tracksJson = SwaaraSupabaseClient.get("user_playlist_tracks", "playlist_id=eq.$pId&order=position.asc", token)
                            ?: error("Unable to fetch cloud playlist tracks")
                        if (targetLocalId > 0) {
                            for (t in 0 until tracksJson.length()) {
                                val track = tracksJson.getJSONObject(t)
                                val sId = track.getString("song_id")
                                val sTitle = track.optString("title", "Track")
                                val sArtist = track.optString("artist", "Unknown")
                                val sArtwork = track.optString("artwork_url").takeIf { it.isNotBlank() && it != "null" }
                                val pos = track.optInt("position", t)

                                val song = Song(
                                    id = sId,
                                    title = sTitle,
                                    artistsText = sArtist,
                                    durationText = null,
                                    thumbnailUrl = sArtwork
                                )
                                Database.insert(song)
                                Database.insert(SongPlaylistMap(songId = sId, playlistId = targetLocalId, position = pos))
                            }
                        }
                    }
                }

                // Push local playlists missing in remote
                for (preview in localPreviews) {
                    val plName = preview.playlist.name
                    if (plName !in remotePlaylistNames) {
                        val newRemoteId = UUID.randomUUID().toString()
                        val plObj = JSONObject().apply {
                            put("id", newRemoteId)
                            put("user_id", userId)
                            put("name", plName)
                        }
                        check(SwaaraSupabaseClient.insert("user_playlists", JSONArray().put(plObj).toString(), token)) {
                            "Unable to create cloud playlist"
                        }
                        val songs = Database.playlistSongs(preview.playlist.id).first()
                        val tracksArray = JSONArray()
                        songs.forEachIndexed { idx, song ->
                            tracksArray.put(JSONObject().apply {
                                put("playlist_id", newRemoteId)
                                put("song_id", song.id)
                                put("title", song.title)
                                put("artist", song.artistsText ?: "Unknown")
                                put("artwork_url", song.thumbnailUrl ?: "")
                                put("position", idx)
                            })
                        }
                        if (tracksArray.length() > 0 && !SwaaraSupabaseClient.insert("user_playlist_tracks", tracksArray.toString(), token)) {
                            // Best-effort rollback prevents a failed first upload looking complete on retry.
                            SwaaraSupabaseClient.delete("user_playlists", "id=eq.$newRemoteId", token)
                            error("Unable to upload playlist tracks")
                        }
                    }
                }
                totalSyncedPlaylists = (localPreviews.map { it.playlist.name }.toSet() + remotePlaylistNames).size

                // Profile is created by the auth trigger. PATCH avoids requiring an INSERT policy.
                val profilePayload = JSONObject().apply {
                    put("display_name", auth.displayName)
                    put("avatar_url", auth.avatarUrl ?: "")
                }
                check(SwaaraSupabaseClient.patch("profiles", profilePayload.toString(), "id=eq.$userId", token)) {
                    "Unable to update cloud profile"
                }

                auth.lastSyncTimestamp = System.currentTimeMillis()
                val msg = "Merged $totalSyncedFavs favorites and $totalSyncedPlaylists playlists. Edits and deletions are not yet fully synchronized."
                _syncStatus.value = SyncStatus.Success(totalSyncedFavs, totalSyncedPlaylists, msg)

                SyncResult(
                    success = true,
                    favoritesCount = totalSyncedFavs,
                    playlistsCount = totalSyncedPlaylists,
                    message = msg
                )
            } catch (e: CancellationException) {
                _syncStatus.value = SyncStatus.Idle
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Sync error", e)
                val err = "Sync failed: ${e.localizedMessage ?: "Network error"}"
                _syncStatus.value = SyncStatus.Error(err)
                SyncResult(success = false, message = err)
            }
        }
    }
}
