package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "RecordedAudioBuffer"

/**
 * Lightweight 16 kHz mono 16-bit PCM capture & playback helper.
 * Enables "Hear Yourself vs. Sensei" playback and true microphone F0 pitch contour analysis
 * while safely no-oping on devices/emulators without audio input hardware.
 */
class RecordedAudioPracticeBuffer(
    private val sampleRateHz: Int = 16_000,
    private val maxSeconds: Int = 8
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRecordingFlag = AtomicBoolean(false)
    private val isPlayingFlag = AtomicBoolean(false)

    private var recordJob: Job? = null
    private var playJob: Job? = null
    @Volatile
    private var capturedSamples: ShortArray = ShortArray(0)

    val sampleRate: Int get() = sampleRateHz

    fun hasAudio(): Boolean = capturedSamples.isNotEmpty()

    fun getSamplesSnapshot(): ShortArray = capturedSamples.copyOf()

    fun setSamplesForTesting(samples: ShortArray) {
        capturedSamples = samples.copyOf()
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        onWaveformNormalized: (Float) -> Unit,
        onFinished: (ShortArray, List<Int>) -> Unit,
        onUnavailable: () -> Unit
    ) {
        stopPlayback()
        if (isRecordingFlag.getAndSet(true)) return

        recordJob = scope.launch {
            val minBufferBytes = AudioRecord.getMinBufferSize(
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBufferBytes <= 0) {
                isRecordingFlag.set(false)
                withContext(Dispatchers.Main) { onUnavailable() }
                return@launch
            }

            var audioRecord: AudioRecord? = null
            val maxSamples = sampleRateHz * maxSeconds
            val collected = ArrayList<Short>(sampleRateHz * 4)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRateHz,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBufferBytes, 4096)
                )
                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    isRecordingFlag.set(false)
                    withContext(Dispatchers.Main) { onUnavailable() }
                    return@launch
                }

                audioRecord.startRecording()
                val chunk = ShortArray(512)
                while (isActive && isRecordingFlag.get() && collected.size < maxSamples) {
                    val readCount = audioRecord.read(chunk, 0, chunk.size)
                    if (readCount > 0) {
                        val remaining = (maxSamples - collected.size).coerceAtMost(readCount)
                        for (i in 0 until remaining) {
                            collected.add(chunk[i])
                        }
                        val rms = AudioPitchAnalyzer.computeRms(chunk, 0, readCount)
                        val normalized = (rms / 6500.0).toFloat().coerceIn(0.06f, 1f)
                        withContext(Dispatchers.Main) {
                            onWaveformNormalized(normalized)
                        }
                    } else {
                        break
                    }
                }
            } catch (se: SecurityException) {
                Log.w(TAG, "Microphone permission missing for PCM recording", se)
                isRecordingFlag.set(false)
                withContext(Dispatchers.Main) { onUnavailable() }
                return@launch
            } catch (e: Exception) {
                Log.w(TAG, "Unable to capture PCM sample", e)
                isRecordingFlag.set(false)
                withContext(Dispatchers.Main) { onUnavailable() }
                return@launch
            } finally {
                isRecordingFlag.set(false)
                runCatching { audioRecord?.stop() }
                runCatching { audioRecord?.release() }
            }

            val finalSamples = ShortArray(collected.size) { idx -> collected[idx] }
            if (finalSamples.isNotEmpty()) {
                capturedSamples = finalSamples
                val contour = AudioPitchAnalyzer.extractNormalizedContour(
                    samples = finalSamples,
                    sampleRate = sampleRateHz,
                    segments = 4
                )
                withContext(Dispatchers.Main) {
                    onFinished(finalSamples, contour)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onUnavailable()
                }
            }
        }
    }

    fun stopRecording() {
        isRecordingFlag.set(false)
    }

    fun playCapturedAudio(onComplete: () -> Unit) {
        val snapshot = capturedSamples.copyOf()
        if (snapshot.isEmpty()) {
            onComplete()
            return
        }
        stopPlayback()
        isPlayingFlag.set(true)

        playJob = scope.launch {
            var track: AudioTrack? = null
            try {
                val byteLen = snapshot.size * 2
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRateHz)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(byteLen, 4096))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(snapshot, 0, snapshot.size)
                track.play()

                val durationMs = ((snapshot.size.toDouble() / sampleRateHz) * 1000.0).toLong().coerceAtLeast(150L)
                var elapsed = 0L
                while (isActive && isPlayingFlag.get() && elapsed < durationMs) {
                    kotlinx.coroutines.delay(50L)
                    elapsed += 50L
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to play captured PCM sample", e)
            } finally {
                isPlayingFlag.set(false)
                runCatching { track?.stop() }
                runCatching { track?.release() }
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    fun stopPlayback() {
        isPlayingFlag.set(false)
        playJob?.cancel()
        playJob = null
    }

    fun release() {
        isRecordingFlag.set(false)
        isPlayingFlag.set(false)
        recordJob?.cancel()
        playJob?.cancel()
        recordJob = null
        playJob = null
    }
}
