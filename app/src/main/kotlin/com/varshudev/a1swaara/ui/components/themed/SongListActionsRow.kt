package com.varshudev.a1swaara.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.core.ui.utils.roundedShape

@Composable
fun RowScope.SongListActionsRow(
    filterQuery: String? = null,
    onFilterQueryChange: ((String?) -> Unit)? = null,
    leadingContent: @Composable (RowScope.() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    var isFiltering by rememberSaveable { mutableStateOf(filterQuery != null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val colorPalette = LocalAppearance.current.colorPalette

    if (onFilterQueryChange != null) AnimatedContent(
        targetState = isFiltering,
        label = "",
        modifier = if (isFiltering) Modifier.weight(1f) else Modifier
    ) { filtering ->
        if (filtering) {
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .clip(14.dp.roundedShape)
                    .background(
                        if (colorPalette.isDefault && colorPalette.isDark) SwaaraElevatedSurface
                        else colorPalette.background1
                    )
                    .border(1.dp, colorPalette.glassBorder, 14.dp.roundedShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.secondaryAccent),
                    modifier = Modifier.size(15.dp)
                )

                TextField(
                    value = filterQuery.orEmpty(),
                    onValueChange = onFilterQueryChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (filterQuery.isNullOrBlank()) onFilterQueryChange("")
                        focusManager.clearFocus()
                    }),
                    hintText = stringResource(R.string.filter_placeholder),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (!it.hasFocus) {
                                keyboardController?.hide()
                                if (filterQuery?.isBlank() == true) {
                                    onFilterQueryChange(null)
                                    isFiltering = false
                                }
                            }
                        }
                )

                Image(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            onFilterQueryChange(null)
                            isFiltering = false
                            focusManager.clearFocus()
                        }
                )
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(12.dp.roundedShape)
                    .background(
                        if (colorPalette.isDefault && colorPalette.isDark) SwaaraElevatedSurface
                        else colorPalette.background1
                    )
                    .border(1.dp, colorPalette.glassBorder, 12.dp.roundedShape)
                    .clickable { isFiltering = true }
            ) {
                Image(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.secondaryAccent),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    if (!isFiltering) Spacer(modifier = Modifier.weight(1f))

    leadingContent?.invoke(this)

    trailingContent?.invoke(this)
}
