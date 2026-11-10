package com.varshudev.a1swaara.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.varshudev.a1swaara.R
import coil3.compose.AsyncImage

@Composable
fun SwaaraStoryCardDialog(
    songTitle: String,
    artistName: String,
    artworkUri: Uri?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val shareStoryText = "Listening to $songTitle by $artistName on A1 Swaara"

    val shareIntent = {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, songTitle)
            putExtra(Intent.EXTRA_TEXT, shareStoryText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Song"))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            SwaaraGlassContainer(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = SwaaraModalSurface,
                borderBrush = Brush.linearGradient(
                    listOf(
                        SwaaraMagenta.copy(alpha = 0.50f),
                        SwaaraViolet.copy(alpha = 0.30f),
                        SwaaraCyan.copy(alpha = 0.45f)
                    )
                ),
                borderWidth = 1.2.dp,
                shadowElevation = 24.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share_social),
                            contentDescription = null,
                            tint = SwaaraMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Share Story Card",
                            color = SwaaraPrimaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Story Sticker Card Preview
                    SwaaraGlassContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = SwaaraElevatedSurface,
                        borderColor = SwaaraMagenta.copy(alpha = 0.45f),
                        borderWidth = 1.2.dp,
                        shadowElevation = 18.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Artwork with Neon Glow
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = SwaaraMagenta, spotColor = SwaaraViolet)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SwaaraDeepSurface)
                            ) {
                                AsyncImage(
                                    model = artworkUri,
                                    contentDescription = songTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Waveform Graphic
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(24.dp)
                            ) {
                                val bars = listOf(8, 14, 22, 12, 18, 24, 10, 16, 20, 14, 8, 18, 22, 12)
                                bars.forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Brush.verticalGradient(listOf(SwaaraMagenta, SwaaraCyan)))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = songTitle,
                                color = SwaaraPrimaryText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = artistName,
                                color = SwaaraCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // App Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.musical_notes),
                                    contentDescription = null,
                                    tint = SwaaraMagenta,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "A1 SWAARA",
                                    color = SwaaraPrimaryText.copy(alpha = 0.85f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = SwaaraSecondaryText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                shareIntent()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SwaaraViolet),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Share Card", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
