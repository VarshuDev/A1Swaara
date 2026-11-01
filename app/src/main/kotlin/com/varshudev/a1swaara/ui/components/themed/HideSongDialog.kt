package com.varshudev.a1swaara.ui.components.themed

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import com.varshudev.a1swaara.LocalAppContainer
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.models.Song
import com.varshudev.a1swaara.service.isLocal
import com.varshudev.a1swaara.utils.removeSongFromQueue

@OptIn(UnstableApi::class)
@Composable
fun HideSongDialog(
    song: Song,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onHideSong: ((Song) -> Unit)? = null
) {
    val binder = LocalPlayerServiceBinder.current
    val defaultOnHideSong = onHideSong ?: LocalAppContainer.current.songsRepository::deleteSong

    ConfirmationDialog(
        text = stringResource(R.string.confirm_hide_song),
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm()
            runCatching {
                binder?.player?.removeSongFromQueue(song.id)
                if (!song.isLocal) binder?.cache?.removeResource(song.id)
                defaultOnHideSong(song)
            }
        },
        modifier = modifier
    )
}
