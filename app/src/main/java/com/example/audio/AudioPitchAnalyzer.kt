package com.example.audio

import kotlin.math.abs

/**
 * Zero-cost on-device speech DSP helper using time-domain autocorrelation (AMDF/YIN-inspired)
 * to extract fundamental vocal frequency (F0 in Hz) and 4-point intonation contours.
 */
object AudioPitchAnalyzer {

    private const val MIN_VOICE_HZ = 75f
    private const val MAX_VOICE_HZ = 420f
    private const val MIN_RMS_THRESHOLD = 280.0

    fun computeRms(samples: ShortArray, start: Int = 0, end: Int = samples.size): Double {
        if (samples.isEmpty() || end <= start) return 0.0
        var sumSq = 0.0
        val count = end - start
        for (i in start until end) {
            val s = samples[i].toDouble()
            sumSq += s * s
        }
        return kotlin.math.sqrt(sumSq / count)
    }

    /**
     * Estimates fundamental pitch (F0) in Hz from 16-bit PCM mono audio samples.
     * Returns null for silence or unvoiced frames.
     */
    fun estimatePitchHz(samples: ShortArray, sampleRate: Int = 16_000): Float? {
        if (samples.size < sampleRate / 40) return null
        if (computeRms(samples) < MIN_RMS_THRESHOLD) return null

        val minLag = (sampleRate / MAX_VOICE_HZ).toInt().coerceAtLeast(16)
        val maxLag = (sampleRate / MIN_VOICE_HZ).toInt().coerceAtMost(samples.size / 2)
        if (maxLag <= minLag) return null

        var bestLag = -1
        var bestCorr = -1.0

        for (lag in minLag..maxLag) {
            var num = 0.0
            var denomA = 0.0
            var denomB = 0.0
            val limit = samples.size - lag
            for (i in 0 until limit) {
                val a = samples[i].toDouble()
                val b = samples[i + lag].toDouble()
                num += a * b
                denomA += a * a
                denomB += b * b
            }
            val denom = kotlin.math.sqrt(denomA * denomB)
            val corr = if (denom > 1e-6) num / denom else 0.0
            if (corr > bestCorr) {
                bestCorr = corr
                bestLag = lag
            }
        }

        return if (bestLag > 0 && bestCorr >= 0.45) {
            sampleRate.toFloat() / bestLag.toFloat()
        } else {
            null
        }
    }

    /**
     * Extracts normalized pitch contour keypoints in range [20, 90] across [segments] windows.
     */
    fun extractNormalizedContour(
        samples: ShortArray,
        sampleRate: Int = 16_000,
        segments: Int = 4
    ): List<Int> {
        if (samples.isEmpty() || segments <= 0) return List(4) { 50 }
        val chunkSize = (samples.size / segments).coerceAtLeast(1)
        val rawHz = (0 until segments).map { idx ->
            val start = idx * chunkSize
            val end = ((idx + 1) * chunkSize).coerceAtMost(samples.size)
            val window = samples.copyOfRange(start, end)
            estimatePitchHz(window, sampleRate)
        }
        val valid = rawHz.filterNotNull()
        if (valid.isEmpty()) return listOf(48, 62, 58, 44)
        val minHz = valid.minOrNull() ?: 100f
        val maxHz = valid.maxOrNull() ?: 260f
        val span = (maxHz - minHz).coerceAtLeast(25f)

        return rawHz.mapIndexed { idx, hz ->
            val effectiveHz = hz ?: valid.getOrElse(idx.coerceAtMost(valid.lastIndex)) { 150f }
            val norm = ((effectiveHz - minHz) / span).coerceIn(0f, 1f)
            (25 + (norm * 60f).toInt()).coerceIn(20, 90)
        }
    }

    fun normalizeRmsDb(rmsdB: Float): Float {
        // Android SpeechRecognizer reports roughly -2 dB (quiet) to +12 dB (loud)
        val clamped = rmsdB.coerceIn(0f, 12f)
        return (clamped / 12f).coerceIn(0.06f, 1f)
    }

    fun smoothWaveformUpdate(current: List<Float>, nextNormalizedSample: Float): List<Float> {
        val targetSize = 32
        val base = if (current.size == targetSize) current else List(targetSize) { 0.08f }
        val last = base.lastOrNull() ?: 0.08f
        val smoothed = (last * 0.35f) + (nextNormalizedSample.coerceIn(0.05f, 1f) * 0.65f)
        val shifted = base.drop(1) + smoothed
        return shifted.mapIndexed { idx, v ->
            if (idx < shifted.lastIndex - 6 && abs(v - 0.08f) > 0.01f) v else v
        }
    }
}
