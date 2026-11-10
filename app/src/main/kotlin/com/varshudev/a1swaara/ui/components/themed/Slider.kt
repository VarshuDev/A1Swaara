package com.varshudev.a1swaara.ui.components.themed

import androidx.annotation.IntRange
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.secondaryAccent

@Composable
fun Slider(
    state: Float,
    setState: (Float) -> Unit,
    onSlideComplete: () -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    @IntRange(from = 0) steps: Int = 0
) {
    val (colorPalette) = LocalAppearance.current

    androidx.compose.material3.Slider(
        value = state,
        onValueChange = setState,
        onValueChangeFinished = onSlideComplete,
        valueRange = range,
        modifier = modifier,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = if (colorPalette.isDefault) colorPalette.secondaryAccent else colorPalette.accent,
            activeTrackColor = colorPalette.accent,
            inactiveTrackColor = colorPalette.text.copy(alpha = 0.18f),
            activeTickColor = colorPalette.onAccent.copy(alpha = 0.5f),
            inactiveTickColor = colorPalette.text.copy(alpha = 0.3f)
        )
    )
}
