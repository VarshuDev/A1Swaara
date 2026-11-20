package com.varshudev.a1swaara.extractor

import com.varshudev.a1swaara.preferences.PlayerPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import java.util.Locale

class NewPipeAudioTrackSelectionTest {

    private fun buildStream(
        id: String,
        url: String,
        format: MediaFormat,
        bitrate: Int,
        trackId: String?,
        trackName: String?,
        locale: Locale?,
        trackType: AudioTrackType?
    ): AudioStream = AudioStream.Builder()
        .setId(id)
        .setContent(url, true)
        .setMediaFormat(format)
        .setDeliveryMethod(DeliveryMethod.PROGRESSIVE_HTTP)
        .setAverageBitrate(bitrate)
        .setAudioTrackId(trackId)
        .setAudioTrackName(trackName)
        .setAudioLocale(locale)
        .setAudioTrackType(trackType)
        .build()

    @Test
    fun `always prefers original audio track over higher bitrate dubbed track`() {
        val englishDubHighBitrate = buildStream(
            id = "251-en",
            url = "https://example.com/en-dub.webm",
            format = MediaFormat.OPUS,
            bitrate = 192,
            trackId = "en.4",
            trackName = "English (US)",
            locale = Locale.US,
            trackType = AudioTrackType.DUBBED
        )
        val hindiOriginal = buildStream(
            id = "251-hi",
            url = "https://example.com/hi-orig.webm",
            format = MediaFormat.OPUS,
            bitrate = 128,
            trackId = "hi.4",
            trackName = "Hindi (India) original",
            locale = Locale("hi", "IN"),
            trackType = AudioTrackType.ORIGINAL
        )
        val descriptiveAudio = buildStream(
            id = "251-desc",
            url = "https://example.com/desc.webm",
            format = MediaFormat.OPUS,
            bitrate = 256,
            trackId = "en.3",
            trackName = "English descriptive",
            locale = Locale.US,
            trackType = AudioTrackType.DESCRIPTIVE
        )

        val selected = NewPipeExtractorClient.selectDefaultAudioStream(
            streams = listOf(englishDubHighBitrate, descriptiveAudio, hindiOriginal),
            quality = PlayerPreferences.AudioQuality.Maximum
        )

        assertNotNull(selected)
        assertEquals("hi.4", selected?.audioTrackId)
        assertEquals("https://example.com/hi-orig.webm", selected?.content)
    }

    @Test
    fun `deduplicates bitrates per language and lets user explicitly switch to dubbed track`() {
        val hindiLow = buildStream(
            id = "249-hi",
            url = "https://example.com/hi-64.webm",
            format = MediaFormat.OPUS,
            bitrate = 64,
            trackId = "hi.4",
            trackName = "Hindi",
            locale = Locale("hi"),
            trackType = AudioTrackType.ORIGINAL
        )
        val hindiHigh = buildStream(
            id = "251-hi",
            url = "https://example.com/hi-160.webm",
            format = MediaFormat.OPUS,
            bitrate = 160,
            trackId = "hi.4",
            trackName = "Hindi",
            locale = Locale("hi"),
            trackType = AudioTrackType.ORIGINAL
        )
        val englishDub = buildStream(
            id = "251-en",
            url = "https://example.com/en-160.webm",
            format = MediaFormat.OPUS,
            bitrate = 160,
            trackId = "en.4",
            trackName = "English",
            locale = Locale.ENGLISH,
            trackType = AudioTrackType.DUBBED
        )

        val options = NewPipeExtractorClient.extractAudioTrackOptions(
            streams = listOf(englishDub, hindiLow, hindiHigh),
            quality = PlayerPreferences.AudioQuality.Maximum
        )

        assertEquals(2, options.size)
        assertTrue(options[0].isOriginal)
        assertEquals("hi.4", options[0].trackKey)
        assertEquals(160, options[0].bitrateKbps)
        assertEquals("en.4", options[1].trackKey)
        assertTrue(options[1].isDubbed)

        val switched = NewPipeExtractorClient.selectDefaultAudioStream(
            streams = listOf(englishDub, hindiLow, hindiHigh),
            preferredTrackKey = "en.4",
            quality = PlayerPreferences.AudioQuality.Maximum
        )
        assertEquals("en.4", switched?.audioTrackId)
        assertEquals("https://example.com/en-160.webm", switched?.content)
    }
}
