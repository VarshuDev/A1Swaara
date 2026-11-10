package com.varshudev.a1swaara.ui.screens.settings

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.LocalPlayerAwareWindowInsets
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.varshudev.a1swaara.ui.components.themed.Scaffold
import com.varshudev.a1swaara.ui.screens.GlobalRoutes
import com.varshudev.a1swaara.ui.screens.Route
import com.varshudev.a1swaara.utils.Logcat
import com.varshudev.a1swaara.utils.color
import com.varshudev.a1swaara.utils.logcat
import com.varshudev.a1swaara.utils.secondary
import com.varshudev.a1swaara.utils.semiBold
import com.varshudev.a1swaara.utils.smoothScrollToTop
import app.a1swaara.compose.routing.RouteHandler
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.core.ui.utils.ActivityIntentBundleAccessor
import app.a1swaara.core.ui.utils.roundedShape
import kotlinx.coroutines.delay

@Route
@Composable
fun LogsScreen() {
    val saveableStateHolder = rememberSaveableStateHolder()
    val (tabIndex, onTabChanged) = rememberSaveable { mutableIntStateOf(0) }

    RouteHandler {
        GlobalRoutes()

        Content {
            Scaffold(
                key = "logs",
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChange = onTabChanged,
                tabColumnContent = {
                    tab(0, R.string.logs, R.drawable.library, canHide = false)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> LogsList()
                    }
                }
            }
        }
    }
}

@Composable
fun LogsList(modifier: Modifier = Modifier) = Box(modifier = modifier.fillMaxSize()) {
    val logs = logcat()
    val state = rememberLazyListState()

    val (_, typography) = LocalAppearance.current
    val context = LocalContext.current

    var initial by remember { mutableStateOf(true) }
    val firstVisibleItemIndex by remember { derivedStateOf { state.firstVisibleItemIndex } }

    LaunchedEffect(logs.size) {
        if (initial && logs.isNotEmpty()) {
            delay(200)
            state.scrollToItem(0)
            initial = false
            return@LaunchedEffect
        }
        if (logs.isEmpty() || firstVisibleItemIndex > 1) return@LaunchedEffect

        state.smoothScrollToTop()
    }

    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
            .asPaddingValues(),
        reverseLayout = true
    ) {
        fun get(i: Int) = logs[logs.size - i - 1]

        items(
            count = logs.size,
            key = { get(it).id }
        ) { i ->
            when (val line = get(i)) {
                is Logcat.FormattedLine -> FormattedLine(
                    line = line,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                )

                is Logcat.RawLine -> BasicText(
                    text = line.raw,
                    style = typography.xs,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item(key = "bottom_fab_spacer") {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }

    FloatingActionsContainerWithScrollToTop(
        lazyListState = state,
        scrollIcon = R.drawable.chevron_down,
        icon = R.drawable.share_social,
        onClick = {
            val extras = ActivityIntentBundleAccessor.bundle {
                text = logs.joinToString(separator = "\n") {
                    when (it) {
                        is Logcat.FormattedLine ->
                            "[${it.timestamp}] ${it.level.name} (${it.pid}) ${it.tag} - ${it.message}"

                        is Logcat.RawLine -> it.raw
                    }
                }
            }

            context.startActivity(
                Intent(Intent.ACTION_SEND).apply {
                    putExtras(extras)
                    type = "text/plain"
                }.let { Intent.createChooser(it, null) }
            )
        }
    )
}

@Composable
fun LazyItemScope.FormattedLine(
    line: Logcat.FormattedLine,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current

    val accentColor = remember(line, colorPalette) {
        when (line.level) {
            Logcat.FormattedLine.Level.Error -> colorPalette.red
            Logcat.FormattedLine.Level.Warning -> colorPalette.yellow
            Logcat.FormattedLine.Level.Debug -> colorPalette.secondaryAccent
            Logcat.FormattedLine.Level.Info -> colorPalette.accent
            Logcat.FormattedLine.Level.Unknown -> colorPalette.textDisabled
        }
    }

    var singleLine by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .clip(14.dp.roundedShape)
            .background(SwaaraDeepSurface)
            .border(1.dp, accentColor.copy(alpha = 0.35f), 14.dp.roundedShape)
            .clickable { singleLine = !singleLine }
            .padding(14.dp)
            .animateItem()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(8.dp.roundedShape)
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), 8.dp.roundedShape)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(
                            when (line.level) {
                                Logcat.FormattedLine.Level.Error -> R.drawable.alert_circle
                                Logcat.FormattedLine.Level.Warning -> R.drawable.warning_outline
                                Logcat.FormattedLine.Level.Debug -> R.drawable.bug_outline
                                Logcat.FormattedLine.Level.Info -> R.drawable.information_circle_outline
                                Logcat.FormattedLine.Level.Unknown -> R.drawable.help_outline
                            }
                        ),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(accentColor),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BasicText(
                        text = line.level.name,
                        style = typography.xxs.semiBold.copy(color = accentColor)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = line.tag,
                    style = typography.xxs.semiBold.copy(color = colorPalette.text)
                )
                BasicText(
                    text = line.timestamp.toString(),
                    style = typography.xxs.secondary
                )
            }
        }
        AnimatedContent(
            targetState = singleLine,
            label = "",
            transitionSpec = { EnterTransition.None togetherWith fadeOut() }
        ) { currentSingleLine ->
            BasicText(
                text = line.message,
                style = typography.xs.copy(color = colorPalette.text),
                modifier = Modifier.padding(top = 8.dp),
                maxLines = if (currentSingleLine) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
