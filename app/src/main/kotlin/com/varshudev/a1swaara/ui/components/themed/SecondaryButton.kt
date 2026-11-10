package com.varshudev.a1swaara.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.primaryButton
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    @DrawableRes iconId: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val (colorPalette) = LocalAppearance.current
    val backgroundColor = if (colorPalette.isDefault && colorPalette.isDark) {
        SwaaraElevatedSurface.copy(alpha = 0.94f)
    } else {
        colorPalette.primaryButton
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = colorPalette.accent.copy(alpha = 0.2f)
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, colorPalette.glassBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .size(48.dp)
    ) {
        Image(
            painter = painterResource(iconId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.text),
            modifier = Modifier
                .align(Alignment.Center)
                .size(18.dp)
        )
    }
}
