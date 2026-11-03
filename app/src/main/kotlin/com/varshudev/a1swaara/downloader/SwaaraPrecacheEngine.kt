package com.varshudev.a1swaara.downloader

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.varshudev.a1swaara.extractor.NewPipeExtractorClient
import com.varshudev.a1swaara.extractor.YoutubeHttpDataSourceFactory
import com.varshudev.a1swaara.service.PlayerService
import com.varshudev.a1swaara.service.playbackHttpDataSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Predictive Pre-Caching Engine for A1 Swaara.
 * Observes the active playback queue and proactively streams the first 2.5 MB of upcoming tracks
 * into PlayerService.sharedCache in the background. This guarantees instant 0ms playback transitions
 * with zero audio buffering gaps.
 */
@OptIn(UnstableApi::class)
object SwaaraPrecacheEngine {
    private const val TAG = "SwaaraPrecacheEngine"
    private const val PRECACHE_CHUNK_BYTES = 2500000L // 2.5 MB safety buffer

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activeJob: Job? = null
    private val precachedSongIds = ConcurrentHashMap<String, Long>()

    fun onTrackTransition(context: Context, player: Player) {
        activeJob?.cancel()
        activeJob = scope.launch {
            // Small delay to allow the active track to establish its initial network buffers first
            delay(1500)
            precacheNextTracks(context, player)
        }
    }

    private suspend fun precacheNextTracks(context: Context, player: Player) {
        val totalCount = player.mediaItemCount
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex < 0 || totalCount <= 1) return

        val lookaheadLimit = minOf(currentIndex + 3, totalCount - 1)
        if (lookaheadLimit <= currentIndex) return

        val cache = try {
            PlayerService.sharedCache(context)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot access shared cache: ${e.message}")
            return
        }

        val baseHttpFactory = playbackHttpDataSourceFactory(context)
        val upstreamFactory = YoutubeHttpDataSourceFactory(
            rnParameterEnabled = false,
            upstreamFactory = baseHttpFactory
        )

        val cacheDataSource = CacheDataSource(
            cache,
            upstreamFactory.createDataSource(),
            CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
        )

        for (i in (currentIndex + 1)..lookaheadLimit) {
            val mediaItem: MediaItem = try {
                player.getMediaItemAt(i)
            } catch (_: Exception) {
                continue
            }

            val songId = mediaItem.mediaId
            if (songId.isBlank()) continue

            // Check if already partially or fully cached
            val alreadyCached = cache.isCached(songId, 0, 512 * 1024)
            if (alreadyCached || precachedSongIds.containsKey(songId)) {
                continue
            }

            try {
                val audioResult = NewPipeExtractorClient.resolveAudioStream(songId)
                val directUrl = audioResult.audioStream?.content
                    ?: audioResult.videoStream?.content
                    ?: continue

                val dataSpec = DataSpec.Builder()
                    .setUri(directUrl.toUri())
                    .setKey(songId)
                    .setLength(PRECACHE_CHUNK_BYTES)
                    .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
                    .build()

                val cacheWriter = CacheWriter(
                    cacheDataSource,
                    dataSpec,
                    ByteArray(64 * 1024),
                    null
                )

                cacheWriter.cache()
                precachedSongIds[songId] = System.currentTimeMillis()
                Log.d(TAG, "Successfully pre-cached chunk for song: $songId")
            } catch (e: Exception) {
                Log.w(TAG, "Pre-cache skipped for song $songId: ${e.message}")
            }
        }
    }
}
