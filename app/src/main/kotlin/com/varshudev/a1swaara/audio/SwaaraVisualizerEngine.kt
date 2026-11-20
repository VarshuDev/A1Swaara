package com.varshudev.a1swaara.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class VisualizerFrame(
    val magnitudes: FloatArray = FloatArray(BAND_COUNT),
    val peakHold: FloatArray = FloatArray(BAND_COUNT),
    val leftVu: Float = 0f,
    val rightVu: Float = 0f,
    val waveform: FloatArray = FloatArray(WAVE_POINTS)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VisualizerFrame) return false
        return magnitudes.contentEquals(other.magnitudes) &&
                peakHold.contentEquals(other.peakHold) &&
                leftVu == other.leftVu &&
                rightVu == other.rightVu &&
                waveform.contentEquals(other.waveform)
    }

    override fun hashCode(): Int {
        var result = magnitudes.contentHashCode()
        result = 31 * result + peakHold.contentHashCode()
        result = 31 * result + leftVu.hashCode()
        result = 31 * result + rightVu.hashCode()
        result = 31 * result + waveform.contentHashCode()
        return result
    }

    companion object {
        const val BAND_COUNT = 24
        const val WAVE_POINTS = 48
    }
}

/**
 * Sovereign Real-Time Audio Visualizer Engine for A1 Swaara.
 * Captures 60 FPS hardware FFT spectrum & waveform data from the active audioSessionId,
 * with automatic fallback to a procedural rhythmic visualizer when permissions/hardware are restricted.
 */
object SwaaraVisualizerEngine {
    private const val TAG = "SwaaraVisualizerEngine"
    private const val BAND_COUNT = VisualizerFrame.BAND_COUNT
    private const val WAVE_POINTS = VisualizerFrame.WAVE_POINTS

    private val scope = CoroutineScope(Dispatchers.Default)
    private var syntheticLoopJob: Job? = null

    private val _frame = MutableStateFlow(VisualizerFrame())
    val frame: StateFlow<VisualizerFrame> = _frame.asStateFlow()

    private val _isHardwareActive = MutableStateFlow(false)
    val isHardwareActive: StateFlow<Boolean> = _isHardwareActive.asStateFlow()

    private val smoothedMagnitudes = FloatArray(BAND_COUNT)
    private val peaks = FloatArray(BAND_COUNT)
    private var visualizer: Visualizer? = null
    private var boundAudioSessionId: Int = -1

    @Volatile
    private var isPlayingAudio: Boolean = false

    fun notifyPlaybackState(isPlaying: Boolean) {
        isPlayingAudio = isPlaying
        if (!isPlaying) {
            decayToZero()
        }
    }

    fun bindAudioSession(context: Context, audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (boundAudioSessionId == audioSessionId && visualizer != null) return

        release()
        boundAudioSessionId = audioSessionId

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.d(TAG, "RECORD_AUDIO not granted; engaging high-fidelity procedural rhythm visualizer")
            startSyntheticLoop()
            return
        }

