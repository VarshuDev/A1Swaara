package com.varshudev.a1swaara.ui.screens.profile

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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.varshudev.a1swaara.Database
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.cloud.SwaaraAccountSyncEngine
import com.varshudev.a1swaara.cloud.SwaaraCloudAuth
import com.varshudev.a1swaara.cloud.SwaaraGoogleAuthManager
import com.varshudev.a1swaara.cloud.SwaaraSupabaseClient
import com.varshudev.a1swaara.cloud.SwaaraTopListenerManager
import com.varshudev.a1swaara.cloud.SyncStatus
import com.varshudev.a1swaara.ui.components.SwaaraColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SwaaraProfileScreen(
    onDismiss: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { SwaaraCloudAuth.getInstance(context) }
    val isSignedInFlowVal by auth.isSignedInFlow.collectAsState()

    var isSignedIn by remember { mutableStateOf(auth.isSignedIn) }
    var displayName by remember { mutableStateOf(auth.displayName) }
    var userEmail by remember { mutableStateOf(auth.userEmail) }
    var avatarUrl by remember { mutableStateOf(auth.avatarUrl) }

    val initialLocalStats = remember { SwaaraTopListenerManager.getLocalUserStats(context) }
    var totalHours by remember { mutableDoubleStateOf(initialLocalStats.totalHours) }
    var totalTracks by remember { mutableLongStateOf(initialLocalStats.totalTracks) }
    var streakDays by remember { mutableIntStateOf(initialLocalStats.streakDays) }

    val syncStatus by SwaaraAccountSyncEngine.syncStatus.collectAsState()
    val favorites by Database.favorites().collectAsState(initial = emptyList())
    val eventsCount by Database.eventsCount().collectAsState(initial = 0)

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var isSavingName by remember { mutableStateOf(false) }

    var showSignOutConfirmDialog by remember { mutableStateOf(false) }

    fun refreshStats() {
        // 1. Immediately read latest local stats on main thread so UI is instant
        val local = SwaaraTopListenerManager.getLocalUserStats(context)
        totalHours = local.totalHours
        totalTracks = local.totalTracks
        streakDays = local.streakDays

        // 2. Fetch and reconcile cloud-merged stats on IO coroutine
        coroutineScope.launch(Dispatchers.IO) {
            val stats = SwaaraTopListenerManager.getUserStats(context)
            withContext(Dispatchers.Main) {
                totalHours = stats.totalHours
                totalTracks = stats.totalTracks
                streakDays = stats.streakDays
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshStats()
    }

    LaunchedEffect(eventsCount) {
        if (eventsCount > 0) {
            refreshStats()
        }
    }

    LaunchedEffect(isSignedInFlowVal) {
        isSignedIn = isSignedInFlowVal
        displayName = auth.displayName
        userEmail = auth.userEmail
        avatarUrl = auth.avatarUrl
        refreshStats()
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        Dialog(
            onDismissRequest = { showEditNameDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SwaaraColors.DeepSurface)
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Edit Display Name",
                        color = SwaaraColors.PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This name appears across Live Jams and Leaderboards",
                        color = SwaaraColors.SecondaryText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SwaaraColors.ElevatedSurface)
                            .padding(14.dp)
                    ) {
                        BasicTextField(
                            value = editNameInput,
                            onValueChange = { editNameInput = it },
                            textStyle = TextStyle(color = SwaaraColors.PrimaryText, fontSize = 14.sp),
                            cursorBrush = SolidColor(SwaaraColors.Magenta),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .clickable { showEditNameDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Cancel", color = SwaaraColors.SecondaryText, fontSize = 13.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SwaaraColors.Magenta)
                                .clickable(enabled = editNameInput.isNotBlank() && !isSavingName) {
                                    val newName = editNameInput.trim()
                                    if (newName.isNotBlank()) {
                                        isSavingName = true
                                        auth.displayName = newName
                                        displayName = newName

                                        coroutineScope.launch(Dispatchers.IO) {
                                            if (auth.isSignedIn) {
                                                val payload = JSONObject().apply {
                                                    put("display_name", newName)
                                                }
                                                SwaaraSupabaseClient.patch("profiles", payload.toString(), "id=eq.${auth.userId}", auth.accessToken)
                                            }
                                            withContext(Dispatchers.Main) {
                                                isSavingName = false
                                                showEditNameDialog = false
                                                Toast.makeText(context, "Handle updated to $newName", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSavingName) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(text = "Save", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showSignOutConfirmDialog) {
        Dialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SwaaraColors.DeepSurface)
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Disconnect Account",
                        color = SwaaraColors.PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Signing out reverts to a local guest profile. Your backed-up playlists and stats remain securely saved in your cloud account.",
                        color = SwaaraColors.SecondaryText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .clickable { showSignOutConfirmDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Cancel", color = SwaaraColors.SecondaryText, fontSize = 13.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE53935))
                                .clickable {
                                    showSignOutConfirmDialog = false
                                    SwaaraGoogleAuthManager.signOut(context)
                                    isSignedIn = false
                                    displayName = auth.displayName
                                    userEmail = null
                                    avatarUrl = null
                                    Toast.makeText(context, "Signed out from Google", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Sign Out", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SwaaraColors.Obsidian)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // 1. Top Navigation Bar
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
                        text = "Profile & Account",
                        color = SwaaraColors.PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                refreshStats()
                                Toast.makeText(context, "Telemetry updated", Toast.LENGTH_SHORT).show()
                            },
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

            // 2. Sovereign Identity Hero Card
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SwaaraColors.ElevatedSurface)
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(SwaaraColors.DeepSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.person),
                                    contentDescription = null,
                                    tint = SwaaraColors.SecondaryText,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Display Name with Edit Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = displayName,
                                color = SwaaraColors.PrimaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        editNameInput = displayName
                                        showEditNameDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.search),
                                    contentDescription = "Edit Name",
                                    tint = SwaaraColors.SecondaryText,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isSignedIn) (userEmail ?: "Google Connected") else "Guest Profile (Local Device)",
                            color = SwaaraColors.SecondaryText,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Account Tier Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SwaaraColors.DeepSurface)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isSignedIn) "Cloud Sync Active" else "Guest Mode",
                                color = if (isSignedIn) SwaaraColors.Cyan else SwaaraColors.SecondaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isSignedIn) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SwaaraColors.DeepSurface)
                                    .clickable { showSignOutConfirmDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Sign Out",
                                    color = Color(0xFFFF5555),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .clickable {
                                        coroutineScope.launch {
                                            SwaaraGoogleAuthManager.launchGoogleSignIn(context)
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
                                        text = "Sign in with Google",
                                        color = Color.Black,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Telemetry Grid (4 Stat Cards)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Stream Time
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(text = "Stream time", color = SwaaraColors.SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$totalHours hrs", color = SwaaraColors.Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Card 2: Tracks Played
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(text = "Tracks played", color = SwaaraColors.SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$totalTracks", color = SwaaraColors.Magenta, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 3: Daily Streak
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(text = "Listening streak", color = SwaaraColors.SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$streakDays days", color = Color(0xFFFFB300), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Card 4: Cloud Vault Items
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(text = "Favorites", color = SwaaraColors.SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "${favorites.size} tracks", color = SwaaraColors.Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Supabase Cloud Sync Center
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SwaaraColors.ElevatedSurface)
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Cloud Sync", color = SwaaraColors.PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (syncStatus) {
                                        is SyncStatus.Syncing -> "Synchronizing library with Supabase..."
                                        is SyncStatus.Success -> "All playlists & favorites synced"
                                        is SyncStatus.Error -> "Sync failed. Tap to retry."
                                        else -> "Cloud backups active"
                                    },
                                    color = when (syncStatus) {
                                        is SyncStatus.Error -> Color(0xFFFF5252)
                                        is SyncStatus.Syncing -> SwaaraColors.Cyan
                                        else -> SwaaraColors.SecondaryText
                                    },
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Sync Action Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SwaaraColors.DeepSurface)
                                    .clickable(enabled = syncStatus !is SyncStatus.Syncing) {
                                        SwaaraAccountSyncEngine.triggerSync(context)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (syncStatus is SyncStatus.Syncing) {
                                    CircularProgressIndicator(color = SwaaraColors.Cyan, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(text = "Sync Now", color = SwaaraColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Auto-sync Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Background Auto-Backup", color = SwaaraColors.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(text = "Back up favorites whenever you like a song", color = SwaaraColors.SecondaryText, fontSize = 11.sp)
                            }
                            Switch(
                                checked = auth.isAutoSyncEnabled,
                                onCheckedChange = { auth.isAutoSyncEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SwaaraColors.Magenta
                                )
                            )
                        }
                    }
                }
            }

            // 5. Leaderboard Shortcut Showcase
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SwaaraColors.ElevatedSurface)
                        .clickable { onNavigateToLeaderboard() }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.musical_notes),
                                contentDescription = null,
                                tint = SwaaraColors.Gold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Top Listener Leaderboard",
                                    color = SwaaraColors.PrimaryText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "See global fan rankings & streaming crowns",
                                    color = SwaaraColors.SecondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = "View",
                            color = SwaaraColors.Cyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
