package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Game haptic feedback event types.
 */
sealed interface HapticEffect {
    object Placement : HapticEffect
    data class LineClear(val comboStreak: Int = 1) : HapticEffect
    object Pickup : HapticEffect
    object SnapPreview : HapticEffect
    object GameOver : HapticEffect
    object PowerUp : HapticEffect
}

/**
 * AppHaptics utility providing rich physical vibrations on modern and legacy devices.
 */
object AppHaptics {

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Executes appropriate tactile vibration based on the [HapticEffect].
     */
    fun perform(
        context: Context,
        effect: HapticEffect,
        composeHaptic: HapticFeedback? = null
    ) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        when (effect) {
            is HapticEffect.Placement -> {
                composeHaptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vibratePlacement(vibrator)
            }
            is HapticEffect.LineClear -> {
                composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
                vibrateLineClear(vibrator, effect.comboStreak)
            }
            is HapticEffect.Pickup -> {
                composeHaptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vibrateLight(vibrator, durationMs = 15)
            }
            is HapticEffect.SnapPreview -> {
                vibrateLight(vibrator, durationMs = 10)
            }
            is HapticEffect.GameOver -> {
                composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
                vibrateHeavy(vibrator, durationMs = 70)
            }
            is HapticEffect.PowerUp -> {
                vibrateWaveform(vibrator, longArrayOf(0, 25, 30, 45, 30, 80), intArrayOf(0, 100, 0, 180, 0, 255))
            }
        }
    }

    private fun vibratePlacement(vibrator: Vibrator) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, 200))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateLineClear(vibrator: Vibrator, comboStreak: Int) {
        try {
            if (comboStreak > 1) {
                // Multi-pulse wave for combo clears
                val pulseCount = comboStreak.coerceIn(2, 5)
                val timings = mutableListOf<Long>(0)
                val amplitudes = mutableListOf<Int>(0)
                for (i in 0 until pulseCount) {
                    timings.add(35)
                    amplitudes.add((180 + (i * 20)).coerceAtMost(255))
                    timings.add(40)
                    amplitudes.add(0)
                }
                vibrateWaveform(vibrator, timings.toLongArray(), amplitudes.toIntArray())
            } else {
                // Crisp double-burst for single line clear
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 35, 40, 55)
                    val amplitudes = intArrayOf(0, 200, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (_: Exception) {}
    }

    private fun vibrateLight(vibrator: Vibrator, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, 120))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateHeavy(vibrator: Vibrator, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateWaveform(vibrator: Vibrator, timings: LongArray, amplitudes: IntArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings.sum())
            }
        } catch (_: Exception) {}
    }
}