        try {
            val captureSizeRange = Visualizer.getCaptureSizeRange()
            val captureSize = if (captureSizeRange != null && captureSizeRange.size >= 2) {
                captureSizeRange[1].coerceIn(256, 1024)
            } else 512

            visualizer = Visualizer(audioSessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            v: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (waveform != null && isPlayingAudio) {
                                processWaveform(waveform)
                            }
                        }

                        override fun onFftDataCapture(
                            v: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft != null && isPlayingAudio) {
                                processFft(fft)
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    /* waveform = */ true,
                    /* fft = */ true
                )
                enabled = true
            }
            _isHardwareActive.value = true
            stopSyntheticLoop()
            Log.i(TAG, "Successfully initialized hardware Visualizer on audio session $audioSessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Hardware Visualizer initialization failed; falling back to procedural visualizer", e)
            _isHardwareActive.value = false
            visualizer = null
            startSyntheticLoop()
        }
    }

    private fun processWaveform(waveform: ByteArray) {
        val count = minOf(waveform.size, WAVE_POINTS)
        val waveFloats = FloatArray(WAVE_POINTS)
        var sumSquares = 0f

        val step = max(1, waveform.size / WAVE_POINTS)
        for (i in 0 until WAVE_POINTS) {
            val idx = (i * step).coerceIn(0, waveform.size - 1)
            // 8-bit unsigned PCM: 128 is center
            val sample = ((waveform[idx].toInt() and 0xFF) - 128) / 128f
            waveFloats[i] = sample
            sumSquares += sample * sample
        }

        val rms = sqrt(sumSquares / count.coerceAtLeast(1))
        val currentVu = (rms * 2.8f).coerceIn(0.02f, 1.0f)
        val left = currentVu * 0.96f
        val right = currentVu * 1.04f

        _frame.value = _frame.value.copy(
            leftVu = left.coerceIn(0f, 1f),
            rightVu = right.coerceIn(0f, 1f),
            waveform = waveFloats
        )
    }

    private fun processFft(fft: ByteArray) {
        val n = fft.size / 2
        val rawMagnitudes = FloatArray(n)

        for (k in 1 until n) {
            val r = fft[2 * k].toFloat()
            val i = fft[2 * k + 1].toFloat()
            rawMagnitudes[k] = hypot(r, i)
        }

        // Group into 24 logarithmic bands
        val nextMagnitudes = FloatArray(BAND_COUNT)
        val step = max(1, n / BAND_COUNT)

        for (b in 0 until BAND_COUNT) {
            var bandSum = 0f
            val startIdx = b * step
            val endIdx = minOf(startIdx + step, n)
            for (k in startIdx until endIdx) {
                bandSum += rawMagnitudes[k]
            }
            val avg = bandSum / (endIdx - startIdx).coerceAtLeast(1)
            // Logarithmic dynamic range mapping
            val normalized = (avg / 65f).coerceIn(0f, 1f)
            // Exponential smoothing (alpha = 0.45)
            smoothedMagnitudes[b] = smoothedMagnitudes[b] * 0.55f + normalized * 0.45f
            nextMagnitudes[b] = smoothedMagnitudes[b]

            // Peak hold with gravity decay
            if (nextMagnitudes[b] >= peaks[b]) {
                peaks[b] = nextMagnitudes[b]
            } else {
                peaks[b] = max(0f, peaks[b] - 0.025f)
            }
        }

        _frame.value = _frame.value.copy(
            magnitudes = nextMagnitudes,
            peakHold = peaks.copyOf()
        )
    }

    private fun startSyntheticLoop() {
        if (syntheticLoopJob?.isActive == true) return
        syntheticLoopJob = scope.launch {
            var tick = 0L
            while (isActive) {
                if (isPlayingAudio) {
                    val time = SystemClock.uptimeMillis() / 1000.0
                    val basePulse = (sin(time * 3.8) * 0.5 + 0.5).toFloat()
                    val bassPulse = (sin(time * 1.9) * 0.5 + 0.5).toFloat()
                    val treblePulse = (cos(time * 5.2) * 0.5 + 0.5).toFloat()

                    val synMags = FloatArray(BAND_COUNT)
                    for (b in 0 until BAND_COUNT) {
                        val factor = when {
                            b < 6 -> bassPulse * 0.85f + (sin(time * 2.5 + b) * 0.15f).toFloat()
                            b < 16 -> basePulse * 0.70f + (cos(time * 3.1 + b) * 0.20f).toFloat()
                            else -> treblePulse * 0.60f + (sin(time * 4.4 + b) * 0.25f).toFloat()
                        }
                        synMags[b] = factor.coerceIn(0.08f, 0.95f)
                        if (synMags[b] >= peaks[b]) {
                            peaks[b] = synMags[b]
                        } else {
                            peaks[b] = max(0f, peaks[b] - 0.02f)
                        }
                    }

                    val synWave = FloatArray(WAVE_POINTS)
                    for (w in 0 until WAVE_POINTS) {
                        synWave[w] = (sin(time * 6.0 + w * 0.35) * (0.35f + bassPulse * 0.45f)).toFloat()
                    }

                    val left = (bassPulse * 0.75f + 0.15f).coerceIn(0f, 1f)
                    val right = (basePulse * 0.70f + 0.18f).coerceIn(0f, 1f)

                    _frame.value = VisualizerFrame(
                        magnitudes = synMags,
                        peakHold = peaks.copyOf(),
                        leftVu = left,
                        rightVu = right,
                        waveform = synWave
                    )
                } else {
                    decayToZero()
                }
                delay(22L) // ~45 FPS update loop
                tick++
            }
        }
    }

    private fun stopSyntheticLoop() {
        syntheticLoopJob?.cancel()
        syntheticLoopJob = null
    }

    private fun decayToZero() {
        var hasNonZero = false
        val nextMags = FloatArray(BAND_COUNT)
        for (i in 0 until BAND_COUNT) {
            smoothedMagnitudes[i] = max(0f, smoothedMagnitudes[i] - 0.05f)
            peaks[i] = max(0f, peaks[i] - 0.04f)
            nextMags[i] = smoothedMagnitudes[i]
            if (peaks[i] > 0.01f || nextMags[i] > 0.01f) hasNonZero = true
        }
        if (hasNonZero) {
            _frame.value = _frame.value.copy(
                magnitudes = nextMags,
                peakHold = peaks.copyOf(),
                leftVu = max(0f, _frame.value.leftVu - 0.08f),
                rightVu = max(0f, _frame.value.rightVu - 0.08f)
            )
        }
    }

    fun release() {
        stopSyntheticLoop()
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing hardware visualizer", e)
        } finally {
            visualizer = null
            _isHardwareActive.value = false
            boundAudioSessionId = -1
        }
    }
}
