package com.varshudev.a1swaara.ui.screens.artist

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varshudev.a1swaara.LocalAppContainer
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.models.Artist
import com.varshudev.a1swaara.preferences.UIStatePreferences
import com.varshudev.a1swaara.ui.components.LocalMenuState
import com.varshudev.a1swaara.ui.components.themed.Header
import com.varshudev.a1swaara.ui.components.themed.HeaderIconButton
import com.varshudev.a1swaara.ui.components.themed.HeaderPlaceholder
import com.varshudev.a1swaara.ui.components.themed.NonQueuedMediaItemMenu
import com.varshudev.a1swaara.ui.components.themed.RetryMessage
import com.varshudev.a1swaara.ui.components.themed.Scaffold
import com.varshudev.a1swaara.ui.components.themed.adaptiveThumbnailContent
import com.varshudev.a1swaara.ui.components.themed.swaaraGlassTrackSurface
import com.varshudev.a1swaara.ui.items.AlbumItem
import com.varshudev.a1swaara.ui.items.AlbumItemPlaceholder
import com.varshudev.a1swaara.ui.items.SongItem
import com.varshudev.a1swaara.ui.items.SongItemPlaceholder
import com.varshudev.a1swaara.ui.items.VideoItem
import com.varshudev.a1swaara.ui.items.VideoItemPlaceholder
import com.varshudev.a1swaara.ui.modifiers.songSwipeActions
import com.varshudev.a1swaara.ui.screens.GlobalRoutes
import com.varshudev.a1swaara.ui.screens.Route
import com.varshudev.a1swaara.ui.screens.albumRoute
import com.varshudev.a1swaara.ui.screens.leaderboardRoute
import com.varshudev.a1swaara.ui.screens.searchresult.ItemsPage
import com.varshudev.a1swaara.ui.state.LoadState
import com.varshudev.a1swaara.ui.state.contentOrNull
import com.varshudev.a1swaara.ui.viewmodels.ArtistViewModel
import com.varshudev.a1swaara.utils.asMediaItem
import com.varshudev.a1swaara.utils.forcePlay
import app.a1swaara.compose.persist.PersistMapCleanup
import app.a1swaara.compose.persist.persist
import app.a1swaara.compose.routing.RouteHandler
import app.a1swaara.core.ui.Dimensions
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.providers.youtubemusic.innertube.YoutubeMusicInnertube
import com.valentinilk.shimmer.shimmer

