package com.varshudev.a1swaara.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.utils.color
import com.varshudev.a1swaara.utils.medium
import com.varshudev.a1swaara.utils.semiBold
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.utils.roundedShape

@Composable
fun TextToggle(
    state: Boolean,
    toggleState: () -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    onLabel: String = stringResource(R.string.on_label),
    offLabel: String = stringResource(R.string.off_label)
) {
    val (colorPalette, typography) = LocalAppearance.current
    val shape = 20.dp.roundedShape

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .background(
                if (state) colorPalette.accent.copy(alpha = 0.16f)
                else colorPalette.background1
            )
            .border(
                width = 1.dp,
                color = if (state) colorPalette.accent.copy(alpha = 0.45f) else colorPalette.glassBorder,
                shape = shape
            )
            .clickable(onClick = toggleState)
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .animateContentSize()
    ) {
        BasicText(
            text = "$name ",
            style = typography.xxs.medium
        )

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                val slideDirection =
                    if (targetState) AnimatedContentTransitionScope.SlideDirection.Up
                    else AnimatedContentTransitionScope.SlideDirection.Down

                ContentTransform(
                    targetContentEnter = slideIntoContainer(slideDirection) + fadeIn(),
                    initialContentExit = slideOutOfContainer(slideDirection) + fadeOut()
                )
            },
            label = ""
        ) {
            BasicText(
                text = if (it) onLabel else offLabel,
                style = typography.xxs.semiBold.color(
                    if (it) colorPalette.accent else colorPalette.textSecondary
                )
            )
        }
    }
}
