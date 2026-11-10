package com.varshudev.a1swaara.ui.screens.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.models.Song
import com.varshudev.a1swaara.ui.components.LocalMenuState
import com.varshudev.a1swaara.ui.components.themed.CollectionTrackSectionHeader
import com.varshudev.a1swaara.ui.components.themed.HideSongDialog
import com.varshudev.a1swaara.ui.components.themed.NonQueuedMediaItemMenu
import com.varshudev.a1swaara.ui.components.themed.PrimaryButton
import com.varshudev.a1swaara.ui.components.themed.SongCollectionScreen
import com.varshudev.a1swaara.ui.components.themed.songCollectionItems
import com.varshudev.a1swaara.ui.components.themed.swaaraGlassTrackSurface
import com.varshudev.a1swaara.ui.items.SongItem
import com.varshudev.a1swaara.ui.modifiers.songSwipeActions
import com.varshudev.a1swaara.utils.PlaylistDownloadFloatingButton
import com.varshudev.a1swaara.utils.LocalPlaybackActions
import com.varshudev.a1swaara.utils.asMediaItem
import com.varshudev.a1swaara.utils.rememberMediaItems
import app.a1swaara.core.ui.Dimensions
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.utils.isLandscape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumSongs(
    songs: ImmutableList<Song>,
    headerContent: @Composable (
        beforeContent: (@Composable () -> Unit)?,
        afterContent: (@Composable () -> Unit)?
    ) -> Unit,
    thumbnailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    afterHeaderContent: (@Composable () -> Unit)? = null
) {
    val (colorPalette) = LocalAppearance.current
    val playbackActions = LocalPlaybackActions.current
    val menuState = LocalMenuState.current
    val lazyListState = rememberLazyListState()
    val mediaItems = rememberMediaItems(songs)
    var hidingSong by rememberSaveable { mutableStateOf<String?>(null) }

    SongCollectionScreen(
        thumbnailContent = thumbnailContent,
        listState = lazyListState,
        modifier = modifier,
        listBackground = colorPalette.background0,
        onShuffle = { playbackActions.shufflePlay(mediaItems) },
        floatingActionsContent = {
            PlaylistDownloadFloatingButton(mediaItems.toImmutableList())
            PrimaryButton(
                icon = R.drawable.enqueue,
                onClick = { playbackActions.enqueue(mediaItems) }
            )
        },
        headerContent = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                headerContent(null, null)

                if (!isLandscape) thumbnailContent()
                afterHeaderContent?.invoke()
                if (songs.isNotEmpty()) {
                    CollectionTrackSectionHeader(count = songs.size)
                }
            }
        }
    ) {
        songCollectionItems(
            items = songs,
            isLoading = songs.isEmpty(),
            key = { _, song -> song.id },
        ) { index, song ->
            if (hidingSong == song.id) HideSongDialog(
                song = song,
                onDismiss = { hidingSong = null },
                onConfirm = { hidingSong = null }
            )

            SongItem(
                song = song,
                index = index,
                thumbnailSize = Dimensions.thumbnails.song,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 3.dp)
                    .songSwipeActions(
                        key = songs,
                        mediaItem = song.asMediaItem,
                        songToHide = song,
                        onSwipeLeftRequested = { hidingSong = it.id }
                    )
                    .swaaraGlassTrackSurface()
                    .combinedClickable(
                        onLongClick = {
                            menuState.display {
                                NonQueuedMediaItemMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = song.asMediaItem
                                )
                            }
                        },
                        onClick = {
                            playbackActions.playAtIndex(mediaItems, index)
                        }
                    )
            )
        }
    }
}
