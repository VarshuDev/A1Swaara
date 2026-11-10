package com.varshudev.a1swaara.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.ui.components.ShimmerHost
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.items.SongItemPlaceholder
import com.varshudev.a1swaara.LocalPlayerAwareWindowInsets
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.utils.medium
import com.varshudev.a1swaara.utils.semiBold
import app.a1swaara.core.ui.Dimensions
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.core.ui.utils.roundedShape

@Composable
fun SongCollectionScreen(
    thumbnailContent: @Composable () -> Unit,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
        .asPaddingValues(),
    listBackground: Color = LocalAppearance.current.colorPalette.background0,
    shuffleIcon: Int = R.drawable.shuffle,
    shuffleVisible: Boolean = true,
    onShuffle: (() -> Unit)? = null,
    floatingActionsContent: (@Composable RowScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit
) = LayoutWithAdaptiveThumbnail(
    thumbnailContent = thumbnailContent,
    modifier = modifier
) {
    val hasFloatingActions = shuffleVisible && (onShuffle != null || floatingActionsContent != null)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier
                .background(listBackground)
                .fillMaxSize()
        ) {
            item(
                key = "header",
                contentType = 0
            ) {
                headerContent()
            }

            content()

            if (hasFloatingActions) {
                item(
                    key = "bottom_fab_spacer",
                    contentType = "bottom_fab_spacer"
                ) {
                    Spacer(modifier = Modifier.height(84.dp))
                }
            }
        }

        if (hasFloatingActions) {
            FloatingActionsContainerWithScrollToTop(
                lazyListState = listState,
                icon = shuffleIcon,
                onClick = onShuffle,
                actionsContent = floatingActionsContent
            )
        }
    }
}

@Composable
fun Modifier.swaaraGlassTrackSurface(
    shape: Shape = 16.dp.roundedShape
): Modifier {
    val colorPalette = LocalAppearance.current.colorPalette
    val surfaceColor = if (colorPalette.isDefault && colorPalette.isDark) {
        SwaaraDeepSurface
    } else {
        colorPalette.background1
    }

    return this
        .clip(shape)
        .background(surfaceColor)
        .border(
            width = 0.75.dp,
            color = colorPalette.glassBorder.copy(alpha = 0.7f),
            shape = shape
        )
}

@Composable
fun CollectionTrackSectionHeader(
    count: Int,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.songs)
) {
    val (colorPalette, typography) = LocalAppearance.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 6.dp)
    ) {
        BasicText(
            text = title,
            style = typography.m.semiBold.copy(color = colorPalette.text)
        )

        Box(
            modifier = Modifier
                .clip(10.dp.roundedShape)
                .background(colorPalette.secondaryAccent.copy(alpha = 0.14f))
                .border(
                    width = 1.dp,
                    color = colorPalette.secondaryAccent.copy(alpha = 0.35f),
                    shape = 10.dp.roundedShape
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            BasicText(
                text = pluralStringResource(R.plurals.song_count_plural, count, count),
                style = typography.xxs.medium.copy(color = colorPalette.secondaryAccent)
            )
        }
    }
}

fun <T> LazyListScope.songCollectionItems(
    items: List<T>,
    isLoading: Boolean,
    key: ((index: Int, item: T) -> Any)? = null,
    contentType: ((index: Int, item: T) -> Any?)? = null,
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
) {
    if (!isLoading) {
        itemsIndexed(
            items = items,
            key = key,
            contentType = contentType ?: { _, _ -> null },
            itemContent = itemContent,
        )
    } else {
        item(key = "loading") {
            ShimmerHost(modifier = Modifier.fillMaxWidth()) {
                repeat(6) {
                    SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
                }
            }
        }
    }
}

fun matchesSongCollectionQuery(
    query: String?,
    vararg values: String?
): Boolean {
    val normalizedQuery = query?.trim().orEmpty()

    return normalizedQuery.isEmpty() || values.any {
        it?.contains(normalizedQuery, ignoreCase = true) == true
    }
}
