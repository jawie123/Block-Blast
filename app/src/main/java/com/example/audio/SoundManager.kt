package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * SoundManager using Android's SoundPool for crisp, zero-latency arcade sound effects
 * and MediaPlayer for a looping ambient puzzle background music track.
 */
class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val musicLock = Any()

    @Volatile
    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            updateMusicPlaybackState()
        }

    @Volatile
    var isSoundEnabled: Boolean = true

    @Volatile
    var isHapticsEnabled: Boolean = true

    @Volatile
    private var isMusicForegrounded: Boolean = false

    @Volatile
    private var musicFilePath: String? = null

    private var mediaPlayer: MediaPlayer? = null

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // Sound IDs loaded into SoundPool
    private var soundPickupId: Int = 0
    private var soundPlaceId: Int = 0
    private var soundLineClearId: Int = 0
    private var soundComboId: Int = 0
    private var soundGameOverId: Int = 0
    private var soundPowerupId: Int = 0
    private var soundHammerId: Int = 0

    init {
        scope.launch {
            initSounds()
        }
    }

    private fun initSounds() {
        try {
            val sampleRate = 22050

            // 1. Placement Sound: punchy arcade pop (260Hz down to 120Hz)
            val placeSamples = generateToneSamples(
                frequencies = doubleArrayOf(260.0, 180.0, 120.0),
                durationsMs = doubleArrayOf(20.0, 25.0, 30.0),
                sampleRate = sampleRate,
                decay = true
            )
            val placeFile = writeWavFile("snd_place.wav", placeSamples, sampleRate)
            soundPlaceId = soundPool.load(placeFile.absolutePath, 1)

            // 2. Pickup Sound: gentle bright chirp (520Hz to 660Hz)
            val pickupSamples = generateToneSamples(
                frequencies = doubleArrayOf(520.0, 660.0),
                durationsMs = doubleArrayOf(20.0, 25.0),
                sampleRate = sampleRate,
                decay = true
            )
            val pickupFile = writeWavFile("snd_pickup.wav", pickupSamples, sampleRate)
            soundPickupId = soundPool.load(pickupFile.absolutePath, 1)

            // 3. Line Clear Sound: ascending chord chime (C5, E5, G5, C6)
            val clearSamples = generateToneSamples(
                frequencies = doubleArrayOf(523.25, 659.25, 783.99, 1046.50),
                durationsMs = doubleArrayOf(60.0, 60.0, 70.0, 120.0),
                sampleRate = sampleRate,
                decay = true
            )
            val clearFile = writeWavFile("snd_clear.wav", clearSamples, sampleRate)
            soundLineClearId = soundPool.load(clearFile.absolutePath, 1)

            // 4. Combo Cheer: sparkling high-energy fanfare chord
            val comboSamples = generateToneSamples(
                frequencies = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51),
                durationsMs = doubleArrayOf(50.0, 50.0, 60.0, 80.0, 180.0),
                sampleRate = sampleRate,
                decay = true
            )
            val comboFile = writeWavFile("snd_combo.wav", comboSamples, sampleRate)
            soundComboId = soundPool.load(comboFile.absolutePath, 1)

            // 5. Game Over: sad descending slide (440Hz down to 146Hz)
            val overSamples = generateToneSamples(
                frequencies = doubleArrayOf(440.0, 370.0, 293.66, 220.0, 146.83),
                durationsMs = doubleArrayOf(90.0, 90.0, 110.0, 130.0, 220.0),
                sampleRate = sampleRate,
                decay = true
            )
            val overFile = writeWavFile("snd_gameover.wav", overSamples, sampleRate)
            soundGameOverId = soundPool.load(overFile.absolutePath, 1)

            // 6. PowerUp / Second Chance: ascending sci-fi whoosh
            val powerSamples = generateToneSamples(
                frequencies = doubleArrayOf(220.0, 293.66, 370.0, 440.0, 587.33, 740.0, 880.0, 1174.66),
                durationsMs = doubleArrayOf(30.0, 30.0, 30.0, 35.0, 35.0, 40.0, 50.0, 150.0),
                sampleRate = sampleRate,
                decay = false
            )
            val powerFile = writeWavFile("snd_powerup.wav", powerSamples, sampleRate)
            soundPowerupId = soundPool.load(powerFile.absolutePath, 1)

            // 7. Hammer Smash Sound: heavy arcade strike & crystal shatter
            val hammerSamples = generateToneSamples(
                frequencies = doubleArrayOf(180.0, 90.0, 600.0, 1100.0, 1600.0),
                durationsMs = doubleArrayOf(35.0, 50.0, 30.0, 40.0, 90.0),
                sampleRate = sampleRate,
                decay = true
            )
            val hammerFile = writeWavFile("snd_hammer.wav", hammerSamples, sampleRate)
            soundHammerId = soundPool.load(hammerFile.absolutePath, 1)

            // 8. Ambient Looping Background Music (16s seamless chill synth pad + chimes)
            val bgmSamples = generateAmbientMusicSamples(sampleRate = sampleRate)
            val bgmFile = writeWavFile("bgm_ambient.wav", bgmSamples, sampleRate)
            musicFilePath = bgmFile.absolutePath
            updateMusicPlaybackState()
        } catch (_: Exception) {}
    }

    fun setMusicForegrounded(foregrounded: Boolean) {
        isMusicForegrounded = foregrounded
        updateMusicPlaybackState()
    }

    private fun updateMusicPlaybackState() {
        synchronized(musicLock) {
            val shouldPlay = isMusicEnabled && isMusicForegrounded
            try {
                if (shouldPlay) {
                    val path = musicFilePath ?: return
                    val player = mediaPlayer ?: MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        setDataSource(path)
                        isLooping = true
                        setVolume(0.18f, 0.18f)
                        prepare()
                    }.also { mediaPlayer = it }

                    if (!player.isPlaying) {
                        player.start()
                    }
                } else {
                    val player = mediaPlayer
                    if (player != null && player.isPlaying) {
                        player.pause()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun playClick() {
        playPickup()
    }

    fun playPickup() {
        if (!isSoundEnabled) return
        if (soundPickupId != 0) {
            soundPool.play(soundPickupId, 0.85f, 0.85f, 1, 0, 1.0f)
        }
        vibrate(15)
    }

    fun playPlace() {
        if (!isSoundEnabled) return
        if (soundPlaceId != 0) {
            soundPool.play(soundPlaceId, 1f, 1f, 1, 0, 1.0f)
        }
        vibrate(25)
    }

    /**
     * Plays line clear chime with pitch scaling higher with combo streak.
     */
    fun playLineClear(comboStreak: Int) {
        if (!isSoundEnabled) return
        val pitchRate = (1.0f + (comboStreak.coerceIn(0, 6) * 0.12f)).coerceIn(0.8f, 2.0f)
        if (soundLineClearId != 0) {
            soundPool.play(soundLineClearId, 1f, 1f, 2, 0, pitchRate)
        }
        vibrateHeavy()
    }

    fun playComboCheer() {
        if (!isSoundEnabled) return
        if (soundComboId != 0) {
            soundPool.play(soundComboId, 1f, 1f, 3, 0, 1.0f)
        }
        vibrateHeavy()
    }

    fun playGameOver() {
        if (!isSoundEnabled) return
        if (soundGameOverId != 0) {
            soundPool.play(soundGameOverId, 1f, 1f, 4, 0, 1.0f)
        }
        vibrate(60)
    }

    fun playPowerup() {
        if (!isSoundEnabled) return
        if (soundPowerupId != 0) {
            soundPool.play(soundPowerupId, 1f, 1f, 3, 0, 1.0f)
        }
        vibrateHeavy()
    }

    fun playHammerSmash() {
        if (!isSoundEnabled) return
        if (soundHammerId != 0) {
            soundPool.play(soundHammerId, 1f, 1f, 3, 0, 1.0f)
        } else {
            playPlace()
        }
        vibrateHeavy()
    }

    private fun writeWavFile(filename: String, samples: ShortArray, sampleRate: Int): File {
        val file = File(context.cacheDir, filename)
        val dataSize = samples.size * 2
        val totalSize = dataSize + 36

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                // RIFF chunk
                put('R'.code.toByte()); put('I'.code.toByte()); put('F'.code.toByte()); put('F'.code.toByte())
                putInt(totalSize)
                put('W'.code.toByte()); put('A'.code.toByte()); put('V'.code.toByte()); put('E'.code.toByte())
                // fmt chunk
                put('f'.code.toByte()); put('m'.code.toByte()); put('t'.code.toByte()); put(' '.code.toByte())
                putInt(16) // Subchunk1Size
                putShort(1) // AudioFormat (PCM)
                putShort(1) // NumChannels (Mono)
                putInt(sampleRate)
                putInt(sampleRate * 2) // ByteRate
                putShort(2) // BlockAlign
                putShort(16) // BitsPerSample
                // data chunk
                put('d'.code.toByte()); put('a'.code.toByte()); put('t'.code.toByte()); put('a'.code.toByte())
                putInt(dataSize)
            }
            out.write(header.array())

            val sampleBytes = ByteBuffer.allocate(dataSize).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                for (s in samples) {
                    putShort(s)
                }
            }
            out.write(sampleBytes.array())
        }
        return file
    }

    private fun generateToneSamples(
        frequencies: DoubleArray,
        durationsMs: DoubleArray,
        sampleRate: Int,
        decay: Boolean
    ): ShortArray {
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
                val rawSample = 0.8 * sin(2.0 * PI * freq * t) + 0.2 * sin(4.0 * PI * freq * t)
                val envelope = if (decay) {
                    (1.0 - (i.toDouble() / segSamples)).coerceIn(0.0, 1.0)
                } else {
                    1.0
                }
                val value = (rawSample * envelope * Short.MAX_VALUE * 0.75).toInt()
                buffer[sampleOffset + i] = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            sampleOffset += segSamples
        }
        return buffer
    }

    /**
     * Synthesizes a 16-second seamless ambient chill puzzle loop using soft
     * sine pads and gentle pentatonic marimba bells (Cmaj9 -> Am9 -> Fmaj7 -> G6).
     */
    private fun generateAmbientMusicSamples(sampleRate: Int): ShortArray {
        val barDurationSec = 4.0
        val barCount = 4
        val totalDurationSec = barDurationSec * barCount
        val totalSamples = (sampleRate * totalDurationSec).toInt()
        val samplesPerBar = (sampleRate * barDurationSec).toInt()
        val buffer = ShortArray(totalSamples)

        val chords = arrayOf(
            doubleArrayOf(261.63, 329.63, 392.00, 587.33), // Cmaj9
            doubleArrayOf(220.00, 261.63, 329.63, 493.88), // Am9
            doubleArrayOf(174.61, 220.00, 261.63, 329.63), // Fmaj7
            doubleArrayOf(196.00, 246.94, 293.66, 329.63)  // G6
        )
        val arpeggioNotes = arrayOf(
            doubleArrayOf(523.25, 659.25, 783.99, 659.25, 587.33, 659.25, 783.99, 1046.50),
            doubleArrayOf(440.00, 523.25, 659.25, 523.25, 493.88, 523.25, 659.25, 880.00),
            doubleArrayOf(349.23, 440.00, 523.25, 440.00, 523.25, 659.25, 523.25, 698.46),
            doubleArrayOf(392.00, 493.88, 587.33, 493.88, 587.33, 659.25, 783.99, 587.33)
        )

        val stepSamples = samplesPerBar / 8
        for (i in 0 until totalSamples) {
            val barIdx = (i / samplesPerBar).coerceIn(0, barCount - 1)
            val barOffset = i - (barIdx * samplesPerBar)
            val barProgress = barOffset.toDouble() / samplesPerBar
            val t = i.toDouble() / sampleRate

            // Smooth bell-shaped envelope per bar avoids any loop-seam click
            val padEnvelope = sin(PI * barProgress).coerceIn(0.0, 1.0)
            val chord = chords[barIdx]
            var padSum = 0.0
            for (freq in chord) {
                padSum += sin(2.0 * PI * freq * t)
            }
            val padSample = (padSum / chord.size) * padEnvelope * 0.45

            // Gentle plucked chime on each 8th beat inside the bar
            val stepIdx = (barOffset / stepSamples).coerceIn(0, 7)
            val stepOffset = barOffset - (stepIdx * stepSamples)
            val stepT = stepOffset.toDouble() / sampleRate
            val arpFreq = arpeggioNotes[barIdx][stepIdx]
            val attack = (stepOffset / (sampleRate * 0.015)).coerceIn(0.0, 1.0)
            val decay = exp(-stepT * 4.5)
            val arpSample = sin(2.0 * PI * arpFreq * stepT) * attack * decay * 0.25

            val mixed = (padSample + arpSample) * Short.MAX_VALUE * 0.55
            buffer[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
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

    fun release() {
        synchronized(musicLock) {
            try {
                mediaPlayer?.stop()
            } catch (_: Exception) {}
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
        try {
            soundPool.release()
        } catch (_: Exception) {}
    }
}
