package com.varshudev.a1swaara.ui.screens.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.LocalPlayerAwareWindowInsets
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.ui.components.LocalMenuState
import com.varshudev.a1swaara.ui.components.ShimmerHost
import com.varshudev.a1swaara.ui.components.themed.Attribution
import com.varshudev.a1swaara.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.varshudev.a1swaara.ui.components.themed.LayoutWithAdaptiveThumbnail
import com.varshudev.a1swaara.ui.components.themed.NonQueuedMediaItemMenu
import com.varshudev.a1swaara.ui.components.themed.SecondaryTextButton
import com.varshudev.a1swaara.ui.components.themed.TextPlaceholder
import com.varshudev.a1swaara.ui.components.themed.swaaraGlassTrackSurface
import com.varshudev.a1swaara.ui.items.AlbumItem
import com.varshudev.a1swaara.ui.items.AlbumItemPlaceholder
import com.varshudev.a1swaara.ui.items.SongItem
import com.varshudev.a1swaara.ui.items.SongItemPlaceholder
import com.varshudev.a1swaara.ui.modifiers.songSwipeActions
import com.varshudev.a1swaara.utils.asMediaItem
import com.varshudev.a1swaara.utils.forcePlay
import com.varshudev.a1swaara.utils.medium
import com.varshudev.a1swaara.utils.semiBold
import app.a1swaara.core.ui.Dimensions
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.core.ui.tertiaryAccent
import app.a1swaara.core.ui.utils.isLandscape
import app.a1swaara.core.ui.utils.roundedShape
import app.a1swaara.providers.youtubemusic.innertube.YoutubeMusicInnertube
import app.a1swaara.providers.youtubemusic.innertube.models.NavigationEndpoint

