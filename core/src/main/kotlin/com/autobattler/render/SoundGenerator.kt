package com.autobattler.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.math.*

/**
 * Generates procedural sound effects as WAV data.
 * No external audio files needed — all sounds are synthesized at runtime.
 */
object SoundGenerator {

    private var swordClashSound: Sound? = null
    private var arrowSound: Sound? = null
    private var healSound: Sound? = null
    private var deathSound: Sound? = null
    private var victorySound: Sound? = null
    private var defeatSound: Sound? = null

    private const val SAMPLE_RATE = 22050

    fun getSwordClash(): Sound {
        if (swordClashSound == null) {
            swordClashSound = createSound(generateSwordClash())
        }
        return swordClashSound!!
    }

    fun getArrowShot(): Sound {
        if (arrowSound == null) {
            arrowSound = createSound(generateArrowWhoosh())
        }
        return arrowSound!!
    }

    fun getHealChime(): Sound {
        if (healSound == null) {
            healSound = createSound(generateHealChime())
        }
        return healSound!!
    }

    fun getDeath(): Sound {
        if (deathSound == null) {
            deathSound = createSound(generateDeath())
        }
        return deathSound!!
    }

    fun getVictory(): Sound {
        if (victorySound == null) {
            victorySound = createSound(generateVictory())
        }
        return victorySound!!
    }

    fun getDefeat(): Sound {
        if (defeatSound == null) {
            defeatSound = createSound(generateDefeat())
        }
        return defeatSound!!
    }

    fun dispose() {
        swordClashSound?.dispose()
        arrowSound?.dispose()
        healSound?.dispose()
        deathSound?.dispose()
        victorySound?.dispose()
        defeatSound?.dispose()
        swordClashSound = null
        arrowSound = null
        healSound = null
        deathSound = null
        victorySound = null
        defeatSound = null
    }

    // ─── Sword Clash: Sharp metallic burst ──────────────────────────────────────

    private fun generateSwordClash(): ShortArray {
        val duration = 0.25f
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)
        val random = java.util.Random(42)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            // Fast exponential decay
            val envelope = exp(-t * 25.0).toFloat()

            // Mix of metallic frequencies + noise
            val metallic = (sin(2.0 * PI * 3200 * t) * 0.3 +
                sin(2.0 * PI * 4800 * t) * 0.2 +
                sin(2.0 * PI * 6400 * t) * 0.15 +
                sin(2.0 * PI * 1200 * t) * 0.1).toFloat()

            // White noise burst
            val noise = (random.nextFloat() * 2f - 1f) * 0.4f

