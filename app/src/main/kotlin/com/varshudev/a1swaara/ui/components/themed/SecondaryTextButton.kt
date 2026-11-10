package com.varshudev.a1swaara.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.utils.center
import com.varshudev.a1swaara.utils.disabled
import com.varshudev.a1swaara.utils.medium
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.primaryButton
import app.a1swaara.core.ui.utils.roundedShape

@Composable
fun SecondaryTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alternative: Boolean = false
) {
    val (colorPalette, typography) = LocalAppearance.current
    val shape = 20.dp.roundedShape
    val containerColor = when {
        alternative -> colorPalette.background0
        colorPalette.isDefault && colorPalette.isDark -> SwaaraElevatedSurface
        else -> colorPalette.primaryButton
    }

    BasicText(
        text = text,
        style = typography.xxs.medium.center.copy(
            color = if (enabled) colorPalette.text else colorPalette.textDisabled
        ),
        modifier = modifier
            .alpha(if (enabled) 1f else 0.55f)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, colorPalette.glassBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp)
    )
}
