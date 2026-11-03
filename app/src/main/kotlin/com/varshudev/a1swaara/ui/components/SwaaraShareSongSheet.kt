package com.varshudev.a1swaara.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.utils.toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwaaraShareSongSheet(
    title: String,
    artist: String,
    artworkUrl: String?,
    videoId: String?,
    onOpenStoryCreator: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shareUrl = if (!videoId.isNullOrBlank()) "https://youtube.com/watch?v=$videoId" else "https://github.com/VarshuDev/A1Swaara_apk"
    val deepLinkUrl = "swaara://song?id=${videoId.orEmpty()}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SwaaraDeepSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Share Song",
                        color = SwaaraPrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Instant deep link and social sharing",
                        color = SwaaraSecondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track Preview Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF14141E))
                    .border(1.dp, Color(0xFF262636), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = artworkUrl ?: R.drawable.app_icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = SwaaraPrimaryText,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = artist,
                        color = SwaaraSecondaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "320 KBPS CD-QUALITY AUDIO",
                        color = SwaaraCyan,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.05.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // URL Copy Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A26))
                    .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_link),
                        contentDescription = null,
                        tint = SwaaraCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = shareUrl,
                        color = SwaaraPrimaryText,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SwaaraCyan.copy(alpha = 0.15f))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("A1 Swaara Song Link", shareUrl)
                            clipboard.setPrimaryClip(clip)
                            context.toast(context.getString(R.string.share_link_copied))
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = null,
                            tint = SwaaraCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Copy",
                            color = SwaaraCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Share Link via Apps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismiss()
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Listen to \"$title\" by $artist on A1 Swaara:\n$shareUrl"
                            )
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Track via..."))
                    }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.share_social),
                    contentDescription = null,
                    tint = SwaaraMagenta,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "Share Link to Apps",
                        color = SwaaraPrimaryText,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Send direct web & deep link to WhatsApp, Telegram, or Messages",
                        color = SwaaraSecondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            // Option 2: Share 9:16 Visual Story Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismiss()
                        onOpenStoryCreator()
                    }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.disc),
                    contentDescription = null,
                    tint = SwaaraViolet,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "Create Visual Story Card",
                        color = SwaaraPrimaryText,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Export stylized 9:16 Instagram Story image or MP4 clip with lyrics",
                        color = SwaaraSecondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
