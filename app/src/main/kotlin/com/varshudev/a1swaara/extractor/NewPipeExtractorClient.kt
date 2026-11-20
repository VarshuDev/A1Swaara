package com.varshudev.a1swaara.extractor

import android.content.Context
import android.util.Log
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.downloader.Downloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import com.varshudev.a1swaara.preferences.PlayerPreferences
import kotlin.math.abs
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

object NewPipeExtractorClient {
    @Volatile private var initialized = false
    private val initializationLock = Any()
    private val lock = ReentrantLock()
    private val downloaderLock = Any()
    private val inFlightAudioResolutionsLock = Any()
    private val inFlightAudioResolutions = mutableMapOf<String, FutureTask<NewPipeAudioResult>>()
    private val appContextRef = AtomicReference<Context?>()
    @Volatile private var sharedSystemDownloader: Downloader? = null
    private var lastSuccessfulDnsIndex: Int? = null

    private val userPreferredTrackKeys = ConcurrentHashMap<String, String>()
    private val _availableTracksByVideoId = MutableStateFlow<Map<String, List<SwaaraAudioTrackOption>>>(emptyMap())
    val availableTracksByVideoId: StateFlow<Map<String, List<SwaaraAudioTrackOption>>> = _availableTracksByVideoId.asStateFlow()

    private val _selectedTrackKeyByVideoId = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedTrackKeyByVideoId: StateFlow<Map<String, String>> = _selectedTrackKeyByVideoId.asStateFlow()

    fun getPreferredTrackKey(videoId: String): String? = userPreferredTrackKeys[videoId]

    fun setPreferredTrackKey(videoId: String, trackKey: String) {
        userPreferredTrackKeys[videoId] = trackKey
        _selectedTrackKeyByVideoId.value = _selectedTrackKeyByVideoId.value + (videoId to trackKey)
    }

    fun getAvailableTracks(videoId: String): List<SwaaraAudioTrackOption> =
        _availableTracksByVideoId.value[videoId].orEmpty()

    /**
     * Populates available audio language tracks for a videoId if not yet in memory
     * (for example, when playback was served directly from ExoPlayer's disk cache).
     */
    fun ensureTracksLoaded(videoId: String) {
        if (videoId.isBlank() || videoId.startsWith("local:")) return
        if (_availableTracksByVideoId.value.containsKey(videoId)) return
        preloadAudioStream(videoId)
    }

    fun ensureInitialized(context: Context) {
        appContextRef.set(context.applicationContext)
        ensureInitialized()
    }

    fun ensureInitialized() {
        if (initialized) return
        synchronized(initializationLock) {
            if (initialized) return
            configureDownloader(NewPipeDnsTarget.System)
            initialized = true
        }
    }

    @Throws(IOException::class, ExtractionException::class)
    fun resolveAudioStream(videoId: String): NewPipeAudioResult {
        ensureInitialized()
        return awaitAudioResolution(
            videoId = videoId,
            task = audioResolutionTask(videoId),
            timeoutSeconds = PLAYBACK_RESOLVE_TIMEOUT_SECONDS,
        )
    }

    /** Opportunistic preload that shares in-flight resolutions and queues safely. */
    fun preloadAudioStream(videoId: String): NewPipeAudioResult? {
        ensureInitialized()
        val task = synchronized(inFlightAudioResolutionsLock) {
            inFlightAudioResolutions[videoId] ?: audioResolutionTaskLocked(videoId)
        }

        return runCatching {
            awaitAudioResolution(
                videoId = videoId,
                task = task,
                timeoutSeconds = PRELOAD_RESOLVE_TIMEOUT_SECONDS,
            )
        }.getOrNull()
    }

    /**
     * Shares an in-flight extraction by video ID. A waiting caller timing out or being interrupted
     * must not cancel the shared NewPipe task: playback or a pre-cache request may still need it.
     * The task removes itself only after it has actually completed.
     */
    private fun audioResolutionTask(videoId: String): FutureTask<NewPipeAudioResult> =
        synchronized(inFlightAudioResolutionsLock) {
            audioResolutionTaskLocked(videoId)
        }

