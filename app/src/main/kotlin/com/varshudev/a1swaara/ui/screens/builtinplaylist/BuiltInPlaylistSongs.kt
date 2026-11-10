package com.varshudev.a1swaara.ui.screens.builtinplaylist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.varshudev.a1swaara.LocalAppContainer
import com.varshudev.a1swaara.LocalPlayerAwareWindowInsets
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.models.Song
import com.varshudev.a1swaara.preferences.DataPreferences
import com.varshudev.a1swaara.ui.components.LocalMenuState
import com.varshudev.a1swaara.ui.components.themed.CollectionTrackSectionHeader
import com.varshudev.a1swaara.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.varshudev.a1swaara.ui.components.themed.Header
import com.varshudev.a1swaara.ui.components.themed.HeaderSongSortBy
import com.varshudev.a1swaara.ui.components.themed.HideSongDialog
import com.varshudev.a1swaara.ui.components.themed.InHistoryMediaItemMenu
import com.varshudev.a1swaara.ui.components.themed.NonQueuedMediaItemMenu
import com.varshudev.a1swaara.ui.components.themed.PrimaryButton
import com.varshudev.a1swaara.ui.components.themed.SongListActionsRow
import com.varshudev.a1swaara.ui.components.themed.ValueSelectorDialog
import com.varshudev.a1swaara.ui.components.themed.matchesSongCollectionQuery
import com.varshudev.a1swaara.ui.components.themed.swaaraGlassTrackSurface
import com.varshudev.a1swaara.ui.items.SongItem
import com.varshudev.a1swaara.ui.items.SongTotalPlayTimeOverlay
import com.varshudev.a1swaara.ui.modifiers.songSwipeActions
import com.varshudev.a1swaara.ui.viewmodels.BuiltInPlaylistSongsViewModel
import com.varshudev.a1swaara.utils.LocalPlaybackActions
import com.varshudev.a1swaara.utils.PlaylistDownloadFloatingButton
import com.varshudev.a1swaara.utils.asMediaItem
import com.varshudev.a1swaara.utils.medium
import com.varshudev.a1swaara.utils.rememberMediaItems
import app.a1swaara.compose.persist.persistList
import app.a1swaara.core.data.enums.BuiltInPlaylist
import app.a1swaara.core.data.enums.SongSortBy
import app.a1swaara.core.data.enums.SortOrder
import app.a1swaara.core.ui.Dimensions
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.core.ui.utils.enumSaver
import app.a1swaara.core.ui.utils.roundedShape
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BuiltInPlaylistSongs(
    builtInPlaylist: BuiltInPlaylist,
    modifier: Modifier = Modifier
) = with(DataPreferences) {
    val viewModel: BuiltInPlaylistSongsViewModel = viewModel(
        factory = BuiltInPlaylistSongsViewModel.factory(LocalAppContainer.current.builtInPlaylistRepository)
    )
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playbackActions = LocalPlaybackActions.current

    var songs by persistList<Song>("${builtInPlaylist.name}/songs")
    var hidingSong by rememberSaveable { mutableStateOf<String?>(null) }
    var filterQuery by rememberSaveable { mutableStateOf<String?>(null) }
    val displayedSongs = songs.filter {
        matchesSongCollectionQuery(filterQuery, it.title, it.artistsText)
    }
    val mediaItems = rememberMediaItems(displayedSongs)

    var sortBy by rememberSaveable(stateSaver = enumSaver()) { mutableStateOf(SongSortBy.DateAdded) }
    var sortOrder by rememberSaveable(stateSaver = enumSaver()) { mutableStateOf(SortOrder.Descending) }

    LaunchedEffect(binder, sortBy, sortOrder) {
        viewModel.observeSongs(
            builtInPlaylist = builtInPlaylist,
            sortBy = sortBy,
            sortOrder = sortOrder,
            topPeriodMillis = topListPeriod.duration?.inWholeMilliseconds,
            topLength = topListLength,
            isOfflineCached = { song -> binder?.isCached(song) ?: false }
        ).collect { songs = it.toImmutableList() }
    }

    val lazyListState = rememberLazyListState()

    Box(modifier = modifier) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues(),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            item(
                key = "header",
                contentType = 0
            ) {
                Header(
                    title = when (builtInPlaylist) {
                        BuiltInPlaylist.Favorites -> stringResource(R.string.favorites)
                        BuiltInPlaylist.Offline -> stringResource(R.string.offline)
                        BuiltInPlaylist.Top -> stringResource(
                            R.string.format_my_top_playlist,
                            topListLength
                        )

                        BuiltInPlaylist.History -> stringResource(R.string.history)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    SongListActionsRow(
                        filterQuery = filterQuery,
                        onFilterQueryChange = { filterQuery = it },
                        trailingContent = {
                            if (builtInPlaylist.sortable) HeaderSongSortBy(
                                sortBy = sortBy,
                                setSortBy = { sortBy = it },
                                sortOrder = sortOrder,
                                setSortOrder = { sortOrder = it }
                            )

                            if (builtInPlaylist == BuiltInPlaylist.Top) {
                                var dialogShowing by rememberSaveable { mutableStateOf(false) }
                                val chipShape = 12.dp.roundedShape

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(chipShape)
                                        .background(colorPalette.secondaryAccent.copy(alpha = 0.14f))
                                        .border(
                                            width = 1.dp,
                                            color = colorPalette.secondaryAccent.copy(alpha = 0.35f),
                                            shape = chipShape
                                        )
                                        .clickable { dialogShowing = true }
                                        .padding(horizontal = 12.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.time),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.secondaryAccent),
                                        modifier = Modifier.size(14.dp)
                                    )

                                    BasicText(
                                        text = topListPeriod.displayName(),
                                        style = typography.xxs.medium.copy(color = colorPalette.text)
                                    )

                                    Image(
                                        painter = painterResource(R.drawable.chevron_down),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.secondaryAccent),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }

                                if (dialogShowing) ValueSelectorDialog(
                                    onDismiss = { dialogShowing = false },
                                    title = stringResource(
                                        R.string.format_view_top_of_header,
                                        topListLength
                                    ),
                                    selectedValue = topListPeriod,
                                    values = DataPreferences.TopListPeriod.entries.toImmutableList(),
                                    onValueSelect = { topListPeriod = it },
                                    valueText = { it.displayName() }
                                )
                            }
                        }
                    )
                }
            }

            if (displayedSongs.isNotEmpty()) {
                item(
                    key = "section_header",
                    contentType = "section_header"
                ) {
                    CollectionTrackSectionHeader(count = displayedSongs.size)
                }
            }

            itemsIndexed(
                items = displayedSongs,
                key = { _, song -> song.id },
                contentType = { _, song -> song }
            ) { index, song ->
                if (hidingSong == song.id) HideSongDialog(
                    song = song,
                    onDismiss = { hidingSong = null },
                    onConfirm = { hidingSong = null }
                )

                SongItem(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                        .animateItem()
                        .songSwipeActions(
                            key = displayedSongs,
                            mediaItem = song.asMediaItem,
                            songToHide = song,
                            onSwipeLeftRequested = { hidingSong = it.id }
                        )
                        .swaaraGlassTrackSurface()
                        .combinedClickable(
                            onLongClick = {
                                menuState.display {
                                    when (builtInPlaylist) {
                                        BuiltInPlaylist.Offline -> InHistoryMediaItemMenu(
                                            song = song,
                                            onDismiss = menuState::hide
                                        )

                                        BuiltInPlaylist.Favorites,
                                        BuiltInPlaylist.Top,
                                        BuiltInPlaylist.History -> NonQueuedMediaItemMenu(
                                            mediaItem = song.asMediaItem,
                                            onDismiss = menuState::hide
                                        )
                                    }
                                }
                            },
                            onClick = {
                                playbackActions.playAtIndex(mediaItems, index)
                            }
                        ),
                    song = song,
                    index = if (builtInPlaylist == BuiltInPlaylist.Top) index else null,
                    thumbnailSize = Dimensions.thumbnails.song,
                    onThumbnailContent = if (sortBy == SongSortBy.PlayTime) {
                        { SongTotalPlayTimeOverlay(song.totalPlayTimeMs) }
                    } else null
                )
            }

            item(
                key = "bottom_fab_spacer",
                contentType = "bottom_fab_spacer"
            ) {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            actionsContent = {
                if (builtInPlaylist != BuiltInPlaylist.Offline) {
                    PlaylistDownloadFloatingButton(mediaItems.toImmutableList())
                }
                PrimaryButton(
                    icon = R.drawable.enqueue,
                    onClick = { playbackActions.enqueue(mediaItems) }
                )
            },
            onClick = {
                playbackActions.shufflePlay(mediaItems)
            }
        )
    }
}
