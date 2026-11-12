package com.varshudev.a1swaara.bugreport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwaaraShakeDetectorTest {

    private val msToNs = 1_000_000L

    @Test
    fun `stationary phone gravity at arbitrary orientation never triggers`() {
        val evaluator = SwaaraShakeEvaluator()
        var tMs = 1000L
        var triggered = false
        // Resting upright and tilted at 1g (~9.81 m/s^2) over 200 samples (~4s)
        for (i in 0 until 200) {
            if (evaluator.processSample(5.6f, 5.7f, 5.6f, tMs * msToNs)) {
                triggered = true
            }
            tMs += 20L
        }
        assertFalse("Stationary gravity must never trigger shake popup", triggered)
    }

    @Test
    fun `violent shake during warmup window is ignored`() {
        val evaluator = SwaaraShakeEvaluator()
        // Seed at t=1000ms
        assertFalse(evaluator.processSample(0f, 9.81f, 0f, 1000L * msToNs))
        // 4 alternating high-G peaks inside first 250ms (< 300ms WARMUP_IGNORE_NS)
        assertFalse(evaluator.processSample(22f, 9.81f, 0f, 1060L * msToNs))
        assertFalse(evaluator.processSample(-22f, 9.81f, 0f, 1130L * msToNs))
        assertFalse(evaluator.processSample(22f, 9.81f, 0f, 1200L * msToNs))
        assertFalse(evaluator.processSample(-22f, 9.81f, 0f, 1270L * msToNs))
    }

    @Test
    fun `single unidirectional bump does not trigger`() {
        val evaluator = SwaaraShakeEvaluator()
        var tMs = 1000L
        // Warmup stationary for 400ms at 20ms intervals
        repeat(21) {
            assertFalse(evaluator.processSample(0f, 9.81f, 0f, tMs * msToNs))
            tMs += 20L
        }
        // Single strong impact along +X axis
        assertFalse(evaluator.processSample(26f, 9.81f, 0f, tMs * msToNs))
        tMs += 90L
        assertFalse(evaluator.processSample(24f, 9.81f, 0f, tMs * msToNs))
    }

    @Test
    fun `intentional double shake with four alternating reversals triggers and respects cooldown`() {
        val evaluator = SwaaraShakeEvaluator()
        var tMs = 1000L
        // Warmup stationary for 400ms
        repeat(21) {
            assertFalse(evaluator.processSample(0f, 9.81f, 0f, tMs * msToNs))
            tMs += 20L
        }

        // Peak 1: +X
        assertFalse(evaluator.processSample(24f, 9.81f, 0f, tMs * msToNs))
        tMs += 110L
        // Peak 2: -X
        assertFalse(evaluator.processSample(-24f, 9.81f, 0f, tMs * msToNs))
        tMs += 110L
        // Peak 3: +X
        assertFalse(evaluator.processSample(24f, 9.81f, 0f, tMs * msToNs))
        tMs += 110L
        // Peak 4: -X (completes double shake, span = 330ms in 200..1100ms)
        val triggered = evaluator.processSample(-24f, 9.81f, 0f, tMs * msToNs)
        assertTrue("Intentional 4-peak double shake should trigger", triggered)

        // Subsequent immediate shakes within 2.5s cooldown must be suppressed
        var cooldownTriggered = false
        for (i in 0 until 4) {
            tMs += 110L
            val x = if (i % 2 == 0) 24f else -24f
            if (evaluator.processSample(x, 9.81f, 0f, tMs * msToNs)) {
                cooldownTriggered = true
            }
        }
        assertFalse("Cooldown window should block back-to-back triggers", cooldownTriggered)
    }

    @Test
    fun `notifyBlocked resets partial sequence and restarts warmup`() {
        val evaluator = SwaaraShakeEvaluator()
        var tMs = 1000L
        repeat(21) {
            evaluator.processSample(0f, 9.81f, 0f, tMs * msToNs)
            tMs += 20L
        }
        // Accumulate 3 strokes right before lock/background
        assertFalse(evaluator.processSample(24f, 9.81f, 0f, tMs * msToNs))
        tMs += 110L
        assertFalse(evaluator.processSample(-24f, 9.81f, 0f, tMs * msToNs))
        tMs += 110L
        assertFalse(evaluator.processSample(24f, 9.81f, 0f, tMs * msToNs))

        evaluator.notifyBlocked()
        tMs += 110L
        // Fourth stroke after unblocking must NOT complete stale pre-lock sequence
        assertFalse(evaluator.processSample(-24f, 9.81f, 0f, tMs * msToNs))
    }
}
