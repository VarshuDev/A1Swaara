package com.varshudev.a1swaara.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.varshudev.a1swaara.Database
import com.varshudev.a1swaara.LocalAppContainer
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.models.Album
import com.varshudev.a1swaara.models.Artist
import com.varshudev.a1swaara.models.Playlist
import com.varshudev.a1swaara.models.PlaylistPreview
import com.varshudev.a1swaara.models.Song
import com.varshudev.a1swaara.ui.screens.albumRoute
import com.varshudev.a1swaara.ui.screens.artistRoute
import com.varshudev.a1swaara.ui.screens.localPlaylistRoute
import com.varshudev.a1swaara.ui.components.SwaaraBorderSubtle
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraDisplayItem
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraGlassContainer
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraModalSurface
import com.varshudev.a1swaara.ui.components.SwaaraPink
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraPurple
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.components.SwaaraTrendingShelf
import com.varshudev.a1swaara.ui.components.SwaaraViolet
import com.varshudev.a1swaara.ui.screens.LocalOpenNowPlaying
import com.varshudev.a1swaara.utils.asMediaItem
import com.varshudev.a1swaara.utils.forcePlay
import com.varshudev.a1swaara.utils.playTrack
import com.varshudev.a1swaara.utils.playTrackWithService
import com.varshudev.a1swaara.utils.thumbnail
import app.a1swaara.core.data.enums.ArtistSortBy
import app.a1swaara.core.data.enums.PlaylistSortBy
import app.a1swaara.core.data.enums.SortOrder
import app.a1swaara.providers.youtubemusic.innertube.models.NavigationEndpoint
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import com.varshudev.a1swaara.service.PlayerService
import com.varshudev.a1swaara.utils.intent
import com.varshudev.a1swaara.cloud.SwaaraCloudSync
import com.varshudev.a1swaara.cloud.SwaaraJamEngine
import com.varshudev.a1swaara.ui.screens.jamHubRoute
import com.varshudev.a1swaara.ui.screens.liveJamRoute
import com.varshudev.a1swaara.ui.components.SwaaraCloudSyncDialog
import com.varshudev.a1swaara.ui.components.SwaaraJamRoomDialog
import com.varshudev.a1swaara.ui.components.SwaaraSharePlaylistDialog
import com.varshudev.a1swaara.cloud.SwaaraSharedPlaylists
import com.varshudev.a1swaara.preferences.DataPreferences
import com.varshudev.a1swaara.downloader.SwaaraFastDownloader
import com.varshudev.a1swaara.utils.forcePlayFromBeginning
import com.varshudev.a1swaara.utils.forcePlayAtIndex
import com.varshudev.a1swaara.utils.hasPermission
import com.varshudev.a1swaara.utils.toast
import com.varshudev.a1swaara.ui.screens.home.musicFilesAsFlow
import app.a1swaara.core.ui.utils.isAtLeastAndroid13
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.collect

