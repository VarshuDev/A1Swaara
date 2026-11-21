package com.varshudev.a1swaara.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.notifications.SwaaraNotificationItem
import com.varshudev.a1swaara.notifications.SwaaraNotificationStore
import com.varshudev.a1swaara.notifications.SwaaraNotificationType
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

// ---------------------------------------------------------------------------
// Filter tab model
// ---------------------------------------------------------------------------

private enum class NotificationFilterCategory(val label: String) {
    ALL("All"),
    DROPS("Drops"),
    STASH("Stash"),
    ANNOUNCEMENTS("News")
}

// ---------------------------------------------------------------------------
// Sheet root
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwaaraNotificationSheet(onDismiss: () -> Unit) {
    val context        = LocalContext.current
    val uriHandler     = LocalUriHandler.current
    val binder         = LocalPlayerServiceBinder.current
    val openNowPlaying = LocalOpenNowPlaying.current

    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notifications by SwaaraNotificationStore.notifications.collectAsState()
    val unreadCount   by SwaaraNotificationStore.unreadCount.collectAsState()
    var filter by remember { mutableStateOf(NotificationFilterCategory.ALL) }

    val filteredList = remember(notifications, filter) {
        when (filter) {
            NotificationFilterCategory.ALL           -> notifications
            NotificationFilterCategory.DROPS         -> notifications.filter { it.type == SwaaraNotificationType.MUSIC_DROP }
            NotificationFilterCategory.STASH         -> notifications.filter { it.type == SwaaraNotificationType.SMART_STASH }
            NotificationFilterCategory.ANNOUNCEMENTS -> notifications.filter {
                it.type == SwaaraNotificationType.ANNOUNCEMENT || it.type == SwaaraNotificationType.COMMUNITY
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = SwaaraObsidian,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 0.dp)
        ) {

            // ---------------------------------------------------------------
            // Header — clean title + inline unread count + right-side actions
            // ---------------------------------------------------------------
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: title + unread dot-count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = "Notifications",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = SwaaraPrimaryText
                    )
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SwaaraMagenta)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text       = unreadCount.toString(),
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }
                    }
                }

                // Right: ghost action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unreadCount > 0) {
                        Text(
                            text     = "Mark read",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color    = SwaaraMagenta,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { SwaaraNotificationStore.markAllAsRead(context) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (notifications.isNotEmpty()) {
                        if (unreadCount > 0) Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable { SwaaraNotificationStore.clearAll(context) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter           = painterResource(id = R.drawable.delete),
                                contentDescription = "Clear all",
                                tint              = SwaaraSecondaryText,
                                modifier          = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------------------------------------------------------------
            // Filter tabs — text-only underline style
            // ---------------------------------------------------------------
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                NotificationFilterCategory.values().forEach { tab ->
                    val selected = tab == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { filter = tab }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = tab.label,
                            fontSize   = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (selected) SwaaraPrimaryText else SwaaraSecondaryText
                        )
                    }
                    // Active underline
                    if (selected) {
                        // We can't do an underline at this level cleanly without absolute offsets,
                        // so instead we use font weight + color contrast only — simpler = better.
                    }
                }
            }

            // Divider under tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.07f))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---------------------------------------------------------------
            // Content
            // ---------------------------------------------------------------
            if (filteredList.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxWidth(),
                    contentPadding  = PaddingValues(bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        NotificationRow(
                            item          = item,
                            onClick       = { SwaaraNotificationStore.markAsRead(context, item.id) },
                            onPlayClick   = {
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
                                    try { uriHandler.openUri(url) } catch (_: Exception) {}
                                    onDismiss()
                                }
                            },
                            onDismissClick = { SwaaraNotificationStore.deleteNotification(context, item.id) }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Individual notification row — flat, editorial, no icon box bloat
// ---------------------------------------------------------------------------

@Composable
private fun NotificationRow(
    item: SwaaraNotificationItem,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onExploreClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    val (typeLabel, accentColor) = when (item.type) {
        SwaaraNotificationType.MUSIC_DROP    -> "Music Drop"    to SwaaraCyan
        SwaaraNotificationType.SMART_STASH   -> "Stash"         to Color(0xFF4ADE80)
        SwaaraNotificationType.COMMUNITY     -> "Community"     to SwaaraMagenta
        SwaaraNotificationType.ANNOUNCEMENT  -> "News"          to SwaaraViolet
    }

    val rowBg = if (item.isRead) SwaaraModalSurface else SwaaraElevatedSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
    ) {
        // Unread left accent stripe
        if (!item.isRead) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(72.dp) // approximate row height; will clip naturally
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(SwaaraMagenta)
                    .align(Alignment.CenterStart)
            )
        }

        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = if (!item.isRead) 15.dp else 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {

                // Type label + timestamp on one line
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text       = typeLabel,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = accentColor
                    )
                    Text(
                        text     = "·",
                        fontSize = 10.sp,
                        color    = SwaaraSecondaryText
                    )
                    Text(
                        text     = formatRelativeTimestamp(item.timestamp),
                        fontSize = 10.sp,
                        color    = SwaaraSecondaryText
                    )
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(SwaaraMagenta)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Title
                Text(
                    text       = item.title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = SwaaraPrimaryText,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Body
                Text(
                    text       = item.message,
                    fontSize   = 12.sp,
                    color      = SwaaraSecondaryText,
                    lineHeight = 17.sp,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )

                // Action buttons — text-only chips, no gradient fill
                val hasPlay    = !item.playQuery.isNullOrBlank()
                val hasExplore = !item.actionUrl.isNullOrBlank()
                if (hasPlay || hasExplore) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (hasPlay) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onPlayClick)
                                    .padding(horizontal = 0.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    painter           = painterResource(id = R.drawable.play),
                                    contentDescription = null,
                                    tint              = SwaaraMagenta,
                                    modifier          = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text       = "Play",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = SwaaraMagenta
                                )
                            }
                        }
                        if (hasExplore) {
                            Text(
                                text       = "Open",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color      = SwaaraCyan,
                                modifier   = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onExploreClick)
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Dismiss — quiet, top-right
            Box(
                modifier = Modifier
                    .padding(start = 6.dp, top = 2.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismissClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter           = painterResource(id = R.drawable.close),
                    contentDescription = "Dismiss",
                    tint              = SwaaraSecondaryText.copy(alpha = 0.4f),
                    modifier          = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Empty state — single-line, no decoration
// ---------------------------------------------------------------------------

@Composable
private fun EmptyState() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text     = "Nothing new yet",
            fontSize = 15.sp,
            color    = SwaaraSecondaryText
        )
    }
}

// ---------------------------------------------------------------------------
// Timestamp formatter
// ---------------------------------------------------------------------------

private fun formatRelativeTimestamp(timestamp: Long): String {
    val diff    = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
    val minutes = diff / 60_000
    val hours   = minutes / 60
    val days    = hours / 24

    return when {
        minutes < 1  -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24   -> "${hours}h ago"
        days == 1L   -> "Yesterday"
        days < 7     -> "${days}d ago"
        else         -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
