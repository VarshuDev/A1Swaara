package com.varshudev.a1swaara.preferences

import android.media.audiofx.PresetReverb
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.varshudev.a1swaara.GlobalPreferencesHolder
import com.varshudev.a1swaara.R

object PlayerPreferences : GlobalPreferencesHolder() {
    val isInvincibilityEnabledProperty = boolean(false)
    var isInvincibilityEnabled by isInvincibilityEnabledProperty
    val trackLoopEnabledProperty = boolean(false)
    var trackLoopEnabled by trackLoopEnabledProperty
    val queueLoopEnabledProperty = boolean(true)
    var queueLoopEnabled by queueLoopEnabledProperty
    val skipSilenceProperty = boolean(false)
    var skipSilence by skipSilenceProperty
    val volumeNormalizationProperty = boolean(false)
    var volumeNormalization by volumeNormalizationProperty
    val crossfadeDurationSecondsProperty = int(0)
    var crossfadeDurationSeconds by crossfadeDurationSecondsProperty
    val pauseWhenVolumeAtMinimumProperty = boolean(true)
    var pauseWhenVolumeAtMinimum by pauseWhenVolumeAtMinimumProperty
    val volumeNormalizationBaseGainProperty = float(5.00f)
    var volumeNormalizationBaseGain by volumeNormalizationBaseGainProperty
    val reverbProperty = enum(Reverb.None)
    var reverb by reverbProperty
    val resumePlaybackWhenDeviceConnectedProperty = boolean(false)
    var resumePlaybackWhenDeviceConnected by resumePlaybackWhenDeviceConnectedProperty
    val speedProperty = float(1f)
    var speed by speedProperty
    val pitchProperty = float(1f)
    var pitch by pitchProperty
    var minimumSilence by long(2_000_000L)
    var persistentQueue by boolean(true)
    var stopWhenClosed by boolean(false)

    var isShowingLyrics by boolean(false)
    var isShowingSynchronizedLyrics by boolean(false)

    var isShowingPrevButtonCollapsed by boolean(false)
    var horizontalSwipeToClose by boolean(false)
    var horizontalSwipeToRemoveItem by boolean(false)

    var playerLayout by enum(PlayerLayout.New)
    var seekBarStyle by enum(SeekBarStyle.Wavy)
    var wavySeekBarQuality by enum(WavySeekBarQuality.Great)
    var showLike by boolean(false)
    var showRemaining by boolean(false)
    var lyricsKeepScreenAwake by boolean(false)
    var lyricsShowSystemBars by boolean(true)

    var handleAudioFocus by boolean(true)

    var pauseCache by boolean(false)

    val sponsorBlockEnabledProperty = boolean(false)
    var sponsorBlockEnabled by sponsorBlockEnabledProperty

    // -- Swaara Lossless Audio Engine (v1.1.4) --

    val audioQualityProperty = enum(AudioQuality.Maximum)
    var audioQuality by audioQualityProperty

    val losslessModeProperty = boolean(true)
    var losslessMode by losslessModeProperty

    /**
     * Audio quality tier for stream selection.
     * Auto: Highest available (same as Maximum, but semantically "let the app decide").
     * Normal: Prefer streams near 128kbps.
     * High: Prefer streams near 192kbps.
     * Maximum: Always select the absolute highest bitrate stream available.
     */
    enum class AudioQuality(
        val targetBitrateKbps: Int?,
        val displayName: @Composable () -> String
    ) {
        Auto(
            targetBitrateKbps = null,
            displayName = { stringResource(R.string.audio_quality_auto) }
        ),
        Normal(
            targetBitrateKbps = 128,
            displayName = { stringResource(R.string.audio_quality_normal) }
        ),
        High(
            targetBitrateKbps = 192,
            displayName = { stringResource(R.string.audio_quality_high) }
        ),
        Maximum(
            targetBitrateKbps = null,
            displayName = { stringResource(R.string.audio_quality_maximum) }
        )
    }

    enum class PlayerLayout(val displayName: @Composable () -> String) {
        Classic(displayName = { stringResource(R.string.classic_player_layout_name) }),
        New(displayName = { stringResource(R.string.new_player_layout_name) })
    }

    enum class SeekBarStyle(val displayName: @Composable () -> String) {
        Static(displayName = { stringResource(R.string.static_seek_bar_name) }),
        Wavy(displayName = { stringResource(R.string.wavy_seek_bar_name) })
    }

    enum class WavySeekBarQuality(
        val quality: Float,
        val displayName: @Composable () -> String
    ) {
        Poor(quality = 50f, displayName = { stringResource(R.string.seek_bar_quality_poor) }),
        Low(quality = 25f, displayName = { stringResource(R.string.seek_bar_quality_low) }),
        Medium(quality = 15f, displayName = { stringResource(R.string.seek_bar_quality_medium) }),
        High(quality = 5f, displayName = { stringResource(R.string.seek_bar_quality_high) }),
        Great(quality = 1f, displayName = { stringResource(R.string.seek_bar_quality_great) }),
        Subpixel(
            quality = 0.5f,
            displayName = { stringResource(R.string.seek_bar_quality_subpixel) }
        )
    }

    enum class Reverb(
        val preset: Short,
        val displayName: @Composable () -> String
    ) {
        None(
            preset = PresetReverb.PRESET_NONE,
            displayName = { stringResource(R.string.none) }
        ),
        SmallRoom(
            preset = PresetReverb.PRESET_SMALLROOM,
            displayName = { stringResource(R.string.reverb_small_room) }
        ),
        MediumRoom(
            preset = PresetReverb.PRESET_MEDIUMROOM,
            displayName = { stringResource(R.string.reverb_medium_room) }
        ),
        LargeRoom(
            preset = PresetReverb.PRESET_LARGEROOM,
            displayName = { stringResource(R.string.reverb_large_room) }
        ),
        MediumHall(
            preset = PresetReverb.PRESET_MEDIUMHALL,
            displayName = { stringResource(R.string.reverb_medium_hall) }
        ),
        LargeHall(
            preset = PresetReverb.PRESET_LARGEHALL,
            displayName = { stringResource(R.string.reverb_large_hall) }
        ),
        Plate(
            preset = PresetReverb.PRESET_PLATE,
            displayName = { stringResource(R.string.reverb_plate) }
        )
    }
}
