package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    private fun playTone(
        frequencies: DoubleArray,
        durationsMs: DoubleArray,
        decay: Boolean = true
    ) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 22050
                val totalDurationMs = durationsMs.sum()
                val totalSamples = (sampleRate * (totalDurationMs / 1000.0)).toInt()
                val buffer = ShortArray(totalSamples)

                var sampleOffset = 0
                for (seg in frequencies.indices) {
                    val freq = frequencies[seg]
                    val segMs = durationsMs[seg]
                    val segSamples = (sampleRate * (segMs / 1000.0)).toInt()

                    for (i in 0 until segSamples) {
                        if (sampleOffset + i >= totalSamples) break
                        val t = i.toDouble() / sampleRate
                        val rawSample = sin(2.0 * PI * freq * t)
                        val envelope = if (decay) {
                            (1.0 - (i.toDouble() / segSamples)).coerceIn(0.0, 1.0)
                        } else {
                            1.0
                        }
                        val value = (rawSample * envelope * Short.MAX_VALUE * 0.7).toInt()
                        buffer[sampleOffset + i] = value.toShort()
                    }
                    sampleOffset += segSamples
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                // Release after playing
                scope.launch {
                    kotlinx.coroutines.delay(totalDurationMs.toLong() + 100)
                    try {
                        audioTrack.stop()
                        audioTrack.release()
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    fun playPickup() {
        playTone(doubleArrayOf(440.0, 587.33), doubleArrayOf(30.0, 40.0), decay = true)
        vibrate(15)
    }

    fun playPlace() {
        playTone(doubleArrayOf(220.0, 330.0), doubleArrayOf(35.0, 45.0), decay = true)
        vibrate(25)
    }

    fun playLineClear(comboStreak: Int) {
        // Ascending chord pitches scale up with combo streak
        val baseFreq = when (comboStreak.coerceIn(0, 8)) {
            0 -> 392.0 // G4
            1 -> 440.0 // A4
            2 -> 523.25 // C5
            3 -> 587.33 // D5
            4 -> 659.25 // E5
            5 -> 783.99 // G5
            6 -> 880.00 // A5
            else -> 1046.50 // C6
        }

        val freqs = doubleArrayOf(baseFreq, baseFreq * 1.25, baseFreq * 1.5, baseFreq * 2.0)
        val durations = doubleArrayOf(50.0, 50.0, 60.0, 120.0)
        playTone(freqs, durations, decay = true)
        vibrateHeavy()
    }

    fun playComboCheer() {
        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
        val durations = doubleArrayOf(60.0, 60.0, 60.0, 80.0, 180.0)
        playTone(freqs, durations, decay = true)
        vibrateHeavy()
    }

    fun playGameOver() {
        val freqs = doubleArrayOf(440.0, 392.0, 349.23, 293.66)
        val durations = doubleArrayOf(100.0, 100.0, 120.0, 250.0)
        playTone(freqs, durations, decay = true)
        vibrate(60)
    }

    fun playPowerup() {
        val freqs = doubleArrayOf(261.63, 329.63, 392.0, 523.25, 659.25, 783.99, 1046.50)
        val durations = doubleArrayOf(30.0, 30.0, 30.0, 30.0, 40.0, 50.0, 150.0)
        playTone(freqs, durations, decay = false)
        vibrateHeavy()
    }

    private fun vibrate(durationMs: Long) {
        if (!isHapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateHeavy() {
        if (!isHapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 40, 50)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(60)
            }
        } catch (_: Exception) {}
    }
}