    private fun audioResolutionTaskLocked(videoId: String): FutureTask<NewPipeAudioResult> =
        inFlightAudioResolutions[videoId] ?: FutureTask {
            lock.lockInterruptibly()
            try {
                resolveAudioStreamWithFallback(videoId)
            } finally {
                lock.unlock()
            }
        }.also { task ->
            inFlightAudioResolutions[videoId] = task
            RESOLVER_EXECUTOR.execute {
                task.run()
                synchronized(inFlightAudioResolutionsLock) {
                    inFlightAudioResolutions.remove(videoId, task)
                }
            }
        }

    @Throws(IOException::class, ExtractionException::class)
    private fun awaitAudioResolution(
        videoId: String,
        task: FutureTask<NewPipeAudioResult>,
        timeoutSeconds: Long,
    ): NewPipeAudioResult = try {
        task.get(timeoutSeconds, TimeUnit.SECONDS)
    } catch (error: TimeoutException) {
        throw IOException("NewPipe stream resolution timed out for $videoId", error)
    } catch (error: ExecutionException) {
        val cause = error.cause ?: error
        when (cause) {
            is IOException -> throw cause
            is ExtractionException -> throw cause
            else -> throw IOException("NewPipe stream resolution failed for $videoId", cause)
        }
    } catch (error: InterruptedException) {
        Thread.currentThread().interrupt()
        throw IOException("NewPipe stream resolution interrupted for $videoId", error)
    }

    private fun resolveAudioStreamWithFallback(videoId: String): NewPipeAudioResult {
        var firstError: Throwable? = null
        var lastError: Throwable? = null

        fun recordRetryableFailure(error: Throwable, dnsTarget: NewPipeDnsTarget) {
            if (error.isThreadInterruption()) {
                Thread.currentThread().interrupt()
                throw error
            }
            if (!error.isRetriableNetworkFailure()) throw error
            Log.w(TAG, "Audio stream resolve failed videoId=$videoId dnsTarget=${dnsTarget.label}", error)
            if (firstError == null) firstError = error
            lastError = error
        }

        dnsFallbackTargets().forEach { dnsTarget ->
            try {
                Log.d(TAG, "Resolving audio stream videoId=$videoId dnsTarget=${dnsTarget.label}")
                val result = resolveAudioStream(videoId, dnsTarget)
                if (dnsTarget is NewPipeDnsTarget.Resolved) {
                    lastSuccessfulDnsIndex = dnsTarget.index
                }
                return result
            } catch (error: IOException) {
                recordRetryableFailure(error, dnsTarget)
            } catch (error: ExtractionException) {
                recordRetryableFailure(error, dnsTarget)
            }
        }

        lastError?.let { error ->
            firstError?.takeIf { it !== error }?.let(error::addSuppressed)
            throw error
        }

        throw ExtractionException("No DNS fallback targets configured")
    }

