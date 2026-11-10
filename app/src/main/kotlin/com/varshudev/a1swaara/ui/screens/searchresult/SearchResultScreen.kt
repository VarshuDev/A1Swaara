package com.varshudev.a1swaara.ui.screens.searchresult

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.varshudev.a1swaara.LocalAppContainer
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.preferences.UIStatePreferences
import com.varshudev.a1swaara.ui.components.LocalMenuState
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.themed.Header
import com.varshudev.a1swaara.ui.components.themed.NonQueuedMediaItemMenu
import com.varshudev.a1swaara.ui.components.themed.Scaffold
import com.varshudev.a1swaara.ui.components.themed.swaaraGlassTrackSurface
import com.varshudev.a1swaara.ui.items.AlbumItem
import com.varshudev.a1swaara.ui.items.AlbumItemPlaceholder
import com.varshudev.a1swaara.ui.items.ArtistItem
import com.varshudev.a1swaara.ui.items.ArtistItemPlaceholder
import com.varshudev.a1swaara.ui.items.PlaylistItem
import com.varshudev.a1swaara.ui.items.PlaylistItemPlaceholder
import com.varshudev.a1swaara.ui.items.SongItem
import com.varshudev.a1swaara.ui.items.SongItemPlaceholder
import com.varshudev.a1swaara.ui.items.VideoItem
import com.varshudev.a1swaara.ui.items.VideoItemPlaceholder
import com.varshudev.a1swaara.ui.modifiers.songSwipeActions
import com.varshudev.a1swaara.ui.screens.GlobalRoutes
import com.varshudev.a1swaara.ui.screens.Route
import com.varshudev.a1swaara.ui.screens.albumRoute
import com.varshudev.a1swaara.ui.screens.artistRoute
import com.varshudev.a1swaara.ui.screens.playlistRoute
import com.varshudev.a1swaara.ui.viewmodels.SearchResultViewModel
import com.varshudev.a1swaara.utils.asMediaItem
import com.varshudev.a1swaara.utils.forcePlay
import com.varshudev.a1swaara.utils.semiBold
import app.a1swaara.compose.persist.LocalPersistMap
import app.a1swaara.compose.persist.PersistMapCleanup
import app.a1swaara.compose.routing.RouteHandler
import app.a1swaara.core.ui.Dimensions
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.core.ui.utils.roundedShape

