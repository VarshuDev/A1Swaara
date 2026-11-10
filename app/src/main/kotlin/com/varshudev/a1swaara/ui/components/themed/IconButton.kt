package com.varshudev.a1swaara.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.core.ui.utils.roundedShape
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface

@Composable
fun HeaderIconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = true)
) {
    val (colorPalette) = LocalAppearance.current

    HeaderIconButton(
        onClick = onClick,
        icon = icon,
        modifier = modifier,
        indication = indication,
        enabled = enabled,
        color = if (enabled) colorPalette.text else colorPalette.textDisabled
    )
}

@Composable
fun HeaderIconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = true)
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val shape = 12.dp.roundedShape
    val isAccent = color == colorPalette.accent || color == colorPalette.secondaryAccent
    val bgColor = when {
        isAccent -> color.copy(alpha = 0.14f)
        colorPalette.isDefault && colorPalette.isDark -> SwaaraElevatedSurface.copy(alpha = 0.9f)
        else -> colorPalette.background1
    }
    val borderColor = if (isAccent) color.copy(alpha = 0.45f) else colorPalette.glassBorder
    val iconColor = if (enabled) color else colorPalette.textDisabled

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .size(38.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                indication = indication,
                interactionSource = remember { MutableInteractionSource() },
                enabled = enabled,
                onClick = onClick
            )
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false)
) {
    val (colorPalette) = LocalAppearance.current

    IconButton(
        onClick = onClick,
        icon = icon,
        modifier = modifier,
        indication = indication,
        enabled = enabled,
        color = if (enabled) colorPalette.text else colorPalette.textDisabled
    )
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false)
) = Image(
    painter = painterResource(icon),
    contentDescription = null,
    colorFilter = ColorFilter.tint(color),
    modifier = Modifier
        .clickable(
            indication = indication,
            interactionSource = remember { MutableInteractionSource() },
            enabled = enabled,
            onClick = onClick
        )
        .then(modifier)
)
