package com.varshudev.a1swaara.ui.screens.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import com.varshudev.a1swaara.Database
import com.varshudev.a1swaara.LocalAppContainer
import com.varshudev.a1swaara.LocalPlayerServiceBinder
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.service.PlayerService
import com.varshudev.a1swaara.ui.screens.LocalOpenNowPlaying
import com.varshudev.a1swaara.updater.SwaaraUpdateManager
import com.varshudev.a1swaara.utils.intent
import com.varshudev.a1swaara.utils.playTrack
import com.varshudev.a1swaara.utils.playTrackWithService
import com.varshudev.a1swaara.utils.toast
import com.varshudev.a1swaara.ui.components.SwaaraBrandLoader
import com.varshudev.a1swaara.ui.components.SwaaraHomeFeedSkeleton
import com.varshudev.a1swaara.ui.components.SwaaraDisplayItem
import com.varshudev.a1swaara.ui.components.SwaaraGlassContainer
import com.varshudev.a1swaara.ui.components.SwaaraLanguageChips
import com.varshudev.a1swaara.ui.components.SwaaraQuickGridItem
import com.varshudev.a1swaara.ui.components.SwaaraTrendingShelf
import com.varshudev.a1swaara.ui.components.SwaaraCuratedMix
import com.varshudev.a1swaara.ui.components.SwaaraCuratedShelf
import com.varshudev.a1swaara.ui.components.SwaaraObsidian
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraViolet
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.utils.forcePlay
import com.varshudev.a1swaara.utils.thumbnail
import app.a1swaara.providers.youtubemusic.innertube.models.NavigationEndpoint
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.varshudev.a1swaara.ui.screens.artistRoute
import com.varshudev.a1swaara.ui.screens.profileRoute
import com.varshudev.a1swaara.ui.screens.leaderboardRoute
import com.varshudev.a1swaara.ui.screens.jamHubRoute
import com.varshudev.a1swaara.ui.screens.podcastHubRoute
import com.varshudev.a1swaara.ui.screens.podcast.SwaaraPodcastEnvironment
import com.varshudev.a1swaara.ui.screens.liveJamRoute
import com.varshudev.a1swaara.ui.screens.searchRoute
import androidx.compose.ui.text.style.TextAlign
import java.util.Calendar
import com.varshudev.a1swaara.cloud.SwaaraCommunityCharts
import com.varshudev.a1swaara.cloud.TrendingSong
import com.varshudev.a1swaara.cloud.SwaaraListenTogether
import com.varshudev.a1swaara.cloud.RoomState
import com.varshudev.a1swaara.cloud.SwaaraJamEngine
import com.varshudev.a1swaara.cloud.JamRoomState
import com.varshudev.a1swaara.cloud.SwaaraCloudAuth
import com.varshudev.a1swaara.cloud.SwaaraGoogleAuthManager
import com.varshudev.a1swaara.cloud.SupabaseUser
import com.varshudev.a1swaara.cloud.SwaaraRemoteConfig
import com.varshudev.a1swaara.cloud.SwaaraAnnouncement
import com.varshudev.a1swaara.cloud.SwaaraFestiveVibe
import androidx.compose.ui.platform.LocalUriHandler
import com.varshudev.a1swaara.ui.components.SwaaraCommunityShelf
import com.varshudev.a1swaara.ui.components.SwaaraJamRoomDialog


enum class SwaaraFeedMode {
    MUSIC,
    PODCASTS
}
data class CuratedMix(
    val title: String,
    val description: String,
    val query: String,
    val gradient: List<Color>,
    val iconId: Int
)

data class SwaaraArtistDisplayItem(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null
)

