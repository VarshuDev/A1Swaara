package com.varshudev.a1swaara.utils

import android.content.Intent
import androidx.media3.common.MediaItem
import com.varshudev.a1swaara.service.PlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

fun PlayerService.Binder?.playMediaItem(mediaItem: MediaItem) {
    if (this == null) return
    stopRadio()
    prefetchMediaItems(listOf(mediaItem))
    player.forcePlay(mediaItem)
}

fun PlayerService.Binder?.playSongAtIndex(
    mediaItems: List<MediaItem>,
    index: Int
) {
    if (this == null) return
    stopRadio()
    prefetchMediaItems(mediaItems)
    player.forcePlayAtIndex(mediaItems, index)
}

fun PlayerService.Binder?.shufflePlay(mediaItems: List<MediaItem>) {
    if (this == null || mediaItems.isEmpty()) return
    stopRadio()
    prefetchMediaItems(mediaItems)
    player.forcePlayFromBeginning(mediaItems.shuffled())
}

fun PlayerService.Binder?.playTrack(
    videoId: String,
    title: String,
    artist: String?,
    thumbnailUrl: String?,
    durationText: String? = null,
    isPodcast: Boolean = false
) {
    if (this == null) return
    val cleanArtist = artist?.replace(Regex("[,&\\s]{2,}"), ", ")?.trim(',', ' ') ?: "Artist"
    val highResThumb = thumbnailUrl?.thumbnail(1200) ?: "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
    val mediaItem = MediaItem.Builder()
        .setMediaId(videoId)
        .setUri(videoId)
        .setCustomCacheKey(videoId)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(cleanArtist)
                .setArtworkUri(android.net.Uri.parse(highResThumb))
                .setExtras(
                    app.a1swaara.core.ui.utils.SongBundleAccessor.bundle {
                        this.artistNames = listOf(cleanArtist)
                        this.durationText = durationText
                        this.isFromPersistentQueue = false
                    }.apply {
                        putBoolean("is_podcast", isPodcast)
                    }
                )
                .build()
        )
        .build()

    stopRadio()
    prefetchMediaItems(listOf(mediaItem))
    player.forcePlay(mediaItem)

    val watchEndpoint = app.a1swaara.providers.youtubemusic.innertube.models.NavigationEndpoint.Endpoint.Watch(videoId = videoId)
    setupRadio(watchEndpoint)
}

fun playTrackWithService(
    context: android.content.Context,
    binder: PlayerService.Binder?,
    videoId: String,
    title: String,
    artist: String?,
    thumbnailUrl: String?,
    durationText: String? = null,
    isPodcast: Boolean = false,
    onStarted: () -> Unit = {}
) {
    val active = binder ?: PlayerService.activeBinder.value
    if (active != null) {
        active.playTrack(videoId, title, artist, thumbnailUrl, durationText, isPodcast)
        onStarted()
        return
    }

    try {
        context.startService(Intent(context, PlayerService::class.java))
        context.bindService(
            Intent(context, PlayerService::class.java),
            object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                    if (service is PlayerService.Binder) {
                        PlayerService.activeBinder.value = service
                    }
                }
                override fun onServiceDisconnected(name: android.content.ComponentName?) {}
            },
            android.content.Context.BIND_AUTO_CREATE
        )
    } catch (_: Exception) {}

    CoroutineScope(Dispatchers.Main).launch {
        try {
            withTimeout(5000L) {
                val resolved = PlayerService.activeBinder.filterNotNull().first()
                resolved.playTrack(videoId, title, artist, thumbnailUrl, durationText, isPodcast)
                onStarted()
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerBinder", "Timed out waiting for PlayerService binder", e)
            context.toast("Starting player took longer than expected. Please tap again.")
        }
    }
}
