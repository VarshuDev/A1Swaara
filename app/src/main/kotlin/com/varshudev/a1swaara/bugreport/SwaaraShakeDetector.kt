package com.varshudev.a1swaara.bugreport

import android.app.KeyguardManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.varshudev.a1swaara.preferences.DataPreferences
import kotlin.math.sqrt

/**
 * Pure JVM-testable directional-reversal double-shake evaluator ("Rage Shake").
 *
 * Isolates static gravity using an adaptive low-pass / high-pass filter and requires four
 * alternating directional acceleration peaks (three reversals) within a bounded window.
 */
internal class SwaaraShakeEvaluator {

    private var isGravitySeeded = false
    private val gravity = FloatArray(3)
    private var startedAtNs = 0L
    private var lastEventNs = 0L
    private var cooldownUntilNs = 0L
    private var wasBlocked = false

    private var peakCount = 0
    private var firstPeakMs = 0L
    private var lastPeakMs = 0L
    private var lastPeakX = 0f
    private var lastPeakY = 0f
    private var lastPeakZ = 0f
    private var lastPeakMag = 0f

    var lastSequenceSpanMs: Long = 0L
        private set

    companion object {
        // High-pass filter time constant (seconds) to isolate dynamic shake linear acceleration from gravity
        internal const val GRAVITY_TAU_SEC = 0.16f

        // Minimum linear acceleration peak (m/s^2, gravity removed: 1g = 9.81 m/s^2)
        internal const val PEAK_LINEAR_ACCEL_MS2 = 11.0f

        // Consecutive strokes must reverse direction (cos(theta) < -0.20 => >101.5 deg apart)
        internal const val REVERSAL_COS_THRESHOLD = -0.20f

        // Number of alternating high-G strokes required (Peak 0 + 3 reversals = intentional double shake)
        internal const val REQUIRED_PEAK_COUNT = 4

        // Ignore sensor transients immediately after start()/unlock for 300ms
        internal const val WARMUP_IGNORE_NS = 300_000_000L

        // Re-anchor gravity if hardware sensor delivery stalled/batched >250ms
        internal const val MAX_EVENT_GAP_NS = 250_000_000L

        // Minimum separation between alternating strokes (avoids double-counting the same half-swing)
        internal const val MIN_REVERSAL_INTERVAL_MS = 65L

        // Maximum allowed pause between consecutive strokes in a double-shake sequence
        internal const val MAX_REVERSAL_GAP_MS = 520L

        // Minimum total span for 4 peaks (filters single sharp mechanical shocks/table impacts)
        internal const val MIN_SEQUENCE_SPAN_MS = 200L

        // Total sliding window for completing the double shake
        internal const val MAX_SEQUENCE_WINDOW_MS = 1100L

        // Post-trigger cooldown (monotonic sensor nanoseconds)
        internal const val TRIGGER_COOLDOWN_NS = 2_500_000_000L
    }

    fun resetSequence() {
        peakCount = 0
        firstPeakMs = 0L
        lastPeakMs = 0L
        lastPeakX = 0f
        lastPeakY = 0f
        lastPeakZ = 0f
        lastPeakMag = 0f
    }

    fun resetAll() {
        isGravitySeeded = false
        startedAtNs = 0L
        lastEventNs = 0L
        wasBlocked = false
        resetSequence()
    }

    fun notifyBlocked() {
        wasBlocked = true
        resetSequence()
    }

