package com.varshudev.a1swaara.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SwaaraEqPreset(val name: String, val levels: List<Float>)

/**
 * Sovereign Audio DSP Engine for A1 Swaara.
 * Maintains persistent 5-band hardware equalization, sub-bass enhancement, and 3D virtualizer
 * across all screen lifecycles and background playback in PlayerService.
 */
object SwaaraAudioEngine {
    private const val TAG = "SwaaraAudioEngine"
    private const val PREFS_NAME = "a1swaara_audio_dsp_prefs"

    val DEFAULT_PRESETS = listOf(
        SwaaraEqPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
        SwaaraEqPreset("Bass Heavy", listOf(6f, 4f, 1f, -1f, -2f)),
        SwaaraEqPreset("Electronic", listOf(4f, 2f, -1f, 3f, 5f)),
        SwaaraEqPreset("Rock", listOf(5f, 3f, -1f, 2f, 4f)),
        SwaaraEqPreset("Pop", listOf(-1f, 2f, 5f, 3f, -1f)),
        SwaaraEqPreset("Acoustic", listOf(3f, 2f, 2f, 3f, 2f)),
        SwaaraEqPreset("Vocal Boost", listOf(-2f, -1f, 4f, 4f, 1f)),
        SwaaraEqPreset("Audiophile Pure", listOf(1.5f, 0.5f, -0.5f, 1.0f, 2.0f))
    )

    val BAND_FREQUENCIES = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    private var sharedPrefs: SharedPreferences? = null

    private val _isEqEnabled = MutableStateFlow(true)
    val isEqEnabled: StateFlow<Boolean> = _isEqEnabled.asStateFlow()

    private val _presetIndex = MutableStateFlow(0)
    val presetIndex: StateFlow<Int> = _presetIndex.asStateFlow()

    private val _bandLevels = MutableStateFlow(listOf(0f, 0f, 0f, 0f, 0f))
    val bandLevels: StateFlow<List<Float>> = _bandLevels.asStateFlow()

    private val _bassBoostLevel = MutableStateFlow(500f)
    val bassBoostLevel: StateFlow<Float> = _bassBoostLevel.asStateFlow()

    private val _virtualizerLevel = MutableStateFlow(350f)
    val virtualizerLevel: StateFlow<Float> = _virtualizerLevel.asStateFlow()

    private val _isBoomActive = MutableStateFlow(false)
    val isBoomActive: StateFlow<Boolean> = _isBoomActive.asStateFlow()

    @Volatile
    private var activeAudioSessionId: Int = -1

    @Volatile
    private var equalizer: Equalizer? = null

    @Volatile
    private var bassBoost: BassBoost? = null

    @Volatile
    private var virtualizer: Virtualizer? = null

    fun initialize(context: Context) {
        if (sharedPrefs != null) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs = prefs

        _isEqEnabled.value = prefs.getBoolean("is_eq_enabled", true)
        _presetIndex.value = prefs.getInt("preset_index", 0).coerceIn(0, DEFAULT_PRESETS.size - 1)

        val rawLevels = prefs.getString("band_levels", null)
        _bandLevels.value = if (!rawLevels.isNullOrBlank()) {
            try {
                rawLevels.split(",").map { it.trim().toFloat() }.let {
                    if (it.size == 5) it else DEFAULT_PRESETS[_presetIndex.value].levels
                }
            } catch (_: Exception) {
                DEFAULT_PRESETS[_presetIndex.value].levels
            }
        } else {
            DEFAULT_PRESETS[_presetIndex.value].levels
        }

        _bassBoostLevel.value = prefs.getFloat("bass_boost_level", 500f)
        _virtualizerLevel.value = prefs.getFloat("virtualizer_level", 350f)
        _isBoomActive.value = prefs.getBoolean("is_boom_active", false)
    }

