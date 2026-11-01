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

    var totalHours by remember { mutableDoubleStateOf(0.0) }
    var totalTracks by remember { mutableLongStateOf(0L) }
    var streakDays by remember { mutableIntStateOf(1) }

    val syncStatus by SwaaraAccountSyncEngine.syncStatus.collectAsState()
    val favorites by Database.favorites().collectAsState(initial = emptyList())

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var isSavingName by remember { mutableStateOf(false) }

    var showSignOutConfirmDialog by remember { mutableStateOf(false) }

    fun refreshStats() {
        coroutineScope.launch(Dispatchers.IO) {
            val stats = SwaaraSupabaseClient.get("global_listening_stats", "user_id=eq.${auth.userId}", auth.accessToken)
            if (stats != null && stats.length() > 0) {
                val row = stats.getJSONObject(0)
                val sec = row.optLong("total_seconds", 0L)
                val tracks = row.optLong("total_tracks_played", 0L)
                val streak = row.optInt("current_streak_days", 1)
                withContext(Dispatchers.Main) {
                    totalHours = Math.round((sec / 3600.0) * 10.0) / 10.0
                    totalTracks = tracks
                    streakDays = streak
                }
            }
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
                    .clip(RoundedCornerShape(24.dp))
                    .background(SwaaraColors.DeepSurface)
                    .border(1.dp, SwaaraColors.Magenta.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "EDIT LISTENER HANDLE",
                        color = SwaaraColors.PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This name appears across Live Jams and Leaderboards",
                        color = SwaaraColors.SecondaryText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SwaaraColors.ElevatedSurface)
                            .border(1.dp, SwaaraColors.Cyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        BasicTextField(
                            value = editNameInput,
                            onValueChange = { editNameInput = it },
                            textStyle = TextStyle(color = SwaaraColors.PrimaryText, fontSize = 14.sp),
                            cursorBrush = SolidColor(SwaaraColors.Cyan),
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
                                .background(Brush.horizontalGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta)))
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
                                Text(text = "Save Handle", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                    .clip(RoundedCornerShape(22.dp))
                    .background(SwaaraColors.DeepSurface)
                    .border(1.dp, Color.Red.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DISCONNECT ACCOUNT",
                        color = SwaaraColors.PrimaryText,
                        fontSize = 16.sp,
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
                                .background(Color.Red.copy(alpha = 0.2f))
                                .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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
                            Text(text = "Sign Out", color = Color(0xFFFF5555), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SwaaraColors.ElevatedSurface)
                            .border(1.dp, SwaaraColors.CardBorder, CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.chevron_down),
                            contentDescription = "Back",
                            tint = SwaaraColors.PrimaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "ACCOUNT & PROFILE",
                        color = SwaaraColors.PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SwaaraColors.ElevatedSurface)
                            .border(1.dp, SwaaraColors.CardBorder, CircleShape)
                            .clickable { refreshStats() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.sync),
                            contentDescription = "Refresh",
                            tint = SwaaraColors.Cyan,
                            modifier = Modifier.size(17.dp)
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
                        .clip(RoundedCornerShape(24.dp))
                        .background(SwaaraColors.ElevatedSurface)
                        .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(24.dp))
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with Cyber Glow Ring
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta)))
                                .border(2.dp, SwaaraColors.Cyan, CircleShape),
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
                                    tint = Color.White,
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
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(SwaaraColors.DeepSurface)
                                    .clickable {
                                        editNameInput = displayName
                                        showEditNameDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.search),
                                    contentDescription = "Edit Name",
                                    tint = SwaaraColors.Cyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isSignedIn) (userEmail ?: "Google Connected") else "Guest Profile (Local Device)",
                            color = if (isSignedIn) SwaaraColors.Cyan else SwaaraColors.SecondaryText,
                            fontSize = 12.5.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Account Tier Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isSignedIn) {
                                        Modifier.background(Brush.horizontalGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta)))
                                    } else {
                                        Modifier.background(SwaaraColors.DeepSurface)
                                    }
                                )
                                .border(1.dp, if (isSignedIn) SwaaraColors.Magenta else SwaaraColors.CardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isSignedIn) "[SOVEREIGN PRO • CLOUD SYNC ACTIVE]" else "[GUEST • SIGN IN FOR CLOUD & JAMS]",
                                color = if (isSignedIn) Color.White else SwaaraColors.SecondaryText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (isSignedIn) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Red.copy(alpha = 0.15f))
                                    .border(1.dp, Color.Red.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .clickable { showSignOutConfirmDialog = true }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Sign Out from Google",
                                    color = Color(0xFFFF5555),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta)))
                                    .clickable {
                                        coroutineScope.launch {
                                            SwaaraGoogleAuthManager.launchGoogleSignIn(context)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign In with Google",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Cyber Telemetry Grid (4 Stat Cards)
            item {
                Spacer(modifier = Modifier.height(18.dp))
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
                                .clip(RoundedCornerShape(16.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .border(1.dp, SwaaraColors.Cyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(text = "STREAM TIME", color = SwaaraColors.SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "$totalHours hrs", color = SwaaraColors.Cyan, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Card 2: Tracks Played
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .border(1.dp, SwaaraColors.Magenta.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(text = "TRACKS PLAYED", color = SwaaraColors.SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "$totalTracks", color = SwaaraColors.Magenta, fontSize = 19.sp, fontWeight = FontWeight.Bold)
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
                                .clip(RoundedCornerShape(16.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(text = "LISTENING STREAK", color = SwaaraColors.SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "$streakDays Days", color = Color(0xFFFFB300), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Card 4: Cloud Vault Items
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SwaaraColors.ElevatedSurface)
                                .border(1.dp, SwaaraColors.Violet.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(text = "SAVED VAULT", color = SwaaraColors.SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "${favorites.size} Tracks", color = SwaaraColors.Violet, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Supabase Cloud Sync Center
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SwaaraColors.ElevatedSurface)
                        .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "SUPABASE CLOUD SYNC", color = SwaaraColors.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

                            // Sync Action Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SwaaraColors.Cyan.copy(alpha = 0.15f))
                                    .border(1.dp, SwaaraColors.Cyan, RoundedCornerShape(10.dp))
                                    .clickable(enabled = syncStatus !is SyncStatus.Syncing) {
                                        SwaaraAccountSyncEngine.triggerSync(context)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (syncStatus is SyncStatus.Syncing) {
                                    CircularProgressIndicator(color = SwaaraColors.Cyan, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(text = "Sync Now", color = SwaaraColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                Text(text = "Back up favorites whenever you like a song", color = SwaaraColors.SecondaryText, fontSize = 10.5.sp)
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
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SwaaraColors.ElevatedSurface)
                        .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(20.dp))
                        .clickable { onNavigateToLeaderboard() }
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(0xFFFFB300), Color(0xFFFF5722)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.musical_notes),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Top Listener Leaderboard",
                                    color = SwaaraColors.PrimaryText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
