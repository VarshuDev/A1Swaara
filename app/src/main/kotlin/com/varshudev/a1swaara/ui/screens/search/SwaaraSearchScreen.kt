package com.varshudev.a1swaara.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.varshudev.a1swaara.LocalAppContainer
import com.varshudev.a1swaara.LocalPlayerAwareWindowInsets
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.models.SearchQuery
import com.varshudev.a1swaara.models.toUiMood
import com.varshudev.a1swaara.ui.components.ShimmerHost
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraErrorRed
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraObsidian
import com.varshudev.a1swaara.ui.components.SwaaraPink
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraPurple
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.screens.LocalOpenNowPlaying
import com.varshudev.a1swaara.ui.screens.moodRoute
import com.varshudev.a1swaara.utils.asMediaItem
import com.varshudev.a1swaara.utils.forcePlay
import com.varshudev.a1swaara.utils.playTrack
import com.varshudev.a1swaara.utils.playTrackWithService
import com.varshudev.a1swaara.utils.thumbnail
import app.a1swaara.providers.youtubemusic.innertube.YoutubeMusicInnertube
import app.a1swaara.providers.youtubemusic.innertube.models.NavigationEndpoint
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope as parallelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.varshudev.a1swaara.ui.screens.albumRoute
import com.varshudev.a1swaara.ui.screens.artistRoute
import com.varshudev.a1swaara.ui.screens.playlistRoute
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwaaraSearchScreen(
    initialTextInput: String = "",
    onSearch: ((String) -> Unit)? = null,
    onViewPlaylist: ((String) -> Unit)? = null,
    onSearchSubmit: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val binder = LocalPlayerServiceBinder.current
    val openNowPlaying = LocalOpenNowPlaying.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember(initialTextInput) { mutableStateOf(initialTextInput) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filters = remember { listOf("All", "Songs", "Albums", "Artists", "Playlists", "Podcasts") }

    val recentQueries by container.onlineSearchRepository
        .observeHistory("")
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var searchResults by remember { mutableStateOf<List<YoutubeMusicInnertube.SongItem>>(emptyList()) }
    var artistResults by remember { mutableStateOf<List<YoutubeMusicInnertube.ArtistItem>>(emptyList()) }
    var albumResults by remember { mutableStateOf<List<YoutubeMusicInnertube.AlbumItem>>(emptyList()) }
    var playlistResults by remember { mutableStateOf<List<YoutubeMusicInnertube.PlaylistItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf(false) }

    var discoverMoods by remember { mutableStateOf<List<YoutubeMusicInnertube.Mood.Item>>(emptyList()) }
    var isLoadingMoods by remember { mutableStateOf(true) }
    var moodsError by remember { mutableStateOf(false) }
    var moodsReloadTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(moodsReloadTick) {
        isLoadingMoods = true
        moodsError = false
        val result = withContext(Dispatchers.IO) {
            runCatching { container.homeRepository.fetchDiscoverPage() }.getOrNull()
        }
        val fetched = result?.getOrNull()?.moods.orEmpty()
        discoverMoods = fetched
        moodsError = fetched.isEmpty() && (result == null || result.isFailure)
        isLoadingMoods = false
    }

    suspend fun executeSearch(rawQuery: String, filterIndex: Int) {
        val q = rawQuery.trim()
        if (q.isEmpty()) return

        var failedAll = false
        var songsOut = emptyList<YoutubeMusicInnertube.SongItem>()
        var artistsOut = emptyList<YoutubeMusicInnertube.ArtistItem>()
        var albumsOut = emptyList<YoutubeMusicInnertube.AlbumItem>()
        var playlistsOut = emptyList<YoutubeMusicInnertube.PlaylistItem>()

        withContext(Dispatchers.IO) {
            when (filterIndex) {
                2 -> {
                    val res = runCatching { container.searchResultRepository.searchAlbums(q, null) }.getOrNull()
                    failedAll = res == null || res.isFailure
                    albumsOut = res?.getOrNull()?.items ?: emptyList()
                }
                3 -> {
                    val res = runCatching { container.searchResultRepository.searchArtists(q, null) }.getOrNull()
                    failedAll = res == null || res.isFailure
                    artistsOut = res?.getOrNull()?.items ?: emptyList()
                }
                4 -> {
                    val res = runCatching { container.searchResultRepository.searchPlaylists(q, null) }.getOrNull()
                    failedAll = res == null || res.isFailure
                    playlistsOut = res?.getOrNull()?.items ?: emptyList()
                }
                5 -> {
                    val qShows = if (q.contains("podcast", ignoreCase = true)) q else "$q podcast"
                    val qEpisodes = if (q.contains("podcast", ignoreCase = true)) q else "$q podcast episode"
                    val (showsRes, episodesRes) = parallelScope {
                        val s = async { runCatching { container.searchResultRepository.searchPlaylists(qShows, null) }.getOrNull() }
                        val e = async { runCatching { container.searchResultRepository.searchSongs(qEpisodes, null) }.getOrNull() }
                        s.await() to e.await()
                    }
                    failedAll = (showsRes == null || showsRes.isFailure) && (episodesRes == null || episodesRes.isFailure)
                    playlistsOut = showsRes?.getOrNull()?.items ?: emptyList()
                    songsOut = episodesRes?.getOrNull()?.items ?: emptyList()
                }
                else -> {
                    val (songsRes, artistsRes, albumsRes) = parallelScope {
                        val s = async { runCatching { container.searchResultRepository.searchSongs(q, null) }.getOrNull() }
                        val ar = if (filterIndex == 0) {
                            async { runCatching { container.searchResultRepository.searchArtists(q, null) }.getOrNull() }
                        } else null
                        val al = if (filterIndex == 0) {
                            async { runCatching { container.searchResultRepository.searchAlbums(q, null) }.getOrNull() }
                        } else null
                        Triple(s.await(), ar?.await(), al?.await())
                    }
                    failedAll = (songsRes == null || songsRes.isFailure) &&
                        (artistsRes == null || artistsRes.isFailure) &&
                        (albumsRes == null || albumsRes.isFailure)
                    songsOut = songsRes?.getOrNull()?.items ?: emptyList()
                    artistsOut = artistsRes?.getOrNull()?.items?.take(1) ?: emptyList()
                    albumsOut = albumsRes?.getOrNull()?.items?.take(2) ?: emptyList()
                }
            }
        }

        searchResults = songsOut
        artistResults = artistsOut
        albumResults = albumsOut
        playlistResults = playlistsOut
        searchError = failedAll
        isLoading = false
    }

    // Instant Real-Time Auto-Search as the user types
    LaunchedEffect(searchQuery, selectedFilterIndex) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            artistResults = emptyList()
            albumResults = emptyList()
            playlistResults = emptyList()
            searchError = false
            isLoading = false
            return@LaunchedEffect
        }
        delay(220) // Fast debounce
        isLoading = true
        searchError = false
        executeSearch(searchQuery, selectedFilterIndex)
    }

    val performExplicitSearch: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            focusManager.clearFocus()
            isLoading = true
            searchError = false
            coroutineScope.launch {
                val q = query.trim()
                withContext(Dispatchers.IO) {
                    container.onlineSearchRepository.saveHistory(q)
                }
                executeSearch(q, selectedFilterIndex)
            }
        }
    }

    val bottomClearance = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SwaaraObsidian)
            .padding(bottom = bottomClearance)
    ) {
        // Search Header Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search songs, artists, albums...", color = Color.White.copy(alpha = 0.40f), fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "Search",
                        tint = SwaaraPurple,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            searchResults = emptyList()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performExplicitSearch(searchQuery) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SwaaraPurple,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = Color(0xFF141424),
                    unfocusedContainerColor = Color(0xFF12121E)
                ),
                shape = RoundedCornerShape(22.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEachIndexed { index, filter ->
                    val isSelected = selectedFilterIndex == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) Brush.horizontalGradient(listOf(SwaaraPink, SwaaraPurple))
                                else Brush.horizontalGradient(listOf(Color(0xFF161626), Color(0xFF161626)))
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedFilterIndex = index
                                if (searchQuery.isNotBlank()) performExplicitSearch(searchQuery)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Search Content / Results / History
        if (isLoading) {
            ShimmerHost(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(7) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SwaaraDeepSurface)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.42f)
                                    .height(11.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.07f))
                            )
                        }
                    }
                }
            }
        } else if (artistResults.isNotEmpty() || albumResults.isNotEmpty() || playlistResults.isNotEmpty() || searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                if (artistResults.isNotEmpty()) {
                    item {
                        Text(
                            text = if (selectedFilterIndex == 3) "Artists (${artistResults.size})" else "Top Artist",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(artistResults) { artist ->
                        val artistName = artist.info?.name ?: "Artist"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131322))
                                .clickable {
                                    focusManager.clearFocus()
                                    container.onlineSearchRepository.saveHistory(artistName)
                                    artistRoute.global(artist.key)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(SwaaraPink, SwaaraPurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = artistName.take(1),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                AsyncImage(
                                    model = artist.thumbnail?.url?.thumbnail(800),
                                    contentDescription = artistName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = artistName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Artist Profile • Tap to view discography",
                                    color = SwaaraCyan,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                painter = painterResource(R.drawable.chevron_forward),
                                contentDescription = "View Artist",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (albumResults.isNotEmpty()) {
                    item {
                        Text(
                            text = if (selectedFilterIndex == 2) "Albums (${albumResults.size})" else "Top Albums",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp, top = if (artistResults.isNotEmpty()) 8.dp else 0.dp)
                        )
                    }

                    items(albumResults) { album ->
                        val albumTitle = album.info?.name ?: "Album"
                        val author = album.authors?.joinToString { it.name.orEmpty() }?.replace(Regex("[,&\\s]{2,}"), ", ")?.trim(',', ' ') ?: "Artist"
                        val year = album.year?.let { " • $it" } ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131322))
                                .clickable {
                                    focusManager.clearFocus()
                                    container.onlineSearchRepository.saveHistory(albumTitle)
                                    albumRoute.global(album.key)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = album.thumbnail?.url?.thumbnail(800),
                                    contentDescription = albumTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = albumTitle,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$author$year • Album",
                                    color = SwaaraCyan,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.chevron_forward),
                                contentDescription = "View Album",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (playlistResults.isNotEmpty()) {
                    item {
                        Text(
                            text = if (selectedFilterIndex == 5) "Podcast Shows (${playlistResults.size})" else if (selectedFilterIndex == 4) "Playlists (${playlistResults.size})" else "Top Playlists",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp, top = if (artistResults.isNotEmpty() || albumResults.isNotEmpty()) 8.dp else 0.dp)
                        )
                    }

                    items(playlistResults) { playlist ->
                        val playlistTitle = playlist.info?.name ?: "Playlist"
                        val author = playlist.channel?.name ?: "Curated Playlist"
                        val songsCount = playlist.songCount?.let { " • $it tracks" } ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131322))
                                .clickable {
                                    focusManager.clearFocus()
                                    container.onlineSearchRepository.saveHistory(playlistTitle)
                                    playlistRoute.global(playlist.key, null, null, false)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = playlist.thumbnail?.url?.thumbnail(800),
                                    contentDescription = playlistTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlistTitle,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (selectedFilterIndex == 5) "$author$songsCount • Podcast Show" else "$author$songsCount • Playlist",
                                    color = SwaaraPink,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.chevron_forward),
                                contentDescription = "View Playlist",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (searchResults.isNotEmpty() && (selectedFilterIndex == 0 || selectedFilterIndex == 1 || selectedFilterIndex == 5)) {
                    if ((selectedFilterIndex == 0 || selectedFilterIndex == 5) && (artistResults.isNotEmpty() || albumResults.isNotEmpty() || playlistResults.isNotEmpty())) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedFilterIndex == 5) "Podcast Episodes (${searchResults.size})" else "Songs",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                items(searchResults) { song ->
                    val videoId = song.key
                    val songTitle = song.info?.name ?: "Unknown Title"
                    val artistNames = song.authors?.joinToString { it.name.orEmpty() }?.replace(Regex("[,&\\s]{2,}"), ", ")?.trim(',', ' ') ?: "Artist"
                    val highResThumbnail = song.thumbnail?.url?.thumbnail(800) ?: "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF131322))
                            .clickable {
                                focusManager.clearFocus()
                                coroutineScope.launch {
                                    container.onlineSearchRepository.saveHistory(songTitle)
                                }
                                playTrackWithService(
                                    context = context,
                                    binder = binder,
                                    videoId = videoId,
                                    title = songTitle,
                                    artist = artistNames,
                                    thumbnailUrl = highResThumbnail,
                                    onStarted = { openNowPlaying() }
                                )
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.DarkGray)
                        ) {
                            AsyncImage(
                                model = highResThumbnail,
                                contentDescription = songTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = songTitle,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = artistNames,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(SwaaraPink, SwaaraPurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.play),
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        } else if (searchQuery.isEmpty()) {
            // Recent Searches & Live Browse Moods & Genres Grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (recentQueries.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            color = SwaaraPrimaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Clear all",
                            color = SwaaraSecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { container.onlineSearchRepository.clearHistory() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        recentQueries.take(8).forEach { item: SearchQuery ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SwaaraElevatedSurface)
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        searchQuery = item.query
                                        performExplicitSearch(item.query)
                                    }
                                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.query,
                                    color = SwaaraPrimaryText.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            container.onlineSearchRepository.deleteHistory(item)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.close),
                                        contentDescription = "Remove recent search",
                                        tint = SwaaraSecondaryText,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                }

                Text(
                    text = "Browse All Moods & Genres",
                    color = SwaaraPrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                when {
                    isLoadingMoods && discoverMoods.isEmpty() -> {
                        ShimmerHost(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            repeat(4) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    repeat(2) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(58.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(SwaaraElevatedSurface)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    discoverMoods.isNotEmpty() -> {
                        val rows = discoverMoods.chunked(2)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            rows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { moodItem ->
                                        val stripe = Color(moodItem.stripeColor)
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(58.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(SwaaraElevatedSurface)
                                                .border(1.dp, stripe.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                                .clickable {
                                                    focusManager.clearFocus()
                                                    moodRoute.global(moodItem.toUiMood())
                                                }
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(28.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(stripe)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = moodItem.title,
                                                color = SwaaraPrimaryText,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SwaaraDeepSurface)
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (moodsError) "Couldn't load Browse genres" else "No genres available right now",
                                color = SwaaraPrimaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap retry to refresh live YouTube Music moods",
                                color = SwaaraSecondaryText,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.horizontalGradient(listOf(SwaaraPurple, SwaaraPink)))
                                    .clickable { moodsReloadTick++ }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Retry",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (searchError) {
                        Icon(
                            painter = painterResource(id = R.drawable.alert_circle),
                            contentDescription = null,
                            tint = SwaaraErrorRed,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Connection error while searching",
                            color = SwaaraPrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Check your internet connection and retry.",
                            color = SwaaraSecondaryText,
                            fontSize = 12.5.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.horizontalGradient(listOf(SwaaraPurple, SwaaraMagenta)))
                                .clickable { performExplicitSearch(searchQuery) }
                                .padding(horizontal = 22.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "Retry Search",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "No results found for '$searchQuery'",
                            color = SwaaraSecondaryText,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