    fun processSample(
        rawX: Float,
        rawY: Float,
        rawZ: Float,
        timestampNs: Long
    ): Boolean {
        if (timestampNs <= 0L) return false
        if (!rawX.isFinite() || !rawY.isFinite() || !rawZ.isFinite()) return false

        // Re-seed gravity on first sample, post-block resume, or sensor delivery stall
        if (wasBlocked || !isGravitySeeded || timestampNs - lastEventNs > MAX_EVENT_GAP_NS) {
            wasBlocked = false
            isGravitySeeded = true
            startedAtNs = timestampNs
            lastEventNs = timestampNs
            gravity[0] = rawX
            gravity[1] = rawY
            gravity[2] = rawZ
            resetSequence()
            return false
        }

        val dtNs = timestampNs - lastEventNs
        if (dtNs <= 0L) return false
        lastEventNs = timestampNs

        // Adaptive alpha low-pass filter isolates static gravity regardless of OEM sensor frequency
        val dtSec = (dtNs / 1_000_000_000f).coerceIn(0.002f, 0.2f)
        val alpha = GRAVITY_TAU_SEC / (GRAVITY_TAU_SEC + dtSec)
        gravity[0] = alpha * gravity[0] + (1f - alpha) * rawX
        gravity[1] = alpha * gravity[1] + (1f - alpha) * rawY
        gravity[2] = alpha * gravity[2] + (1f - alpha) * rawZ

        // Pure linear acceleration (gravity removed) in m/s^2
        val linX = rawX - gravity[0]
        val linY = rawY - gravity[1]
        val linZ = rawZ - gravity[2]

        // Hold off peak evaluation during warm-up and trigger cooldown while keeping gravity filter converged
        if (timestampNs - startedAtNs < WARMUP_IGNORE_NS || timestampNs < cooldownUntilNs) {
            resetSequence()
            return false
        }

        val linearMag = sqrt((linX * linX + linY * linY + linZ * linZ).toDouble()).toFloat()
        if (linearMag < PEAK_LINEAR_ACCEL_MS2) return false

        val nowMs = timestampNs / 1_000_000L

        if (peakCount == 0 ||
            nowMs - lastPeakMs > MAX_REVERSAL_GAP_MS ||
            nowMs - firstPeakMs > MAX_SEQUENCE_WINDOW_MS
        ) {
            peakCount = 1
            firstPeakMs = nowMs
            lastPeakMs = nowMs
            lastPeakX = linX
            lastPeakY = linY
            lastPeakZ = linZ
            lastPeakMag = linearMag
            return false
        }

        val dot = linX * lastPeakX + linY * lastPeakY + linZ * lastPeakZ
        val cosAngle = dot / (linearMag * lastPeakMag)
        val elapsedSinceLastPeak = nowMs - lastPeakMs

        if (elapsedSinceLastPeak < MIN_REVERSAL_INTERVAL_MS) {
            // Update stronger sample within the same half-stroke if direction hasn't reversed
            if (cosAngle > 0f && linearMag > lastPeakMag) {
                lastPeakX = linX
                lastPeakY = linY
                lastPeakZ = linZ
                lastPeakMag = linearMag
            }
            return false
        }

        // Require genuine directional reversal (> ~101.5 degrees) from the prior stroke
        if (cosAngle <= REVERSAL_COS_THRESHOLD) {
            peakCount++
            lastPeakMs = nowMs
            lastPeakX = linX
            lastPeakY = linY
            lastPeakZ = linZ
            lastPeakMag = linearMag

            val totalSpan = nowMs - firstPeakMs
            if (peakCount >= REQUIRED_PEAK_COUNT &&
                totalSpan in MIN_SEQUENCE_SPAN_MS..MAX_SEQUENCE_WINDOW_MS
            ) {
                lastSequenceSpanMs = totalSpan
                resetSequence()
                cooldownUntilNs = timestampNs + TRIGGER_COOLDOWN_NS
                return true
            }
        }
        return false
    }
}

/**
 * Gravity-isolated, directional-reversal double-shake detector ("Rage Shake") for A1 Swaara.
 */
class SwaaraShakeDetector(
    private val context: Context,
    private val canTrigger: () -> Boolean = { true },
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    private val evaluator = SwaaraShakeEvaluator()
    private var isRegistered = false

    companion object {
        private const val TAG = "SwaaraShakeDetector"
    }

    fun start() {
        if (!DataPreferences.isShakeToReportEnabled) {
            stop()
            return
        }
        if (isRegistered) return
        accelerometer?.let { sensor ->
            evaluator.resetAll()
            isRegistered = sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            ) == true
            if (isRegistered) {
                Log.d(TAG, "Accelerometer listener registered (SENSOR_DELAY_GAME) for double-shake reporting.")
            }
        }
    }

    fun stop() {
        if (isRegistered) {
            sensorManager?.unregisterListener(this)
            isRegistered = false
        }
        evaluator.resetAll()
    }

    private fun isDeviceInteractiveAndUnlocked(): Boolean {
        val interactive = powerManager?.isInteractive ?: true
        val locked = keyguardManager?.isKeyguardLocked ?: false
        return interactive && !locked
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isRegistered || event == null || event.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        if (!DataPreferences.isShakeToReportEnabled) {
            evaluator.resetSequence()
            return
        }

        if (!isDeviceInteractiveAndUnlocked() || !canTrigger()) {
            evaluator.notifyBlocked()
            return
        }

        val triggered = evaluator.processSample(
            rawX = event.values[0],
            rawY = event.values[1],
            rawZ = event.values[2],
            timestampNs = event.timestamp
        )
        if (triggered) {
            triggerHapticFeedback()
            Log.i(
                TAG,
                "Double shake gesture verified (${SwaaraShakeEvaluator.REQUIRED_PEAK_COUNT} alternating peaks in ${evaluator.lastSequenceSpanMs}ms). Opening bug report."
            )
            onShake()
        }
    }

    private fun triggerHapticFeedback() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(75)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
