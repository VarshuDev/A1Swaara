package com.varshudev.a1swaara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.audio.SwaaraAudioEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwaaraEqualizerSheet(
    audioSessionId: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(audioSessionId) {
        if (audioSessionId > 0) {
            SwaaraAudioEngine.bindAudioSession(context, audioSessionId)
        }
    }

    val isEqEnabled by SwaaraAudioEngine.isEqEnabled.collectAsState()
    val selectedPresetIndex by SwaaraAudioEngine.presetIndex.collectAsState()
    val bandLevels by SwaaraAudioEngine.bandLevels.collectAsState()
    val bassBoostLevel by SwaaraAudioEngine.bassBoostLevel.collectAsState()
    val virtualizerLevel by SwaaraAudioEngine.virtualizerLevel.collectAsState()

    val presets = SwaaraAudioEngine.DEFAULT_PRESETS
    val bandFrequencies = SwaaraAudioEngine.BAND_FREQUENCIES

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SwaaraObsidian,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.20f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.musical_notes),
                        contentDescription = null,
                        tint = SwaaraMagenta,
                        modifier = Modifier.size(24.dp)
                    )

                    Column {
                        Text(
                            text = "Studio Equalizer",
                            color = SwaaraPrimaryText,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Parametric frequency bands & acoustic room acoustics",
                            color = SwaaraSecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Switch(
                    checked = isEqEnabled,
                    onCheckedChange = { SwaaraAudioEngine.setEqEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SwaaraMagenta,
                        uncheckedThumbColor = SwaaraSecondaryText,
                        uncheckedTrackColor = SwaaraElevatedSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Presets Selector Chips
            Text(
                text = "Acoustic Presets",
                color = SwaaraPrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEachIndexed { idx, preset ->
                    val isSelected = selectedPresetIndex == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) SwaaraMagenta
                                else SwaaraElevatedSurface
                            )
                            .clickable {
                                SwaaraAudioEngine.setPreset(idx)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.name,
                            color = if (isSelected) Color.White else SwaaraSecondaryText,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5-Band Slider Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SwaaraDeepSurface)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frequency Bands",
                            color = SwaaraPrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEqEnabled) "Active" else "Bypassed",
                            color = if (isEqEnabled) SwaaraMagenta else SwaaraSecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    bandFrequencies.forEachIndexed { bandIdx, freqName ->
                        val currentDb = bandLevels.getOrElse(bandIdx) { 0f }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = freqName,
                                color = SwaaraPrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(65.dp)
                            )

                            Slider(
                                value = currentDb,
                                onValueChange = { newVal ->
                                    SwaaraAudioEngine.setBandLevel(bandIdx, newVal)
                                },
                                valueRange = -10f..10f,
                                enabled = isEqEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = SwaaraMagenta,
                                    activeTrackColor = SwaaraMagenta,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.10f)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "${if (currentDb > 0) "+" else ""}${currentDb.toInt()} dB",
                                color = if (currentDb > 0) SwaaraCyan else SwaaraSecondaryText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(48.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bass Boost & 3D Spatial Surround Sliders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bass Boost Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SwaaraDeepSurface)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Bass Boost",
                            color = SwaaraMagenta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(bassBoostLevel / 10).toInt()}%",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = bassBoostLevel,
                            onValueChange = { SwaaraAudioEngine.setBassBoost(it) },
                            valueRange = 0f..1000f,
                            enabled = isEqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = SwaaraMagenta,
                                activeTrackColor = SwaaraMagenta,
                                inactiveTrackColor = Color.White.copy(alpha = 0.10f)
                            )
                        )
                    }
                }

                // 3D Spatial Virtualizer Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SwaaraDeepSurface)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "3D Spatial",
                            color = SwaaraCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(virtualizerLevel / 10).toInt()}%",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = virtualizerLevel,
                            onValueChange = { SwaaraAudioEngine.setVirtualizer(it) },
                            valueRange = 0f..1000f,
                            enabled = isEqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = SwaaraCyan,
                                activeTrackColor = SwaaraCyan,
                                inactiveTrackColor = Color.White.copy(alpha = 0.10f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
