package com.varshudev.a1swaara.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.SwaaraErrorRed
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.utils.center
import com.varshudev.a1swaara.utils.color
import com.varshudev.a1swaara.utils.isInPip
import com.varshudev.a1swaara.utils.medium
import app.a1swaara.core.ui.LocalAppearance

@Composable
fun PlaybackError(
    isDisplayed: Boolean,
    messageProvider: @Composable () -> String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = onDismiss,
    onSkipNext: (() -> Unit)? = null
) = Box(modifier = modifier) {
    val (_, typography) = LocalAppearance.current
    val message by rememberUpdatedState(newValue = messageProvider())
    val pip = isInPip()

    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Spacer(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                }
                .fillMaxSize()
                .background(Color.Black.copy(0.8f))
        )
    }

    AnimatedContent(
        targetState = message.takeIf { isDisplayed },
        transitionSpec = {
            ContentTransform(
                targetContentEnter = slideInVertically { -it },
                initialContentExit = slideOutVertically { -it },
                sizeTransform = null
            )
        },
        label = "",
        modifier = Modifier.fillMaxWidth()
    ) { currentMessage ->
        if (currentMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SwaaraDeepSurface)
                    .border(1.dp, SwaaraErrorRed.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    text = currentMessage,
                    style = typography.xs.center.medium.color(SwaaraPrimaryText),
                    maxLines = if (pip) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )
                if (!pip && (onRetry != null || onSkipNext != null)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onRetry != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SwaaraMagenta.copy(alpha = 0.18f))
                                    .border(1.dp, SwaaraMagenta.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                    .clickable { onRetry() }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                BasicText(
                                    text = "Retry",
                                    style = typography.xs.copy(
                                        color = SwaaraPrimaryText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        if (onSkipNext != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SwaaraCyan.copy(alpha = 0.14f))
                                    .border(1.dp, SwaaraCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                    .clickable { onSkipNext() }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                BasicText(
                                    text = "Skip Next",
                                    style = typography.xs.copy(
                                        color = SwaaraCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
