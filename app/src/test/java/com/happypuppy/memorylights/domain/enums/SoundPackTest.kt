package com.happypuppy.memorylights.domain.enums

import com.happypuppy.memorylights.data.manager.SimonSoundManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.log10

class SoundPackTest {

    @Test
    fun `all 7 sound packs are available and map to 49 distinct non-zero raw resources`() {
        assertEquals(7, SoundPack.entries.size)
        assertTrue(SoundPack.entries.all { it.isAvailable })

        val resolvedIds = mutableSetOf<Int>()
        SoundPack.entries.forEach { pack ->
            SimonButton.entries.forEach { button ->
                val name = "${pack.resourcePrefix}_${button.name.lowercase()}_tone"
                val id = SimonSoundManager.getResourceId(name)
                assertNotEquals("Missing resource mapping for $name", 0, id)
                assertTrue("Duplicate resource ID for $name", resolvedIds.add(id))
            }
            val errorName = "${pack.resourcePrefix}_error_tone"
            val errorId = SimonSoundManager.getResourceId(errorName)
            assertNotEquals("Missing resource mapping for $errorName", 0, errorId)
            assertTrue("Duplicate resource ID for $errorName", resolvedIds.add(errorId))
        }
        assertEquals(49, resolvedIds.size)
    }

    @Test
    fun `all 49 raw wav files are unique 44_1kHz 16-bit mono PCM normalized to -1 dBFS`() {
        val rawDir = listOf(
            File("src/main/res/raw"),
            File("app/src/main/res/raw")
        ).firstOrNull { it.isDirectory }
            ?: error("Could not locate res/raw directory from ${File(".").absolutePath}")

        val hashes = mutableSetOf<String>()
        val digest = MessageDigest.getInstance("SHA-256")

        SoundPack.entries.forEach { pack ->
            val fileNames = SimonButton.entries.map {
                "${pack.resourcePrefix}_${it.name.lowercase()}_tone.wav"
            } + "${pack.resourcePrefix}_error_tone.wav"

            fileNames.forEach { fileName ->
                val file = File(rawDir, fileName)
                assertTrue("Missing WAV file: ${file.path}", file.isFile)
                val bytes = file.readBytes()
                val hash = digest.digest(bytes).joinToString("") { "%02x".format(it) }
                assertTrue("Duplicate WAV content detected for $fileName", hashes.add(hash))

                // Parse standard RIFF/WAVE header and sample peak
                assertTrue("WAV too short: $fileName", bytes.size >= 44)
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                val riff = String(bytes, 0, 4, Charsets.US_ASCII)
                val wave = String(bytes, 8, 4, Charsets.US_ASCII)
                assertEquals("RIFF", riff)
                assertEquals("WAVE", wave)

                val channels = buf.getShort(22).toInt()
                val sampleRate = buf.getInt(24)
                val bitsPerSample = buf.getShort(34).toInt()
                assertEquals("$fileName channels", 1, channels)
                assertEquals("$fileName sampleRate", 44_100, sampleRate)
                assertEquals("$fileName bitsPerSample", 16, bitsPerSample)

                // Find data chunk
                var offset = 12
                var dataOffset = -1
                var dataSize = 0
                while (offset + 8 <= bytes.size) {
                    val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
                    val chunkSize = buf.getInt(offset + 4)
                    if (chunkId == "data") {
                        dataOffset = offset + 8
                        dataSize = chunkSize
                        break
                    }
                    offset += 8 + chunkSize + (chunkSize and 1)
                }
                assertTrue("Missing data chunk in $fileName", dataOffset > 0 && dataOffset + dataSize <= bytes.size)

                var peak = 0
                var sum = 0L
                var sampleCount = 0
                var pos = dataOffset
                while (pos + 1 < dataOffset + dataSize) {
                    val sample = buf.getShort(pos).toInt()
                    val magnitude = abs(sample)
                    if (magnitude > peak) peak = magnitude
                    sum += sample
                    sampleCount++
                    pos += 2
                }
                assertTrue("No samples in $fileName", sampleCount > 0)
                assertTrue("$fileName clipped (peak=$peak)", peak < 32767)
                val dbfs = 20.0 * log10(peak / 32767.0)
                assertTrue("$fileName peak $dbfs dBFS out of [-1.2, -0.95] range", dbfs in -1.2..-0.95)

                // Zero significant DC offset: STANDARD/ELECTRONIC/RETRO/FUNNY/MUSICAL/SCI_FI must be < 0.01.
                // Pre-existing NATURE WAVs (nature_blue_tone.wav ~-0.027, nature_orange_tone.wav ~+0.013)
                // are kept untouched from the shipped pack and bounded below 0.03.
                val normalizedDcMean = abs(sum.toDouble() / sampleCount) / 32767.0
                val maxAllowedDc = if (pack == SoundPack.NATURE) 0.03 else 0.01
                assertTrue(
                    "$fileName DC offset $normalizedDcMean exceeds threshold $maxAllowedDc",
                    normalizedDcMean < maxAllowedDc
                )
            }
        }
        assertEquals(49, hashes.size)
    }
}