@Composable
fun SwaaraLibraryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val binder = LocalPlayerServiceBinder.current
    val openNowPlaying = LocalOpenNowPlaying.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = remember { listOf("Playlists", "Songs", "Downloads", "Device Files", "Albums", "Artists", "Heavy Rotation", "History") }

    val audioPermission = remember {
        if (isAtLeastAndroid13) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasAudioPermission by remember {
        mutableStateOf(context.hasPermission(audioPermission))
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasAudioPermission = it }
    )

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            context.musicFilesAsFlow(container.songsRepository).collect()
        }
    }

    val localSongs by Database.localSongsByRowIdDesc()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var searchQuery by remember { mutableStateOf("") }

    val filteredLocalSongs = remember(localSongs, searchQuery) {
        if (searchQuery.isBlank()) localSongs
        else localSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            (it.artistsText?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    val likedSongs by Database.favorites()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val allSongs by Database.songsByRowIdDesc()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val offlineSongs = remember(allSongs, binder, DataPreferences.downloadedSongIds) {
        runCatching {
            allSongs.filter { song ->
                runCatching {
                    DataPreferences.isSongExplicitlyDownloaded(song.id) && binder?.isCached(song.id) == true
                }.getOrDefault(false)
            }
        }.getOrDefault(emptyList())
    }
    var isOfflineOnlyMode by remember { mutableStateOf(false) }

    val savedAlbums by Database.albumsByRowIdDesc()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val heavyRotationSongs by Database.songsByPlayTimeDesc()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val historySongs by Database.history()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val userPlaylists by Database.playlistPreviews(PlaylistSortBy.DateAdded, SortOrder.Descending)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val artistsList by Database.artists(ArtistSortBy.DateAdded, SortOrder.Descending)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var recommendedSongs by remember { mutableStateOf<List<SwaaraDisplayItem>>(emptyList()) }

    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showJamDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    if (showCloudSyncDialog) {
        SwaaraCloudSyncDialog(onDismiss = { showCloudSyncDialog = false })
    }
    if (showJamDialog) {
        SwaaraJamRoomDialog(onDismiss = { showJamDialog = false })
    }
    if (showImportDialog) {
        SwaaraSharePlaylistDialog(onDismiss = { showImportDialog = false })
    }

    // Load trending library recommendations defensively
    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) {
                val result = runCatching {
                    container.searchResultRepository.searchSongs("top chartbusters", null)?.getOrNull()?.items
                }.getOrNull()
                if (!result.isNullOrEmpty()) {
                    val mapped = result.take(10).mapNotNull { item ->
                        runCatching {
                            SwaaraDisplayItem(
                                id = item.key,
                                title = item.info?.name ?: "Top Track",
                                subtitle = item.authors?.joinToString { it.name.orEmpty() } ?: "Artist",
                                thumbnailUrl = item.thumbnail?.url?.thumbnail(800) ?: "https://i.ytimg.com/vi/${item.key}/maxresdefault.jpg"
                            )
                        }.getOrNull()
                    }
                    withContext(Dispatchers.Main) {
                        recommendedSongs = mapped
                    }
                }
            }
        }
    }
    // Play helper with infinite auto radio
    val playSong: (Song) -> Unit = { song ->
        runCatching {
            playTrackWithService(
                context = context,
                binder = binder,
                videoId = song.id,
                title = song.title,
                artist = song.artistsText,
                thumbnailUrl = song.thumbnailUrl,
                durationText = song.durationText,
                onStarted = { openNowPlaying() }
            )
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        Dialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                SwaaraGlassContainer(
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = SwaaraModalSurface,
                    borderBrush = Brush.linearGradient(
                        listOf(
                            SwaaraMagenta.copy(alpha = 0.50f),
                            SwaaraViolet.copy(alpha = 0.30f),
                            SwaaraCyan.copy(alpha = 0.45f)
                        )
                    ),
                    borderWidth = 1.2.dp,
                    shadowElevation = 24.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "New Playlist",
                            color = SwaaraPrimaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = {
                                Text(
                                    "My Awesome Playlist",
                                    color = SwaaraSecondaryText.copy(alpha = 0.7f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SwaaraPrimaryText,
                                unfocusedTextColor = SwaaraPrimaryText,
                                focusedBorderColor = SwaaraViolet,
                                unfocusedBorderColor = SwaaraBorderSubtle,
                                focusedContainerColor = SwaaraElevatedSurface,
                                unfocusedContainerColor = SwaaraElevatedSurface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                Text("Cancel", color = SwaaraSecondaryText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPlaylistName.isNotBlank()) {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            Database.insert(Playlist(name = newPlaylistName.trim()))
                                        }
                                        newPlaylistName = ""
                                        showCreatePlaylistDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SwaaraViolet),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07070B))
            .verticalScroll(rememberScrollState())
            .padding(bottom = 130.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Your Library",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${allSongs.size} tracks • ${userPlaylists.size} playlists • ${savedAlbums.size} albums",
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 13.sp
                )
            }

            // Action Buttons: Jam Room + Cloud Sync + Create Playlist
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Jam Room Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B1B2C))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable {
                            if (SwaaraJamEngine.currentRoom.value != null) {
                                liveJamRoute.global()
                            } else {
                                jamHubRoute.global()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.radio),
                        contentDescription = "Live Jam",
                        tint = SwaaraPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Cloud Sync Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B1B2C))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable { showCloudSyncDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.sync),
                        contentDescription = "Cloud Sync",
                        tint = SwaaraCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Offline Only Toggle Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOfflineOnlyMode) SwaaraCyan.copy(alpha = 0.25f)
                            else Color(0xFF1B1B2C)
                        )
                        .border(
                            1.dp,
                            if (isOfflineOnlyMode) SwaaraCyan else Color.White.copy(alpha = 0.12f),
                            CircleShape
                        )
                        .clickable {
                            isOfflineOnlyMode = !isOfflineOnlyMode
                            context.toast(
                                if (isOfflineOnlyMode) "Offline Mode Enabled"
                                else "Offline Mode Disabled"
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.download),
                        contentDescription = "Offline Mode",
                        tint = if (isOfflineOnlyMode) SwaaraCyan else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Create Playlist Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(SwaaraPink, SwaaraPurple)))
                        .clickable { showCreatePlaylistDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.add),
                        contentDescription = "New Playlist",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Offline Mode Active Banner
        if (isOfflineOnlyMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SwaaraCyan.copy(alpha = 0.15f))
                    .border(1.dp, SwaaraCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.download),
                        contentDescription = null,
                        tint = SwaaraCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offline Mode Active: Showing only local tracks (${offlineSongs.size} tracks)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Tab Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) Brush.horizontalGradient(listOf(SwaaraPink, SwaaraPurple))
                            else Brush.horizontalGradient(listOf(Color(0xFF151524), Color(0xFF151524)))
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedTabIndex = index }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Liked Songs Gradient Card
        if (selectedTabIndex == 0 || selectedTabIndex == 1) {
            SwaaraGlassContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable {
                        likedSongs.firstOrNull()?.let { playSong(it) }
                    },
                shape = RoundedCornerShape(22.dp),
                backgroundColor = Color(0xFF1F1235).copy(alpha = 0.88f),
                borderColor = SwaaraPink.copy(alpha = 0.40f),
                borderWidth = 1.2.dp,
                shadowElevation = 14.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = SwaaraPink)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(SwaaraPink, SwaaraPurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.heart),
                            contentDescription = "Liked Songs",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Liked Songs",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${likedSongs.size} favorite tracks",
                            color = SwaaraCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Play Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(SwaaraPink, SwaaraPurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.play),
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> {
                // Playlists Tab: User Playlists + Liked + Heavy Rotation
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Playlists",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Import Shared Playlist Quick Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1B1B2C))
                                .border(1.dp, SwaaraCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .clickable { showImportDialog = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.download),
                                    contentDescription = null,
                                    tint = SwaaraCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Import Code",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SwaaraCyan
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (userPlaylists.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131322))
                                .clickable { showCreatePlaylistDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.add),
                                    contentDescription = null,
                                    tint = SwaaraPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Create your first playlist",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to organize your favorite tracks",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        userPlaylists.forEach { playlist: PlaylistPreview ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF131322))
                                    .clickable {
                                        localPlaylistRoute.global(playlist.id)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF880E4F)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.playlist),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${playlist.songCount} songs",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }

                                // Quick Share Playlist Button
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .clickable {
                                            coroutineScope.launch {
                                                val res = SwaaraSharedPlaylists.publishPlaylist(context, playlist.id)
                                                if (res.success && res.code != null) {
                                                    val sendIntent = android.content.Intent().apply {
                                                        action = android.content.Intent.ACTION_SEND
                                                        putExtra(
                                                            android.content.Intent.EXTRA_TEXT,
                                                            "Check out my playlist '${playlist.playlist.name}' on A1 Swaara! Use share code: ${res.code}"
                                                        )
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Playlist Code"))
                                                } else {
                                                    context.toast(res.message)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.share_social),
                                        contentDescription = "Share Playlist",
                                        tint = SwaaraCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

            }

            1 -> {
                // Songs Tab: All Songs or Offline Filtered Songs
                val songsToDisplay = if (isOfflineOnlyMode) offlineSongs else allSongs
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text(
                        text = if (isOfflineOnlyMode) "Offline Downloaded Tracks (${songsToDisplay.size})" else "All Saved Songs (${songsToDisplay.size})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (songsToDisplay.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isOfflineOnlyMode) "No tracks downloaded yet" else "No saved songs yet",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }

                    songsToDisplay.take(50).forEach { song ->
                        val isTrackDownloaded = DataPreferences.isSongExplicitlyDownloaded(song.id) && binder?.isCached(song.id) == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF131322))
                                .clickable { playSong(song) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl?.thumbnail(800) ?: "https://i.ytimg.com/vi/${song.id}/maxresdefault.jpg",
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artistsText ?: "Artist",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isTrackDownloaded) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(SwaaraCyan.copy(alpha = 0.20f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.download),
                                        contentDescription = "Downloaded",
                                        tint = SwaaraCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Offline Hub Tab: Dedicated Downloads & Smart Cache Manager
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    // 1. Offline Storage & Status Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF0F2027),
                                        Color(0xFF203A43),
                                        Color(0xFF2C5364)
                                    )
                                )
                            )
                            .border(1.2.dp, SwaaraCyan.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Smart Offline Hub",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "${offlineSongs.size} tracks cached • ~${offlineSongs.size * 8} MB stored",
                                        color = SwaaraCyan,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "320 KBPS HQ",
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons: Smart Download Liked + Play All Offline
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Smart Download Liked Songs Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                                        .clickable {
                                            val unDownloadedLiked = likedSongs.filter { song ->
                                                !DataPreferences.isSongExplicitlyDownloaded(song.id) || binder?.isCached(song.id) != true
                                            }
                                            if (unDownloadedLiked.isEmpty()) {
                                                context.toast("All liked tracks are already downloaded offline!")
                                            } else {
                                                context.toast("Starting background download of ${unDownloadedLiked.size} liked tracks...")
                                                unDownloadedLiked.forEach { song ->
                                                    SwaaraFastDownloader.download(context, song.asMediaItem)
                                                }
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.download),
                                            contentDescription = null,
                                            tint = SwaaraCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Auto-Save Liked",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Play All Offline Mix
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.horizontalGradient(listOf(SwaaraCyan, SwaaraPurple)))
                                        .clickable {
                                            val active = binder ?: com.varshudev.a1swaara.service.PlayerService.activeBinder.value
                                            if (offlineSongs.isNotEmpty() && active != null) {
                                                active.player.forcePlayFromBeginning(offlineSongs.map { it.asMediaItem })
                                                openNowPlaying()
                                                context.toast("Playing offline mix")
                                            } else {
                                                context.toast("No offline tracks to play")
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.play),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Play Offline Mix",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Downloaded Music (${offlineSongs.size})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (offlineSongs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = null,
                                    tint = SwaaraCyan,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No offline downloads yet",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap the download icon on any song to save offline in HQ 320 kbps.",
                                    color = Color.White.copy(alpha = 0.50f),
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        offlineSongs.forEach { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF131322))
                                    .clickable { playSong(song) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.DarkGray)
                                ) {
                                    AsyncImage(
                                        model = song.thumbnailUrl?.thumbnail(800) ?: "https://i.ytimg.com/vi/${song.id}/maxresdefault.jpg",
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artistsText ?: "Artist",
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SwaaraCyan.copy(alpha = 0.18f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "320 KBPS",
                                        color = SwaaraCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Device Files Tab (On-Device Local Storage Audio)
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    if (!hasAudioPermission) {
                        // Permission Prompt Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF2C194D),
                                            Color(0xFF1B1B2F),
                                            Color(0xFF0F172A)
                                        )
                                    )
                                )
                                .border(1.2.dp, SwaaraPurple.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                                .padding(22.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(SwaaraPink, SwaaraPurple))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.disc),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Scan On-Device Music",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Allow A1 Swaara to index and play your offline MP3, FLAC, M4A, and WAV music files stored directly on your device.",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Brush.horizontalGradient(listOf(SwaaraPink, SwaaraPurple)))
                                            .clickable { audioPermissionLauncher.launch(audioPermission) }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Grant Permission",
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .clickable {
                                                context.startActivity(
                                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                        data = Uri.fromParts("package", context.packageName, null)
                                                    }
                                                )
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Settings",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 13.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // 1. Device Audio Header & Quick Actions Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF1B1B3A),
                                            Color(0xFF261447),
                                            Color(0xFF121224)
                                        )
                                    )
                                )
                                .border(1.2.dp, SwaaraPink.copy(alpha = 0.40f), RoundedCornerShape(22.dp))
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "On-Device Storage",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "${filteredLocalSongs.size} Tracks Indexed • Pure Offline",
                                            color = SwaaraCyan,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "FLAC / MP3",
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Play All Local Songs Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Brush.horizontalGradient(listOf(SwaaraPink, SwaaraPurple)))
                                            .clickable {
                                                if (filteredLocalSongs.isNotEmpty()) {
                                                    binder?.player?.forcePlayFromBeginning(filteredLocalSongs.map { it.asMediaItem })
                                                    openNowPlaying()
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(R.drawable.play),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Play All",
                                                color = Color.White,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Shuffle All Local Songs Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                            .clickable {
                                                if (filteredLocalSongs.isNotEmpty()) {
                                                    binder?.player?.forcePlayFromBeginning(filteredLocalSongs.shuffled().map { it.asMediaItem })
                                                    openNowPlaying()
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(R.drawable.shuffle),
                                                contentDescription = null,
                                                tint = SwaaraCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Shuffle",
                                                color = Color.White,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Local Tracks (${filteredLocalSongs.size})",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (filteredLocalSongs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(R.drawable.musical_notes),
                                        contentDescription = null,
                                        tint = SwaaraPurple,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No on-device music files found",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add MP3, FLAC, or audio files to your Music or Downloads folder.",
                                        color = Color.White.copy(alpha = 0.50f),
                                        fontSize = 12.5.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            filteredLocalSongs.forEachIndexed { index, song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF131322))
                                        .clickable {
                                            binder?.player?.forcePlayAtIndex(filteredLocalSongs.map { it.asMediaItem }, index)
                                            openNowPlaying()
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Brush.linearGradient(listOf(Color(0xFF2C194D), Color(0xFF1B1B2F)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!song.thumbnailUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = song.thumbnailUrl,
                                                contentDescription = song.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.matchParentSize()
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.musical_notes),
                                                contentDescription = null,
                                                tint = SwaaraCyan,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${song.artistsText ?: "Device Audio"} • ${song.durationText ?: ""}",
                                            color = Color.White.copy(alpha = 0.55f),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SwaaraPurple.copy(alpha = 0.22f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "LOCAL",
                                            color = SwaaraPurple,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            4 -> {
                // Albums Tab: Saved / Bookmarked Albums
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text(
                        text = "Saved Albums (${savedAlbums.size})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (savedAlbums.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(R.drawable.disc),
                                    contentDescription = null,
                                    tint = SwaaraPurple,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Saved Albums Yet",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap the bookmark icon on any album to save it here",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        savedAlbums.forEach { album: Album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF131322))
                                    .clickable {
                                        albumRoute.global(album.id)
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
                                        model = album.thumbnailUrl?.thumbnail(800),
                                        contentDescription = album.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = album.title ?: "Album",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = listOfNotNull(album.authorsText, album.year).joinToString(" • ").ifBlank { "Album" },
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.chevron_forward),
                                    contentDescription = "View Album",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            5 -> {
                // Artists Tab
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text(
                        text = "Followed Artists (${artistsList.size})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (artistsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(R.drawable.person),
                                    contentDescription = null,
                                    tint = SwaaraPurple,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Followed Artists Yet",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap the bookmark icon on any artist profile to follow them",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        artistsList.forEach { artist: Artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF131322))
                                    .clickable {
                                        artistRoute.global(artist.id)
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(SwaaraPink, SwaaraPurple))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = artist.name?.take(1) ?: "A",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    AsyncImage(
                                        model = artist.thumbnailUrl?.thumbnail(800),
                                        contentDescription = artist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artist.name ?: "Artist",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Artist Profile",
                                        color = SwaaraCyan,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.chevron_forward),
                                    contentDescription = "View Artist",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            6 -> {
                // Heavy Rotation (Most Played Songs)
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text(
                        text = "Heavy Rotation (Most Played)",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    heavyRotationSongs.take(25).forEachIndexed { idx, song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF131322))
                                .clickable { playSong(song) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                color = if (idx < 3) SwaaraPink else Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(28.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl?.thumbnail(800) ?: "https://i.ytimg.com/vi/${song.id}/maxresdefault.jpg",
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artistsText ?: "Artist",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            7 -> {
                // History Tab
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text(
                        text = "Recently Played",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    historySongs.take(25).forEach { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF131322))
                                .clickable { playSong(song) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl?.thumbnail(800) ?: "https://i.ytimg.com/vi/${song.id}/maxresdefault.jpg",
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artistsText ?: "Artist",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Recommendations Section (Always fills Library with fresh music)
        if (recommendedSongs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SwaaraTrendingShelf(
                title = "Recommended For Your Library",
                subtitle = "Discover and save tracks to your collection",
                items = recommendedSongs,
                onItemClick = { item ->
                    playTrackWithService(
                        context = context,
                        binder = binder,
                        videoId = item.id,
                        title = item.title,
                        artist = item.subtitle,
                        thumbnailUrl = item.thumbnailUrl,
                        onStarted = { openNowPlaying() }
                    )
                }
            )
        }
    }
}
