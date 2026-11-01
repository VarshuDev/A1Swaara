package com.varshudev.a1swaara.ui.screens.jam

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.cloud.JamQueueItem
import com.varshudev.a1swaara.cloud.JamRoomState
import com.varshudev.a1swaara.cloud.JamVibe
import com.varshudev.a1swaara.cloud.SwaaraJamEngine
import com.varshudev.a1swaara.ui.components.SwaaraAddMusicToJamSheet
import com.varshudev.a1swaara.ui.components.SwaaraColors
import com.varshudev.a1swaara.ui.components.SwaaraDedicationBanner
import com.varshudev.a1swaara.ui.components.SwaaraDedicationDialog
import com.varshudev.a1swaara.utils.forceSeekToNext
import com.varshudev.a1swaara.utils.forceSeekToPrevious
import com.varshudev.a1swaara.utils.playTrack
import com.varshudev.a1swaara.utils.shouldBePlaying
import kotlinx.coroutines.launch

@Composable
fun SwaaraLiveJamScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player

    val currentRoom by SwaaraJamEngine.currentRoom.collectAsState()
    val dedications by SwaaraJamEngine.activeDedications.collectAsState()
    val queue by SwaaraJamEngine.activeQueue.collectAsState()
    val vibes by SwaaraJamEngine.activeVibes.collectAsState()

    var showDedicationDialog by remember { mutableStateOf(false) }
    var showAddMusicSheet by remember { mutableStateOf(false) }

    val activeDedication = dedications.firstOrNull()

    // Host Auto-Advance: When track ends, advance to highest-voted queued track
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val room = SwaaraJamEngine.currentRoom.value
                    if (room != null && room.isHost) {
                        SwaaraJamEngine.advanceToNextQueueItem(context)
                    }
                }
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
        }
    }

    // Listener Playback Synchronization (lockstep)
    LaunchedEffect(currentRoom?.currentSongId, currentRoom?.isPlaying) {
        val room = currentRoom
        if (room != null && !room.isHost && room.currentSongId.isNotBlank()) {
            val curId = player?.currentMediaItem?.mediaId
            if (curId != room.currentSongId) {
                binder?.playTrack(
                    videoId = room.currentSongId,
                    title = room.currentTitle,
                    artist = room.currentArtist,
                    thumbnailUrl = room.currentArtworkUrl
                )
            }
            val elapsed = if (room.isPlaying) (System.currentTimeMillis() - room.playbackUpdatedAt).coerceAtLeast(0) else 0L
            val targetPos = room.playbackPositionMs + elapsed
            if (player != null && kotlin.math.abs(player.currentPosition - targetPos) > 3000) {
                player.seekTo(targetPos)
            }
            if (player != null) {
                if (room.isPlaying && !player.shouldBePlaying) {
                    player.play()
                } else if (!room.isPlaying && player.shouldBePlaying) {
                    player.pause()
                }
            }
        }
    }

    // Dedication Dialog
    if (showDedicationDialog && currentRoom != null) {
        val sId = currentRoom?.currentSongId ?: ""
        val sTitle = currentRoom?.currentTitle?.ifBlank { "Current Track" } ?: "Current Track"
        SwaaraDedicationDialog(
            songId = sId,
            songTitle = sTitle,
            onDismiss = { showDedicationDialog = false }
        )
    }

    // Add Music to Jam Sheet
    if (showAddMusicSheet && currentRoom != null) {
        SwaaraAddMusicToJamSheet(
            onDismiss = { showAddMusicSheet = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SwaaraColors.Obsidian)
            .statusBarsPadding()
    ) {
        val room = currentRoom
        if (room == null) {
            // Clean empty state / Timeout notification
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SwaaraColors.ElevatedSurface)
                        .border(1.dp, SwaaraColors.CardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.musical_notes),
                        contentDescription = null,
                        tint = SwaaraColors.Magenta,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Active Jam Session",
                    color = SwaaraColors.PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This session was closed or expired after 10 minutes of inactivity.",
                    color = SwaaraColors.SecondaryText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta)))
                        .clickable { onDismiss() }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(text = "Back to Jam Hub", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Active Jam Room Studio Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // 1. Top Header Area
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dismiss/Back
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SwaaraColors.ElevatedSurface)
                                .border(1.dp, SwaaraColors.CardBorder, CircleShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.chevron_down),
                                contentDescription = "Dismiss",
                                tint = SwaaraColors.PrimaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Room Code & Live Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.horizontalGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta)))
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "LIVE JAM",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SwaaraColors.ElevatedSurface)
                                    .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Jam Room Code", room.roomCode))
                                        Toast.makeText(context, "Room Code ${room.roomCode} copied!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = room.roomCode,
                                    color = SwaaraColors.Cyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            // Role Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (room.isHost) Color(0xFFFFB300).copy(alpha = 0.2f) else SwaaraColors.Cyan.copy(alpha = 0.15f))
                                    .border(
                                        1.dp,
                                        if (room.isHost) Color(0xFFFFB300) else SwaaraColors.Cyan,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (room.isHost) "[HOST]" else "[LISTENER]",
                                    color = if (room.isHost) Color(0xFFFFB300) else SwaaraColors.Cyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Share and Leave
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SwaaraColors.ElevatedSurface)
                                    .border(1.dp, SwaaraColors.CardBorder, CircleShape)
                                    .clickable {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Join my A1 Swaara Live Jam")
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Join my live music jam session on A1 Swaara! Room Code: ${room.roomCode}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Invite friends to Jam"))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.share_social),
                                    contentDescription = "Invite",
                                    tint = SwaaraColors.Cyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Red.copy(alpha = 0.15f))
                                    .border(1.dp, Color.Red.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            SwaaraJamEngine.leaveRoom(context)
                                            onDismiss()
                                        }
                                    }
                                    .padding(horizontal = 11.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (room.isHost) "End" else "Leave",
                                    color = Color(0xFFFF5555),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 2. Room Presence Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = room.roomName,
                            color = SwaaraColors.PrimaryText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Text(
                                text = "Hosted by ${room.hostName} • ${room.activeListenersCount} in sync",
                                color = SwaaraColors.SecondaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 3. Central Turntable Experience
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SwaaraVinylTurntable(
                            artworkUrl = room.currentArtworkUrl,
                            isPlaying = room.isPlaying
                        )
                    }
                }

                // 4. High-Performance Canvas Audio Spectrum Bars
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SwaaraJamSpectrumCanvas(
                        isPlaying = room.isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .padding(horizontal = 36.dp)
                    )
                }

                // 5. Dedication Marquee Banner
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    SwaaraDedicationBanner(dedication = activeDedication)
                }

                // 6. Track Metadata & Playback Controls
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = room.currentTitle.ifBlank { "Waiting for host to play..." },
                            color = SwaaraColors.PrimaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = room.currentArtist.ifBlank { "Swaara Live Session" },
                            color = SwaaraColors.SecondaryText,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (room.isHost) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(SwaaraColors.ElevatedSurface)
                                        .clickable { player?.forceSeekToPrevious() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.play_skip_back),
                                        contentDescription = "Previous",
                                        tint = SwaaraColors.PrimaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(24.dp))

                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta)))
                                        .shadow(16.dp, CircleShape, spotColor = SwaaraColors.Magenta)
                                        .clickable {
                                            if (player?.isPlaying == true) player.pause() else player?.play()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = if (room.isPlaying) R.drawable.pause else R.drawable.play),
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(24.dp))

                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(SwaaraColors.ElevatedSurface)
                                        .clickable { player?.forceSeekToNext() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.play_skip_forward),
                                        contentDescription = "Next",
                                        tint = SwaaraColors.PrimaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            // Listener View: In-Sync Indicator
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SwaaraColors.ElevatedSurface)
                                    .border(1.dp, SwaaraColors.Cyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (room.isPlaying) SwaaraColors.Cyan else SwaaraColors.SecondaryText)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (room.isPlaying) "In Lockstep Sync with Host Playback" else "Host Playback Paused",
                                        color = if (room.isPlaying) SwaaraColors.Cyan else SwaaraColors.SecondaryText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Dedicate Song Action
                if (room.allowDedications) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SwaaraColors.ElevatedSurface)
                                    .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(14.dp))
                                    .clickable { showDedicationDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.musical_notes),
                                        contentDescription = "Dedicate",
                                        tint = SwaaraColors.Magenta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Dedicate this Song to Someone",
                                        color = SwaaraColors.PrimaryText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 8. Collaborative Up Next Queue & Add Songs Header
                item {
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UP NEXT IN JAM (${queue.size})",
                            color = SwaaraColors.SecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        // + Add Songs Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta)))
                                .clickable { showAddMusicSheet = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ Add Songs",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (queue.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .clickable { showAddMusicSheet = true }
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No queued tracks yet.",
                                    color = SwaaraColors.PrimaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap here or '+ Add Songs' to search and queue tracks for this session.",
                                    color = SwaaraColors.TertiaryText,
                                    fontSize = 11.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(queue, key = { idx, it -> "${it.id}_$idx" }) { _, item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SwaaraColors.DeepSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!item.artworkUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = item.artworkUrl,
                                            contentDescription = item.title,
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
                                        text = item.title,
                                        color = SwaaraColors.PrimaryText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.artist} • added by ${item.addedByName}",
                                        color = SwaaraColors.SecondaryText,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Upvote Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SwaaraColors.DeepSurface)
                                        .border(1.dp, SwaaraColors.Magenta.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            SwaaraJamEngine.upvoteQueueItem(item.id)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.arrow_up),
                                            contentDescription = "Upvote",
                                            tint = SwaaraColors.Magenta,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${item.votes}",
                                            color = SwaaraColors.PrimaryText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Host Actions: Play Now & Delete
                                if (room.isHost) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SwaaraColors.Violet.copy(alpha = 0.25f))
                                            .border(1.dp, SwaaraColors.Violet, RoundedCornerShape(10.dp))
                                            .clickable {
                                                SwaaraJamEngine.playQueueItemNow(context, item)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "Play",
                                            color = SwaaraColors.Cyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red.copy(alpha = 0.15f))
                                            .clickable {
                                                SwaaraJamEngine.removeQueueItem(item.id)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.close),
                                            contentDescription = "Remove",
                                            tint = Color(0xFFFF5555),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 9. Floating Vibe Reaction Emitter Overlay
            SwaaraFloatingVibesLayer(
                vibes = vibes,
                modifier = Modifier.fillMaxSize()
            )

            // 10. Sticky Bottom Reaction Dock
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(SwaaraColors.ElevatedSurface.copy(alpha = 0.94f))
                        .border(1.dp, SwaaraColors.Magenta.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val vibeList = listOf(
                        "FIRE" to Color(0xFFFF5722),
                        "BOOM" to SwaaraColors.Magenta,
                        "BASS" to SwaaraColors.Cyan,
                        "VIBE" to SwaaraColors.Violet,
                        "LOVE" to Color(0xFFFF2A6D)
                    )

                    vibeList.forEach { (type, color) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(color.copy(alpha = 0.18f))
                                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .clickable {
                                    SwaaraJamEngine.sendVibe(context, type)
                                }
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "[$type]",
                                color = color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Flagship Spinning Vinyl Record Turntable with Concentric Radial Grooves,
 * Sweep Sheen Reflection, Reactive Beat Aura, and Physical Tone-Arm Needle.
 */
@Composable
fun SwaaraVinylTurntable(
    artworkUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val toneArmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 25f else -5f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ToneArmAngle"
    )

    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraScale"
    )

    Box(
        modifier = modifier.size(270.dp),
        contentAlignment = Alignment.Center
    ) {
        // Reactive Pulsing Beat Aura Glow
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(235.dp)
                    .graphicsLayer {
                        scaleX = auraScale
                        scaleY = auraScale
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(
                                SwaaraColors.Magenta.copy(alpha = 0.40f),
                                SwaaraColors.Violet.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Rotating Vinyl Record
        Box(
            modifier = Modifier
                .size(245.dp)
                .graphicsLayer {
                    rotationZ = if (isPlaying) rotation else 0f
                }
                .shadow(20.dp, CircleShape, spotColor = SwaaraColors.Magenta.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Outer edge rim
                drawCircle(color = Color(0xFF0F0F13), radius = radius, center = center)

                // Concentric sound grooves
                val grooveRadii = floatArrayOf(0.94f, 0.90f, 0.86f, 0.82f, 0.78f, 0.74f, 0.70f, 0.66f, 0.62f, 0.58f, 0.54f, 0.50f)
                for (rFactor in grooveRadii) {
                    drawCircle(
                        color = Color(0xFF24242C),
                        radius = radius * rFactor,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Vinyl Sheen reflection
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.18f to Color.White.copy(alpha = 0.08f),
                        0.25f to Color.Transparent,
                        0.5f to Color.Transparent,
                        0.68f to Color.White.copy(alpha = 0.08f),
                        0.75f to Color.Transparent,
                        1.0f to Color.Transparent
                    ),
                    radius = radius,
                    center = center
                )

                // Center Label Rim
                drawCircle(color = Color(0xFF1C1C24), radius = radius * 0.44f, center = center)
            }

            // Center Label Artwork
            Box(
                modifier = Modifier
                    .size(102.dp)
                    .clip(CircleShape)
                    .background(SwaaraColors.ElevatedSurface)
                    .border(2.dp, SwaaraColors.Magenta.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = "Vinyl Label",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.musical_notes),
                        contentDescription = null,
                        tint = SwaaraColors.Magenta,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Center spindle hole
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0E))
                        .border(1.5.dp, Color(0xFFCCCCCC), CircleShape)
                )
            }
        }

        // Stylus Tone-Arm Needle
        Canvas(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.TopEnd)
                .offset(x = 12.dp, y = (-8).dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.85f, 0.15f)
                    rotationZ = toneArmAngle
                }
        ) {
            val pivot = Offset(size.width * 0.85f, size.height * 0.15f)
            val head = Offset(size.width * 0.22f, size.height * 0.85f)

            drawCircle(color = Color(0xFF33333E), radius = 9.dp.toPx(), center = pivot)
            drawCircle(color = SwaaraColors.Cyan, radius = 3.5.dp.toPx(), center = pivot)

            drawLine(
                color = Color(0xFFCCCCCC),
                start = pivot,
                end = head,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawCircle(
                color = SwaaraColors.Magenta,
                radius = 5.5.dp.toPx(),
                center = head
            )
        }
    }
}

