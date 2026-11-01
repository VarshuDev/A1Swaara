package com.varshudev.a1swaara.player

import androidx.media3.common.Player
import com.varshudev.a1swaara.preferences.PlayerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * High-fidelity DJ crossfade engine for seamless track transitions.
 * Smoothly attenuates audio volume near the end of the current track and
 * smoothly ramps volume up upon transitioning to the next track.
 */
object SwaaraCrossfadeEngine {
    private var fadeJob: Job? = null
    private var isFadingOut = false
    private var isFadingIn = false

    fun startCrossfadeMonitor(scope: CoroutineScope, player: Player) {
        fadeJob?.cancel()
        fadeJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val crossfadeSeconds = PlayerPreferences.crossfadeDurationSeconds
                if (crossfadeSeconds <= 0 || !player.isPlaying || isFadingIn) {
                    if (!isFadingIn && player.volume < 1.0f && crossfadeSeconds <= 0) {
                        player.volume = 1.0f
                    }
                    delay(500)
                    continue
                }

                val duration = player.duration
                val currentPosition = player.currentPosition

                if (duration > 0 && currentPosition > 0) {
                    val remainingMs = duration - currentPosition
                    val fadeDurationMs = crossfadeSeconds * 1000L

                    if (remainingMs in 1..fadeDurationMs) {
                        isFadingOut = true
                        val fraction = (remainingMs.toFloat() / fadeDurationMs).coerceIn(0.02f, 1.0f)
                        // Exponential audio taper for natural perceptual fading
                        val targetVolume = fraction.pow(1.4f)
                        player.volume = targetVolume
                        delay(60)
                        continue
                    } else if (remainingMs > fadeDurationMs && isFadingOut) {
                        // User seeked backwards or skipped
                        isFadingOut = false
                        player.volume = 1.0f
                    }
                }

                delay(250)
            }
        }
    }

    /**
     * Invoked when ExoPlayer transitions to a new media item.
     * Smoothly ramps volume from near-zero to full volume.
     */
    fun onTrackTransition(scope: CoroutineScope, player: Player) {
        val crossfadeSeconds = PlayerPreferences.crossfadeDurationSeconds
        if (crossfadeSeconds <= 0) {
            player.volume = 1.0f
            return
        }

        isFadingOut = false
        isFadingIn = true
        player.volume = 0.05f

        scope.launch(Dispatchers.Main) {
            val fadeInDurationMs = (crossfadeSeconds * 400L).coerceIn(400L, 2500L)
            val steps = 25
            val stepDelay = fadeInDurationMs / steps

            for (step in 1..steps) {
                if (!isActive || !player.isPlaying) break
                val fraction = (step.toFloat() / steps).coerceIn(0.05f, 1.0f)
                val targetVolume = fraction.pow(1.2f)
                player.volume = targetVolume
                delay(stepDelay)
            }
            player.volume = 1.0f
            isFadingIn = false
        }
    }

    fun stop() {
        fadeJob?.cancel()
        fadeJob = null
        isFadingOut = false
        isFadingIn = false
    }
}
