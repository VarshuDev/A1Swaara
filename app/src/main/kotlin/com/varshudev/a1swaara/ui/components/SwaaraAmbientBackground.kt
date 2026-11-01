package com.varshudev.a1swaara.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.varshudev.a1swaara.utils.AmbientColors
import com.varshudev.a1swaara.utils.FastBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAmbientColors = staticCompositionLocalOf {
    AmbientColors(
        dominant = SwaaraViolet,
        vibrant = SwaaraMagenta,
        darkVibrant = SwaaraDeepSurface
    )
}

/**
 * Universal ambient fluid background that guarantees a smooth, gorgeous blur and atmospheric
 * dynamic palette lighting across all Android versions.
 *
 * Tier 1: Dynamic 3-orb morphing fluid mesh Canvas with organic drift and breathing pulsation.
 * Tier 2: CPU Fast StackBlur on downscaled bitmap for silky smooth diffusion on Android 11 and below.
 * Tier 3: Hardware RenderEffect blur layer on Android 12+ (API 31+).
 * Tier 4: Dark obsidian scrim and vignette for optimal text readability and contrast.
 */
@Composable
fun SwaaraAmbientBackground(
    artworkUri: Any?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    var blurredBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var ambientColors by remember {
        mutableStateOf(
            AmbientColors(
                dominant = SwaaraViolet,
                vibrant = SwaaraMagenta,
                darkVibrant = SwaaraDeepSurface
            )
        )
    }

    LaunchedEffect(artworkUri) {
        if (artworkUri == null || (artworkUri is String && artworkUri.isBlank())) {
            blurredBitmap = null
            ambientColors = AmbientColors(
                dominant = SwaaraViolet,
                vibrant = SwaaraMagenta,
                darkVibrant = SwaaraDeepSurface
            )
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val rawBitmap = result.image.toBitmap()
                    // Downscale to 48x48 for ultra-fast, smooth blur
                    val scaled = Bitmap.createScaledBitmap(rawBitmap, 48, 48, true)
                    val colors = FastBlur.extractAmbientColors(scaled)
                    val blurred = FastBlur.stackBlur(scaled, radius = 10, canReuse = true)
                    withContext(Dispatchers.Main) {
                        ambientColors = colors
                        blurredBitmap = blurred
                    }
                }
            } catch (_: Throwable) {
                // Keep default ambient colors on error
            }
        }
    }

    val animatedVibrant by animateColorAsState(
        targetValue = ambientColors.vibrant,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ambient_vibrant"
    )
    val animatedDominant by animateColorAsState(
        targetValue = ambientColors.dominant,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ambient_dominant"
    )
    val animatedDarkVibrant by animateColorAsState(
        targetValue = ambientColors.darkVibrant,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ambient_dark_vibrant"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_fluid_mesh")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val driftX by infiniteTransition.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_x"
    )

    val driftY by infiniteTransition.animateFloat(
        initialValue = -0.03f,
        targetValue = 0.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SwaaraObsidian)
    ) {
        // Tier 1: Universal 3-Orb Fluid Mesh (GPU-rendered with organic drift)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Top-center radiant vibrant orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedVibrant.copy(alpha = 0.44f),
                        animatedVibrant.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(
                        canvasW * (0.35f + driftX),
                        canvasH * (0.24f + driftY)
                    ),
                    radius = canvasW * 1.05f * pulseScale
                ),
                center = Offset(
                    canvasW * (0.35f + driftX),
                    canvasH * (0.24f + driftY)
                ),
                radius = canvasW * 1.05f * pulseScale
            )

            // Center-right dominant harmonic orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedDominant.copy(alpha = 0.38f),
                        animatedDominant.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(
                        canvasW * (0.72f - driftX),
                        canvasH * (0.58f - driftY)
                    ),
                    radius = canvasW * 0.95f * (2f - pulseScale)
                ),
                center = Offset(
                    canvasW * (0.72f - driftX),
                    canvasH * (0.58f - driftY)
                ),
                radius = canvasW * 0.95f * (2f - pulseScale)
            )

            // Lower-left deep accent orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedDarkVibrant.copy(alpha = 0.32f),
                        animatedDarkVibrant.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(
                        canvasW * (0.20f + driftY),
                        canvasH * (0.80f + driftX)
                    ),
                    radius = canvasW * 0.85f * pulseScale
                ),
                center = Offset(
                    canvasW * (0.20f + driftY),
                    canvasH * (0.80f + driftX)
                ),
                radius = canvasW * 0.85f * pulseScale
            )
        }

        // Tier 2 & 3: Blurred Bitmap Artwork (CPU StackBlur + optional API 31+ hardware blur)
        blurredBitmap?.let { bmp ->
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier
                    .fillMaxSize()
                    .blur(32.dp)
            } else {
                Modifier.fillMaxSize()
            }

            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = blurModifier.alpha(0.50f)
            )
        }

        // Tier 4: Dark obsidian gradient scrim to guarantee crisp foreground contrast (>= 4.5:1)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SwaaraObsidian.copy(alpha = 0.42f),
                            SwaaraObsidian.copy(alpha = 0.65f),
                            SwaaraObsidian.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // Provide ambient dynamic colors down the tree to NowPlaying and Lyrics
        CompositionLocalProvider(LocalAmbientColors provides ambientColors) {
            content()
        }
    }
}
