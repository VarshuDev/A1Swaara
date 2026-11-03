package com.varshudev.a1swaara.ui.screens.leaderboard

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.cloud.ArtistTopListener
import com.varshudev.a1swaara.cloud.GlobalTopListener
import com.varshudev.a1swaara.cloud.SwaaraCloudAuth
import com.varshudev.a1swaara.cloud.SwaaraTopListenerManager
import com.varshudev.a1swaara.ui.components.SwaaraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SwaaraLeaderboardScreen(
    initialArtist: String? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { SwaaraCloudAuth.getInstance(context) }

    var selectedTab by remember { mutableIntStateOf(if (initialArtist != null) 1 else 0) }
    var artistQuery by remember { mutableStateOf(initialArtist ?: "") }

    var globalLeaders by remember { mutableStateOf<List<GlobalTopListener>>(emptyList()) }
    var artistLeaders by remember { mutableStateOf<List<ArtistTopListener>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val popularArtists = remember {
        listOf("All Artists", "Navjot Ahuja", "Arijit Singh", "Vasuki Vaibhav", "Sai Abhyankkar", "Sachin Basrur")
    }

    fun loadLeaderboards(force: Boolean = false) {
        isLoading = true
        coroutineScope.launch {
            if (selectedTab == 0) {
                globalLeaders = SwaaraTopListenerManager.getGlobalLeaderboard(limit = 25, forceRefresh = force)
            } else {
                artistLeaders = SwaaraTopListenerManager.getArtistLeaderboard(artistQuery, limit = 25)
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedTab) {
        loadLeaderboards()
    }

    LaunchedEffect(artistQuery) {
        if (selectedTab == 1) {
            delay(350)
            loadLeaderboards()
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
                        text = "LEADERBOARD",
                        color = SwaaraColors.PrimaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SwaaraColors.ElevatedSurface)
                            .border(1.dp, SwaaraColors.CardBorder, CircleShape)
                            .clickable { loadLeaderboards(force = true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.sync),
                            contentDescription = "Refresh",
                            tint = SwaaraColors.PrimaryText,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // 2. Tab Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Global Hall of Fame
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedTab == 0) Brush.horizontalGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta))
                                else SolidColor(SwaaraColors.ElevatedSurface)
                            )
                            .clickable { selectedTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Global Hall of Fame",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    // Artist Superfans
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedTab == 1) Brush.horizontalGradient(listOf(SwaaraColors.Violet, SwaaraColors.Magenta))
                                else SolidColor(SwaaraColors.ElevatedSurface)
                            )
                            .clickable { selectedTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Artist Superfans",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 3. User Rank Banner
            item {
                val currentUserId = auth.userId
                val myRank = globalLeaders.find { it.userId == currentUserId }
                if (auth.isSignedIn) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(SwaaraColors.ElevatedSurface, SwaaraColors.DeepSurface)))
                            .border(1.dp, SwaaraColors.Cyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SwaaraColors.Cyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (myRank != null) "#${myRank.rank}" else "-",
                                    color = SwaaraColors.Cyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = auth.displayName,
                                    color = SwaaraColors.PrimaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (myRank != null) "${myRank.totalHours} hrs • ${myRank.totalTracks} tracks streamed"
                                    else "Listening stats active • Stream tracks to climb ranks",
                                    color = SwaaraColors.SecondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SwaaraColors.ElevatedSurface.copy(alpha = 0.7f))
                            .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.person),
                                contentDescription = null,
                                tint = SwaaraColors.Magenta,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in via Account tab to link your streams, claim your rank, and earn fan tier badges.",
                                color = SwaaraColors.SecondaryText,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Artist Search Field & Suggestion Chips (Tab 1 only)
            if (selectedTab == 1) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SwaaraColors.ElevatedSurface)
                            .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = null,
                                tint = SwaaraColors.SecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (artistQuery.isEmpty()) {
                                    Text(
                                        text = "Filter by artist (e.g. Navjot, Arijit, Vasuki)...",
                                        color = SwaaraColors.SecondaryText.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                }
                                BasicTextField(
                                    value = artistQuery,
                                    onValueChange = { artistQuery = it },
                                    textStyle = TextStyle(color = SwaaraColors.PrimaryText, fontSize = 13.sp),
                                    cursorBrush = SolidColor(SwaaraColors.Magenta),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (artistQuery.isNotEmpty()) {
                                Icon(
                                    painter = painterResource(id = R.drawable.close),
                                    contentDescription = "Clear",
                                    tint = SwaaraColors.SecondaryText,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { artistQuery = "" }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(popularArtists) { chip ->
                            val isChipSelected = (chip == "All Artists" && artistQuery.isEmpty()) ||
                                (chip.equals(artistQuery, ignoreCase = true))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isChipSelected) SwaaraColors.Magenta.copy(alpha = 0.25f)
                                        else SwaaraColors.ElevatedSurface
                                    )
                                    .border(
                                        1.dp,
                                        if (isChipSelected) SwaaraColors.Magenta else SwaaraColors.CardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        artistQuery = if (chip == "All Artists") "" else chip
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = chip,
                                    color = if (isChipSelected) SwaaraColors.PrimaryText else SwaaraColors.SecondaryText,
                                    fontSize = 11.sp,
                                    fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // 5. Loading State
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = SwaaraColors.Magenta,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // 6. Global Leaderboard List
            if (!isLoading && selectedTab == 0) {
                if (globalLeaders.isEmpty()) {
                    item {
                        EmptyLeaderboardNotice("No listeners recorded yet on the Global Hall of Fame.")
                    }
                } else {
                    items(globalLeaders) { leader ->
                        val isMe = (auth.isSignedIn && leader.userId == auth.userId)
                        LeaderboardRow(
                            rank = leader.rank,
                            title = leader.displayName,
                            subtitle = "${leader.totalHours} stream hours • ${leader.totalTracks} tracks",
                            badge = if (leader.rank == 1) "CHAMPION" else if (leader.rank <= 3) "PODIUM" else null,
                            avatarUrl = leader.avatarUrl,
                            isCurrentUser = isMe
                        )
                    }
                }
            }

            // 7. Artist Superfans List
            if (!isLoading && selectedTab == 1) {
                if (artistLeaders.isEmpty()) {
                    item {
                        val msg = if (artistQuery.isBlank()) "No recorded artist streams found yet in the community."
                        else "No recorded streams for \"$artistQuery\" yet. Be the first to listen!"
                        EmptyLeaderboardNotice(
                            message = msg,
                            onSelectArtist = { selected ->
                                artistQuery = selected
                            }
                        )
                    }
                } else {
                    items(artistLeaders) { fan ->
                        val hours = Math.round((fan.totalSeconds / 3600.0) * 10.0) / 10.0
                        val artistTag = if (fan.artistName.isNotBlank() && artistQuery.isBlank()) " • ${fan.artistName}" else ""
                        val isMe = (auth.isSignedIn && fan.userId == auth.userId)
                        LeaderboardRow(
                            rank = fan.rank,
                            title = fan.displayName,
                            subtitle = "$hours hrs • ${fan.playCount} plays$artistTag",
                            badge = fan.badge,
                            avatarUrl = fan.avatarUrl,
                            isCurrentUser = isMe
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    title: String,
    subtitle: String,
    badge: String?,
    avatarUrl: String?,
    isCurrentUser: Boolean = false
) {
    val rankColor = when (rank) {
        1 -> SwaaraColors.Gold
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> SwaaraColors.SecondaryText
    }

    val borderColor = when {
        isCurrentUser -> SwaaraColors.Cyan.copy(alpha = 0.8f)
        rank == 1 -> SwaaraColors.Gold.copy(alpha = 0.5f)
        rank == 2 -> Color(0xFFC0C0C0).copy(alpha = 0.4f)
        rank == 3 -> Color(0xFFCD7F32).copy(alpha = 0.4f)
        else -> SwaaraColors.CardBorder
    }

    val backgroundBrush = when (rank) {
        1 -> Brush.horizontalGradient(listOf(SwaaraColors.Gold.copy(alpha = 0.12f), SwaaraColors.ElevatedSurface))
        2 -> Brush.horizontalGradient(listOf(Color(0xFFC0C0C0).copy(alpha = 0.08f), SwaaraColors.ElevatedSurface))
        3 -> Brush.horizontalGradient(listOf(Color(0xFFCD7F32).copy(alpha = 0.08f), SwaaraColors.ElevatedSurface))
        else -> Brush.horizontalGradient(listOf(SwaaraColors.ElevatedSurface, SwaaraColors.ElevatedSurface))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundBrush)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number or Podium Indicator
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.18f))
                    .border(1.dp, rankColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    color = rankColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar with Podium Ring
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SwaaraColors.DeepSurface)
                    .border(
                        if (rank in 1..3) 1.5.dp else 1.dp,
                        if (rank in 1..3) rankColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
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

            // Name and Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = SwaaraColors.PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SwaaraColors.Cyan.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "YOU",
                                color = SwaaraColors.Cyan,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    color = SwaaraColors.SecondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Badge / Podium Tag
            val podiumTag = when (rank) {
                1 -> "GOLD"
                2 -> "SILVER"
                3 -> "BRONZE"
                else -> null
            }
            val displayTag = badge ?: podiumTag

            if (displayTag != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (rank) {
                                1 -> SwaaraColors.Gold.copy(alpha = 0.2f)
                                2 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                                3 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                                else -> SwaaraColors.Cyan.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            1.dp,
                            when (rank) {
                                1 -> SwaaraColors.Gold.copy(alpha = 0.4f)
                                2 -> Color(0xFFC0C0C0).copy(alpha = 0.4f)
                                3 -> Color(0xFFCD7F32).copy(alpha = 0.4f)
                                else -> SwaaraColors.Cyan.copy(alpha = 0.3f)
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = displayTag,
                        color = when (rank) {
                            1 -> SwaaraColors.Gold
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> SwaaraColors.Cyan
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLeaderboardNotice(
    message: String,
    onSelectArtist: ((String) -> Unit)? = null
) {
    val suggestions = remember {
        listOf("Vasuki Vaibhav", "Sachin Basrur", "Sanjith Hegde", "Arijit Singh")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SwaaraColors.ElevatedSurface)
            .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SwaaraColors.Violet.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.musical_notes),
                    contentDescription = null,
                    tint = SwaaraColors.Magenta,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                color = SwaaraColors.PrimaryText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Stream tracks to claim the top listener throne on the Hall of Fame.",
                color = SwaaraColors.SecondaryText,
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            if (onSelectArtist != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "POPULAR LEADERBOARDS",
                    color = SwaaraColors.SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { artist ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SwaaraColors.DeepSurface)
                                .border(1.dp, SwaaraColors.CardBorder, RoundedCornerShape(10.dp))
                                .clickable { onSelectArtist(artist) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = artist,
                                color = SwaaraColors.PrimaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