    @Suppress("DEPRECATION")
    fun bindAudioSession(context: Context, audioSessionId: Int) {
        initialize(context)
        if (audioSessionId <= 0) return

        if (activeAudioSessionId == audioSessionId && equalizer != null) {
            applyActiveSettingsToHardware()
            return
        }

        releaseHardwareEffects()
        activeAudioSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _isEqEnabled.value
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to instantiate Equalizer: ${e.message}")
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _isEqEnabled.value || _isBoomActive.value
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to instantiate BassBoost: ${e.message}")
        }

        try {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _isEqEnabled.value || _isBoomActive.value
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to instantiate Virtualizer: ${e.message}")
        }

        applyActiveSettingsToHardware()
    }

    private fun applyActiveSettingsToHardware() {
        val eq = equalizer
        val bb = bassBoost
        val vz = virtualizer

        if (eq != null) {
            runCatching {
                eq.enabled = _isEqEnabled.value
                val range = eq.bandLevelRange
                val minMillibels = range?.getOrNull(0) ?: -1500.toShort()
                val maxMillibels = range?.getOrNull(1) ?: 1500.toShort()
                val levels = _bandLevels.value
                val numBands = eq.numberOfBands.toInt()

                for (i in 0 until minOf(numBands, levels.size)) {
                    val targetMillibels = (levels[i] * 100).toInt().coerceIn(minMillibels.toInt(), maxMillibels.toInt()).toShort()
                    eq.setBandLevel(i.toShort(), targetMillibels)
                }
            }
        }

        if (bb != null) {
            runCatching {
                bb.enabled = _isEqEnabled.value || _isBoomActive.value
                val strength = if (_isBoomActive.value) 1000 else _bassBoostLevel.value.toInt()
                bb.setStrength(strength.coerceIn(0, 1000).toShort())
            }
        }

        if (vz != null) {
            runCatching {
                vz.enabled = _isEqEnabled.value || _isBoomActive.value
                val strength = if (_isBoomActive.value) 950 else _virtualizerLevel.value.toInt()
                vz.setStrength(strength.coerceIn(0, 1000).toShort())
            }
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        _isEqEnabled.value = enabled
        applyActiveSettingsToHardware()
        sharedPrefs?.edit()?.putBoolean("is_eq_enabled", enabled)?.apply()
    }

    fun setPreset(index: Int) {
        if (index !in DEFAULT_PRESETS.indices) return
        _presetIndex.value = index
        val targetLevels = DEFAULT_PRESETS[index].levels
        _bandLevels.value = targetLevels
        applyActiveSettingsToHardware()
        sharedPrefs?.edit()
            ?.putInt("preset_index", index)
            ?.putString("band_levels", targetLevels.joinToString(","))
            ?.apply()
    }

    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        if (bandIndex !in 0..4) return
        val updated = _bandLevels.value.toMutableList()
        updated[bandIndex] = levelDb.coerceIn(-10f, 10f)
        _bandLevels.value = updated
        applyActiveSettingsToHardware()
        sharedPrefs?.edit()
            ?.putString("band_levels", updated.joinToString(","))
            ?.apply()
    }

    fun setBassBoost(level: Float) {
        val clamped = level.coerceIn(0f, 1000f)
        _bassBoostLevel.value = clamped
        applyActiveSettingsToHardware()
        sharedPrefs?.edit()?.putFloat("bass_boost_level", clamped)?.apply()
    }

    fun setVirtualizer(level: Float) {
        val clamped = level.coerceIn(0f, 1000f)
        _virtualizerLevel.value = clamped
        applyActiveSettingsToHardware()
        sharedPrefs?.edit()?.putFloat("virtualizer_level", clamped)?.apply()
    }

    fun setBoomActive(active: Boolean) {
        _isBoomActive.value = active
        applyActiveSettingsToHardware()
        sharedPrefs?.edit()?.putBoolean("is_boom_active", active)?.apply()
    }

    fun releaseHardwareEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        activeAudioSessionId = -1
    }
}
