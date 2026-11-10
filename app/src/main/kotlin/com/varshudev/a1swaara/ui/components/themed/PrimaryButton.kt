package com.varshudev.a1swaara.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.primaryButton
import app.a1swaara.core.ui.utils.roundedShape
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraViolet

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false
) {
    val (colorPalette) = LocalAppearance.current
    val shape = 18.dp.roundedShape

    val backgroundModifier = when {
        accent && colorPalette.isDefault -> Modifier.background(
            Brush.linearGradient(listOf(SwaaraMagenta, SwaaraViolet))
        )
        accent -> Modifier.background(colorPalette.accent)
        colorPalette.isDefault && colorPalette.isDark -> Modifier.background(
            SwaaraElevatedSurface.copy(alpha = 0.94f)
        )
        else -> Modifier.background(colorPalette.primaryButton)
    }

    val borderStroke = when {
        accent -> BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
        colorPalette.isDefault && colorPalette.isDark -> BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    SwaaraMagenta.copy(alpha = 0.45f),
                    SwaaraCyan.copy(alpha = 0.35f)
                )
            )
        )
        else -> BorderStroke(1.dp, colorPalette.glassBorder)
    }

    val iconTint = if (accent) colorPalette.onAccent else colorPalette.text

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(
                elevation = if (accent) 14.dp else 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = if (accent) colorPalette.accent.copy(alpha = 0.45f) else SwaaraViolet.copy(alpha = 0.25f)
            )
            .clip(shape)
            .then(backgroundModifier)
            .border(borderStroke, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .size(58.dp)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconTint),
            modifier = Modifier
                .align(Alignment.Center)
                .size(20.dp)
        )
    }
}