private val sectionTextModifier = Modifier
    .padding(horizontal = 16.dp)
    .padding(top = 24.dp, bottom = 8.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistOverview(
    youtubeArtistPage: YoutubeMusicInnertube.ArtistPage?,
    onViewAllSongsClick: () -> Unit,
    onViewAllAlbumsClick: () -> Unit,
    onViewAllSinglesClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    thumbnailContent: @Composable () -> Unit,
    headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit,
    modifier: Modifier = Modifier
) = LayoutWithAdaptiveThumbnail(
    thumbnailContent = thumbnailContent,
    modifier = modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val scrollState = rememberScrollState()

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    windowInsets
                        .only(WindowInsetsSides.Vertical)
                        .asPaddingValues()
                )
        ) {
            Box(modifier = Modifier.padding(endPaddingValues)) {
                headerContent {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        youtubeArtistPage?.shuffleEndpoint?.let { endpoint ->
                            SecondaryTextButton(
                                text = stringResource(R.string.shuffle),
                                onClick = {
                                    binder?.stopRadio()
                                    binder?.playRadio(endpoint)
                                }
                            )
                        }
                        youtubeArtistPage?.subscribersCountText?.let { subscribers ->
                            val pillShape = 12.dp.roundedShape
                            Box(
                                modifier = Modifier
                                    .clip(pillShape)
                                    .background(colorPalette.secondaryAccent.copy(alpha = 0.14f))
                                    .border(
                                        width = 1.dp,
                                        color = colorPalette.secondaryAccent.copy(alpha = 0.35f),
                                        shape = pillShape
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                BasicText(
                                    text = stringResource(R.string.format_subscribers, subscribers),
                                    style = typography.xxs.medium.copy(color = colorPalette.secondaryAccent),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (!isLandscape) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .size(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient Glow behind artist portrait
                    Box(
                        modifier = Modifier
                            .size(165.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        colorPalette.tertiaryAccent.copy(alpha = 0.45f),
                                        colorPalette.accent.copy(alpha = 0.22f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        colorPalette.accent,
                                        colorPalette.tertiaryAccent,
                                        colorPalette.secondaryAccent,
                                        colorPalette.accent
                                    )
                                ),
                                CircleShape
                            )
                    ) {
                        thumbnailContent()
                    }
                }
            }

            youtubeArtistPage?.let { artist ->
                artist.songs?.let { songs ->
                    ArtistOverviewSectionHeader(
                        title = stringResource(R.string.songs),
                        onViewAllClick = if (artist.songsEndpoint != null) onViewAllSongsClick else null,
                        pillColor = colorPalette.accent,
                        modifier = Modifier.padding(endPaddingValues)
                    )

                    songs.forEach { song ->
                        SongItem(
                            song = song,
                            thumbnailSize = Dimensions.thumbnails.song,
                            modifier = Modifier
                                .padding(endPaddingValues)
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .songSwipeActions(
                                    key = songs,
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
                                        val mediaItem = song.asMediaItem
                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(mediaItem)
                                        binder?.setupRadio(
                                            NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                        )
                                    }
                                )
                        )
                    }
                }

                artist.albums?.let { albums ->
                    ArtistOverviewSectionHeader(
                        title = stringResource(R.string.albums),
                        onViewAllClick = if (artist.albumsEndpoint != null) onViewAllAlbumsClick else null,
                        pillColor = colorPalette.secondaryAccent,
                        modifier = Modifier.padding(endPaddingValues)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(endPaddingValues)
                    ) {
                        items(
                            items = albums,
                            key = YoutubeMusicInnertube.AlbumItem::key
                        ) { album ->
                            AlbumItem(
                                album = album,
                                thumbnailSize = Dimensions.thumbnails.album,
                                alternative = true,
                                modifier = Modifier
                                    .padding(vertical = 3.dp)
                                    .swaaraGlassTrackSurface()
                                    .clickable {
                                        onAlbumClick(album.key)
                                    }
                            )
                        }
                    }
                }

                artist.singles?.let { singles ->
                    ArtistOverviewSectionHeader(
                        title = stringResource(R.string.singles),
                        onViewAllClick = if (artist.singlesEndpoint != null) onViewAllSinglesClick else null,
                        pillColor = colorPalette.secondaryAccent,
                        modifier = Modifier.padding(endPaddingValues)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(endPaddingValues)
                    ) {
                        items(
                            items = singles,
                            key = YoutubeMusicInnertube.AlbumItem::key
                        ) { album ->
                            AlbumItem(
                                album = album,
                                thumbnailSize = Dimensions.thumbnails.album,
                                alternative = true,
                                modifier = Modifier
                                    .padding(vertical = 3.dp)
                                    .swaaraGlassTrackSurface()
                                    .clickable(onClick = { onAlbumClick(album.key) })
                            )
                        }
                    }
                }

                artist.description?.let { description ->
                    Attribution(
                        text = description,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .padding(vertical = 16.dp, horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(84.dp))
            } ?: ArtistOverviewBodyPlaceholder()
        }

        youtubeArtistPage?.radioEndpoint?.let { endpoint ->
            FloatingActionsContainerWithScrollToTop(
                scrollState = scrollState,
                icon = R.drawable.radio,
                onClick = {
                    binder?.stopRadio()
                    binder?.playRadio(endpoint)
                }
            )
        }
    }
}

@Composable
private fun ArtistOverviewSectionHeader(
    title: String,
    onViewAllClick: (() -> Unit)?,
    pillColor: Color,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val pillShape = 12.dp.roundedShape

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 10.dp)
    ) {
        BasicText(
            text = title,
            style = typography.m.semiBold.copy(color = colorPalette.text)
        )

        onViewAllClick?.let { onClick ->
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .background(pillColor.copy(alpha = 0.14f))
                    .border(
                        width = 1.dp,
                        color = pillColor.copy(alpha = 0.35f),
                        shape = pillShape
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                BasicText(
                    text = stringResource(R.string.view_all),
                    style = typography.xxs.semiBold.copy(color = pillColor)
                )
            }
        }
    }
}

@Composable
fun ArtistOverviewBodyPlaceholder(modifier: Modifier = Modifier) = ShimmerHost(
    modifier = modifier
) {
    TextPlaceholder(modifier = sectionTextModifier)

    repeat(5) {
        SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
    }

    repeat(2) {
        TextPlaceholder(modifier = sectionTextModifier)

        Row {
            repeat(2) {
                AlbumItemPlaceholder(
                    thumbnailSize = Dimensions.thumbnails.album,
                    alternative = true
                )
            }
        }
    }
}