    private fun Throwable.isThreadInterruption(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is InterruptedException) return true
            if (current is InterruptedIOException && Thread.currentThread().isInterrupted) return true
            current = current.cause
        }
        return false
    }

    private fun Throwable.isRetriableNetworkFailure(): Boolean =
        generateSequence(this) { error -> error.cause }.any { error ->
            error is UnknownHostException ||
                error is SocketTimeoutException ||
                error is ConnectException ||
                error is NoRouteToHostException ||
                error is SocketException ||
                error is EOFException
        }

    @Throws(IOException::class, ExtractionException::class)
    private fun resolveAudioStream(videoId: String, dnsTarget: NewPipeDnsTarget): NewPipeAudioResult {
        configureDownloader(dnsTarget)

        val url = "https://www.youtube.com/watch?v=$videoId"
        val service = ServiceList.YouTube as YoutubeService
        val streamInfo = StreamInfo.getInfo(service, url)
        logStreamInfo(videoId, dnsTarget, streamInfo)
        val audioStreams = getPlayableAudioStreams(streamInfo)
        val trackOptions = extractAudioTrackOptions(audioStreams)
        val preferredTrackKey = userPreferredTrackKeys[videoId]
        val selectedOption = selectAudioTrackOption(trackOptions, preferredTrackKey)
        val audioStream = selectedOption?.stream ?: selectBestQualityStream(audioStreams)

        if (trackOptions.isNotEmpty()) {
            _availableTracksByVideoId.value = _availableTracksByVideoId.value + (videoId to trackOptions)
            selectedOption?.let { opt ->
                _selectedTrackKeyByVideoId.value = _selectedTrackKeyByVideoId.value + (videoId to opt.trackKey)
            }
        }

        val videoStream = if (audioStream == null) {
            selectDefaultMuxedVideoStream(getPlayableMuxedVideoStreams(streamInfo))
        } else {
            null
        }

        if (audioStream == null && videoStream == null) {
            throw ExtractionException(
                "No playable audio stream found; " +
                    "audio=${streamInfo.audioStreams?.size ?: 0} " +
                    "video=${streamInfo.videoStreams?.size ?: 0} " +
                    "videoOnly=${streamInfo.videoOnlyStreams?.size ?: 0} " +
                    "errors=${streamInfo.errors?.joinToString { it.message.orEmpty() }.orEmpty()}"
            )
        }

        if (audioStream != null) {
            Log.i(
                TAG,
                "Audio stream resolved videoId=$videoId dnsTarget=${dnsTarget.label} " +
                    "itag=${audioStream.itag} bitrate=${audioStream.averageBitrate} " +
                    "track=${selectedOption?.displayName} type=${audioStream.audioTrackType} " +
                    "totalTracks=${trackOptions.size}"
            )
        } else if (videoStream != null) {
            Log.i(
                TAG,
                "Muxed video stream resolved videoId=$videoId dnsTarget=${dnsTarget.label} " +
                    "itag=${videoStream.itag} bitrate=${videoStream.bitrate}"
            )
        }

        return NewPipeAudioResult(
            streamInfo = streamInfo,
            audioStreams = audioStreams,
            audioStream = audioStream,
            videoStream = videoStream,
            availableTracks = trackOptions,
            selectedTrackKey = selectedOption?.trackKey
        )
    }

    private fun configureDownloader(dnsTarget: NewPipeDnsTarget) {
        NewPipe.init(
            createDownloader(dnsTarget),
            Localization.DEFAULT,
            ContentCountry.DEFAULT
        )
    }

    private fun createDownloader(dnsTarget: NewPipeDnsTarget): Downloader {
        val context = appContextRef.get()
        val fallback = NewPipeDownloader(NewPipeDownloader.client(dnsTarget))

        // kathttp3 resolves the URL hostname itself, so it is used for NewPipe's normal resolver
        // path. Fixed-address retry uses the OkHttp DNS override fallback.
        return if (dnsTarget == NewPipeDnsTarget.System) {
            synchronized(downloaderLock) {
                sharedSystemDownloader ?: createSystemDownloader(fallback, context)
                    .also { sharedSystemDownloader = it }
            }
        } else {
            fallback
        }
    }

    private fun getPlayableAudioStreams(streamInfo: StreamInfo): List<AudioStream> =
        streamInfo.audioStreams
            ?.filter { stream ->
                stream.isUrl &&
                    stream.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                    stream.isPlayable()
            }
            ?.preferNonIosAudioStreams()
            .orEmpty()

    private fun getPlayableMuxedVideoStreams(streamInfo: StreamInfo): List<VideoStream> =
        streamInfo.videoStreams
            ?.filter { stream ->
                !stream.isVideoOnly && stream.isUrl && stream.isPlayable()
            }
            ?.preferNonIosVideoStreams()
            .orEmpty()

    private fun logStreamInfo(videoId: String, dnsTarget: NewPipeDnsTarget, streamInfo: StreamInfo) {
        Log.d(
            TAG,
            "StreamInfo videoId=$videoId dnsTarget=${dnsTarget.label} " +
                "type=${streamInfo.streamType} " +
                "audio=${streamInfo.audioStreams?.size ?: 0} " +
                "video=${streamInfo.videoStreams?.size ?: 0} " +
                "videoOnly=${streamInfo.videoOnlyStreams?.size ?: 0} " +
                "dash=${streamInfo.dashMpdUrl?.isNotBlank() == true} " +
                "hls=${streamInfo.hlsUrl?.isNotBlank() == true} " +
                "errors=${streamInfo.errors?.size ?: 0}"
        )
        streamInfo.audioStreams.orEmpty().forEach { stream ->
            Log.d(TAG, "  audio ${stream.describe()}")
        }
        streamInfo.videoStreams.orEmpty().forEach { stream ->
            Log.d(TAG, "  video ${stream.describe()}")
        }
        streamInfo.errors.orEmpty().forEach { error ->
            Log.w(TAG, "  streamInfo error videoId=$videoId", error)
        }
    }

    private fun AudioStream.describe(): String =
        "itag=$itag format=$format delivery=$deliveryMethod isUrl=$isUrl bitrate=$averageBitrate " +
            "trackId=$audioTrackId trackName=$audioTrackName locale=$audioLocale trackType=$audioTrackType " +
            "content=${content.take(80)}"

    private fun VideoStream.describe(): String =
        "itag=$itag format=$format delivery=$deliveryMethod isUrl=$isUrl videoOnly=$isVideoOnly " +
            "content=${content.take(80)}"

    private fun Stream.isPlayable(): Boolean {
        if (deliveryMethod == DeliveryMethod.TORRENT) return false
        if (deliveryMethod == DeliveryMethod.HLS && format == MediaFormat.OPUS) return false
        return true
    }

    private fun List<AudioStream>.preferNonIosAudioStreams(): List<AudioStream> {
        val nonIosStreams = filterNot { stream ->
            stream.isUrl && YoutubeParsingHelper.isIosStreamingUrl(stream.content)
        }

        return nonIosStreams.ifEmpty { this }
    }

    private fun List<VideoStream>.preferNonIosVideoStreams(): List<VideoStream> {
        val nonIosStreams = filterNot { stream ->
            stream.isUrl && YoutubeParsingHelper.isIosStreamingUrl(stream.content)
        }

        return nonIosStreams.ifEmpty { this }
    }

    internal fun extractAudioTrackOptions(
        streams: List<AudioStream>?,
        quality: PlayerPreferences.AudioQuality = runCatching { PlayerPreferences.audioQuality }
            .getOrDefault(PlayerPreferences.AudioQuality.Auto)
    ): List<SwaaraAudioTrackOption> {
        if (streams.isNullOrEmpty()) return emptyList()

        val grouped = LinkedHashMap<String, MutableList<AudioStream>>()
        for (stream in streams) {
            val key = stream.normalizedTrackKey()
            grouped.getOrPut(key) { mutableListOf() }.add(stream)
        }

        return grouped.mapNotNull { (trackKey, groupStreams) ->
            val best = selectBestQualityStream(groupStreams, quality) ?: return@mapNotNull null
            val trackName = groupStreams.firstNotNullOfOrNull { it.audioTrackName?.trim()?.takeIf(String::isNotEmpty) }
            val locale = groupStreams.firstNotNullOfOrNull { it.audioLocale }
            val trackType = groupStreams.firstNotNullOfOrNull { it.audioTrackType }
            val displayName = buildTrackDisplayName(trackName, locale, trackType)

            SwaaraAudioTrackOption(
                trackKey = trackKey,
                displayName = displayName,
                languageCode = locale?.toLanguageTag()?.takeIf { it.isNotBlank() },
                trackType = trackType,
                bitrateKbps = best.averageBitrate.takeIf { it > 0 } ?: 0,
                codec = best.format?.name,
                stream = best
            )
        }.sortedWith(
            compareBy<SwaaraAudioTrackOption> { it.defaultPriorityRank() }
                .thenBy { it.displayName.lowercase(Locale.ENGLISH) }
        )
    }

    internal fun selectAudioTrackOption(
        options: List<SwaaraAudioTrackOption>,
        preferredTrackKey: String? = null
    ): SwaaraAudioTrackOption? {
        if (options.isEmpty()) return null
        if (!preferredTrackKey.isNullOrBlank()) {
            options.firstOrNull { it.trackKey == preferredTrackKey }?.let { return it }
        }
        // options are already sorted with ORIGINAL first, then neutral/secondary, then DUBBED, then DESCRIPTIVE
        return options.firstOrNull()
    }

    internal fun selectDefaultAudioStream(
        streams: List<AudioStream>?,
        preferredTrackKey: String? = null,
        quality: PlayerPreferences.AudioQuality = runCatching { PlayerPreferences.audioQuality }
            .getOrDefault(PlayerPreferences.AudioQuality.Auto)
    ): AudioStream? {
        if (streams.isNullOrEmpty()) return null
        val options = extractAudioTrackOptions(streams, quality)
        val selected = selectAudioTrackOption(options, preferredTrackKey)?.stream
            ?: selectBestQualityStream(streams, quality)

        runCatching {
            Log.i(
                TAG,
                "Stream selected quality=${quality.name} " +
                    "codec=${selected?.format?.name} " +
                    "bitrate=${selected?.averageBitrate}kbps " +
                    "itag=${selected?.itag} " +
                    "trackType=${selected?.audioTrackType} " +
                    "trackName=${selected?.audioTrackName}"
            )
        }
        return selected
    }

    fun applySelectedTrack(result: NewPipeAudioResult, trackKey: String): NewPipeAudioResult? {
        val options = result.availableTracks.ifEmpty { extractAudioTrackOptions(result.audioStreams) }
        val chosen = options.firstOrNull { it.trackKey == trackKey } ?: return null
        val videoId = result.streamInfo.id
        if (!videoId.isNullOrBlank()) {
            setPreferredTrackKey(videoId, trackKey)
            _availableTracksByVideoId.value = _availableTracksByVideoId.value + (videoId to options)
        }
        return result.copy(
            audioStream = chosen.stream,
            availableTracks = options,
            selectedTrackKey = chosen.trackKey
        )
    }

    private fun AudioStream.normalizedTrackKey(): String {
        val id = audioTrackId?.trim()
        if (!id.isNullOrEmpty()) return id
        val lang = audioLocale?.toLanguageTag()?.trim().orEmpty()
        val name = audioTrackName?.trim().orEmpty()
        val type = audioTrackType?.name.orEmpty()
        return if (lang.isEmpty() && name.isEmpty() && type.isEmpty()) {
            DEFAULT_TRACK_KEY
        } else {
            "$lang|$name|$type"
        }
    }

    private fun buildTrackDisplayName(
        rawTrackName: String?,
        locale: Locale?,
        trackType: AudioTrackType?
    ): String {
        val base = rawTrackName?.takeIf { it.isNotBlank() }
            ?: locale?.getDisplayName(Locale.ENGLISH)?.takeIf { it.isNotBlank() }
            ?: locale?.getDisplayLanguage(Locale.ENGLISH)?.takeIf { it.isNotBlank() }
            ?: "Default Audio"

        val normalizedBase = base.replaceFirstChar { it.uppercase() }
        val hasOriginalWord = normalizedBase.contains("original", ignoreCase = true)
        val hasDubWord = normalizedBase.contains("dub", ignoreCase = true)
        val hasDescriptiveWord = normalizedBase.contains("descriptive", ignoreCase = true)

        return when {
            trackType == AudioTrackType.ORIGINAL && !hasOriginalWord -> "$normalizedBase (Original)"
            trackType == AudioTrackType.DUBBED && !hasDubWord -> "$normalizedBase (Dubbed)"
            trackType == AudioTrackType.DESCRIPTIVE && !hasDescriptiveWord -> "$normalizedBase (Descriptive)"
            else -> normalizedBase
        }
    }

    private fun selectBestQualityStream(
        streams: List<AudioStream>,
        quality: PlayerPreferences.AudioQuality = runCatching { PlayerPreferences.audioQuality }
            .getOrDefault(PlayerPreferences.AudioQuality.Auto)
    ): AudioStream? {
        if (streams.isEmpty()) return null
        return when (quality) {
            PlayerPreferences.AudioQuality.Normal -> {
                // Pick stream closest to 128kbps
                streams.minByOrNull { abs(it.averageBitrate - 128) }
            }
            PlayerPreferences.AudioQuality.High -> {
                // Pick stream closest to 192kbps
                streams.minByOrNull { abs(it.averageBitrate - 192) }
            }
            else -> {
                // Auto and Maximum: always pick highest quality
                streams.maxWithOrNull(audioStreamComparator)
            }
        }
    }

    private val audioStreamComparator = compareBy<AudioStream> { stream ->
        stream.format.audioQualityRank()
    }.thenBy { stream ->
        stream.averageBitrate
    }

    private fun selectDefaultMuxedVideoStream(streams: List<VideoStream>): VideoStream? =
        streams.minWithOrNull(
            compareBy<VideoStream> { stream ->
                stream.bitrate.takeIf { it > 0 } ?: Int.MAX_VALUE
            }.thenBy { stream ->
                stream.itag
            }
        )

    private fun MediaFormat?.audioQualityRank() = when (this) {
        MediaFormat.MP3 -> 0
        MediaFormat.WEBMA -> 1
        MediaFormat.M4A -> 2
        MediaFormat.OPUS -> 3
        else -> -1
    }

    private fun dnsFallbackTargets(): List<NewPipeDnsTarget> {
        val startIndex = lastSuccessfulDnsIndex
            ?.plus(1)
            ?.mod(DNS_RESOLVED_ADDRESS_ATTEMPTS)
            ?: 0

        val resolvedTargets = (0 until DNS_RESOLVED_ADDRESS_ATTEMPTS).map { offset ->
            NewPipeDnsTarget.Resolved((startIndex + offset) % DNS_RESOLVED_ADDRESS_ATTEMPTS)
        }

        return listOf(NewPipeDnsTarget.System) + resolvedTargets
    }

    private const val TAG = "NewPipeExtractorClient"
    private const val DEFAULT_TRACK_KEY = "default"
    private const val DNS_RESOLVED_ADDRESS_ATTEMPTS = 4
    private const val PLAYBACK_RESOLVE_TIMEOUT_SECONDS = 45L
    private const val PRELOAD_RESOLVE_TIMEOUT_SECONDS = 10L
    private val RESOLVER_EXECUTOR = Executors.newCachedThreadPool()
}

