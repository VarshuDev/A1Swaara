package com.varshudev.a1swaara.ui.screens.settings

import com.varshudev.a1swaara.BuildConfig

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varshudev.a1swaara.ui.screens.onboarding.SwaaraFeatureTourScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.ui.components.SwaaraObsidian
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraViolet
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.statusBarsPadding
import app.a1swaara.core.ui.utils.roundedShape

data class SettingCategory(
    val id: Int,
    val title: String,
    val description: String,
    val iconId: Int,
    val accentColor: Color
)

@Composable
fun SwaaraSettingsScreen(
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    var activeSubpageId by remember { mutableIntStateOf(-1) }
    var showTourOverlay by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            SettingCategory(
                id = 0,
                title = "Appearance & Theming",
                description = "Dark, AMOLED modes, colors, fonts & roundness",
                iconId = R.drawable.color_palette,
                accentColor = SwaaraMagenta
            ),
            SettingCategory(
                id = 1,
                title = "Audio & Playback",
                description = "Streaming quality, volume, equalizer & playback",
                iconId = R.drawable.play,
                accentColor = SwaaraViolet
            ),
            SettingCategory(
                id = 2,
                title = "Cache & Storage",
                description = "Audio cache limits, storage usage & cleanup",
                iconId = R.drawable.server,
                accentColor = SwaaraCyan
            ),
            SettingCategory(
                id = 3,
                title = "Backup & Data",
                description = "Export/Import backups & listening history",
                iconId = R.drawable.server,
                accentColor = SwaaraMagenta
            ),
            SettingCategory(
                id = 4,
                title = "Accounts & Sync",
                description = "Playlist synchronization & cloud streaming",
                iconId = R.drawable.sync,
                accentColor = SwaaraViolet
            ),
            SettingCategory(
                id = 5,
                title = "Advanced & Other",
                description = "Discord RPC, Android Auto, HTTP/3 & diagnostics",
                iconId = R.drawable.shapes,
                accentColor = SwaaraCyan
            ),
            SettingCategory(
                id = 6,
                title = "About A1 Swaara",
                description = "Developed by VarshuDev (@varshudev) · v${BuildConfig.VERSION_NAME} Official Release",
                iconId = R.drawable.information,
                accentColor = SwaaraMagenta
            )
        )
    }

    val rootModifier = if (onDismiss != null) {
        modifier
            .fillMaxSize()
            .background(SwaaraObsidian)
            .statusBarsPadding()
            .padding(bottom = 120.dp)
    } else {
        modifier
            .fillMaxSize()
            .background(SwaaraObsidian)
            .padding(bottom = 120.dp)
    }

    Box(
        modifier = rootModifier
    ) {
        if (activeSubpageId == -1) {
            // Root Settings Menu
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Header with Sovereign Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onDismiss != null) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SwaaraElevatedSurface)
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.chevron_back),
                                    contentDescription = "Back",
                                    tint = SwaaraPrimaryText,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Settings Studio",
                                color = SwaaraPrimaryText,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Acoustics, storage, visual identity & preferences",
                                color = SwaaraSecondaryText,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SwaaraMagenta.copy(alpha = 0.15f))
                            .border(1.dp, SwaaraMagenta.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = SwaaraMagenta,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Hero System Card: High Fidelity Audio & Sovereign Engine
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    SwaaraMagenta.copy(alpha = 0.20f),
                                    SwaaraViolet.copy(alpha = 0.12f),
                                    SwaaraDeepSurface
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(SwaaraMagenta.copy(alpha = 0.45f), SwaaraCyan.copy(alpha = 0.25f))),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SwaaraCyan)
                            )
                            Text(
                                text = "A1 SWAARA SOVEREIGN ENGINE",
                                color = SwaaraCyan,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Studio Master Acoustics & Zero Ad Telemetry",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "100% sovereign audio streaming with 320 kbps bitrates and gapless looping playback.",
                            color = SwaaraSecondaryText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Feature Tour Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SwaaraElevatedSurface)
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(SwaaraMagenta.copy(alpha = 0.5f), SwaaraCyan.copy(alpha = 0.5f))),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { showTourOverlay = true }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(SwaaraMagenta, SwaaraViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.hand_gesture),
                                contentDescription = "Feature Tour",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Feature Walkthrough & Tour",
                                color = SwaaraPrimaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Animated gesture guide showing what we did & how to use A1 Swaara",
                                color = SwaaraSecondaryText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Icon(
                            painter = painterResource(id = R.drawable.chevron_forward),
                            contentDescription = null,
                            tint = SwaaraCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category List Cards
                categories.forEach { cat ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SwaaraDeepSurface)
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                            .clickable { activeSubpageId = cat.id }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color-Coded Icon Container
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(cat.accentColor.copy(alpha = 0.14f))
                                    .border(1.dp, cat.accentColor.copy(alpha = 0.30f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = cat.iconId),
                                    contentDescription = cat.title,
                                    tint = cat.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.title,
                                    color = SwaaraPrimaryText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cat.description,
                                    color = SwaaraSecondaryText,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Subtle Navigation Chevron
                            Icon(
                                painter = painterResource(id = R.drawable.chevron_forward),
                                contentDescription = null,
                                tint = SwaaraSecondaryText.copy(alpha = 0.50f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Sub-Settings Page Container
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Sub-header with Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { activeSubpageId = -1 },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SwaaraElevatedSurface)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.chevron_back),
                            contentDescription = "Back",
                            tint = SwaaraPrimaryText,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = categories.find { it.id == activeSubpageId }?.title ?: "Settings",
                        color = SwaaraPrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Sub-Settings Screen Content
                Box(modifier = Modifier.fillMaxSize()) {
                    when (activeSubpageId) {
                        0 -> AppearanceSettings()
                        1 -> PlayerSettings()
                        2 -> CacheSettings()
                        3 -> DatabaseSettings()
                        4 -> AccountsAndSyncSubpage()
                        5 -> OtherSettings()
                        6 -> About()
                    }
                }
            }
        }

        if (showTourOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SwaaraObsidian)
            ) {
                SwaaraFeatureTourScreen(
                    onTourCompleted = { showTourOverlay = false }
                )
            }
        }
    }
}

@Composable
private fun AccountsAndSyncSubpage() {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(12.dp.roundedShape)
                    .background(if (selectedTab == 0) SwaaraMagenta.copy(alpha = 0.20f) else SwaaraDeepSurface)
                    .border(
                        1.dp,
                        if (selectedTab == 0) SwaaraMagenta.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.08f),
                        12.dp.roundedShape
                    )
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Swaara Cloud & Jam",
                    color = if (selectedTab == 0) SwaaraMagenta else SwaaraSecondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(12.dp.roundedShape)
                    .background(if (selectedTab == 1) SwaaraViolet.copy(alpha = 0.20f) else SwaaraDeepSurface)
                    .border(
                        1.dp,
                        if (selectedTab == 1) SwaaraViolet.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.08f),
                        12.dp.roundedShape
                    )
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Piped Account",
                    color = if (selectedTab == 1) SwaaraViolet else SwaaraSecondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> SwaaraCloudSyncScreen()
                1 -> SyncSettings()
            }
        }
    }
}
