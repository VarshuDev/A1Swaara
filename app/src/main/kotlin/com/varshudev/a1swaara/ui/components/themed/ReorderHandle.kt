package com.varshudev.a1swaara.ui.components.themed

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.R
import app.a1swaara.compose.reordering.ReorderingState
import app.a1swaara.compose.reordering.reorder
import app.a1swaara.core.ui.LocalAppearance

@Composable
fun ReorderHandle(
    reorderingState: ReorderingState,
    index: Int,
    modifier: Modifier = Modifier
) = IconButton(
    icon = R.drawable.reorder,
    color = LocalAppearance.current.colorPalette.textSecondary.copy(alpha = 0.65f),
    indication = null,
    onClick = {},
    modifier = modifier
        .reorder(
            reorderingState = reorderingState,
            index = index
        )
        .size(20.dp)
)
