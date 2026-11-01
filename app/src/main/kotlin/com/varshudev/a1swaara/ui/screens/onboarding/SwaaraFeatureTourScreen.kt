package com.varshudev.a1swaara.ui.screens.onboarding

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varshudev.a1swaara.R

private val SwaaraObsidian = Color(0xFF07070B)
private val SwaaraDeepCard = Color(0xFF10101A)
private val SwaaraElevated = Color(0xFF161624)
private val SwaaraMagenta = Color(0xFFFF2DAA)
private val SwaaraPurple = Color(0xFF8B35FF)
private val SwaaraCyan = Color(0xFF20CFFF)
private val SwaaraTextPrimary = Color(0xFFF1F1F5)
private val SwaaraTextSecondary = Color(0xFF8E8EA0)

data class TourStep(
    val title: String,
    val subtitle: String,
    val highlightBadge: String,
    val description: String,
    val gestureTip: String
)

@Composable
fun SwaaraFeatureTourScreen(
    onTourCompleted: () -> Unit
) {
    val context = LocalContext.current
    var currentStepIndex by remember { mutableIntStateOf(0) }

    val tourSteps = remember {
        listOf(
            TourStep(
                title = "Indian Regional Charts",
                subtitle = "Kannada, Hindi, Tamil, Telugu, and Bollywood",
                highlightBadge = "REGIONAL SOVEREIGNTY",
                description = "Explore live chartbusters in 12 Indian regional languages with instant 320 KBPS CD-Quality playback and zero subscription fees.",
                gestureTip = "Tap any language filter pill on the Home screen to instantly pivot your entire music feed."
            ),
            TourStep(
                title = "Swipe-Only Player Navigation",
                subtitle = "Fluid gestures engineered against accidental taps",
                highlightBadge = "SMOOTH GESTURES",
                description = "Now Playing features gesture swipe architecture. Swipe right on the album art to play the next track; swipe left for previous track.",
                gestureTip = "Swipe left and right across the album art anywhere on screen to smoothly change songs."
            ),
            TourStep(
                title = "Synced Karaoke & Tap-to-Seek",
                subtitle = "Word-by-word synchronized lyrics with glowing glow",
                highlightBadge = "STUDIO LYRICS",
                description = "Experience full synchronized lyrics with glowing karaoke borders and live timestamp tags. Touch any line to seek ExoPlayer instantly.",
                gestureTip = "Tap any lyrics line to jump directly to that exact second in the track."
            ),
            TourStep(
                title = "Studio Sound Lab & Audio Quality",
                subtitle = "Switch Bitrates (320k/192k/128k), Slowed + Reverb & 10-Band EQ",
                highlightBadge = "ACOUSTIC SHAPING",
                description = "Tap the Audio Quality pill (320 KBPS / 192 KBPS / 128 KBPS) on the player to select your streaming bitrate. Fine-tune acoustics with Slowed + Reverb, Nightcore, and 10-band EQ.",
                gestureTip = "Tap the Audio Quality pill under the song title on the player anytime to change streaming bitrate."
            ),
            TourStep(
                title = "Smart Offline Hub & Car Mode",
                subtitle = "Zero-data playback and safe road interface",
                highlightBadge = "OFFLINE & DRIVING",
                description = "Keep listening anywhere with our dedicated Downloads Hub. Switch to distraction-free Car Driving Mode with oversized 86dp controls.",
                gestureTip = "Flip the Offline Mode switch in Library to play stored tracks with zero internet usage."
            )
        )
    }

    val completeTour: () -> Unit = {
        val prefs = context.getSharedPreferences("swaara_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("tour_completed", true).apply()
        onTourCompleted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SwaaraObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top Navigation Header: Step Badge + Skip Tour
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SwaaraElevated)
                    .border(1.dp, SwaaraCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "STEP ${currentStepIndex + 1} OF ${tourSteps.size}",
                    color = SwaaraCyan,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Skip Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .clickable { completeTour() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Skip Tour",
                        color = SwaaraTextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.chevron_forward),
                        contentDescription = "Skip",
                        tint = SwaaraTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step Progress Bar Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tourSteps.indices.forEach { index ->
                val isCurrent = index == currentStepIndex
                val isPassed = index < currentStepIndex
                Box(
                    modifier = Modifier
                        .weight(if (isCurrent) 2f else 1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when {
                                isCurrent -> Brush.horizontalGradient(listOf(SwaaraMagenta, SwaaraCyan))
                                isPassed -> Brush.horizontalGradient(listOf(SwaaraPurple.copy(alpha = 0.7f), SwaaraPurple.copy(alpha = 0.7f)))
                                else -> Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f)))
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Animated Slide Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentStepIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(tween(300)))
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut(tween(300)))
                    }
                },
                label = "tourSlideTransition"
            ) { stepIdx ->
                val step = tourSteps[stepIdx]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Interactive Feature Visualizer with Animated Hand Gesture
                    TourGestureVisualizer(
                        stepIndex = stepIdx,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Highlight Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                        .background(SwaaraMagenta.copy(alpha = 0.15f))
                        .border(1.dp, SwaaraMagenta.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = step.highlightBadge,
                            color = SwaaraMagenta,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title
                    Text(
                        text = step.title,
                        color = SwaaraTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subtitle
                    Text(
                        text = step.subtitle,
                        color = SwaaraCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Description
                    Text(
                        text = step.description,
                        color = SwaaraTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Gesture Tip Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SwaaraDeepCard)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(SwaaraCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.hand_gesture),
                                    contentDescription = null,
                                    tint = SwaaraCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = step.gestureTip,
                                color = SwaaraTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bottom Controls: Back + Next / Start Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button (hidden on first step)
            if (currentStepIndex > 0) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SwaaraElevated)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                        .clickable { currentStepIndex-- },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chevron_back),
                        contentDescription = "Back",
                        tint = SwaaraTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Main Action Button (Next or Get Started)
            val isFinalStep = currentStepIndex == tourSteps.size - 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black.copy(alpha = 0.8f),
                        spotColor = SwaaraMagenta.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                SwaaraMagenta,
                                SwaaraPurple,
                                SwaaraCyan
                            )
                        )
                    )
                    .clickable {
                        if (isFinalStep) {
                            completeTour()
                        } else {
                            currentStepIndex++
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isFinalStep) "Explore A1 Swaara" else "Next Step",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                    Icon(
                        painter = painterResource(id = if (isFinalStep) R.drawable.play else R.drawable.chevron_forward),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

/**
 * Renders an animated interactive mockup with a floating hand gesture for each step.
 */
@Composable
private fun TourGestureVisualizer(
    stepIndex: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "handGesture")

    // Gesture Animations
    val horizontalSweep by infiniteTransition.animateFloat(
        initialValue = -70f,
        targetValue = 70f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )

    val verticalBob by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(SwaaraDeepCard)
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        when (stepIndex) {
            0 -> {
                // Step 1: Regional Discovery Chips Mockup
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "EXPLORE BY LANGUAGE",
                        color = SwaaraCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(SwaaraMagenta, SwaaraPurple)))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Kannada (ಕನ್ನಡ)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SwaaraElevated)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Hindi (हिंदी)", color = SwaaraTextSecondary, fontSize = 12.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SwaaraElevated)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Tamil (தமிழ்)", color = SwaaraTextSecondary, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SwaaraElevated)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Telugu (తెలుగు)", color = SwaaraTextSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Animated Pointing Hand Gesture with Ripple Ring
                    Box(contentAlignment = Alignment.Center) {
                        // Expanding Touch Ripple
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .scale(rippleScale)
                                .background(SwaaraCyan.copy(alpha = rippleAlpha), CircleShape)
                        )

                        // Hand Icon
                        Icon(
                            painter = painterResource(id = R.drawable.hand_gesture),
                            contentDescription = "Tap gesture",
                            tint = SwaaraCyan,
                            modifier = Modifier
                                .size(36.dp)
                                .offset(y = verticalBob.dp)
                        )
                    }
                }
            }

            1 -> {
                // Step 2: Swipe-to-Skip Album Card Mockup
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .shadow(24.dp, RoundedCornerShape(20.dp), ambientColor = SwaaraPurple, spotColor = SwaaraMagenta)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(SwaaraMagenta.copy(alpha = 0.6f), SwaaraPurple.copy(alpha = 0.8f))))
                            .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.disc),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(64.dp)
                        )

                        // Dynamic Swipe Navigation Gesture Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(painterResource(R.drawable.play_skip_back), null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            Icon(painterResource(R.drawable.play_skip_forward), null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Animated Hand Gliding Left-to-Right
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Motion Trail Line
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .height(2.dp)
                                .background(Brush.horizontalGradient(listOf(SwaaraMagenta, SwaaraCyan)))
                        )

                        // Gliding Hand
                        Icon(
                            painter = painterResource(id = R.drawable.hand_gesture),
                            contentDescription = "Swipe gesture",
                            tint = Color.White,
                            modifier = Modifier
                                .size(32.dp)
                                .offset { IntOffset(x = horizontalSweep.dp.roundToPx(), y = 0) }
                        )
                    }
                }
            }

            2 -> {
                // Step 3: Synced Karaoke Lyrics Line-Seek Mockup
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("LIVE KARAOKE STREAM", color = SwaaraMagenta, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    // Line 1 (Past)
                    Text("I was caught in the middle of a dream", color = SwaaraTextSecondary.copy(alpha = 0.5f), fontSize = 12.5.sp)

                    // Line 2 (Active Synced Capsule)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SwaaraMagenta.copy(alpha = 0.22f))
                            .border(1.dp, SwaaraMagenta.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sing along with high-fidelity sound",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "[01:24]",
                                color = SwaaraCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Line 3 (Upcoming)
                    Text("Lost in the frequency of the night", color = SwaaraTextSecondary.copy(alpha = 0.5f), fontSize = 12.5.sp)

                    Spacer(modifier = Modifier.height(6.dp))

                    // Tap to seek gesture
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .scale(rippleScale)
                                .background(SwaaraMagenta.copy(alpha = rippleAlpha), CircleShape)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.hand_gesture),
                            contentDescription = "Tap to seek",
                            tint = SwaaraMagenta,
                            modifier = Modifier
                                .size(32.dp)
                                .offset(y = verticalBob.dp)
                        )
                    }
                }
            }

            3 -> {
                // Step 4: Studio Sound Lab Mockup
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("STUDIO SOUND LAB PRESETS", color = SwaaraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(listOf(SwaaraPurple, SwaaraMagenta)))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Slowed + Reverb", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SwaaraElevated)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Nightcore", color = SwaaraTextSecondary, fontSize = 11.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Speed Slider Mockup
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Playback Speed", color = SwaaraTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("0.85x", color = SwaaraCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(6.dp)
                                .background(Brush.horizontalGradient(listOf(SwaaraCyan, SwaaraMagenta)))
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Icon(
                        painter = painterResource(id = R.drawable.hand_gesture),
                        contentDescription = "Slider adjustment gesture",
                        tint = SwaaraCyan,
                        modifier = Modifier
                            .size(32.dp)
                            .offset { IntOffset(x = horizontalSweep.dp.roundToPx() / 2, y = 0) }
                    )
                }
            }

            4 -> {
                // Step 5: Smart Offline & Car Mode Mockup
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("OFFLINE HUB & CAR MODE", color = SwaaraMagenta, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Offline Tile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SwaaraCyan.copy(alpha = 0.15f))
                                .border(1.dp, SwaaraCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(painterResource(R.drawable.download), null, tint = SwaaraCyan, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Offline Only", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Zero Data", color = SwaaraCyan, fontSize = 10.sp)
                            }
                        }

                        // Car Mode Tile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SwaaraMagenta.copy(alpha = 0.15f))
                                .border(1.dp, SwaaraMagenta.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(painterResource(R.drawable.car), null, tint = SwaaraMagenta, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Car Mode", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Safe Driving", color = SwaaraMagenta, fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .scale(rippleScale)
                                .background(SwaaraCyan.copy(alpha = rippleAlpha), CircleShape)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.hand_gesture),
                            contentDescription = "Switch gesture",
                            tint = Color.White,
                            modifier = Modifier
                                .size(32.dp)
                                .offset(y = verticalBob.dp)
                        )
                    }
                }
            }
        }
    }
}