@OptIn(ExperimentalFoundationApi::class)
@Route
@Composable
fun ArtistScreen(browseId: String) {
    val viewModel: ArtistViewModel = viewModel(
        key = "artist:$browseId",
        factory = ArtistViewModel.factory(
            browseId = browseId,
            repository = LocalAppContainer.current.artistRepository
        )
    )
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup(prefix = "artist/$browseId/")

    var persistedArtist by persist<Artist?>("artist/$browseId/artist")

    var artistPage by persist<YoutubeMusicInnertube.ArtistPage?>("artist/$browseId/artistPage")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stateArtist = uiState.contentOrNull()?.artist
    val artist = stateArtist ?: persistedArtist
    val statePage = uiState.contentOrNull()?.page
    val displayedPage = statePage ?: artistPage

    LaunchedEffect(browseId) {
        viewModel.loadArtist(cachedPage = artistPage)
    }
    LaunchedEffect(uiState) {
        if (uiState is LoadState.Content) {
            artistPage = (uiState as LoadState.Content).value.page
            stateArtist?.let { persistedArtist = it }
        }
    }

    RouteHandler {
        GlobalRoutes()

        Content {
            val thumbnailContent = adaptiveThumbnailContent(
                isLoading = uiState is LoadState.Loading && displayedPage == null,
                url = displayedPage?.thumbnail?.url ?: artist?.thumbnailUrl,
                shape = CircleShape
            )

            val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit =
                { textButton ->
                    if (uiState is LoadState.Loading && artist == null) HeaderPlaceholder(modifier = Modifier.shimmer()) else {
                        val (colorPalette) = LocalAppearance.current
                        val context = LocalContext.current

                        Header(title = artist?.name ?: stringResource(R.string.unknown)) {
                            if (textButton != null) {
                                Box(modifier = Modifier.weight(1f)) {
                                    textButton()
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            HeaderIconButton(
                                icon = if (artist?.bookmarkedAt == null) R.drawable.bookmark_outline
                                else R.drawable.bookmark,
                                color = if (artist?.bookmarkedAt == null) colorPalette.text
                                else colorPalette.accent,
                                onClick = { viewModel.toggleBookmark(artist) }
                            )

                            HeaderIconButton(
                                icon = R.drawable.person,
                                color = colorPalette.secondaryAccent,
                                onClick = {
                                    artist?.name?.let { name ->
                                        leaderboardRoute(name)
                                    }
                                }
                            )

                            HeaderIconButton(
                                icon = R.drawable.share_social,
                                color = colorPalette.text,
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "https://music.youtube.com/channel/$browseId"
                                        )
                                    }

                                    context.startActivity(Intent.createChooser(sendIntent, null))
                                }
                            )
                        }
                    }
                }

            Scaffold(
                key = "artist",
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = UIStatePreferences.artistScreenTabIndex,
                onTabChange = { UIStatePreferences.artistScreenTabIndex = it },
                tabColumnContent = {
                    tab(0, R.string.overview, R.drawable.sparkles)
                    tab(1, R.string.songs, R.drawable.musical_notes)
                    tab(2, R.string.albums, R.drawable.disc)
                    tab(3, R.string.singles, R.drawable.disc)
                    tab(4, R.string.videos, R.drawable.film)
                    tab(5, R.string.library, R.drawable.library)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    if (uiState is LoadState.Error && displayedPage == null && currentTabIndex != 4) {
                        RetryMessage(
                            onRetry = { viewModel.loadArtist(force = true) },
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize()
                        )
                    } else when (currentTabIndex) {
                        0 -> ArtistOverview(
                            youtubeArtistPage = displayedPage,
                            thumbnailContent = thumbnailContent,
                            headerContent = headerContent,
                            onAlbumClick = { albumRoute(it) },
                            onViewAllSongsClick = { UIStatePreferences.artistScreenTabIndex = 1 },
                            onViewAllAlbumsClick = { UIStatePreferences.artistScreenTabIndex = 2 },
                            onViewAllSinglesClick = { UIStatePreferences.artistScreenTabIndex = 3 }
                        )

                        1 -> ItemsPage(
                            tag = "artist/$browseId/songs",
                            header = headerContent,
                            provider = displayedPage?.let {
                                { continuation -> viewModel.songsPage(displayedPage, continuation) }
                            },
                            itemContent = { song ->
                                SongItem(
                                    song = song,
                                    thumbnailSize = Dimensions.thumbnails.song,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                        .songSwipeActions(
                                            key = "artist/$browseId/songs",
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

                        2 -> ItemsPage(
                            tag = "artist/$browseId/albums",
                            header = headerContent,
                            emptyItemsText = stringResource(R.string.artist_has_no_albums),
                            provider = displayedPage?.let {
                                { continuation -> viewModel.albumsPage(displayedPage, continuation) }
                            },
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

                        3 -> ItemsPage(
                            tag = "artist/$browseId/singles",
                            header = headerContent,
                            emptyItemsText = stringResource(R.string.artist_has_no_singles),
                            provider = displayedPage?.let {
                                { continuation -> viewModel.singlesPage(displayedPage, continuation) }
                            },
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

                        4 -> ItemsPage(
                            // v3 replaces the carousel's ten-item snapshot with its complete
                            // more-content playlist and continuation pages.
                            tag = "artist/$browseId/videos/v3",
                            header = headerContent,
                            provider = displayedPage?.let {
                                { continuation -> viewModel.videosPage(displayedPage, continuation) }
                            },
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
                                                        onDismiss = menuState::hide,
                                                        mediaItem = video.asMediaItem
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
                                    thumbnailHeight = Dimensions.thumbnails.videoHeight,
                                )
                            }
                        )

                        5 -> ArtistLocalSongs(
                            browseId = browseId,
                            headerContent = headerContent,
                            thumbnailContent = thumbnailContent
                        )
                    }
                }
            }
        }
    }
}
