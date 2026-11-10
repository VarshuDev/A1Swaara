package com.varshudev.a1swaara.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraViolet
import com.varshudev.a1swaara.utils.color
import com.varshudev.a1swaara.utils.disabled
import com.varshudev.a1swaara.utils.medium
import com.varshudev.a1swaara.utils.primary
import com.varshudev.a1swaara.utils.semiBold
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.primaryButton
import app.a1swaara.core.ui.utils.roundedShape

@Composable
fun DialogTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false
) {
    val (colorPalette, typography) = LocalAppearance.current
    val shape = 14.dp.roundedShape

    val backgroundModifier = when {
        !primary -> Modifier.background(Color.Transparent)
        !enabled -> Modifier
            .background(colorPalette.primaryButton.copy(alpha = 0.5f))
            .border(1.dp, colorPalette.glassBorder, shape)
        colorPalette.isDefault -> Modifier.background(
            Brush.linearGradient(listOf(SwaaraMagenta, SwaaraViolet))
        )
        else -> Modifier.background(colorPalette.accent)
    }

    BasicText(
        text = text,
        style = typography.xs.let {
            when {
                !enabled -> it.medium.disabled
                primary -> it.semiBold.primary
                else -> it.medium.color(colorPalette.textSecondary)
            }
        },
        modifier = modifier
            .clip(shape)
            .then(backgroundModifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}
