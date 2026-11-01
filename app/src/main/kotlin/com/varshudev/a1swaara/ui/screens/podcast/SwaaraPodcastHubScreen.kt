package com.varshudev.a1swaara.ui.screens.podcast

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraObsidian
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.screens.searchRoute

@Composable
fun SwaaraPodcastHubScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SwaaraObsidian)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            // Header Bar with Tactile Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val backInteractionSource = remember { MutableInteractionSource() }
                    val isBackPressed by backInteractionSource.collectIsPressedAsState()
                    val backScale by animateFloatAsState(
                        targetValue = if (isBackPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "back_scale"
                    )

                    Box(
                        modifier = Modifier
                            .scale(backScale)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SwaaraElevatedSurface)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                            .clickable(
                                interactionSource = backInteractionSource,
                                indication = null,
                                onClick = onDismiss
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.chevron_back),
                            contentDescription = "Back",
                            tint = SwaaraPrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Podcasts & Shows",
                                color = SwaaraPrimaryText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SwaaraSpeechWaveBars(color = SwaaraCyan)
                        }
                        Text(
                            text = "Talks, Masterclasses & Real Stories",
                            color = SwaaraSecondaryText,
                            fontSize = 12.sp
                        )
                    }
                }

                val searchInteractionSource = remember { MutableInteractionSource() }
                val isSearchPressed by searchInteractionSource.collectIsPressedAsState()
                val searchScale by animateFloatAsState(
                    targetValue = if (isSearchPressed) 0.88f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "search_scale"
                )

                Box(
                    modifier = Modifier
                        .scale(searchScale)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SwaaraElevatedSurface)
                        .border(1.dp, SwaaraCyan.copy(alpha = 0.35f), CircleShape)
                        .clickable(
                            interactionSource = searchInteractionSource,
                            indication = null,
                            onClick = { searchRoute.global("podcast") }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "Search Podcasts",
                        tint = SwaaraCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Embedded Dedicated Podcast Environment
            SwaaraPodcastEnvironment()
        }
    }
}
