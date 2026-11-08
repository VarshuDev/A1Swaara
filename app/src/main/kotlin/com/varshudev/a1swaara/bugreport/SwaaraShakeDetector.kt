package com.varshudev.a1swaara.bugreport

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.varshudev.a1swaara.preferences.DataPreferences
import kotlin.math.sqrt

/**
 * High-sensitivity double-shake gesture detector inspired by Instagram's rage shake.
 * Requires two rapid, distinct acceleration reversals above the physical threshold
 * to trigger the bug reporting sheet, while ignoring normal walking, steps, and desk placement.
 */
class SwaaraShakeDetector(
    private val context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTimestamp: Long = 0L
    private var shakeCount: Int = 0

    companion object {
        private const val TAG = "SwaaraShakeDetector"
        // G-force threshold for an intentional physical shake (gravity is 1.0)
        private const val SHAKE_G_THRESHOLD = 2.15f
        // Minimum time between registered shakes in milliseconds (debounce single movement)
        private const val MIN_TIME_BETWEEN_SHAKES_MS = 240L
        // Maximum time window to register a double shake sequence
        private const val DOUBLE_SHAKE_WINDOW_MS = 950L
        // Cooldown after trigger before detecting shakes again (prevents repeated popups)
        private const val TRIGGER_COOLDOWN_MS = 2500L
    }

    fun start() {
        if (!DataPreferences.isShakeToReportEnabled) return
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d(TAG, "Accelerometer listener registered for double-shake reporting.")
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        shakeCount = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !DataPreferences.isShakeToReportEnabled) return

        val now = System.currentTimeMillis()
        if (now - lastShakeTimestamp < MIN_TIME_BETWEEN_SHAKES_MS) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // Net g-force vector magnitude
        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > SHAKE_G_THRESHOLD) {
            if (now - lastShakeTimestamp > DOUBLE_SHAKE_WINDOW_MS) {
                // Window expired; restart shake count for fresh double-shake
                shakeCount = 0
            }

            shakeCount++
            lastShakeTimestamp = now

            if (shakeCount >= 2) {
                shakeCount = 0
                lastShakeTimestamp = now + TRIGGER_COOLDOWN_MS
                triggerHapticFeedback()
                Log.i(TAG, "Double shake gesture recognized! Triggering bug report popup.")
                onShake()
            }
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
