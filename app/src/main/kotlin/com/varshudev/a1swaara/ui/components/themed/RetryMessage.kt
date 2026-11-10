package com.varshudev.a1swaara.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.utils.center
import com.varshudev.a1swaara.utils.secondary
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.utils.roundedShape

@Composable
fun RetryMessage(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (colorPalette, typography) = LocalAppearance.current
    val shape = 18.dp.roundedShape

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .clip(shape)
            .background(colorPalette.background1)
            .border(1.dp, colorPalette.glassBorder, shape)
            .clickable(onClick = onRetry)
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.sync),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.accent),
            modifier = Modifier.size(20.dp)
        )

        BasicText(
            text = stringResource(R.string.error_message),
            style = typography.xs.secondary.center
        )
    }
}
