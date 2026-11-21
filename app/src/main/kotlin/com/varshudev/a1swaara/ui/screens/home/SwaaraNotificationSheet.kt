package com.varshudev.a1swaara.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.notifications.SwaaraNotificationItem
import com.varshudev.a1swaara.notifications.SwaaraNotificationStore
import com.varshudev.a1swaara.notifications.SwaaraNotificationType
import com.varshudev.a1swaara.ui.components.SwaaraBorderSubtle
import com.varshudev.a1swaara.ui.components.SwaaraBrandGradient
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraModalSurface
import com.varshudev.a1swaara.ui.components.SwaaraObsidian
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.components.SwaaraViolet
import com.varshudev.a1swaara.ui.screens.LocalOpenNowPlaying
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class NotificationFilterCategory(val displayName: String) {
    ALL("All"),
    DROPS("Music Drops"),
    STASH("Smart Stash"),
    ANNOUNCEMENTS("Announcements")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwaaraNotificationSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val binder = LocalPlayerServiceBinder.current
    val openNowPlaying = LocalOpenNowPlaying.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notifications by SwaaraNotificationStore.notifications.collectAsState()
    val unreadCount by SwaaraNotificationStore.unreadCount.collectAsState()
    var selectedCategory by remember { mutableStateOf(NotificationFilterCategory.ALL) }

    val filteredList = remember(notifications, selectedCategory) {
        when (selectedCategory) {
            NotificationFilterCategory.ALL -> notifications
            NotificationFilterCategory.DROPS -> notifications.filter { it.type == SwaaraNotificationType.MUSIC_DROP }
            NotificationFilterCategory.STASH -> notifications.filter { it.type == SwaaraNotificationType.SMART_STASH }
            NotificationFilterCategory.ANNOUNCEMENTS -> notifications.filter {
                it.type == SwaaraNotificationType.ANNOUNCEMENT || it.type == SwaaraNotificationType.COMMUNITY
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SwaaraObsidian,
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: Title & Actions
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
                            .background(SwaaraBrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notification_bell),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Activity & What's New",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SwaaraPrimaryText
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SwaaraMagenta)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$unreadCount New",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Music drops, offline stash, and studio news",
                            fontSize = 12.sp,
                            color = SwaaraSecondaryText
                        )
                    }
                }

                // Header actions: Mark All Read & Clear All
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { SwaaraNotificationStore.markAllAsRead(context) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.checkmark),
                                    contentDescription = "Mark All Read",
                                    tint = SwaaraMagenta,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Read All",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SwaaraMagenta
                                )
                            }
                        }
                    }

                    if (notifications.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable { SwaaraNotificationStore.clearAll(context) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.delete),
                                contentDescription = "Clear All",
                                tint = SwaaraSecondaryText,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationFilterCategory.values().forEach { category ->
                    val isSelected = category == selectedCategory
                    val count = when (category) {
                        NotificationFilterCategory.ALL -> notifications.size
                        NotificationFilterCategory.DROPS -> notifications.count { it.type == SwaaraNotificationType.MUSIC_DROP }
                        NotificationFilterCategory.STASH -> notifications.count { it.type == SwaaraNotificationType.SMART_STASH }
                        NotificationFilterCategory.ANNOUNCEMENTS -> notifications.count {
                            it.type == SwaaraNotificationType.ANNOUNCEMENT || it.type == SwaaraNotificationType.COMMUNITY
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected)
                                    Brush.horizontalGradient(listOf(SwaaraViolet, SwaaraMagenta))
                                else
                                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.04f)))
                            )
                            .border(
                                1.dp,
                                if (isSelected) SwaaraMagenta.copy(alpha = 0.8f) else SwaaraBorderSubtle,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = category.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else SwaaraSecondaryText
                            )
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else SwaaraSecondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notification Items List or Empty State
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SwaaraElevatedSurface)
                                .border(1.dp, SwaaraMagenta.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_notification_bell),
                                contentDescription = null,
                                tint = SwaaraMagenta,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "You're All Caught Up",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SwaaraPrimaryText
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "New releases, Smart Stash offline updates, and studio announcements will appear right here.",
                            fontSize = 12.sp,
                            color = SwaaraSecondaryText,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            onClick = {
                                SwaaraNotificationStore.markAsRead(context, item.id)
                            },
                            onPlayClick = {
                                SwaaraNotificationStore.markAsRead(context, item.id)
                                val q = item.playQuery
                                if (!q.isNullOrBlank()) {
                                    binder?.playFromSearch(q)
                                    openNowPlaying()
                                    onDismiss()
                                }
                            },
                            onExploreClick = {
                                SwaaraNotificationStore.markAsRead(context, item.id)
                                val url = item.actionUrl
                                if (!url.isNullOrBlank()) {
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (_: Exception) {}
                                    onDismiss()
                                }
                            },
                            onDismissClick = {
                                SwaaraNotificationStore.deleteNotification(context, item.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: SwaaraNotificationItem,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onExploreClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    val cardBackground = if (item.isRead) SwaaraModalSurface else SwaaraElevatedSurface
    val cardBorder = if (item.isRead)
        SwaaraBorderSubtle
    else
        SwaaraMagenta.copy(alpha = 0.5f)

    val (badgeText, badgeColor, iconRes) = when (item.type) {
        SwaaraNotificationType.MUSIC_DROP -> Triple("✦ MUSIC DROP", SwaaraCyan, R.drawable.disc)
        SwaaraNotificationType.SMART_STASH -> Triple("⚡ SMART STASH", Color(0xFF00E676), R.drawable.download)
        SwaaraNotificationType.COMMUNITY -> Triple("● COMMUNITY", SwaaraMagenta, R.drawable.radio)
        SwaaraNotificationType.ANNOUNCEMENT -> Triple("★ ANNOUNCEMENT", SwaaraViolet, R.drawable.ic_notification_bell)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Category Icon Badge Box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.14f))
                    .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Category + Time + Unread indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = SwaaraSecondaryText
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = formatRelativeTimestamp(item.timestamp),
                        fontSize = 11.sp,
                        color = SwaaraSecondaryText
                    )

                    if (!item.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(SwaaraMagenta)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SwaaraPrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.message,
                    fontSize = 12.sp,
                    color = SwaaraSecondaryText,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Interactive Action Buttons
                if (!item.playQuery.isNullOrBlank() || !item.actionUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!item.playQuery.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.horizontalGradient(listOf(SwaaraViolet, SwaaraMagenta)))
                                    .clickable(onClick = onPlayClick)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.play),
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Play Now",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (!item.actionUrl.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, SwaaraCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable(onClick = onExploreClick)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Explore",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SwaaraCyan
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Dismiss Button
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismissClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = "Dismiss",
                    tint = SwaaraSecondaryText.copy(alpha = 0.6f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

private fun formatRelativeTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0)
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
