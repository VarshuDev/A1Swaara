package com.varshudev.a1swaara.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.utils.medium
import com.varshudev.a1swaara.utils.secondary
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.utils.roundedShape

@Composable
inline fun Menu(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val surfaceColor = if (colorPalette.isDefault && colorPalette.isDark) {
        SwaaraDeepSurface
    } else {
        colorPalette.background1
    }
    val borderBrush = Brush.verticalGradient(
        listOf(
            if (colorPalette.isDefault) SwaaraMagenta.copy(alpha = 0.28f) else colorPalette.glassBorder,
            colorPalette.glassBorder.copy(alpha = 0.2f),
            Color.Transparent
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceColor)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp, bottom = 8.dp)
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(colorPalette.textDisabled.copy(alpha = 0.45f))
        )

        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuEntry(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    enabled: Boolean = true,
    iconTint: Color? = null,
    textColor: Color? = null,
    onLongClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val (colorPalette, typography) = LocalAppearance.current
    val resolvedIconTint = iconTint ?: colorPalette.text

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(14.dp.roundedShape)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(10.dp.roundedShape)
                .background(
                    when {
                        iconTint != null -> iconTint.copy(alpha = 0.14f)
                        colorPalette.isDefault && colorPalette.isDark -> SwaaraElevatedSurface
                        else -> colorPalette.background2.copy(alpha = 0.72f)
                    }
                )
                .border(
                    width = 0.8.dp,
                    color = if (iconTint != null) iconTint.copy(alpha = 0.28f) else colorPalette.glassBorder,
                    shape = 10.dp.roundedShape
                )
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(resolvedIconTint),
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = text,
                style = if (textColor != null) typography.xs.medium.copy(color = textColor)
                else typography.xs.medium
            )

            secondaryText?.let { secondaryText ->
                BasicText(
                    text = secondaryText,
                    style = typography.xxs.medium.secondary
                )
            }
        }

        trailingContent?.invoke()
    }
}
