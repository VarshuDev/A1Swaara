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
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.varshudev.a1swaara.preferences.PlayerPreferences
import com.varshudev.a1swaara.utils.toast
import java.util.Locale
import kotlin.math.roundToInt

data class StudioPreset(
    val name: String,
    val description: String,
    val speed: Float,
    val pitch: Float,
    val reverb: PlayerPreferences.Reverb
)

private val STUDIO_PRESETS = listOf(
    StudioPreset(
        name = "Original",
        description = "Natural Master",
        speed = 1.00f,
        pitch = 1.00f,
        reverb = PlayerPreferences.Reverb.None
    ),
    StudioPreset(
        name = "Slowed + Reverb",
        description = "0.85x, Ambient Hall",
        speed = 0.85f,
        pitch = 0.90f,
        reverb = PlayerPreferences.Reverb.LargeHall
    ),
    StudioPreset(
        name = "Nightcore",
        description = "1.25x, Pitch Boost",
        speed = 1.25f,
        pitch = 1.20f,
        reverb = PlayerPreferences.Reverb.SmallRoom
    ),
    StudioPreset(
        name = "Vaporwave",
        description = "0.78x, Deep Decay",
        speed = 0.78f,
        pitch = 0.82f,
        reverb = PlayerPreferences.Reverb.LargeHall
    ),
    StudioPreset(
        name = "Sped Up",
        description = "1.30x Energy",
        speed = 1.30f,
        pitch = 1.00f,
        reverb = PlayerPreferences.Reverb.None
    ),
    StudioPreset(
        name = "Lofi Study",
        description = "0.92x, Muted Room",
        speed = 0.92f,
        pitch = 0.95f,
        reverb = PlayerPreferences.Reverb.MediumRoom
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwaaraStudioEffectsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentSpeed by remember { mutableFloatStateOf(PlayerPreferences.speed) }
    var currentPitch by remember { mutableFloatStateOf(PlayerPreferences.pitch) }
    var currentReverb by remember { mutableStateOf(PlayerPreferences.reverb) }

    fun applyPreset(preset: StudioPreset) {
        currentSpeed = preset.speed
        currentPitch = preset.pitch
        currentReverb = preset.reverb

        PlayerPreferences.speed = preset.speed
        PlayerPreferences.pitch = preset.pitch
        PlayerPreferences.reverb = preset.reverb
        context.toast("Applied ${preset.name}")
    }

    fun updateSpeed(newSpeed: Float) {
        val rounded = (newSpeed * 20).roundToInt() / 20f
        currentSpeed = rounded
        PlayerPreferences.speed = rounded
    }

    fun updatePitch(newPitch: Float) {
        val rounded = (newPitch * 20).roundToInt() / 20f
        currentPitch = rounded
        PlayerPreferences.pitch = rounded
    }

    fun updateReverb(newReverb: PlayerPreferences.Reverb) {
        currentReverb = newReverb
        PlayerPreferences.reverb = newReverb
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SwaaraDeepSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Studio Sound Lab",
                        color = SwaaraPrimaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Slowed + Reverb, Nightcore & Sonic Pitch",
                        color = SwaaraSecondaryText,
                        fontSize = 12.sp
                    )
                }

                // Reset Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SwaaraElevatedSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                        .clickable {
                            applyPreset(STUDIO_PRESETS.first())
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Reset",
                        color = SwaaraCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Presets Horizontal Carousel
            Text(
                text = "Preset Soundscapes",
                color = SwaaraPrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                STUDIO_PRESETS.forEach { preset ->
                    val isSelected = (currentSpeed == preset.speed) &&
                            (currentPitch == preset.pitch) &&
                            (currentReverb == preset.reverb)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) Brush.horizontalGradient(
                                    listOf(
                                        SwaaraMagenta.copy(alpha = 0.28f),
                                        SwaaraViolet.copy(alpha = 0.22f)
                                    )
                                ) else Brush.linearGradient(
                                    listOf(
                                        SwaaraElevatedSurface,
                                        SwaaraElevatedSurface
                                    )
                                )
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) SwaaraMagenta.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { applyPreset(preset) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = preset.name,
                                color = if (isSelected) Color.White else SwaaraPrimaryText,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = preset.description,
                                color = if (isSelected) SwaaraCyan else SwaaraSecondaryText,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Speed Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tempo / Speed",
                    color = SwaaraPrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = String.format(Locale.US, "%.2fx", currentSpeed),
                    color = SwaaraMagenta,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = currentSpeed,
                onValueChange = { updateSpeed(it) },
                valueRange = 0.50f..2.00f,
                steps = 29,
                colors = SliderDefaults.colors(
                    thumbColor = SwaaraMagenta,
                    activeTrackColor = SwaaraMagenta,
                    inactiveTrackColor = SwaaraElevatedSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pitch Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vocal Pitch",
                    color = SwaaraPrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = String.format(Locale.US, "%.2fx", currentPitch),
                    color = SwaaraViolet,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = currentPitch,
                onValueChange = { updatePitch(it) },
                valueRange = 0.70f..1.35f,
                steps = 25,
                colors = SliderDefaults.colors(
                    thumbColor = SwaaraViolet,
                    activeTrackColor = SwaaraViolet,
                    inactiveTrackColor = SwaaraElevatedSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Reverb Acoustic Rooms
            Text(
                text = "Atmospheric Reverb",
                color = SwaaraPrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            val reverbOptions = listOf(
                Pair("Off", PlayerPreferences.Reverb.None),
                Pair("Small Room", PlayerPreferences.Reverb.SmallRoom),
                Pair("Medium Room", PlayerPreferences.Reverb.MediumRoom),
                Pair("Large Hall", PlayerPreferences.Reverb.LargeHall),
                Pair("Cathedral", PlayerPreferences.Reverb.Plate)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reverbOptions.forEach { (label, option) ->
                    val isSelected = currentReverb == option

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) SwaaraCyan.copy(alpha = 0.22f)
                                else SwaaraElevatedSurface
                            )
                            .border(
                                1.dp,
                                if (isSelected) SwaaraCyan.copy(alpha = 0.70f)
                                else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { updateReverb(option) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else SwaaraSecondaryText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
