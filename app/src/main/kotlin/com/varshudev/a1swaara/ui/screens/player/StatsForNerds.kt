package com.varshudev.a1swaara.ui.screens.player

import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import com.varshudev.a1swaara.LocalAppContainer
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.models.Format
import com.varshudev.a1swaara.ui.components.SwaaraModalSurface
import com.varshudev.a1swaara.ui.components.themed.SecondaryTextButton
import com.varshudev.a1swaara.ui.viewmodels.PlayerViewModel
import com.varshudev.a1swaara.utils.color
import com.varshudev.a1swaara.utils.medium
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.onOverlay
import app.a1swaara.core.ui.overlay
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.core.ui.utils.roundedShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun StatsForNerds(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) = AnimatedVisibility(
    visible = isDisplayed,
    enter = fadeIn(),
    exit = fadeOut()
) {
    val viewModel: PlayerViewModel = viewModel(
        key = "player",
        factory = PlayerViewModel.factory(LocalAppContainer.current.playerRepository)
    )
    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current

    val coroutineScope = rememberCoroutineScope()

    var cachedBytes by remember(binder, mediaId) {
        mutableLongStateOf(binder?.cache?.getCachedBytes(mediaId, 0, -1) ?: 0L)
    }

    var format by remember { mutableStateOf<Format?>(null) }

    var hasReloaded by rememberSaveable { mutableStateOf(false) }

    suspend fun reload() {
        val currentBinder = binder ?: return

        currentBinder.player.currentMediaItem
            ?.takeIf { it.mediaId == mediaId }
            ?.let { mediaItem ->
                withContext(Dispatchers.IO) {
                    delay(2000)
                    viewModel.refreshFormat(songId = mediaId, mediaItem = mediaItem)
                }
            }
    }

    LaunchedEffect(binder, mediaId) {
        binder ?: return@LaunchedEffect

        viewModel
            .observeFormat(mediaId)
            .distinctUntilChanged()
            .collectLatest { currentFormat ->
                if (currentFormat?.itag != null) format = currentFormat
                else reload()
            }
    }

    DisposableEffect(binder, mediaId) {
        val currentBinder = binder ?: return@DisposableEffect onDispose { }

        val listener = object : Cache.Listener {
            override fun onSpanAdded(cache: Cache, span: CacheSpan) {
                cachedBytes += span.length
            }

            override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
                cachedBytes -= span.length
            }

            override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) = Unit
        }

        currentBinder.cache.addListener(mediaId, listener)

        onDispose {
            currentBinder.cache.removeListener(mediaId, listener)
        }
    }

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .background(Color.Black.copy(alpha = 0.65f))
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .clip(20.dp.roundedShape)
                .background(SwaaraModalSurface)
                .border(1.dp, Color.White.copy(alpha = 0.10f), 20.dp.roundedShape)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    @Composable
                    fun Label(text: String) = BasicText(
                        text = text,
                        maxLines = 1,
                        style = typography.xs.medium.color(colorPalette.textSecondary)
                    )

                    @Composable
                    fun Value(text: String) = BasicText(
                        text = text,
                        maxLines = 1,
                        style = typography.xs.medium.color(colorPalette.secondaryAccent)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Label(text = stringResource(R.string.id))
                        Label(text = stringResource(R.string.itag))
                        Label(text = stringResource(R.string.bitrate))
                        Label(text = stringResource(R.string.size))
                        Label(text = stringResource(R.string.cached))
                        Label(text = stringResource(R.string.loudness))
                    }

                    Column {
                        Value(text = mediaId)
                        Value(text = format?.itag?.toString() ?: stringResource(R.string.unknown))
                        Value(
                            text = when (val rate = format?.bitrate) {
                                null, 0L -> stringResource(R.string.unknown)
                                else -> stringResource(R.string.format_kbps, rate / 1000)
                            }
                        )
                        Value(
                            text = when (val length = format?.contentLength) {
                                null, 0L -> stringResource(R.string.unknown)
                                else -> Formatter.formatShortFileSize(context, length)
                            }
                        )
                        Value(
                            text = buildString {
                                append(Formatter.formatShortFileSize(context, cachedBytes))

                                format?.contentLength?.let {
                                    append(" (${(cachedBytes.toFloat() / it * 100).roundToInt()}%)")
                                }
                            }
                        )
                        Value(
                            text = format?.loudnessDb?.let {
                                stringResource(
                                    R.string.format_db,
                                    "%.2f".format(it)
                                )
                            } ?: stringResource(R.string.unknown)
                        )
                    }
                }

                binder?.let {
                    SecondaryTextButton(
                        text = stringResource(R.string.reload),
                        onClick = {
                            hasReloaded = true

                            coroutineScope.launch {
                                reload()
                            }
                        },
                        enabled = !hasReloaded
                    )
                }
            }
        }
    }
}