@Composable
fun SwaaraHomeScreen(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val container = LocalAppContainer.current
    val binder = LocalPlayerServiceBinder.current
    val openNowPlaying = LocalOpenNowPlaying.current
    var selectedFeedMode by remember { mutableStateOf(SwaaraFeedMode.MUSIC) }
    val coroutineScope = rememberCoroutineScope()

    // Synchronously read persistent user preferences
    val initialPrefs = remember {
        val p = context.getSharedPreferences("swaara_prefs", Context.MODE_PRIVATE)
        val name = p.getString("user_name", "Music Lover") ?: "Music Lover"
        val activeLang = p.getString("active_language", null)
            ?: p.getStringSet("selected_languages", null)?.firstOrNull()
            ?: "for_you"
        Pair(name, activeLang)
    }

    var userName by remember { mutableStateOf(initialPrefs.first) }
    var selectedLanguage by remember { mutableStateOf(initialPrefs.second) }

    var liveTrendingSongs by remember { mutableStateOf<List<SwaaraDisplayItem>>(SwaaraHomeFeedCache.trendingSongs) }
    var liveArtists by remember { mutableStateOf<List<SwaaraArtistDisplayItem>>(SwaaraHomeFeedCache.liveArtists) }
    var quickGridItems by remember { mutableStateOf<List<SwaaraDisplayItem>>(SwaaraHomeFeedCache.quickGridItems) }
    var viralSongs by remember { mutableStateOf<List<SwaaraDisplayItem>>(SwaaraHomeFeedCache.viralSongs) }
    var topTwentyList by remember { mutableStateOf<List<SwaaraDisplayItem>>(SwaaraHomeFeedCache.topTwentyList) }
    var communitySongs by remember { mutableStateOf<List<TrendingSong>>(SwaaraHomeFeedCache.communitySongs) }
    var isRefreshing by remember { mutableStateOf(false) }

    var isLoadingFeed by remember {
        mutableStateOf(!SwaaraHomeFeedCache.hasDataFor(initialPrefs.second))
    }
    val userPrefs = remember { context.getSharedPreferences("swaara_prefs", Context.MODE_PRIVATE) }
    var showJamDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    val activeBroadcast by com.varshudev.a1swaara.cloud.SwaaraPushNotificationEngine.activeNotification.collectAsState()
    var readBroadcastId by remember {
        mutableStateOf(userPrefs.getString("swaara_read_broadcast_id", "") ?: "")
    }
    val hasUnreadBroadcast = activeBroadcast != null && activeBroadcast?.id != readBroadcastId

    val currentJamRoom by SwaaraListenTogether.currentRoom.collectAsState()
    val remoteConfigState by SwaaraRemoteConfig.configState.collectAsState()

    LaunchedEffect(Unit) {
        if (communitySongs.isEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                val trending = SwaaraCommunityCharts.getTrendingSongs(limit = 20)
                withContext(Dispatchers.Main) {
                    communitySongs = trending
                    SwaaraHomeFeedCache.communitySongs = trending
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            SwaaraRemoteConfig.fetchAndApply(context)
            com.varshudev.a1swaara.cloud.SwaaraPushNotificationEngine.checkAndPostNotifications(context)
        }
    }

    val cloudAuth = remember { SwaaraCloudAuth.getInstance(context) }
    val isSignedInFlowVal by cloudAuth.isSignedInFlow.collectAsState()
    var showSignInModal by remember {
        mutableStateOf(!cloudAuth.isSignedIn && !userPrefs.getBoolean("google_signin_prompt_dismissed", false))
    }
    var showSignInBanner by remember {
        mutableStateOf(!cloudAuth.isSignedIn)
    }

    LaunchedEffect(isSignedInFlowVal) {
        if (isSignedInFlowVal) {
            showSignInModal = false
            showSignInBanner = false
        }
    }

    if (showJamDialog) {
        SwaaraJamRoomDialog(onDismiss = { showJamDialog = false })
    }

    if (showNotificationDialog) {
        val currentBroadcast = activeBroadcast
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showNotificationDialog = false
                currentBroadcast?.let { b ->
                    readBroadcastId = b.id
                    userPrefs.edit().putString("swaara_read_broadcast_id", b.id).apply()
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SwaaraElevatedSurface)
                    .border(1.dp, SwaaraMagenta.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(SwaaraViolet, SwaaraMagenta))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notification_bell),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ANNOUNCEMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SwaaraMagenta,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentBroadcast?.title ?: "No new announcements",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SwaaraPrimaryText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentBroadcast?.message
                            ?: "You are up to date! Live broadcast messages and festive announcements will appear here.",
                        fontSize = 13.sp,
                        color = SwaaraSecondaryText,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    if (!currentBroadcast?.actionUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(SwaaraViolet, SwaaraMagenta)))
                                .clickable {
                                    showNotificationDialog = false
                                    readBroadcastId = currentBroadcast!!.id
                                    userPrefs.edit().putString("swaara_read_broadcast_id", currentBroadcast.id).apply()
                                    try {
                                        uriHandler.openUri(currentBroadcast.actionUrl!!)
                                    } catch (_: Exception) {}
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "View Details",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable {
                                showNotificationDialog = false
                                currentBroadcast?.let { b ->
                                    readBroadcastId = b.id
                                    userPrefs.edit().putString("swaara_read_broadcast_id", b.id).apply()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Dismiss",
                            color = SwaaraSecondaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showSignInModal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showSignInModal = false
                userPrefs.edit().putBoolean("google_signin_prompt_dismissed", true).apply()
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SwaaraElevatedSurface)
                    .border(1.dp, SwaaraMagenta.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(SwaaraViolet, SwaaraMagenta))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.person),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sign in to A1 Swaara",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SwaaraPrimaryText
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Connect your Google account to back up your liked songs and playlists, earn Top Listener ranks on leaderboards, and listen together in Live Jam rooms.",
                        fontSize = 13.sp,
                        color = SwaaraSecondaryText,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(SwaaraViolet, SwaaraMagenta)))
                            .clickable {
                                showSignInModal = false
                                userPrefs.edit().putBoolean("google_signin_prompt_dismissed", true).apply()
                                coroutineScope.launch {
                                    SwaaraGoogleAuthManager.launchGoogleSignIn(context)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In with Google",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable {
                                showSignInModal = false
                                userPrefs.edit().putBoolean("google_signin_prompt_dismissed", true).apply()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Continue as Guest",
                            color = SwaaraSecondaryText,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    val curatedMixes = remember(selectedLanguage) {
        val isForYou = selectedLanguage.equals("for_you", ignoreCase = true)
        val langTitle = if (isForYou) "Daily" else selectedLanguage.replaceFirstChar { it.uppercase() }
        val prefix = if (isForYou) "" else "$selectedLanguage "
        listOf(
            SwaaraCuratedMix(
                id = "pulse",
                title = if (isForYou) "Daily Flow" else "$langTitle Pulse",
                description = if (isForYou) "Curated hits tailored for your vibe" else "Today's biggest $langTitle tracks",
                gradientColors = listOf(Color(0xFF2A0845), Color(0xFF6441A5)),
                query = if (isForYou) "latest top trending music hits official" else "$prefix top trending hits",
                iconId = R.drawable.sparkles
            ),
            SwaaraCuratedMix(
                id = "midnight",
                title = "Midnight Swaara",
                description = "Late-night atmospheric sounds",
                gradientColors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
                query = if (isForYou) "lofi beats chill late night relaxing" else "$prefix late night lofi chill",
                iconId = R.drawable.musical_notes
            ),
            SwaaraCuratedMix(
                id = "chill",
                title = "Chill Flow",
                description = "Relaxing beats & smooth vocals",
                gradientColors = listOf(Color(0xFF134E5E), Color(0xFF71B280)),
                query = if (isForYou) "acoustic chill relaxing songs" else "$prefix acoustic chill relaxing",
                iconId = R.drawable.heart
            ),
            SwaaraCuratedMix(
                id = "favourites",
                title = "All-Time Favourites",
                description = "Timeless songs you love",
                gradientColors = listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2)),
                query = if (isForYou) "all time greatest hits evergreen songs" else "$prefix all time evergreen classics",
                iconId = R.drawable.disc
            )
        )
    }

    // Play Function with Infinite YouTube Radio Auto-Queue
    val playSongWithInfiniteRadio: (String, String, String?, String?) -> Unit = { videoId, songTitle, artist, thumb ->
        if (SwaaraUpdateManager.isUpdateAvailable.value && SwaaraUpdateManager.isMandatoryLockActive.value) {
            context.toast("Online music is locked. Please update A1 Swaara to stream online tracks.")
        } else {
            playTrackWithService(
                context = context,
                binder = binder,
                videoId = videoId,
                title = songTitle,
                artist = artist,
                thumbnailUrl = thumb,
                onStarted = { openNowPlaying() }
            )
        }
    }

    // Load Live YouTube Music Feed for Selected Language & Personal History
    val loadLiveFeed: (String, Boolean) -> Unit = { lang, forceRefresh ->
        if (forceRefresh || !SwaaraHomeFeedCache.hasDataFor(lang)) {
            isLoadingFeed = true
            coroutineScope.launch(Dispatchers.IO) {
                // 1. Fetch real user history from Database for Jump Back In
                val userHistoryEvents = runCatching {
                    Database.events().first()
                }.getOrNull().orEmpty()

                val historyDisplayItems = userHistoryEvents
                    .map { it.song }
                    .distinctBy { it.id }
                    .take(6)
                    .map { song ->
                        SwaaraDisplayItem(
                            id = song.id,
                            title = song.title,
                            subtitle = song.artistsText ?: "Artist",
                            thumbnailUrl = song.thumbnailUrl ?: "https://i.ytimg.com/vi/${song.id}/maxresdefault.jpg"
                        )
                    }

                // 2. Build smart queries based on selected category / language
                val isForYou = lang.equals("for_you", ignoreCase = true)

                // If "For You", determine if we can extract top artists/keywords from history
                val topHistoryArtist = if (isForYou && historyDisplayItems.isNotEmpty()) {
                    historyDisplayItems.firstOrNull()?.subtitle?.split(",")?.firstOrNull()?.trim()
                } else null

                val trendingQuery = when {
                    isForYou && !topHistoryArtist.isNullOrBlank() -> "$topHistoryArtist latest songs hits"
                    isForYou -> "top trending music hits official"
                    lang.equals("english", ignoreCase = true) -> "trending top hits official"
                    else -> "$lang latest trending songs official"
                }

                val viralQuery = when {
                    isForYou -> "viral music hits today"
                    lang.equals("english", ignoreCase = true) -> "top viral songs"
                    else -> "$lang viral hits chart"
                }

                val topChartQuery = when {
                    isForYou -> "billboard top 50 global hits"
                    lang.equals("english", ignoreCase = true) -> "billboard top 50"
                    else -> "$lang top 50 songs chart"
                }

                val artistQuery = when {
                    isForYou -> "popular global music artists"
                    lang.equals("english", ignoreCase = true) -> "popular music artists"
                    else -> "$lang popular playback singers"
                }

                val liveSongsResult = container.searchResultRepository.searchSongs(trendingQuery, null)?.getOrNull()?.items
                val viralResult = container.searchResultRepository.searchSongs(viralQuery, null)?.getOrNull()?.items
                val topChartResult = container.searchResultRepository.searchSongs(topChartQuery, null)?.getOrNull()?.items
                val liveArtistsResult = runCatching {
                    container.searchResultRepository.searchArtists(artistQuery, null)?.getOrNull()?.items
                }.getOrNull()

                withContext(Dispatchers.Main) {
                    val mappedSongs = (!liveSongsResult.isNullOrEmpty()).let { hasSongs ->
                        if (hasSongs) {
                            liveSongsResult!!.map { item ->
                                SwaaraDisplayItem(
                                    id = item.key,
                                    title = item.info?.name ?: "Unknown Title",
                                    subtitle = item.authors?.joinToString { it.name.orEmpty() }?.replace(Regex("[,&\\s]{2,}"), ", ")?.trim(',', ' ') ?: "Various Artists",
                                    thumbnailUrl = item.thumbnail?.url?.thumbnail(800) ?: "https://i.ytimg.com/vi/${item.key}/maxresdefault.jpg"
                                )
                            }
                        } else emptyList()
                    }

                    liveTrendingSongs = mappedSongs

                    // Set Jump Back In: Prioritize real user listening history, fallback to trending
                    val resolvedQuickGrid = if (historyDisplayItems.isNotEmpty()) {
                        if (historyDisplayItems.size < 6 && mappedSongs.isNotEmpty()) {
                            val historyIds = historyDisplayItems.map { it.id }.toSet()
                            val filler = mappedSongs.filter { it.id !in historyIds }.take(6 - historyDisplayItems.size)
                            historyDisplayItems + filler
                        } else {
                            historyDisplayItems
                        }
                    } else {
                        mappedSongs.take(6)
                    }
                    quickGridItems = resolvedQuickGrid

                    val resolvedArtists = if (!liveArtistsResult.isNullOrEmpty()) {
                        liveArtistsResult.take(12).map { item ->
                            SwaaraArtistDisplayItem(
                                id = item.key,
                                name = item.info?.name ?: "Artist",
                                thumbnailUrl = item.thumbnail?.url?.thumbnail(800)
                            )
                        }
                    } else emptyList()
                    liveArtists = resolvedArtists

                    val resolvedViral = if (!viralResult.isNullOrEmpty()) {
                        viralResult.map { item ->
                            SwaaraDisplayItem(
                                id = item.key,
                                title = item.info?.name ?: "Viral Hit",
                                subtitle = item.authors?.joinToString { it.name.orEmpty() }?.replace(Regex("[,&\\s]{2,}"), ", ")?.trim(',', ' ') ?: "Artist",
                                thumbnailUrl = item.thumbnail?.url?.thumbnail(800) ?: "https://i.ytimg.com/vi/${item.key}/maxresdefault.jpg"
                            )
                        }
                    } else emptyList()
                    viralSongs = resolvedViral

                    val allSongs = (topChartResult ?: liveSongsResult).orEmpty()
                    val resolvedTopTwenty = if (allSongs.isNotEmpty()) {
                        allSongs.take(20).map { item ->
                            SwaaraDisplayItem(
                                id = item.key,
                                title = item.info?.name ?: "Top Song",
                                subtitle = item.authors?.joinToString { it.name.orEmpty() }?.replace(Regex("[,&\\s]{2,}"), ", ")?.trim(',', ' ') ?: "Artist",
                                thumbnailUrl = item.thumbnail?.url?.thumbnail(800) ?: "https://i.ytimg.com/vi/${item.key}/maxresdefault.jpg"
                            )
                        }
                    } else emptyList()
                    topTwentyList = resolvedTopTwenty

                    // Persist to in-memory cache so leaving and returning from Settings does not reload
                    SwaaraHomeFeedCache.cachedLanguage = lang
                    SwaaraHomeFeedCache.trendingSongs = mappedSongs
                    SwaaraHomeFeedCache.quickGridItems = resolvedQuickGrid
                    SwaaraHomeFeedCache.liveArtists = resolvedArtists
                    SwaaraHomeFeedCache.viralSongs = resolvedViral
                    SwaaraHomeFeedCache.topTwentyList = resolvedTopTwenty
                    SwaaraHomeFeedCache.isInitialized = true

                    isLoadingFeed = false
                    isRefreshing = false
                }
            }
        } else {
            // Restore from cache with zero delay or network overhead
            liveTrendingSongs = SwaaraHomeFeedCache.trendingSongs
            quickGridItems = SwaaraHomeFeedCache.quickGridItems
            liveArtists = SwaaraHomeFeedCache.liveArtists
            viralSongs = SwaaraHomeFeedCache.viralSongs
            topTwentyList = SwaaraHomeFeedCache.topTwentyList
            isLoadingFeed = false
        }
    }

    LaunchedEffect(selectedLanguage) {
        val p = context.getSharedPreferences("swaara_prefs", Context.MODE_PRIVATE)
        userName = p.getString("user_name", "Music Lover") ?: "Music Lover"
        p.edit().putString("active_language", selectedLanguage).apply()
        loadLiveFeed(selectedLanguage, false)
    }

    val greeting = remember {
        val cal = Calendar.getInstance()
        val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (isSunday) {
            when (hour) {
                in 4..11 -> "Sunday Morning Vibes"
                in 12..16 -> "Lazy Sunday Afternoon"
                in 17..21 -> "Sunday Sunset Chill"
                else -> "Quiet Sunday Night"
            }
        } else {
            when (hour) {
                in 4..11 -> "Good Morning"
                in 12..16 -> "Good Afternoon"
                in 17..21 -> "Good Evening"
                else -> "Welcome back"
            }
        }
    }

    val displayGreeting = remember(userName, greeting) {
        val cleanName = userName.trim()
        if (cleanName.isNotBlank() && !cleanName.equals("Music Lover", ignoreCase = true)) {
            "$greeting, $cleanName"
        } else {
            greeting
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SwaaraObsidian)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 130.dp)
    ) {
        // 1. Top Header Area
        val engineRoom by SwaaraJamEngine.currentRoom.collectAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile & Greeting
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { profileRoute.global() }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(SwaaraViolet, SwaaraMagenta)))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!cloudAuth.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = cloudAuth.avatarUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Text(
                            text = cloudAuth.displayName.firstOrNull()?.uppercase() ?: "S",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = displayGreeting,
                    color = SwaaraPrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                // Jam Hub Beacon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SwaaraElevatedSurface)
                        .border(1.dp, SwaaraMagenta.copy(alpha = 0.5f), CircleShape)
                        .clickable { jamHubRoute.global() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.musical_notes),
                        contentDescription = "Jam Hub",
                        tint = SwaaraMagenta,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Manual Refresh Feed Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SwaaraElevatedSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable {
                            if (!isRefreshing && !isLoadingFeed) {
                                isRefreshing = true
                                context.toast("Refreshing chartbusters & cloud feed...")
                                coroutineScope.launch(Dispatchers.IO) {
                                    SwaaraRemoteConfig.fetchAndApply(context)
                                    com.varshudev.a1swaara.cloud.SwaaraPushNotificationEngine.checkAndPostNotifications(context)
                                }
                                loadLiveFeed(selectedLanguage, true)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = SwaaraMagenta,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.sync),
                            contentDescription = "Refresh Feed",
                            tint = SwaaraPrimaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Broadcast & Notification Beacon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SwaaraElevatedSurface)
                        .border(
                            1.dp,
                            if (hasUnreadBroadcast) SwaaraMagenta.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f),
                            CircleShape
                        )
                        .clickable { showNotificationDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notification_bell),
                        contentDescription = "Announcements",
                        tint = if (hasUnreadBroadcast) SwaaraMagenta else SwaaraPrimaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    if (hasUnreadBroadcast) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(SwaaraMagenta)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SwaaraElevatedSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable(onClick = onSearchClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "Search",
                        tint = SwaaraPrimaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SwaaraElevatedSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings),
                        contentDescription = "Settings",
                        tint = SwaaraPrimaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        // Dual Feed Mode Switcher (Music vs Podcasts)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(SwaaraElevatedSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isMusic = selectedFeedMode == SwaaraFeedMode.MUSIC
                val isPodcast = selectedFeedMode == SwaaraFeedMode.PODCASTS

                val musicBgModifier = if (isMusic) {
                    Modifier.background(Brush.horizontalGradient(listOf(SwaaraViolet, SwaaraMagenta)))
                } else {
                    Modifier.background(Color.Transparent)
                }

                val podcastBgModifier = if (isPodcast) {
                    Modifier.background(Brush.horizontalGradient(listOf(SwaaraCyan, SwaaraViolet)))
                } else {
                    Modifier.background(Color.Transparent)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .then(musicBgModifier)
                        .clickable { selectedFeedMode = SwaaraFeedMode.MUSIC }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.musical_notes),
                            contentDescription = null,
                            tint = if (isMusic) Color.White else SwaaraSecondaryText,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Music",
                            fontSize = 12.5.sp,
                            fontWeight = if (isMusic) FontWeight.Bold else FontWeight.Medium,
                            color = if (isMusic) Color.White else SwaaraSecondaryText
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .then(podcastBgModifier)
                        .clickable { selectedFeedMode = SwaaraFeedMode.PODCASTS }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.radio),
                            contentDescription = null,
                            tint = if (isPodcast) Color.Black else SwaaraSecondaryText,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Podcasts",
                            fontSize = 12.5.sp,
                            fontWeight = if (isPodcast) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPodcast) Color.Black else SwaaraSecondaryText
                        )
                    }
                }
            }
        }

        // Google Sign-In / Account Sync Banner (if not logged in)
        if (!cloudAuth.isSignedIn && showSignInBanner) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1E1035), Color(0xFF2A153A))))
                    .border(1.dp, SwaaraViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(SwaaraViolet, SwaaraMagenta))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.person),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Connect Google Account",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Sync playlists & join live jams",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(listOf(SwaaraViolet, SwaaraMagenta)))
                                .clickable {
                                    coroutineScope.launch {
                                        SwaaraGoogleAuthManager.launchGoogleSignIn(context)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Sign In",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { showSignInBanner = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Active Live Jam Room Banner (if user is in a session)
        if (engineRoom != null) {
            val r = engineRoom!!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(SwaaraViolet, SwaaraMagenta)))
                    .clickable { liveJamRoute.global() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.radio),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Live Jam: ${r.roomName} (${r.roomCode})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (r.isHost) "Hosting synchronized playback" else "Listening with ${r.hostName} (${r.activeListenersCount} listeners)",
                                fontSize = 10.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        } else if (currentJamRoom != null) {
            val activeRoom = currentJamRoom!!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(SwaaraViolet, SwaaraMagenta)))
                    .clickable { liveJamRoute.global() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.radio),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Live Jam: ${activeRoom.roomName} (${activeRoom.roomCode})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (activeRoom.isHost) "Hosting synchronized playback" else "Listening with ${activeRoom.hostName}",
                                fontSize = 10.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "OPEN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Cloud Broadcast Announcement Banner
        val activeAnnouncement: SwaaraAnnouncement? = remoteConfigState.announcement
        if (activeAnnouncement != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SwaaraElevatedSurface)
                    .border(1.dp, SwaaraCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SwaaraCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.sparkles),
                            contentDescription = null,
                            tint = SwaaraCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeAnnouncement.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SwaaraPrimaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeAnnouncement.message,
                            fontSize = 11.5.sp,
                            color = SwaaraSecondaryText
                        )
                    }

                    if (activeAnnouncement.isDismissible) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Dismiss",
                            tint = SwaaraSecondaryText,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    SwaaraRemoteConfig.dismissAnnouncement(context, activeAnnouncement.id)
                                }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Cloud Festive Live Broadcast (Flipkart/Amazon Festive Vibe)
        val activeFestiveVibe: SwaaraFestiveVibe? = remoteConfigState.festiveVibe
        if (activeFestiveVibe != null) {
            SwaaraFestiveCard(
                festiveVibe = activeFestiveVibe,
                onDismiss = {
                    SwaaraRemoteConfig.dismissFestiveVibe(context, activeFestiveVibe.id)
                },
                onClick = {
                    val url = activeFestiveVibe.actionUrl
                    if (!url.isNullOrBlank()) {
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            uriHandler.openUri(url)
                        } else {
                            searchRoute.global(url)
                        }
                    } else {
                        searchRoute.global("${activeFestiveVibe.festivalName} songs")
                    }
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (selectedFeedMode == SwaaraFeedMode.PODCASTS) {
            SwaaraPodcastEnvironment()
        } else {
            // 2. Category / Language Discovery Rail
        SwaaraLanguageChips(
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { langId ->
                selectedLanguage = langId
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoadingFeed && quickGridItems.isEmpty()) {
            SwaaraBrandLoader(language = selectedLanguage)
            Spacer(modifier = Modifier.height(10.dp))
            SwaaraHomeFeedSkeleton()
        } else {
            // 3. Primary Content: Jump Back In (2-Column Tactile Grid)
            if (quickGridItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Jump Back In",
                        color = SwaaraPrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pick up where you left off",
                        color = SwaaraSecondaryText,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val chunked = quickGridItems.take(6).chunked(2)
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    SwaaraQuickGridItem(
                                        item = item,
                                        onClick = {
                                            playSongWithInfiniteRadio(item.id, item.title, item.subtitle, item.thumbnailUrl)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 4. Discovery: Curated Mixes & Radio (Editorial Cards)
            SwaaraCuratedShelf(
                title = "Curated Mixes",
                subtitle = "Handcrafted for your daily soundtrack",
                mixes = curatedMixes,
                onMixClick = { mix ->
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            container.searchResultRepository.searchSongs(mix.query, null)?.getOrNull()?.items
                        }
                        result?.firstOrNull()?.let { firstSong ->
                            playSongWithInfiniteRadio(
                                firstSong.key,
                                firstSong.info?.name ?: mix.title,
                                firstSong.authors?.joinToString { it.name.orEmpty() } ?: mix.title,
                                firstSong.thumbnail?.url
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Trending Music: Horizontal Ranked Cards
            if (liveTrendingSongs.isNotEmpty()) {
                val isForYou = selectedLanguage.equals("for_you", ignoreCase = true)
                val shelfTitle = if (isForYou) "Recommended For You" else "Trending ${selectedLanguage.replaceFirstChar { it.uppercase() }} Hits"
                val shelfSubtitle = if (isForYou) "Curated tracks chosen based on your listening vibe" else "Live charts updating continuously with fresh hits"
                SwaaraTrendingShelf(
                    title = shelfTitle,
                    subtitle = shelfSubtitle,
                    items = liveTrendingSongs,
                    onItemClick = { item ->
                        playSongWithInfiniteRadio(item.id, item.title, item.subtitle, item.thumbnailUrl)
                    }
                )
            }

            // A1 Swaara Community Charts Shelf (Live Crowdsourced Leaderboard)
            if (communitySongs.isNotEmpty()) {
                SwaaraCommunityShelf(
                    songs = communitySongs,
                    onSongClick = { song ->
                        playSongWithInfiniteRadio(song.songId, song.title, song.artist, song.artworkUrl)
                    }
                )
            }



            // Top Artists Shelf
            if (liveArtists.isNotEmpty()) {
                val isForYou = selectedLanguage.equals("for_you", ignoreCase = true)
                val langTitle = if (isForYou) "Popular" else selectedLanguage.replaceFirstChar { it.uppercase() }
                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Top $langTitle Artists",
                            color = SwaaraPrimaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap to explore their full discography & albums",
                            color = SwaaraSecondaryText,
                            fontSize = 12.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(liveArtists) { artist ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(88.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        artistRoute.global(artist.id)
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(SwaaraMagenta, SwaaraViolet)))
                                        .border(1.5.dp, SwaaraCyan.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = artist.name.take(1),
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    AsyncImage(
                                        model = artist.thumbnailUrl,
                                        contentDescription = artist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = artist.name,
                                    color = SwaaraPrimaryText,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 6. Viral Hits Shelf
            if (viralSongs.isNotEmpty()) {
                val isForYou = selectedLanguage.equals("for_you", ignoreCase = true)
                val langTitle = if (isForYou) "Global" else selectedLanguage.replaceFirstChar { it.uppercase() }
                Spacer(modifier = Modifier.height(6.dp))
                SwaaraTrendingShelf(
                    title = "Viral & Top Hits",
                    subtitle = "Most viral tracks in $langTitle",
                    items = viralSongs,
                    onItemClick = { item ->
                        playSongWithInfiniteRadio(item.id, item.title, item.subtitle, item.thumbnailUrl)
                    }
                )
            }

            // 7. Top 20 Chartbusters (Clean Ranked List Rows)
            if (topTwentyList.isNotEmpty()) {
                val isForYou = selectedLanguage.equals("for_you", ignoreCase = true)
                val langTitle = if (isForYou) "Global" else selectedLanguage.replaceFirstChar { it.uppercase() }
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Top 20 Chartbusters",
                        color = SwaaraPrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Most streamed songs in $langTitle",
                        color = SwaaraSecondaryText,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    topTwentyList.forEachIndexed { idx, song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SwaaraDeepSurface)
                                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                .clickable {
                                    playSongWithInfiniteRadio(song.id, song.title, song.subtitle, song.thumbnailUrl)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank Number
                            Text(
                                text = (idx + 1).toString().padStart(2, '0'),
                                color = if (idx < 3) SwaaraMagenta else SwaaraSecondaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(26.dp)
                            )

                            // Artwork
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SwaaraElevatedSurface)
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = SwaaraPrimaryText,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = song.subtitle,
                                    color = SwaaraSecondaryText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Play Button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.play),
                                    contentDescription = "Play",
                                    tint = SwaaraPrimaryText,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun SwaaraFestiveCard(
    festiveVibe: SwaaraFestiveVibe,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = remember(festiveVibe.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(festiveVibe.accentColorHex))
        } catch (_: Exception) {
            Color(0xFFFFD700)
        }
    }
    val secondaryColor = remember(festiveVibe.secondaryColorHex) {
        try {
            Color(android.graphics.Color.parseColor(festiveVibe.secondaryColorHex))
        } catch (_: Exception) {
            Color(0xFFFF2DAA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.22f),
                        secondaryColor.copy(alpha = 0.16f),
                        SwaaraElevatedSurface
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.65f),
                        accentColor.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Festive Pill Tag and Dismiss action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.22f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.sparkles),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = festiveVibe.badgeText.uppercase(),
                            color = accentColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                if (festiveVibe.isDismissible) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "Dismiss",
                        tint = SwaaraSecondaryText,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Content Column + Graphic/Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = festiveVibe.title,
                        color = SwaaraPrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = festiveVibe.message,
                        color = SwaaraSecondaryText,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Festive CTA Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.play),
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = festiveVibe.actionText,
                                color = Color.Black,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!festiveVibe.bannerImageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    AsyncImage(
                        model = festiveVibe.bannerImageUrl,
                        contentDescription = festiveVibe.festivalName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }
}
