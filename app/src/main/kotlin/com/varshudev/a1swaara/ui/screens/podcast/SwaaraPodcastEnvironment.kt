package com.varshudev.a1swaara.ui.screens.podcast

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.podcast.SwaaraPodcastEpisode
import com.varshudev.a1swaara.podcast.SwaaraPodcastFeedClient
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.components.SwaaraViolet
import com.varshudev.a1swaara.ui.screens.LocalOpenNowPlaying
import com.varshudev.a1swaara.utils.playTrackWithService
import kotlin.math.sin

@Composable
fun SwaaraPodcastEnvironment(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val openNowPlaying = LocalOpenNowPlaying.current

    val genres = remember { SwaaraPodcastFeedClient.GENRES }
    var selectedGenreIndex by remember { mutableIntStateOf(0) }
    val currentGenre = genres[selectedGenreIndex]

    var podcastEpisodes by remember { mutableStateOf<List<SwaaraPodcastEpisode>>(emptyList()) }
    var isLoadingFeed by remember { mutableStateOf(true) }

    LaunchedEffect(currentGenre) {
        isLoadingFeed = true
        val episodes = SwaaraPodcastFeedClient.fetchPodcasts(currentGenre.query)
        podcastEpisodes = episodes
        isLoadingFeed = false
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Pure Animated Soundwave Hero Showcase
        SwaaraAnimatedSoundwaveHero(
            onExplore = {
                val firstEp = podcastEpisodes.firstOrNull()
                if (firstEp != null) {
                    playTrackWithService(
                        context = context,
                        binder = binder,
                        videoId = firstEp.id,
                        title = firstEp.title,
                        artist = firstEp.author,
                        thumbnailUrl = firstEp.thumbnailUrl,
                        durationText = firstEp.durationText,
                        isPodcast = true,
                        onStarted = { openNowPlaying() }
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(14.dp))

        // 2. Featured Creator Spotlight: Figuring Out with Raj Shamani
        SwaaraRajShamaniSpotlightBanner(
            onPlayLatest = {
                val latestEp = podcastEpisodes.firstOrNull { it.author.contains("Raj Shamani", ignoreCase = true) }
                    ?: podcastEpisodes.firstOrNull()

                if (latestEp != null) {
                    playTrackWithService(
                        context = context,
                        binder = binder,
                        videoId = latestEp.id,
                        title = latestEp.title,
                        artist = latestEp.author,
                        thumbnailUrl = latestEp.thumbnailUrl,
                        durationText = latestEp.durationText,
                        isPodcast = true,
                        onStarted = { openNowPlaying() }
                    )
                }
            },
            onViewShow = {
                // Switch category to Raj Shamani inside the podcast feed
                val rajIndex = genres.indexOfFirst { it.id == "raj_shamani" }
                if (rajIndex >= 0) {
                    selectedGenreIndex = rajIndex
                }
            }
        )
        Spacer(modifier = Modifier.height(18.dp))

        // 3. Category Filter Rail
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genres.forEachIndexed { index, genre ->
                val isSelected = selectedGenreIndex == index
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) SwaaraElevatedSurface else SwaaraDeepSurface,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "genre_chip_bg"
                )
                val chipBorder by animateColorAsState(
                    targetValue = if (isSelected) SwaaraCyan.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.05f),
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "genre_chip_border"
                )
                val chipTextColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else SwaaraSecondaryText,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "genre_chip_text"
                )

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val chipScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "genre_scale"
                )

                Box(
                    modifier = Modifier
                        .scale(chipScale)
                        .clip(RoundedCornerShape(18.dp))
                        .background(chipBg)
                        .border(1.dp, chipBorder, RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { selectedGenreIndex = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            SwaaraSpeechWaveBars(color = SwaaraCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = genre.title,
                            color = chipTextColor,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        if (isLoadingFeed && podcastEpisodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = SwaaraCyan,
                        modifier = Modifier.size(38.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Calibrating ${currentGenre.title} feeds...",
                        color = SwaaraSecondaryText,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // 4. Featured Episodes Showcase (Horizontal Scroll)
            if (podcastEpisodes.size > 3) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Featured Masterclasses",
                                color = SwaaraPrimaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Full-length episodes curated for ${currentGenre.title}",
                                color = SwaaraSecondaryText,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(podcastEpisodes.take(6)) { episode ->
                            SwaaraFeaturedPodcastCard(
                                episode = episode,
                                onClick = {
                                    playTrackWithService(
                                        context = context,
                                        binder = binder,
                                        videoId = episode.id,
                                        title = episode.title,
                                        artist = episode.author,
                                        thumbnailUrl = episode.thumbnailUrl,
                                        durationText = episode.durationText,
                                        isPodcast = true,
                                        onStarted = { openNowPlaying() }
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 5. Authentic Podcast Episodes Feed
            if (podcastEpisodes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Episodes & Talk Shows",
                                color = SwaaraPrimaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "High-fidelity audio stream with optional video view",
                                color = SwaaraSecondaryText,
                                fontSize = 11.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SwaaraCyan.copy(alpha = 0.20f))
                                .border(1.dp, SwaaraCyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "AUDIO ONLY",
                                color = SwaaraCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                podcastEpisodes.forEach { episode ->
                    SwaaraPodcastEpisodeRow(
                        episode = episode,
                        onPlay = {
                            playTrackWithService(
                                context = context,
                                binder = binder,
                                videoId = episode.id,
                                title = episode.title,
                                artist = episode.author,
                                thumbnailUrl = episode.thumbnailUrl,
                                durationText = episode.durationText,
                                isPodcast = true,
                                onStarted = { openNowPlaying() }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SwaaraAnimatedSoundwaveHero(
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_anim")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val heroScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "hero_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .scale(heroScale)
            .shadow(24.dp, RoundedCornerShape(26.dp), ambientColor = Color.Black, spotColor = SwaaraCyan.copy(alpha = auraPulse * 0.4f))
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0F1424), Color(0xFF080B14))))
            .border(1.2.dp, Brush.horizontalGradient(listOf(SwaaraCyan.copy(alpha = auraPulse), SwaaraViolet.copy(alpha = 0.6f))), RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onExplore
            )
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(26.dp))
        ) {
            val width = size.width
            val height = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SwaaraCyan.copy(alpha = 0.15f * auraPulse), SwaaraViolet.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(width * 0.75f, height * 0.35f),
                    radius = width * 0.6f
                )
            )

            val path1 = Path()
            val path2 = Path()
            val step = 10f
            val midY1 = height * 0.70f
            val midY2 = height * 0.82f

            path1.moveTo(0f, midY1)
            path2.moveTo(0f, midY2)

            var x = 0f
            while (x <= width) {
                val y1 = midY1 + sin((x / width * 3 * Math.PI) + phase).toFloat() * 18f
                val y2 = midY2 + sin((x / width * 4 * Math.PI) - phase).toFloat() * 14f
                path1.lineTo(x, y1)
                path2.lineTo(x, y2)
                x += step
            }

            drawPath(
                path = path1,
                color = SwaaraCyan.copy(alpha = 0.35f),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
            drawPath(
                path = path2,
                color = SwaaraViolet.copy(alpha = 0.30f),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(SwaaraCyan, SwaaraViolet)))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PODCAST STUDIO",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SwaaraSpeechWaveBars(color = SwaaraCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VERIFIED FEEDS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SwaaraCyan,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Voice of Swaara",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Curated Indian & Global Masterclasses, Interviews & Real Stories",
                color = SwaaraCyan,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Full-length episodes, investigative journalism, technology, and neuroscience streams. Audio-only default with optional video mode.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(SwaaraCyan, SwaaraViolet)))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.play),
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START STREAMING",
                            color = Color.Black,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = "Tap episode to listen",
                    color = SwaaraSecondaryText,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SwaaraFeaturedPodcastCard(
    episode: SwaaraPodcastEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "featured_scale"
    )

    Column(
        modifier = modifier
            .width(220.dp)
            .scale(cardScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SwaaraElevatedSurface)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
        ) {
            AsyncImage(
                model = episode.thumbnailUrl,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            if (episode.durationText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(0.8.dp, SwaaraCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = episode.durationText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = episode.title,
            color = SwaaraPrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = episode.author,
            color = SwaaraSecondaryText,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SwaaraPodcastEpisodeRow(
    episode: SwaaraPodcastEpisode,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "row_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .scale(rowScale)
            .clip(RoundedCornerShape(18.dp))
            .background(SwaaraElevatedSurface)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPlay
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp, 52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SwaaraDeepSurface)
        ) {
            AsyncImage(
                model = episode.thumbnailUrl,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            if (episode.durationText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.82f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = episode.durationText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                color = SwaaraPrimaryText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.5.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = episode.author,
                    color = SwaaraCyan,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!episode.viewCountText.isNullOrBlank()) {
                    Text(
                        text = " • ${episode.viewCountText}",
                        color = SwaaraSecondaryText,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(SwaaraCyan, SwaaraViolet))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.play),
                contentDescription = "Play",
                tint = Color.Black,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun SwaaraSpeechWaveBars(
    color: Color = SwaaraCyan,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speech_bars")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h1.dp)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h2.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.85f))
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h3.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.7f))
        )
    }
}

@Composable
fun SwaaraRajShamaniSpotlightBanner(
    onPlayLatest: () -> Unit,
    onViewShow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "spotlight_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .scale(scale)
            .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = SwaaraCyan.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0C1426),
                        Color(0xFF131936),
                        Color(0xFF0A0E1D)
                    )
                )
            )
            .border(1.2.dp, Brush.horizontalGradient(listOf(SwaaraCyan.copy(alpha = 0.75f), SwaaraViolet.copy(alpha = 0.5f))), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SwaaraCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "FEATURED SPOTLIGHT",
                        color = SwaaraCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SwaaraSpeechWaveBars(color = SwaaraCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INDIA #1 SHOW",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Figuring Out with Raj Shamani",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Unfiltered conversations on business, startups, wealth creation, psychology, and personal growth with India's biggest changemakers.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                lineHeight = 16.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(SwaaraCyan, SwaaraViolet)))
                        .clickable(onClick = onPlayLatest)
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.play),
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play Latest",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onViewShow)
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View All Episodes",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