data class SwaaraAudioTrackOption(
    val trackKey: String,
    val displayName: String,
    val languageCode: String?,
    val trackType: AudioTrackType?,
    val bitrateKbps: Int,
    val codec: String?,
    val stream: AudioStream
) {
    val isOriginal: Boolean
        get() = trackType == AudioTrackType.ORIGINAL ||
            displayName.contains("original", ignoreCase = true)

    val isDubbed: Boolean
        get() = trackType == AudioTrackType.DUBBED ||
            displayName.contains("dub", ignoreCase = true)

    val isDescriptive: Boolean
        get() = trackType == AudioTrackType.DESCRIPTIVE ||
            displayName.contains("descriptive", ignoreCase = true)

    fun defaultPriorityRank(): Int = when {
        isOriginal -> 0
        trackType == null && !isDubbed && !isDescriptive -> 1
        trackType == AudioTrackType.SECONDARY && !isDubbed && !isDescriptive -> 2
        isDubbed -> 3
        isDescriptive -> 4
        else -> 2
    }
}

data class NewPipeAudioResult(
    val streamInfo: StreamInfo,
    val audioStreams: List<AudioStream>,
    val audioStream: AudioStream?,
    val videoStream: VideoStream?,
    val availableTracks: List<SwaaraAudioTrackOption> = emptyList(),
    val selectedTrackKey: String? = null
)
