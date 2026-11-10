package com.varshudev.a1swaara.ui.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.varshudev.a1swaara.R

data class SleepPreset(val label: String, val minutes: Long)

@Composable
fun SwaaraSleepTimerDialog(
    millisLeft: Long?,
    onSetTimer: (Long) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val presets = remember {
        listOf(
            SleepPreset("15 Mins", 15),
            SleepPreset("30 Mins", 30),
            SleepPreset("45 Mins", 45),
            SleepPreset("60 Mins", 60),
            SleepPreset("90 Mins", 90)
        )
    }

    val isRunning = (millisLeft ?: 0L) > 0L

    val formattedRemaining = remember(millisLeft) {
        if (millisLeft == null || millisLeft <= 0L) "Not active"
        else {
            val totalSeconds = millisLeft / 1000
            val mins = totalSeconds / 60
            val secs = totalSeconds % 60
            String.format("%02d:%02d left", mins, secs)
        }
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
                        .padding(22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SwaaraBrandGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.time),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Sleep Timer",
                                color = SwaaraPrimaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRunning) formattedRemaining else "Fade out & stop playback",
                                color = if (isRunning) SwaaraCyan else SwaaraSecondaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        presets.forEach { preset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SwaaraElevatedSurface)
                                    .border(1.dp, SwaaraBorderSubtle, RoundedCornerShape(14.dp))
                                    .clickable {
                                        onSetTimer(preset.minutes * 60 * 1000L)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = preset.label,
                                    color = SwaaraPrimaryText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.chevron_forward),
                                    contentDescription = null,
                                    tint = SwaaraSecondaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Close", color = SwaaraSecondaryText)
                        }
                        if (isRunning) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onCancelTimer()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SwaaraErrorRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Turn Off Timer", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
