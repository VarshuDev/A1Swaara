package com.varshudev.a1swaara.downloader

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.varshudev.a1swaara.Database
import com.varshudev.a1swaara.extractor.NewPipeExtractorClient
import com.varshudev.a1swaara.extractor.YoutubeHttpDataSourceFactory
import com.varshudev.a1swaara.service.LOCAL_KEY_PREFIX
import com.varshudev.a1swaara.service.PlayerService
import com.varshudev.a1swaara.service.playbackHttpDataSourceFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Autonomous Smart Stash Worker ("Never Run Out of Music").
 * Silently runs only when connected to unmetered Wi-Fi while charging with healthy battery.
 * Precaches top rotation tracks into ExoPlayer's shared disk cache so playback works seamlessly
 * even in dead zones, flights, or complete offline environments.
 */
@OptIn(UnstableApi::class)
class SwaaraSmartStashWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SwaaraSmartStashWorker"
        private const val WORK_NAME = "swaara_smart_stash_worker"
        private const val MAX_STASH_TRACKS = 30
        private const val MAX_STASH_PER_RUN = 10
        private const val STASH_TRACK_BYTES = 8L * 1024L * 1024L // Up to 8 MB per track

        fun upsert(context: Context) = runCatching {
            val workManager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SwaaraSmartStashWorker>(
                6,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Scheduled autonomous Smart Stash worker (Wi-Fi + Charging)")
        }.onFailure {
            Log.e(TAG, "Failed to schedule SwaaraSmartStashWorker", it)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            NewPipeExtractorClient.ensureInitialized(applicationContext)

            val topSongs = Database.songsByPlayTimeDesc(MAX_STASH_TRACKS).first()
                .filter { !it.id.startsWith(LOCAL_KEY_PREFIX) && it.id.isNotBlank() }

            if (topSongs.isEmpty()) {
                return Result.success()
            }

            val cache = PlayerService.sharedCache(applicationContext)
            val baseHttpFactory = playbackHttpDataSourceFactory(applicationContext)
            val upstreamFactory = YoutubeHttpDataSourceFactory(
                rnParameterEnabled = false,
                upstreamFactory = baseHttpFactory
            )
            val cacheDataSource = CacheDataSource(
                cache,
                upstreamFactory.createDataSource(),
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
            )

            var stashedCount = 0
            for (song in topSongs) {
                if (stashedCount >= MAX_STASH_PER_RUN || isStopped) break

                val alreadyCached = runCatching {
                    cache.isCached(song.id, 0, 1024L * 1024L)
                }.getOrDefault(false)
                if (alreadyCached) continue

                try {
                    val audioResult = NewPipeExtractorClient.resolveAudioStream(song.id)
                    val directUrl = audioResult.audioStream?.content
                        ?: audioResult.videoStream?.content
                        ?: continue

                    val dataSpec = DataSpec.Builder()
                        .setUri(directUrl.toUri())
                        .setKey(song.id)
                        .setLength(STASH_TRACK_BYTES)
                        .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
                        .build()

                    val cacheWriter = CacheWriter(
                        cacheDataSource,
                        dataSpec,
                        ByteArray(64 * 1024),
                        null
                    )
                    cacheWriter.cache()
                    stashedCount++
                    Log.d(TAG, "Smart Stash cached track: ${song.title} (${song.id})")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Smart Stash skipped ${song.id}: ${e.message}")
                }
            }

            Log.i(TAG, "Smart Stash run finished: cached $stashedCount new track(s)")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Smart Stash worker failed", e)
            Result.retry()
        }
    }
}