@OptIn(ExperimentalFoundationApi::class)
@Route
@Composable
fun SearchResultScreen(query: String, onSearchAgain: () -> Unit) {
    val viewModel: SearchResultViewModel = viewModel(
        key = "search_result:$query",
        factory = SearchResultViewModel.factory(
            query = query,
            repository = LocalAppContainer.current.searchResultRepository
        )
    )
    val persistMap = LocalPersistMap.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup(prefix = "searchResults/$query/")

    RouteHandler {
        GlobalRoutes()

        Content {
            val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit = {
                val (colorPalette, typography) = LocalAppearance.current
                Header(
                    titleContent = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(16.dp.roundedShape)
                                .background(SwaaraDeepSurface)
                                .border(1.dp, Color.White.copy(alpha = 0.08f), 16.dp.roundedShape)
                                .clickable {
                                    persistMap?.clean("searchResults/$query/")
                                    onSearchAgain()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = null,
                                tint = colorPalette.secondaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicText(
                                text = query,
                                style = typography.m.semiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(10.dp.roundedShape)
                                    .background(colorPalette.secondaryAccent.copy(alpha = 0.14f))
                                    .border(1.dp, colorPalette.secondaryAccent.copy(alpha = 0.30f), 10.dp.roundedShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.pencil),
                                    contentDescription = null,
                                    tint = colorPalette.secondaryAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                BasicText(
                                    text = "Edit",
                                    style = typography.xxs.semiBold.copy(color = colorPalette.secondaryAccent)
                                )
                            }
                        }
                    },
                    actionsContent = {}
                )
            }

            Scaffold(
                key = "searchresult",
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = UIStatePreferences.searchResultScreenTabIndex,
                onTabChange = { UIStatePreferences.searchResultScreenTabIndex = it },
                tabColumnContent = {
                    tab(0, R.string.songs, R.drawable.musical_notes)
                    tab(1, R.string.albums, R.drawable.disc)
                    tab(2, R.string.artists, R.drawable.person)
                    tab(3, R.string.videos, R.drawable.film)
                    tab(4, R.string.playlists, R.drawable.playlist)
                }
            ) { tabIndex ->
                saveableStateHolder.SaveableStateProvider(tabIndex) {
                    when (tabIndex) {
                        0 -> ItemsPage(
                            tag = "searchResults/$query/songs",
                            provider = viewModel::songsPage,
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { song ->
                                SongItem(
                                    song = song,
                                    thumbnailSize = Dimensions.thumbnails.song,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                        .songSwipeActions(
                                            key = "searchResults/$query/songs",
                                            mediaItem = song.asMediaItem
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
                                                binder?.stopRadio()
                                                binder?.player?.forcePlay(song.asMediaItem)
                                                binder?.setupRadio(song.info?.endpoint)
                                            }
                                        )
                                )
                            },
                            itemPlaceholderContent = {
                                SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
                            }
                        )

                        1 -> ItemsPage(
                            tag = "searchResults/$query/albums",
                            provider = viewModel::albumsPage,
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSize = Dimensions.thumbnails.album,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                        .swaaraGlassTrackSurface()
                                        .clickable(onClick = { albumRoute(album.key) })
                                )
                            },
                            itemPlaceholderContent = {
                                AlbumItemPlaceholder(thumbnailSize = Dimensions.thumbnails.album)
                            }
                        )

                        2 -> ItemsPage(
                            tag = "searchResults/$query/artists",
                            provider = viewModel::artistsPage,
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { artist ->
                                ArtistItem(
                                    artist = artist,
                                    thumbnailSize = 64.dp,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                        .swaaraGlassTrackSurface()
                                        .clickable(onClick = { artistRoute(artist.key) })
                                )
                            },
                            itemPlaceholderContent = {
                                ArtistItemPlaceholder(thumbnailSize = 64.dp)
                            }
                        )

                        3 -> ItemsPage(
                            tag = "searchResults/$query/videos",
                            provider = viewModel::videosPage,
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { video ->
                                VideoItem(
                                    video = video,
                                    thumbnailWidth = Dimensions.thumbnails.videoWidth,
                                    thumbnailHeight = Dimensions.thumbnails.videoHeight,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                        .swaaraGlassTrackSurface()
                                        .combinedClickable(
                                            onLongClick = {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        mediaItem = video.asMediaItem,
                                                        onDismiss = menuState::hide
                                                    )
                                                }
                                            },
                                            onClick = {
                                                binder?.stopRadio()
                                                binder?.player?.forcePlay(video.asMediaItem)
                                                binder?.setupRadio(video.info?.endpoint)
                                            }
                                        )
                                )
                            },
                            itemPlaceholderContent = {
                                VideoItemPlaceholder(
                                    thumbnailWidth = Dimensions.thumbnails.videoWidth,
                                    thumbnailHeight = Dimensions.thumbnails.videoHeight
                                )
                            }
                        )

                        4 -> ItemsPage(
                            tag = "searchResults/$query/playlists",
                            provider = viewModel::playlistsPage,
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { playlist ->
                                PlaylistItem(
                                    playlist = playlist,
                                    thumbnailSize = Dimensions.thumbnails.playlist,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                        .swaaraGlassTrackSurface()
                                        .clickable {
                                            playlistRoute(playlist.key, null, null, false)
                                        }
                                )
                            },
                            itemPlaceholderContent = {
                                PlaylistItemPlaceholder(thumbnailSize = Dimensions.thumbnails.playlist)
                            }
                        )
                    }
                }
            }
        }
    }
}