            val sample = (metallic + noise) * envelope
            samples[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    // ─── Arrow Whoosh: Filtered noise sweep ─────────────────────────────────────

    private fun generateArrowWhoosh(): ShortArray {
        val duration = 0.3f
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)
        val random = java.util.Random(123)

        var prevSample = 0f
        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            // Envelope: quick attack, medium decay
            val envelope = sin(PI * t / duration).toFloat() * exp(-t * 6.0).toFloat()

            // Pitched whoosh — rising frequency
            val freq = 400.0 + t * 2000.0
            val tonal = sin(2.0 * PI * freq * t).toFloat() * 0.3f

            // Filtered noise (low-pass via simple averaging)
            val noise = random.nextFloat() * 2f - 1f
            val filtered = prevSample * 0.7f + noise * 0.3f
            prevSample = filtered

            val sample = (tonal + filtered * 0.5f) * envelope
            samples[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    // ─── Heal Chime: Soft rising harmonic tone ──────────────────────────────────

    private fun generateHealChime(): ShortArray {
        val duration = 0.5f
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            // Soft envelope: fade in and out
            val envelope = sin(PI * t / duration).toFloat() * 0.7f

            // Pleasant chord: major triad rising
            val baseFreq = 523.0 + t * 200.0  // C5 rising
            val tone = (sin(2.0 * PI * baseFreq * t) * 0.4 +
                sin(2.0 * PI * baseFreq * 1.25 * t) * 0.3 +  // major third
                sin(2.0 * PI * baseFreq * 1.5 * t) * 0.2 +   // fifth
                sin(2.0 * PI * baseFreq * 2.0 * t) * 0.1).toFloat()  // octave

            val sample = tone * envelope
            samples[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    // ─── Death: Low descending tone + thud ──────────────────────────────────────

    private fun generateDeath(): ShortArray {
        val duration = 0.6f
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)
        val random = java.util.Random(77)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            // Envelope: sharp attack then slow fade
            val envelope = exp(-t * 4.0).toFloat()

            // Descending low tone (body falling feel)
            val freq = 200.0 - t * 150.0  // drops from 200Hz to 50Hz
            val tone = sin(2.0 * PI * freq * t).toFloat() * 0.5f

            // Thud component (very low freq burst at start)
            val thud = if (t < 0.1f) sin(2.0 * PI * 60.0 * t).toFloat() * (1f - t * 10f) else 0f

            // Noise crackle
            val noise = if (t < 0.15f) (random.nextFloat() * 2f - 1f) * 0.3f * (1f - t * 6f) else 0f

            val sample = (tone + thud + noise) * envelope
            samples[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    // ─── Victory: Triumphant ascending fanfare ──────────────────────────────────

    private fun generateVictory(): ShortArray {
        val duration = 1.2f
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        // Three ascending notes: C5 → E5 → G5 then a final C6 chord
        val notes = listOf(
            Triple(0.0f, 0.3f, 523.0),   // C5
            Triple(0.25f, 0.3f, 659.0),  // E5
            Triple(0.5f, 0.3f, 784.0),   // G5
            Triple(0.75f, 0.45f, 1047.0) // C6 (held longer)
        )

        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            var sample = 0f

            for ((start, noteDur, freq) in notes) {
                val noteT = t - start
                if (noteT in 0f..noteDur) {
                    val noteEnv = sin(PI * noteT / noteDur).toFloat() * 0.6f
                    // Rich tone with harmonics
                    val tone = (sin(2.0 * PI * freq * t) * 0.5 +
                        sin(2.0 * PI * freq * 2.0 * t) * 0.2 +
                        sin(2.0 * PI * freq * 1.5 * t) * 0.15).toFloat()
                    sample += tone * noteEnv
                }
            }

            // Final chord shimmer (last 0.3s)
            if (t > 0.9f) {
                val shimmerT = t - 0.9f
                val shimmerEnv = (1f - shimmerT / 0.3f).coerceAtLeast(0f) * 0.3f
                val shimmer = (sin(2.0 * PI * 1047.0 * t) * 0.3 +
                    sin(2.0 * PI * 1319.0 * t) * 0.2 +
                    sin(2.0 * PI * 1568.0 * t) * 0.2).toFloat()
                sample += shimmer * shimmerEnv
            }

            samples[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    // ─── Defeat: Dark descending minor chord ────────────────────────────────────

    private fun generateDefeat(): ShortArray {
        val duration = 1.5f
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        // Descending minor notes: Eb4 → C4 → Ab3 → low rumble
        val notes = listOf(
            Triple(0.0f, 0.4f, 311.0),   // Eb4
            Triple(0.35f, 0.4f, 262.0),  // C4
            Triple(0.7f, 0.5f, 208.0),   // Ab3
            Triple(1.05f, 0.45f, 147.0)  // D3 (dark low end)
        )

        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            var sample = 0f

            for ((start, noteDur, freq) in notes) {
                val noteT = t - start
                if (noteT in 0f..noteDur) {
                    // Slow attack, lingering decay
                    val attack = (noteT / 0.05f).coerceAtMost(1f)
                    val decay = (1f - noteT / noteDur)
                    val noteEnv = attack * decay * 0.6f

                    // Dark tone with minor color
                    val tone = (sin(2.0 * PI * freq * t) * 0.5 +
                        sin(2.0 * PI * freq * 1.2 * t) * 0.2 +  // minor third
                        sin(2.0 * PI * freq * 0.5 * t) * 0.15).toFloat() // sub bass
                    sample += tone * noteEnv
                }
            }

            // Final low rumble fade
            if (t > 1.2f) {
                val rumbleT = t - 1.2f
                val rumbleEnv = (1f - rumbleT / 0.3f).coerceAtLeast(0f) * 0.25f
                val rumble = sin(2.0 * PI * 80.0 * t).toFloat()
                sample += rumble * rumbleEnv
            }

            samples[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    // ─── WAV File Creation ──────────────────────────────────────────────────────

    private fun createSound(samples: ShortArray): Sound {
        val wavBytes = encodeWav(samples)
        // Write to a temp file since libGDX Sound requires a FileHandle
        val tempFile = java.io.File.createTempFile("autobattler_sfx_", ".wav")
        tempFile.deleteOnExit()
        tempFile.writeBytes(wavBytes)
        return Gdx.audio.newSound(Gdx.files.absolute(tempFile.absolutePath))
    }

    private fun encodeWav(samples: ShortArray): ByteArray {
        val byteStream = ByteArrayOutputStream()
        val out = DataOutputStream(byteStream)

        val numChannels = 1
        val bitsPerSample = 16
        val dataSize = samples.size * 2
        val fileSize = 36 + dataSize

        // RIFF header
        out.writeBytes("RIFF")
        out.writeIntLE(fileSize)
        out.writeBytes("WAVE")

        // fmt chunk
        out.writeBytes("fmt ")
        out.writeIntLE(16)              // chunk size
        out.writeShortLE(1)             // PCM format
        out.writeShortLE(numChannels)
        out.writeIntLE(SAMPLE_RATE)
        out.writeIntLE(SAMPLE_RATE * numChannels * bitsPerSample / 8) // byte rate
        out.writeShortLE(numChannels * bitsPerSample / 8) // block align
        out.writeShortLE(bitsPerSample)

        // data chunk
        out.writeBytes("data")
        out.writeIntLE(dataSize)
        for (sample in samples) {
            out.writeShortLE(sample.toInt())
        }

        out.flush()
        return byteStream.toByteArray()
    }

    // Little-endian write helpers
    private fun DataOutputStream.writeIntLE(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun DataOutputStream.writeShortLE(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }
}

