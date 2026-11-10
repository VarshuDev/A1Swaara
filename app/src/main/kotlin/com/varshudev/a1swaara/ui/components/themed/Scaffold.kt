package com.varshudev.a1swaara.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Down
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.End
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Start
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Up
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.varshudev.a1swaara.LocalPlayerAwareWindowInsets
import com.varshudev.a1swaara.preferences.UIStatePreferences
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.utils.isLandscape
import kotlinx.collections.immutable.toImmutableList

@Composable
fun Scaffold(
    key: String,
    topIconButtonId: Int,
    onTopIconButtonClick: () -> Unit,
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    tabColumnContent: TabsBuilder.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    var hiddenTabs by UIStatePreferences.mutableTabStateOf(key)
    val tabs = TabsBuilder.rememberTabs { tabColumnContent() }

    if (isLandscape) {
        Row(
            modifier = modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            NavigationRail(
                topIconButtonId = topIconButtonId,
                onTopIconButtonClick = onTopIconButtonClick,
                tabIndex = tabIndex,
                onTabIndexChange = onTabChange,
                hiddenTabs = hiddenTabs,
                setHiddenTabs = { hiddenTabs = it.toImmutableList() },
                content = tabColumnContent
            )

            AnimatedContent(
                targetState = tabIndex,
                transitionSpec = {
                    val slideDirection = if (targetState > initialState) Up else Down
                    val animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )

                    ContentTransform(
                        targetContentEnter = slideIntoContainer(slideDirection, animationSpec),
                        initialContentExit = slideOutOfContainer(slideDirection, animationSpec),
                        sizeTransform = null
                    )
                },
                content = content,
                label = ""
            )
        }
    } else {
        val baseInsets = LocalPlayerAwareWindowInsets.current
        val childWindowInsets = remember(baseInsets) {
            baseInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        }

        Column(
            modifier = modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            SwaaraScaffoldHeader(
                topIconButtonId = topIconButtonId,
                onTopIconButtonClick = onTopIconButtonClick,
                tabs = tabs,
                tabIndex = tabIndex,
                onTabIndexChange = onTabChange,
                hiddenTabs = hiddenTabs,
                setHiddenTabs = { hiddenTabs = it.toImmutableList() },
                topInsetsPadding = baseInsets
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    .asPaddingValues()
            )

            CompositionLocalProvider(LocalPlayerAwareWindowInsets provides childWindowInsets) {
                AnimatedContent(
                    targetState = tabIndex,
                    transitionSpec = {
                        val slideDirection = if (targetState > initialState) Start else End
                        val slideSpec = spring(
                            dampingRatio = 0.92f,
                            stiffness = Spring.StiffnessMediumLow,
                            visibilityThreshold = IntOffset.VisibilityThreshold
                        )

                        ContentTransform(
                            targetContentEnter = slideIntoContainer(slideDirection, slideSpec) + fadeIn(tween(180)),
                            initialContentExit = slideOutOfContainer(slideDirection, slideSpec) + fadeOut(tween(140)),
                            sizeTransform = null
                        )
                    },
                    content = content,
                    label = "",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}
