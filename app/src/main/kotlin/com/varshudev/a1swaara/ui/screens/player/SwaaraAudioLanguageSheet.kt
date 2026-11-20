package com.varshudev.a1swaara.ui.screens.player

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.extractor.SwaaraAudioTrackOption
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraEmerald
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.components.SwaaraViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwaaraAudioLanguageSheet(
    tracks: List<SwaaraAudioTrackOption>,
    selectedTrackKey: String?,
    onSelectTrack: (SwaaraAudioTrackOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0C0D14),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2A2D42))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Audio Language Track",
                color = SwaaraPrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Switch audio language seamlessly without losing your playback timestamp",
                color = SwaaraSecondaryText,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF131522))
                        .border(1.dp, Color(0xFF22263C), RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Only the original audio track is available for this episode.",
                        color = SwaaraSecondaryText,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tracks, key = { it.trackKey }) { track ->
                        val isSelected = track.trackKey == selectedTrackKey ||
                            (selectedTrackKey == null && track == tracks.firstOrNull())

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                SwaaraMagenta.copy(alpha = 0.22f),
                                                SwaaraViolet.copy(alpha = 0.16f)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF131522), Color(0xFF131522))
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) SwaaraMagenta.copy(alpha = 0.65f) else Color(0xFF22263C),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    onSelectTrack(track)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) SwaaraMagenta.copy(alpha = 0.30f)
                                            else Color(0xFF1C1F32)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.radio),
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else SwaaraCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = track.displayName,
                                            color = SwaaraPrimaryText,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (track.isOriginal) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(SwaaraCyan.copy(alpha = 0.16f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "ORIGINAL",
                                                    color = SwaaraCyan,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }

                                    val specText = buildString {
                                        track.codec?.let { append(it) }
                                        if (track.bitrateKbps > 0) {
                                            if (isNotEmpty()) append(" · ")
                                            append("${track.bitrateKbps} kbps")
                                        }
                                        if (track.isDubbed) {
                                            if (isNotEmpty()) append(" · ")
                                            append("AI / Studio Dub")
                                        }
                                    }.ifBlank { "High-Fidelity Stream" }

                                    Text(
                                        text = specText,
                                        color = SwaaraSecondaryText,
                                        fontSize = 11.sp
                                    )
                                }

                                if (isSelected) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SwaaraEmerald.copy(alpha = 0.16f))
                                            .border(1.dp, SwaaraEmerald.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(SwaaraEmerald)
                                        )
                                        Text(
                                            text = "Active",
                                            color = SwaaraEmerald,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
