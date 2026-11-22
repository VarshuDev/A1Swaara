package com.varshudev.a1swaara.ui.screens.jam

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.varshudev.a1swaara.cloud.JamRoomSummary
import com.varshudev.a1swaara.cloud.SwaaraCloudAuth
import com.varshudev.a1swaara.cloud.SwaaraCloudConfig
import com.varshudev.a1swaara.cloud.SwaaraGoogleAuthManager
import com.varshudev.a1swaara.cloud.SwaaraJamEngine
import com.varshudev.a1swaara.ui.components.SwaaraColors
import kotlinx.coroutines.launch

@Composable
fun SwaaraJamHubScreen(
    onDismiss: () -> Unit,
    onEnterLiveRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentRoom by SwaaraJamEngine.currentRoom.collectAsState()

    var publicRooms by remember { mutableStateOf<List<JamRoomSummary>>(emptyList()) }
    var isLoadingRooms by remember { mutableStateOf(true) }
    var joinCodeInput by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    fun refreshRooms() {
        isLoadingRooms = true
        coroutineScope.launch {
            publicRooms = SwaaraJamEngine.listPublicRooms()
            isLoadingRooms = false
        }
    }

    LaunchedEffect(Unit) {
        refreshRooms()
    }

    if (showCreateDialog) {
        CreateJamRoomDialog(
            onDismiss = { showCreateDialog = false },
            onRoomCreated = {
                showCreateDialog = false
                onEnterLiveRoom()
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SwaaraColors.Obsidian)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            // 1. Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.chevron_down),
                            contentDescription = "Back",
                            tint = SwaaraColors.PrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Jam Sessions",
                        color = SwaaraColors.PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { refreshRooms() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.sync),
                            contentDescription = "Refresh",
                            tint = SwaaraColors.PrimaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Active Session Banner (if already in one)
            if (currentRoom != null) {
                item {
                    val room = currentRoom!!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SwaaraColors.DeepSurface)
                            .clickable { onEnterLiveRoom() }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(SwaaraColors.Magenta)
                                    )
                                    Text(
                                        text = "Live Session Active",
                                        color = SwaaraColors.Magenta,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${room.roomName} (${room.roomCode})",
                                    color = SwaaraColors.PrimaryText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SwaaraColors.Magenta)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(text = "Open Room", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Google Sign-In Gate or Quick Actions
            val isSignedIn = SwaaraCloudAuth.getInstance(context).isSignedIn
            if (!isSignedIn) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SwaaraColors.DeepSurface)
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Sign in to join Live Jam",
                                color = SwaaraColors.PrimaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Collaborate in real time, queue tracks together, and listen with friends.",
                                color = SwaaraColors.SecondaryText,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .clickable {
                                        coroutineScope.launch {
                                            val res = SwaaraGoogleAuthManager.launchGoogleSignIn(context)
                                            if (res.isSuccess) {
                                                refreshRooms()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.person),
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Continue with Google",
                                        color = Color.Black,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        // Create Room Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SwaaraColors.Magenta)
                                .clickable { showCreateDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.musical_notes),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Create a Live Jam Session",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Join via Code
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SwaaraColors.ElevatedSurface)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = joinCodeInput,
                                    onValueChange = { if (it.length <= 6) joinCodeInput = it.uppercase() },
                                    textStyle = TextStyle(
                                        color = SwaaraColors.PrimaryText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    ),
                                    cursorBrush = SolidColor(SwaaraColors.Magenta),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { innerTextField ->
                                        if (joinCodeInput.isEmpty()) {
                                            Text(
                                                text = "Enter 6-digit code",
                                                color = SwaaraColors.SecondaryText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (joinCodeInput.length >= 4) SwaaraColors.Magenta else SwaaraColors.ElevatedSurface)
                                    .clickable(enabled = !isJoining && joinCodeInput.length >= 4) {
                                        isJoining = true
                                        coroutineScope.launch {
                                            val ok = SwaaraJamEngine.joinRoom(context, joinCodeInput)
                                            isJoining = false
                                            if (ok) {
                                                onEnterLiveRoom()
                                            } else {
                                                Toast.makeText(context, "Room not found or inactive", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isJoining) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Join",
                                        color = if (joinCodeInput.length >= 4) Color.White else SwaaraColors.SecondaryText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Public Live Sessions List
            item {
                Spacer(modifier = Modifier.height(28.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Public Jams",
                        color = SwaaraColors.PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isLoadingRooms) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = SwaaraColors.Magenta,
                            strokeWidth = 2.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (!isLoadingRooms && publicRooms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SwaaraColors.ElevatedSurface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No public sessions right now",
                                color = SwaaraColors.PrimaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create a room above and invite friends to listen together!",
                                color = SwaaraColors.SecondaryText,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(publicRooms) { r ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SwaaraColors.ElevatedSurface)
                            .clickable {
                                coroutineScope.launch {
                                    val ok = SwaaraJamEngine.joinRoom(context, r.roomCode)
                                    if (ok) onEnterLiveRoom()
                                }
                            }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = r.roomName,
                                    color = SwaaraColors.PrimaryText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Host: ${r.hostName} • ${r.currentTitle ?: "Vibing"}",
                                    color = SwaaraColors.SecondaryText,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SwaaraColors.DeepSurface)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${r.activeListeners} Listeners",
                                    color = SwaaraColors.Cyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateJamRoomDialog(
    onDismiss: () -> Unit,
    onRoomCreated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player
    val currentItem = player?.currentMediaItem
    val activeTitle = currentItem?.mediaMetadata?.title?.toString()
    val activeArtist = currentItem?.mediaMetadata?.artist?.toString()
    val activeArtwork = currentItem?.mediaMetadata?.artworkUri?.toString()

    var roomName by remember {
        mutableStateOf(if (!activeTitle.isNullOrBlank()) "${activeTitle.take(18)} Jam" else "My Swaara Jam")
    }
    val isPublic = true
    var allowDedications by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(SwaaraColors.DeepSurface)
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Create Jam Session",
                    color = SwaaraColors.PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Active Track Preview if available
                if (currentItem != null && !activeTitle.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SwaaraColors.ElevatedSurface)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SwaaraColors.DeepSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!activeArtwork.isNullOrBlank()) {
                                AsyncImage(
                                    model = activeArtwork,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.musical_notes),
                                    contentDescription = null,
                                    tint = SwaaraColors.Magenta,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kicking off with",
                                color = SwaaraColors.Cyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = activeTitle,
                                color = SwaaraColors.PrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeArtist ?: "A1 Swaara",
                                color = SwaaraColors.SecondaryText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Room Name
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Room Name",
                        color = SwaaraColors.SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SwaaraColors.ElevatedSurface)
                            .padding(14.dp)
                    ) {
                        BasicTextField(
                            value = roomName,
                            onValueChange = { roomName = it },
                            textStyle = TextStyle(color = SwaaraColors.PrimaryText, fontSize = 14.sp),
                            cursorBrush = SolidColor(SwaaraColors.Magenta),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Public Session Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Public Session", color = SwaaraColors.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Private sessions are temporarily unavailable", color = SwaaraColors.SecondaryText, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isPublic,
                        onCheckedChange = null,
                        enabled = false,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SwaaraColors.Magenta)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Allow Dedications Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Allow Dedications", color = SwaaraColors.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Listeners can dedicate tracks", color = SwaaraColors.SecondaryText, fontSize = 11.sp)
                    }
                    Switch(
                        checked = allowDedications,
                        onCheckedChange = { allowDedications = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SwaaraColors.Magenta)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SwaaraColors.ElevatedSurface)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Cancel", color = SwaaraColors.SecondaryText, fontSize = 14.sp)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SwaaraColors.Magenta)
                            .clickable(enabled = !isCreating && roomName.isNotBlank()) {
                                isCreating = true
                                coroutineScope.launch {
                                    val code = SwaaraJamEngine.createRoom(
                                        context = context,
                                        roomName = roomName,
                                        isPublic = isPublic,
                                        allowDedications = allowDedications,
                                        initialSongId = currentItem?.mediaId ?: "",
                                        initialTitle = activeTitle ?: "",
                                        initialArtist = activeArtist ?: "",
                                        initialArtworkUrl = activeArtwork,
                                        isPlaying = player?.isPlaying ?: false,
                                        positionMs = player?.currentPosition ?: 0L
                                    )
                                    isCreating = false
                                    if (code != null) {
                                        onRoomCreated()
                                    } else {
                                        Toast.makeText(context, "Failed to create room", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(text = "Start Session", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
