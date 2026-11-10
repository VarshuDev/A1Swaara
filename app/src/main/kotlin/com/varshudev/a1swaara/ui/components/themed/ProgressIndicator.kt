package com.varshudev.a1swaara.ui.components.themed

import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import app.a1swaara.core.ui.LocalAppearance

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    strokeCap: StrokeCap? = null
) {
    val (colorPalette) = LocalAppearance.current

    if (progress == null) androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier,
        color = colorPalette.accent,
        strokeCap = strokeCap ?: StrokeCap.Round
    ) else androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier,
        color = colorPalette.accent,
        trackColor = colorPalette.text.copy(alpha = 0.14f),
        strokeCap = strokeCap ?: StrokeCap.Round,
        progress = { progress }
    )
}

@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    strokeCap: StrokeCap = StrokeCap.Round
) {
    val (colorPalette) = LocalAppearance.current

    if (progress == null) androidx.compose.material3.LinearProgressIndicator(
        modifier = modifier,
        color = colorPalette.accent,
        trackColor = colorPalette.text.copy(alpha = 0.12f),
        strokeCap = strokeCap
    ) else androidx.compose.material3.LinearProgressIndicator(
        modifier = modifier,
        color = colorPalette.accent,
        trackColor = colorPalette.text.copy(alpha = 0.12f),
        strokeCap = strokeCap,
        progress = { progress }
    )
}
