package com.varshudev.a1swaara.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import app.a1swaara.compose.routing.LocalRouteHandler
import app.a1swaara.compose.routing.ProvideRootRouter
import app.a1swaara.compose.routing.RouteHandler
import app.a1swaara.core.ui.utils.songBundle
import com.varshudev.a1swaara.LocalPlayerAwareWindowInsets
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.bugreport.SwaaraBugReportSheet
import com.varshudev.a1swaara.bugreport.SwaaraCrashRecovery
import com.varshudev.a1swaara.ui.components.SwaaraBorderSubtle
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.SwaaraErrorRed
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraMiniPlayer
import com.varshudev.a1swaara.ui.components.SwaaraNavigationBar
import com.varshudev.a1swaara.ui.components.SwaaraObsidian
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import com.varshudev.a1swaara.ui.screens.downloads.SwaaraDownloadsScreen
import com.varshudev.a1swaara.ui.screens.home.SwaaraHomeScreen
import com.varshudev.a1swaara.ui.screens.library.SwaaraLibraryScreen
import com.varshudev.a1swaara.ui.screens.localplaylist.LocalPlaylistScreen
import com.varshudev.a1swaara.ui.screens.mood.MoodScreen
import com.varshudev.a1swaara.ui.screens.player.SwaaraNowPlayingScreen
import com.varshudev.a1swaara.ui.screens.search.SwaaraSearchScreen
import com.varshudev.a1swaara.ui.screens.settings.SwaaraSettingsScreen
import com.varshudev.a1swaara.updater.SwaaraForceUpdateDialog
import com.varshudev.a1swaara.updater.SwaaraUpdateManager
import com.varshudev.a1swaara.utils.forceSeekToNext
import com.varshudev.a1swaara.utils.forceSeekToPrevious
import com.varshudev.a1swaara.utils.shouldBePlaying
import com.varshudev.a1swaara.utils.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalOpenNowPlaying = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun SwaaraAppShell() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player

    val isUpdateAvailable by SwaaraUpdateManager.isUpdateAvailable.collectAsStateWithLifecycle()
    val isMandatoryLockActive by SwaaraUpdateManager.isMandatoryLockActive.collectAsStateWithLifecycle()
    val updateInfo by SwaaraUpdateManager.updateInfo.collectAsStateWithLifecycle()
    val latestRelease by SwaaraUpdateManager.latestRelease.collectAsStateWithLifecycle()
    var showEnforcedUpdateDialog by remember { mutableStateOf(false) }

    var isNowPlayingOpen by remember { mutableStateOf(false) }
    val openNowPlaying: () -> Unit = { isNowPlayingOpen = true }
    val closeNowPlaying: () -> Unit = { isNowPlayingOpen = false }
    val sheetExpansion = remember { Animatable(0f) }
    val sheetScope = rememberCoroutineScope()
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isNowPlayingOpen) {
        sheetExpansion.animateTo(
            targetValue = if (isNowPlayingOpen) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    var pendingCrashSummary by remember {
        mutableStateOf(SwaaraCrashRecovery.readPendingCrash(context))
    }
    var showCrashReportSheet by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalOpenNowPlaying provides openNowPlaying) {
        ProvideRootRouter {
            val rootRouter = LocalRouteHandler.current
            val hasActiveRoute = rootRouter?.current?.child != null

            var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
            val tabHistory = remember { mutableStateListOf(0) }

            // When update lock is active, force redirect to Downloads screen (tab 3)
            LaunchedEffect(isMandatoryLockActive, isUpdateAvailable) {
                if (isMandatoryLockActive && isUpdateAvailable) {
                    rootRouter?.current?.replace?.invoke(null)
                    selectedTabIndex = 3
                    tabHistory.clear()
                    tabHistory.add(3)
                }
            }

            var currentMediaItem by remember { mutableStateOf(player?.currentMediaItem) }
            var isPlaying by remember { mutableStateOf(player?.shouldBePlaying == true) }
            var isLoading by remember { mutableStateOf(player?.playbackState == Player.STATE_BUFFERING) }
            var progress by remember { mutableFloatStateOf(0f) }

            // Real-time track listener
            DisposableEffect(player) {
                val listener = object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        currentMediaItem = mediaItem
                        if (mediaItem != null && mediaItem.mediaMetadata.extras?.songBundle?.isFromPersistentQueue != true) {
                            isNowPlayingOpen = true
                        }
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        val p = player ?: return
                        val dur = p.duration
                        progress = if (dur > 0) (p.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                    }

                    override fun onEvents(p: Player, events: Player.Events) {
                        currentMediaItem = p.currentMediaItem
                        isPlaying = p.shouldBePlaying
                        isLoading = p.playbackState == Player.STATE_BUFFERING
                    }
                }
                player?.addListener(listener)
                onDispose {
                    player?.removeListener(listener)
                }
            }

            // Poll progress only while actively playing to conserve CPU/battery when paused
            LaunchedEffect(player, isPlaying, currentMediaItem) {
                if (player != null && currentMediaItem != null) {
                    val dur = player.duration
                    val pos = player.currentPosition
                    progress = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                } else {
                    progress = 0f
                }
                while (player != null && isPlaying && currentMediaItem != null) {
                    val dur = player.duration
                    val pos = player.currentPosition
                    progress = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                    delay(300)
                }
            }

            // Back handling: Now Playing -> Detail Route (handled by inner RouteHandler) -> Tab history -> Home -> Exit
            BackHandler(
                enabled = isNowPlayingOpen || (!hasActiveRoute && (selectedTabIndex != 0 || tabHistory.size > 1))
            ) {
                if (isNowPlayingOpen) {
                    closeNowPlaying()
                } else if (tabHistory.size > 1) {
                    tabHistory.removeAt(tabHistory.size - 1)
                    selectedTabIndex = tabHistory.last()
                } else if (selectedTabIndex != 0) {
                    tabHistory.clear()
                    tabHistory.add(0)
                    selectedTabIndex = 0
                }
            }

            var bottomChromeHeightPx by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val baseInsets = LocalPlayerAwareWindowInsets.current
            val shellAwareInsets = remember(baseInsets, bottomChromeHeightPx, density) {
                val bottomChromeDp = with(density) { bottomChromeHeightPx.toDp() }
                val existingBottomDp = with(density) { baseInsets.getBottom(density).toDp() }
                val extraBottomDp = (bottomChromeDp - existingBottomDp).coerceAtLeast(0.dp)
                if (extraBottomDp > 0.dp) baseInsets.add(WindowInsets(bottom = extraBottomDp)) else baseInsets
            }

            val sheetNestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (source != NestedScrollSource.UserInput || containerHeightPx <= 1f) return Offset.Zero
                        if (sheetExpansion.value < 0.999f && available.y < 0f) {
                            val deltaFraction = -available.y / containerHeightPx
                            val next = (sheetExpansion.value + deltaFraction).coerceIn(0f, 1f)
                            val consumedY = -(next - sheetExpansion.value) * containerHeightPx
                            sheetScope.launch { sheetExpansion.snapTo(next) }
                            return Offset(0f, consumedY)
                        }
                        return Offset.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (source != NestedScrollSource.UserInput || containerHeightPx <= 1f) return Offset.Zero
                        if (available.y > 0f) {
                            val deltaFraction = available.y / containerHeightPx
                            val next = (sheetExpansion.value - deltaFraction).coerceIn(0f, 1f)
                            val consumedY = (sheetExpansion.value - next) * containerHeightPx
                            sheetScope.launch { sheetExpansion.snapTo(next) }
                            return Offset(0f, consumedY)
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        if (sheetExpansion.value < 0.999f) {
                            if (sheetExpansion.value < 0.72f || available.y > 1200f) {
                                isNowPlayingOpen = false
                            } else {
                                sheetExpansion.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                        return super.onPostFling(consumed, available)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SwaaraObsidian)
                    .onSizeChanged { containerHeightPx = it.height.toFloat() }
            ) {
                // Detail & Tab Content Area (kept above persistent Mini-Player + 5-tab Bottom Bar)
                CompositionLocalProvider(LocalPlayerAwareWindowInsets provides shellAwareInsets) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        RouteHandler(listenToGlobalEmitter = true) {
                            GlobalRoutes()

                            localPlaylistRoute { playlistId ->
                                LocalPlaylistScreen(playlistId = playlistId)
                            }

                            builtInPlaylistRoute { builtInPlaylist ->
                                BuiltInPlaylistScreen(builtInPlaylist = builtInPlaylist)
                            }

                            moodRoute { mood ->
                                MoodScreen(mood = mood)
                            }

                            Content {
                                Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                                    when (selectedTabIndex) {
                                        0 -> SwaaraHomeScreen(
                                            onSearchClick = {
                                                rootRouter?.current?.replace?.invoke(null)
                                                if (isUpdateAvailable && isMandatoryLockActive) {
                                                    context.toast("Online search is locked. Only downloaded songs can be played.")
                                                    selectedTabIndex = 3
                                                    showEnforcedUpdateDialog = true
                                                } else {
                                                    tabHistory.add(1)
                                                    selectedTabIndex = 1
                                                }
                                            },
                                            onSettingsClick = {
                                                rootRouter?.current?.replace?.invoke(null)
                                                tabHistory.add(4)
                                                selectedTabIndex = 4
                                            }
                                        )
                                        1 -> SwaaraSearchScreen()
                                        2 -> SwaaraLibraryScreen()
                                        3 -> SwaaraDownloadsScreen()
                                        4 -> SwaaraSettingsScreen()
                                    }
                                }
                            }
                        }
                    }
                }

                // Non-blocking Crash Recovery Banner when previous session ended unexpectedly
                AnimatedVisibility(
                    visible = pendingCrashSummary != null && !isNowPlayingOpen,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SwaaraDeepSurface.copy(alpha = 0.96f))
                            .border(1.dp, SwaaraErrorRed.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            BasicText(
                                text = "Previous session stopped unexpectedly",
                                style = TextStyle(
                                    color = SwaaraPrimaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            BasicText(
                                text = "Send diagnostics so we can fix it faster.",
                                style = TextStyle(
                                    color = SwaaraSecondaryText,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicText(
                            text = "Report",
                            style = TextStyle(
                                color = SwaaraMagenta,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showCrashReportSheet = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                        BasicText(
                            text = "Dismiss",
                            style = TextStyle(
                                color = SwaaraSecondaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    SwaaraCrashRecovery.clearPendingCrash(context)
                                    pendingCrashSummary = null
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                // Floating Mini-Player & Bottom Navigation Bar (persists on tabs & detail screens)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            bottomChromeHeightPx = coords.size.height
                        }
                ) {
                    if (currentMediaItem != null) {
                        SwaaraMiniPlayer(
                            mediaItem = currentMediaItem!!,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            progress = progress,
                            onPlayPauseClick = {
                                if (player?.shouldBePlaying == true) player.pause() else player?.play()
                            },
                            onNextClick = { player?.forceSeekToNext() },
                            onPreviousClick = { player?.forceSeekToPrevious() },
                            onTap = openNowPlaying
                        )
                    }

                    SwaaraNavigationBar(
                        selectedIndex = selectedTabIndex,
                        onTabSelected = { newIndex ->
                            if (isUpdateAvailable && isMandatoryLockActive && newIndex in listOf(0, 1, 2)) {
                                context.toast("Online music is locked. Please update A1 Swaara or enjoy downloaded songs.")
                                rootRouter?.current?.replace?.invoke(null)
                                selectedTabIndex = 3
                                showEnforcedUpdateDialog = true
                            } else {
                                rootRouter?.current?.replace?.invoke(null)
                                if (selectedTabIndex != newIndex) {
                                    tabHistory.removeAll { it == newIndex }
                                    tabHistory.add(newIndex)
                                    selectedTabIndex = newIndex
                                }
                            }
                        }
                    )
                }

                // Interactive Spring-Driven Now Playing Sheet
                if (isNowPlayingOpen || sheetExpansion.value > 0.005f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = (1f - sheetExpansion.value) * size.height
                                alpha = (sheetExpansion.value * 1.4f).coerceIn(0f, 1f)
                            }
                            .nestedScroll(sheetNestedScrollConnection)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { deltaY ->
                                    if (containerHeightPx > 1f) {
                                        val next = (sheetExpansion.value - deltaY / containerHeightPx).coerceIn(0f, 1f)
                                        sheetScope.launch { sheetExpansion.snapTo(next) }
                                    }
                                },
                                onDragStopped = { velocityY ->
                                    if (sheetExpansion.value < 0.72f || velocityY > 1200f) {
                                        isNowPlayingOpen = false
                                    } else {
                                        sheetScope.launch {
                                            sheetExpansion.animateTo(
                                                targetValue = 1f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                    ) {
                        SwaaraNowPlayingScreen(
                            onDismiss = closeNowPlaying
                        )
                    }
                }

                // Enforced OTA Update Modal Dialog if user navigates away from Downloads
                if (showEnforcedUpdateDialog && isUpdateAvailable && (updateInfo != null || latestRelease != null)) {
                    SwaaraForceUpdateDialog(
                        release = latestRelease,
                        updateInfo = updateInfo,
                        onDismiss = {
                            showEnforcedUpdateDialog = false
                            rootRouter?.current?.replace?.invoke(null)
                            selectedTabIndex = 3
                        }
                    )
                }

                if (showCrashReportSheet && pendingCrashSummary != null) {
                    SwaaraBugReportSheet(
                        onDismiss = {
                            showCrashReportSheet = false
                            SwaaraCrashRecovery.clearPendingCrash(context)
                            pendingCrashSummary = null
                        },
                        binder = binder,
                        initialDescription = "Automatic Crash Recovery Report:\n$pendingCrashSummary"
                    )
                }
            }
        }
    }
}

