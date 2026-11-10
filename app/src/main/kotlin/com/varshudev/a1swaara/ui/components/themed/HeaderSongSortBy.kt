package com.varshudev.a1swaara.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import app.a1swaara.core.data.enums.SongSortBy
import app.a1swaara.core.data.enums.SortOrder
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.core.ui.utils.roundedShape

// Row content, for convenience, doesn't need modifier/receiver
@Suppress("UnusedReceiverParameter", "ModifierMissing")
@Composable
fun RowScope.HeaderSongSortBy(
    sortBy: SongSortBy,
    setSortBy: (SongSortBy) -> Unit,
    sortOrder: SortOrder,
    setSortOrder: (SortOrder) -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = ""
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortPillButton(
            icon = R.drawable.trending,
            selected = sortBy == SongSortBy.PlayTime,
            onClick = { setSortBy(SongSortBy.PlayTime) }
        )

        SortPillButton(
            icon = R.drawable.text,
            selected = sortBy == SongSortBy.Title,
            onClick = { setSortBy(SongSortBy.Title) }
        )

        SortPillButton(
            icon = R.drawable.time,
            selected = sortBy == SongSortBy.DateAdded,
            onClick = { setSortBy(SongSortBy.DateAdded) }
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(10.dp.roundedShape)
                .background(colorPalette.secondaryAccent.copy(alpha = 0.14f))
                .border(1.dp, colorPalette.secondaryAccent.copy(alpha = 0.35f), 10.dp.roundedShape)
                .clickable { setSortOrder(!sortOrder) }
        ) {
            Image(
                painter = painterResource(R.drawable.arrow_up),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.secondaryAccent),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = sortOrderIconRotation }
            )
        }
    }
}

@Composable
private fun SortPillButton(
    @DrawableRes icon: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val bgColor = when {
        selected -> colorPalette.accent.copy(alpha = 0.16f)
        colorPalette.isDefault && colorPalette.isDark -> SwaaraElevatedSurface.copy(alpha = 0.65f)
        else -> colorPalette.background2.copy(alpha = 0.55f)
    }
    val borderColor = if (selected) colorPalette.accent.copy(alpha = 0.45f) else colorPalette.glassBorder.copy(alpha = 0.55f)
    val iconColor = if (selected) colorPalette.accent else colorPalette.textDisabled

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(10.dp.roundedShape)
            .background(bgColor)
            .border(1.dp, borderColor, 10.dp.roundedShape)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier.size(16.dp)
        )
    }
}
