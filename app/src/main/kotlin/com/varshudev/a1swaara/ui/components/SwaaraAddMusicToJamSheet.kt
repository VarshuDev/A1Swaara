package com.varshudev.a1swaara.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.varshudev.a1swaara.Database
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.cloud.SwaaraJamEngine
import com.varshudev.a1swaara.models.Song
import com.varshudev.a1swaara.repositories.YoutubeMusicInnertubeSearchResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class JamSearchItem(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationText: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwaaraAddMusicToJamSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = YouTube Search, 1 = Favorites, 2 = Now Playing

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<JamSearchItem>>(emptyList()) }

    // Favorites state
    val favorites by Database.favorites().collectAsState(initial = emptyList())

    // Currently playing state
    val currentMedia = player?.currentMediaItem

    // Debounced Search Effect
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length < 2) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        delay(400) // 400ms debounce
        isSearching = true
        withContext(Dispatchers.IO) {
            try {
                val res = YoutubeMusicInnertubeSearchResultRepository.searchSongs(q, null)?.getOrNull()
                val items = res?.items?.map { song ->
                    JamSearchItem(
                        id = song.key,
                        title = song.info?.name ?: "Unknown Song",
                        artist = song.authors?.mapNotNull { it.name }?.joinToString() ?: "Artist",
                        artworkUrl = song.thumbnail?.url,
                        durationText = song.durationText
                    )
                } ?: emptyList()
                withContext(Dispatchers.Main) {
                    searchResults = items
                    isSearching = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSearching = false
                }
            }
        }
    }

    fun addTrackToJam(id: String, title: String, artist: String, art: String?) {
        coroutineScope.launch {
            val success = SwaaraJamEngine.addSongToQueue(
                context = context,
                songId = id,
                title = title,
                artist = artist,
                artworkUrl = art
            )
            if (success) {
                Toast.makeText(context, "Added \"$title\" to Jam Queue", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to add song to queue", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SwaaraColors.Obsidian,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.20f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ADD MUSIC TO JAM",
                        color = SwaaraColors.PrimaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Collaborative queue with live listener upvoting",
                        color = SwaaraColors.SecondaryText,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SwaaraColors.ElevatedSurface)
                        .border(1.dp, SwaaraColors.CardBorder, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "Close",
                        tint = SwaaraColors.SecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SwaaraColors.ElevatedSurface)
                    .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("YouTube Search", "My Favorites", "Now Playing")
                tabs.forEachIndexed { idx, label ->
                    val isSelected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(Brush.horizontalGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta)))
                                } else {
                                    Modifier.background(Color.Transparent)
                                }
                            )
                            .clickable { selectedTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else SwaaraColors.SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                // Tab 0: YouTube Music Live Search
                0 -> {
                    // Search Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SwaaraColors.ElevatedSurface)
                            .border(1.dp, SwaaraColors.Cyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = null,
                                tint = SwaaraColors.Cyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search any song, artist, album...",
                                        color = SwaaraColors.TertiaryText,
                                        fontSize = 13.sp
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = TextStyle(color = SwaaraColors.PrimaryText, fontSize = 13.sp),
                                    cursorBrush = SolidColor(SwaaraColors.Cyan),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    painter = painterResource(id = R.drawable.close),
                                    contentDescription = "Clear",
                                    tint = SwaaraColors.SecondaryText,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SwaaraColors.Cyan, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "Type to discover millions of tracks to queue." else "No songs found for \"$searchQuery\"",
                                color = SwaaraColors.TertiaryText,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults, key = { it.id }) { item ->
                                JamTrackRow(
                                    title = item.title,
                                    artist = item.artist,
                                    artworkUrl = item.artworkUrl,
                                    durationText = item.durationText,
                                    onAdd = { addTrackToJam(item.id, item.title, item.artist, item.artworkUrl) }
                                )
                            }
                        }
                    }
                }

                // Tab 1: Personal Favorites
                1 -> {
                    if (favorites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No liked tracks found in your library.",
                                color = SwaaraColors.TertiaryText,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(favorites, key = { it.id }) { song ->
                                JamTrackRow(
                                    title = song.title,
                                    artist = song.artistsText ?: "Artist",
                                    artworkUrl = song.thumbnailUrl,
                                    durationText = song.durationText,
                                    onAdd = { addTrackToJam(song.id, song.title, song.artistsText ?: "Artist", song.thumbnailUrl) }
                                )
                            }
                        }
                    }
                }

                // Tab 2: Currently Playing Track
                2 -> {
                    if (currentMedia == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No music is currently playing on this device.",
                                color = SwaaraColors.TertiaryText,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        val cTitle = currentMedia.mediaMetadata.title?.toString() ?: "Track"
                        val cArtist = currentMedia.mediaMetadata.artist?.toString() ?: "Artist"
                        val cArt = currentMedia.mediaMetadata.artworkUri?.toString()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            JamTrackRow(
                                title = cTitle,
                                artist = cArtist,
                                artworkUrl = cArt,
                                durationText = "Playing Now",
                                onAdd = { addTrackToJam(currentMedia.mediaId, cTitle, cArtist, cArt) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JamTrackRow(
    title: String,
    artist: String,
    artworkUrl: String?,
    durationText: String?,
    onAdd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SwaaraColors.ElevatedSurface)
            .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SwaaraColors.DeepSurface),
                contentAlignment = Alignment.Center
            ) {
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.musical_notes),
                        contentDescription = null,
                        tint = SwaaraColors.SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SwaaraColors.PrimaryText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (durationText != null) "$artist • $durationText" else artist,
                    color = SwaaraColors.SecondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Add Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta)))
                    .clickable { onAdd() }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Queue",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
