package com.varshudev.a1swaara.utils

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.varshudev.a1swaara.models.Song
import com.varshudev.a1swaara.repositories.SongsRepository
import com.varshudev.a1swaara.service.LOCAL_KEY_PREFIX
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds

fun Context.musicFilesAsFlow(
    songsRepository: SongsRepository
): Flow<List<Song>> = callbackFlow {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            trySend(Unit)
        }
    }

    contentResolver.registerContentObserver(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        true,
        observer
    )
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            true,
            observer
        )
    }

    trySend(Unit)

    awaitClose {
        contentResolver.unregisterContentObserver(observer)
    }
}.map {
    AudioMediaCursor.query(contentResolver) {
        buildList {
            while (next()) {
                if (!isMusic || duration == 0) continue
                add(
                    Song(
                        id = "$LOCAL_KEY_PREFIX$id",
                        title = name,
                        artistsText = artist,
                        durationText = duration.milliseconds.toComponents { minutes, seconds, _ ->
                            "$minutes:${seconds.toString().padStart(2, '0')}"
                        },
                        thumbnailUrl = albumUri.toString()
                    )
                )
            }
        }
    } ?: emptyList()
}.distinctUntilChanged()
    .onEach(songsRepository::upsertSongs)
