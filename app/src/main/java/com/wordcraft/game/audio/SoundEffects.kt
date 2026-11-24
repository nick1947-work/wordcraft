package com.wordcraft.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundEffects {

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)
    var isEnabled: Boolean = true

    // Pentatonic scale frequencies for consecutive letter connections
    private val letterNotes = doubleArrayOf(
        261.63, // C4
        293.66, // D4
        329.63, // E4
        392.00, // G4
        440.00, // A4
        523.25, // C5
        587.33  // D5
    )

    fun playLetterSelect(letterIndexInSelection: Int) {
        if (!isEnabled) return
        val noteIdx = letterIndexInSelection.coerceIn(0, letterNotes.size - 1)
        val freq = letterNotes[noteIdx]
        scope.launch {
            playTone(freq, durationMs = 80, attackMs = 10, decayMs = 70)
        }
    }

    fun playWordValid() {
        if (!isEnabled) return
        scope.launch {
            // Arpeggio C5 -> E5 -> G5 -> C6
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            for (freq in notes) {
                playTone(freq, durationMs = 90, attackMs = 10, decayMs = 80)
            }
        }
    }

    fun playBonusWord() {
        if (!isEnabled) return
        scope.launch {
            // Sparkling chime
            val notes = doubleArrayOf(783.99, 987.77, 1174.66)
            for (freq in notes) {
                playTone(freq, durationMs = 80, attackMs = 10, decayMs = 70)
            }
        }
    }

    fun playWordInvalid() {
        if (!isEnabled) return
        scope.launch {
            playTone(180.0, durationMs = 90, attackMs = 5, decayMs = 85)
            playTone(150.0, durationMs = 120, attackMs = 5, decayMs = 115)
        }
    }

    fun playShuffle() {
        if (!isEnabled) return
        scope.launch {
            playTone(440.0, durationMs = 50, attackMs = 5, decayMs = 45)
            playTone(554.37, durationMs = 50, attackMs = 5, decayMs = 45)
        }
    }

    fun playHint() {
        if (!isEnabled) return
        scope.launch {
            playTone(880.0, durationMs = 70, attackMs = 10, decayMs = 60)
            playTone(1318.51, durationMs = 110, attackMs = 10, decayMs = 100)
        }
    }

    fun playLevelComplete() {
        if (!isEnabled) return
        scope.launch {
            val victoryNotes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
            for (freq in victoryNotes) {
                playTone(freq, durationMs = 120, attackMs = 15, decayMs = 105)
            }
        }
    }

    private fun playTone(freq: Double, durationMs: Int, attackMs: Int, decayMs: Int) {
        try {
            val numSamples = (durationMs * sampleRate) / 1000
            val attackSamples = (attackMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val envelope = when {
                    i < attackSamples -> i.toFloat() / attackSamples
                    else -> 1f - ((i - attackSamples).toFloat() / (numSamples - attackSamples)).coerceIn(0f, 1f)
                }
                val sample = sin(2.0 * PI * i * freq / sampleRate) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.45).toInt().toShort()
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
            Thread.sleep(durationMs.toLong() + 20)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {
            // Ignore audio generation issues
        }
    }
}
