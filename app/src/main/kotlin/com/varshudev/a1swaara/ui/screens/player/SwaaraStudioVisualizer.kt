package com.varshudev.a1swaara.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.audio.SwaaraAudioEngine
import com.varshudev.a1swaara.audio.SwaaraVisualizerEngine
import com.varshudev.a1swaara.audio.VisualizerFrame
import com.varshudev.a1swaara.ui.components.SwaaraColors
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.SwaaraEmerald
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraObsidian
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.components.SwaaraViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class VisualizerStyle(val label: String) {
    NEON_SPECTRUM("Neon Spectrum"),
    FLUID_WAVE("Fluid Wave"),
    ANALOG_VU("Analog VU")
}

@Composable
fun SwaaraStudioVisualizer(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val frame by SwaaraVisualizerEngine.frame.collectAsState()
    val isHardware by SwaaraVisualizerEngine.isHardwareActive.collectAsState()
    val isTubeWarmth by SwaaraAudioEngine.isTubeWarmthActive.collectAsState()
    val isKaraoke by SwaaraAudioEngine.isKaraokeMode.collectAsState()

    var selectedStyle by remember { mutableStateOf(VisualizerStyle.NEON_SPECTRUM) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Studio Telemetry Pill & Style Switcher
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Master DAC Status Strip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(SwaaraDeepSurface)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(SwaaraEmerald.copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = if (isHardware) "32-bit Float DAC • 48 kHz • 320 kbps" else "Studio Master Engine • 320 kbps DSP",
                        color = SwaaraSecondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Style Switcher Tabs
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SwaaraDeepSurface)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VisualizerStyle.entries.forEach { style ->
                    val isSelected = style == selectedStyle

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SwaaraMagenta else Color.Transparent)
                            .clickable { selectedStyle = style }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = style.label,
                            color = if (isSelected) Color.White else SwaaraSecondaryText,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Main Visualizer Stage Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SwaaraDeepSurface),
            contentAlignment = Alignment.Center
        ) {
            when (selectedStyle) {
                VisualizerStyle.NEON_SPECTRUM -> NeonSpectrumCanvas(frame = frame)
                VisualizerStyle.FLUID_WAVE -> FluidWaveCanvas(frame = frame)
                VisualizerStyle.ANALOG_VU -> AnalogVuCanvas(frame = frame)
            }
        }

        // 3. Studio Master DSP Controls (Tube Warmth & Karaoke SingAlong)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tube Warmth Toggle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isTubeWarmth) Color(0xFFE88A2E).copy(alpha = 0.18f) else SwaaraDeepSurface
                    )
                    .clickable { SwaaraAudioEngine.setTubeWarmthActive(!isTubeWarmth) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isTubeWarmth) Color(0xFFE88A2E) else Color(0xFF555B77))
                    )
                    Column {
                        Text(
                            text = "Analog Tube Warmth",
                            color = if (isTubeWarmth) SwaaraPrimaryText else SwaaraSecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isTubeWarmth) "Even Harmonics ON" else "Bypass Clean",
                            color = if (isTubeWarmth) Color(0xFFFFB86B) else Color(0xFF707694),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Karaoke Vocal Suppressor Toggle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isKaraoke) SwaaraCyan.copy(alpha = 0.18f) else SwaaraDeepSurface
                    )
                    .clickable { SwaaraAudioEngine.setKaraokeMode(!isKaraoke) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isKaraoke) SwaaraCyan else Color(0xFF555B77))
                    )
                    Column {
                        Text(
                            text = "Karaoke SingAlong",
                            color = if (isKaraoke) SwaaraPrimaryText else SwaaraSecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isKaraoke) "Vocals Suppressed" else "Full Vocal Track",
                            color = if (isKaraoke) SwaaraCyan else Color(0xFF707694),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeonSpectrumCanvas(frame: VisualizerFrame) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val width = size.width
        val height = size.height
        val count = frame.magnitudes.size
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (count - 1)
        val barWidth = ((width - totalSpacing) / count).coerceAtLeast(2.dp.toPx())

        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                SwaaraCyan,
                SwaaraViolet,
                SwaaraMagenta
            ),
            startY = 0f,
            endY = height
        )

        for (i in 0 until count) {
            val mag = frame.magnitudes[i].coerceIn(0.04f, 1.0f)
            val barHeight = height * mag
            val x = i * (barWidth + barSpacing)
            val y = height - barHeight

            // Draw glowing bar
            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Draw floating peak hold needle
            val peak = frame.peakHold.getOrNull(i)?.coerceIn(0.04f, 1.0f) ?: mag
            val peakY = (height - (height * peak)).coerceIn(0f, height - 3.dp.toPx())

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(x, peakY),
                size = Size(barWidth, 2.5.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun FluidWaveCanvas(frame: VisualizerFrame) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val points = frame.waveform
        if (points.isEmpty()) return@Canvas

        val stepX = width / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        val glowPath = Path()

        for (i in points.indices) {
            val sample = points[i].coerceIn(-1.0f, 1.0f)
            val x = i * stepX
            val y = centerY + sample * (height * 0.42f)

            if (i == 0) {
                path.moveTo(x, y)
                glowPath.moveTo(x, y)
            } else {
                val prevSample = points[i - 1].coerceIn(-1.0f, 1.0f)
                val prevX = (i - 1) * stepX
                val prevY = centerY + prevSample * (height * 0.42f)
                val midX = (prevX + x) / 2f
                val midY = (prevY + y) / 2f
                path.quadraticBezierTo(prevX, prevY, midX, midY)
                glowPath.quadraticBezierTo(prevX, prevY, midX, midY)
            }
        }

        // Draw ambient glow stroke
        drawPath(
            path = glowPath,
            brush = Brush.horizontalGradient(listOf(SwaaraCyan, SwaaraViolet, SwaaraMagenta)),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw core crisp white/cyan line
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AnalogVuCanvas(frame: VisualizerFrame) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val width = size.width
        val height = size.height
        val meterWidth = (width - 16.dp.toPx()) / 2f
        val meterHeight = height

        // Draw Left & Right Analog Meter Dials
        drawAnalogMeter(
            label = "CH-L (LEFT)",
            vu = frame.leftVu,
            topLeft = Offset(0f, 0f),
            size = Size(meterWidth, meterHeight)
        )

        drawAnalogMeter(
            label = "CH-R (RIGHT)",
            vu = frame.rightVu,
            topLeft = Offset(meterWidth + 16.dp.toPx(), 0f),
            size = Size(meterWidth, meterHeight)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAnalogMeter(
    label: String,
    vu: Float,
    topLeft: Offset,
    size: Size
) {
    val x0 = topLeft.x
    val y0 = topLeft.y
    val w = size.width
    val h = size.height

    // Dial background
    drawRoundRect(
        color = Color(0xFF14120E),
        topLeft = Offset(x0, y0),
        size = Size(w, h),
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
    )

    // Inner dial bezel
    drawRoundRect(
        color = Color(0xFF28241D),
        topLeft = Offset(x0 + 4.dp.toPx(), y0 + 4.dp.toPx()),
        size = Size(w - 8.dp.toPx(), h - 8.dp.toPx()),
        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )

    // Needle pivot origin at bottom center
    val pivotX = x0 + w / 2f
    val pivotY = y0 + h * 0.90f
    val needleLength = h * 0.72f

    // Arc scale (-45 degrees to +45 degrees)
    val startAngle = -45f
    val endAngle = 45f
    val angle = (startAngle + (endAngle - startAngle) * vu.coerceIn(0f, 1f))
    val rad = (angle - 90f) * (PI / 180f)

    val needleTipX = pivotX + needleLength * cos(rad).toFloat()
    val needleTipY = pivotY + needleLength * sin(rad).toFloat()

    // Draw needle
    drawLine(
        color = if (vu > 0.85f) Color(0xFFFF3344) else Color(0xFFFFB86B),
        start = Offset(pivotX, pivotY),
        end = Offset(needleTipX, needleTipY),
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Draw pivot cap
    drawCircle(
        color = Color(0xFFD6A76E),
        radius = 5.dp.toPx(),
        center = Offset(pivotX, pivotY)
    )

    // Redline peak indicator LED
    val isPeaking = vu > 0.88f
    drawCircle(
        color = if (isPeaking) Color(0xFFFF2233) else Color(0xFF4A151C),
        radius = 3.5.dp.toPx(),
        center = Offset(x0 + w - 14.dp.toPx(), y0 + 14.dp.toPx())
    )
}
