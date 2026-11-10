package com.varshudev.a1swaara.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.ui.components.FadingRow
import com.varshudev.a1swaara.utils.medium
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.shimmer
import app.a1swaara.core.ui.utils.roundedShape
import kotlin.random.Random

@Composable
fun Header(
    title: String,
    modifier: Modifier = Modifier,
    actionsContent: @Composable RowScope.() -> Unit = {}
) = Header(
    modifier = modifier,
    titleContent = {
        FadingRow {
            BasicText(
                text = title,
                style = LocalAppearance.current.typography.xxl.medium,
                maxLines = 1
            )
        }
    },
    actionsContent = actionsContent
)

@Composable
fun Header(
    titleContent: @Composable () -> Unit,
    actionsContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(top = 6.dp, bottom = 10.dp)
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        titleContent()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        content = actionsContent
    )
}

@Composable
fun HeaderPlaceholder(modifier: Modifier = Modifier) = Column(
    modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(top = 6.dp, bottom = 10.dp)
) {
    val (colorPalette, typography) = LocalAppearance.current
    val text = remember { List(Random.nextInt(6, 16)) { " " }.joinToString(separator = "") }

    Box(
        modifier = Modifier
            .clip(12.dp.roundedShape)
            .background(colorPalette.shimmer)
            .fillMaxWidth(remember { 0.45f + Random.nextFloat() * 0.35f })
    ) {
        BasicText(
            text = text,
            style = typography.xxl.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
}
