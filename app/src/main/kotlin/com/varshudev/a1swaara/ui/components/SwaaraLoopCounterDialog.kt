package com.varshudev.a1swaara.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.player.SwaaraLoopCounter

/**
 * High-precision tactile loop counter modal designed with UI/UX Pro Max principles.
 * Features animated stepper buttons (+ / -), quick preset pills, and glowing glassmorphic aesthetics.
 */
@Composable
fun SwaaraLoopCounterDialog(
    player: Player?,
    onDismiss: () -> Unit
) {
    val initialCount = SwaaraLoopCounter.remainingCount.value
    var selectedCount by remember { mutableIntStateOf(if (initialCount > 0) initialCount else 3) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .background(SwaaraDeepSurface)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Icon
                Icon(
                    painter = painterResource(id = R.drawable.sync),
                    contentDescription = "Loop Counter",
                    tint = SwaaraMagenta,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Custom Loop Repeater",
                    color = SwaaraPrimaryText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Set how many times this song should loop",
                    color = SwaaraSecondaryText,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tactile Stepper Unit (- [Count] +)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minus Button
                    StepperButton(
                        text = "-",
                        enabled = selectedCount > 1,
                        isPrimary = false,
                        onClick = {
                            if (selectedCount > 1) selectedCount--
                        }
                    )

                    // Counter Display with Animated Number Flip
                    Box(
                        modifier = Modifier
                            .height(84.dp)
                            .width(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SwaaraElevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedContent(
                                targetState = selectedCount,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                            slideOutVertically { height -> -height } + fadeOut()
                                        )
                                    } else {
                                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                                            slideOutVertically { height -> height } + fadeOut()
                                        )
                                    }
                                },
                                label = "counter_flip"
                            ) { count ->
                                Text(
                                    text = "$count",
                                    color = Color.White,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 38.sp
                                )
                            }
                            Text(
                                text = if (selectedCount == 1) "repeat" else "repeats",
                                color = SwaaraSecondaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Plus Button
                    StepperButton(
                        text = "+",
                        enabled = selectedCount < 99,
                        isPrimary = true,
                        onClick = {
                            if (selectedCount < 99) selectedCount++
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Preset Pills (1x, 2x, 3x, 5x, 10x)
                Text(
                    text = "Quick Presets",
                    color = SwaaraPrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    val presets = listOf(1, 2, 3, 5, 10)
                    presets.forEach { preset ->
                        val isSelected = selectedCount == preset

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) SwaaraMagenta
                                    else SwaaraElevatedSurface
                                )
                                .clickable { selectedCount = preset }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset}x",
                                color = if (isSelected) Color.White else SwaaraSecondaryText,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Actions: Cancel / Reset & Apply Loop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (initialCount > 0) {
                        // Clear Loop Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SwaaraElevatedSurface)
                                .clickable {
                                    SwaaraLoopCounter.clear(player)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel Loop",
                                color = Color(0xFFEF4444),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Dismiss Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SwaaraElevatedSurface)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = SwaaraSecondaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SwaaraMagenta)
                            .clickable {
                                SwaaraLoopCounter.setLoopCount(selectedCount, player)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loop Track (${selectedCount}x)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperButton(
    text: String,
    enabled: Boolean,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isPrimary && enabled) SwaaraMagenta
                else SwaaraElevatedSurface
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                isPressed = true
                onClick()
                isPressed = false
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = when {
                !enabled -> Color.White.copy(alpha = 0.25f)
                isPrimary -> Color.White
                else -> SwaaraPrimaryText
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp
        )
    }
}