/**
 * High-Performance Single-Pass Canvas Spectrum Visualizer (Zero Allocations in draw loop).
 */
@Composable
fun SwaaraJamSpectrumCanvas(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Visualizer")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 16
        val barWidth = 4.dp.toPx()
        val spacing = (size.width - (barCount * barWidth)) / (barCount - 1)
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val heightRatio = if (isPlaying) {
                val wave = kotlin.math.sin(phase + i * 0.42f)
                val sec = kotlin.math.cos(phase * 1.4f + i * 0.28f)
                ((wave + sec + 2f) / 4f).coerceIn(0.12f, 1f)
            } else {
                0.10f
            }

            val barHeight = maxHeight * heightRatio
            val x = i * (barWidth + spacing)
            val y = maxHeight - barHeight

            val color = when (i % 3) {
                0 -> SwaaraColors.Cyan
                1 -> SwaaraColors.Magenta
                else -> SwaaraColors.Violet
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

/**
 * Floating Vibe Reactions Particle Emitter Overlay.
 */
@Composable
fun SwaaraFloatingVibesLayer(
    vibes: List<JamVibe>,
    modifier: Modifier = Modifier
) {
    val recentVibes = remember(vibes) {
        val now = System.currentTimeMillis()
        vibes.filter { now - it.timestamp < 5000 }
    }

    Box(modifier = modifier.fillMaxSize()) {
        recentVibes.forEachIndexed { idx, vibe ->
            key("${vibe.id}_${vibe.timestamp}_$idx") {
                FloatingVibeBadge(vibe = vibe)
            }
        }
    }
}

@Composable
fun FloatingVibeBadge(vibe: JamVibe) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(vibe.id) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
        )
    }

    val progress = animProgress.value
    if (progress < 1f) {
        val xPercent = remember(vibe.id) {
            val hash = kotlin.math.abs(vibe.id.hashCode()) % 70
            (hash + 15) / 100f
        }
        val yOffset = (-progress * 260).dp
        val alpha = if (progress < 0.2f) progress / 0.2f else (1f - progress).coerceAtLeast(0f)
        val scale = if (progress < 0.3f) 0.6f + (progress / 0.3f) * 0.5f else 1.1f - (progress - 0.3f) * 0.25f

        val (accentColor, label) = when (vibe.vibeType.uppercase()) {
            "FIRE" -> Color(0xFFFF5722) to "FIRE"
            "BOOM" -> SwaaraColors.Magenta to "BOOM"
            "BASS" -> SwaaraColors.Cyan to "BASS"
            "LOVE" -> Color(0xFFFF2A6D) to "LOVE"
            else -> SwaaraColors.Violet to "VIBE"
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (xPercent * 280).dp, y = yOffset - 90.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                        this.scaleX = scale
                        this.scaleY = scale
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(SwaaraColors.ElevatedSurface.copy(alpha = 0.94f))
                    .border(1.5.dp, accentColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[$label] ${vibe.senderName}",
                        color = SwaaraColors.PrimaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
