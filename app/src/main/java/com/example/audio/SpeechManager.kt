package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "SpeechManager"

class SpeechManager(
    private val context: Context,
    private val onSpeakStart: (durationMs: Long, text: String) -> Unit = { _, _ -> },
    private val onSpeakEnd: () -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    @Volatile
    private var isDestroyed = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pcmBuffer = RecordedAudioPracticeBuffer()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isPcmRecording = MutableStateFlow(false)
    val isPcmRecording: StateFlow<Boolean> = _isPcmRecording.asStateFlow()

    private val _isPlayingRecording = MutableStateFlow(false)
    val isPlayingRecording: StateFlow<Boolean> = _isPlayingRecording.asStateFlow()

    private val _hasRecordedAudio = MutableStateFlow(false)
    val hasRecordedAudio: StateFlow<Boolean> = _hasRecordedAudio.asStateFlow()

    private val _lastUserPitchContour = MutableStateFlow<List<Int>?>(null)
    val lastUserPitchContour: StateFlow<List<Int>?> = _lastUserPitchContour.asStateFlow()

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _waveformSamples = MutableStateFlow(List(32) { 0.08f })
    val waveformSamples: StateFlow<List<Float>> = _waveformSamples.asStateFlow()

    private val _speakingEpoch = MutableStateFlow(0L)
    val speakingEpoch: StateFlow<Long> = _speakingEpoch.asStateFlow()

    init {
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (isDestroyed) return
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            textToSpeech?.language = Locale.JAPANESE
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        if (!isDestroyed) {
                            _speakingEpoch.value = _speakingEpoch.value + 1L
                            onSpeakEnd()
                        }
                    }
                }
                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        if (!isDestroyed) {
                            _speakingEpoch.value = _speakingEpoch.value + 1L
                            onSpeakEnd()
                        }
                    }
                }
            })
        } else {
            Log.e(TAG, "TTS initialization failed with status $status")
        }
    }

    fun setLanguage(language: String) {
        if (isDestroyed) return
        val locale = when (language.lowercase()) {
            "japanese", "ja" -> Locale.JAPANESE
            "french", "fr" -> Locale.FRENCH
            "spanish", "es" -> Locale.forLanguageTag("es-ES")
            "german", "de" -> Locale.GERMAN
            "mandarin", "chinese", "zh" -> Locale.CHINESE
            "korean", "ko" -> Locale.KOREAN
            "italian", "it" -> Locale.ITALIAN
            else -> Locale.ENGLISH
        }
        if (isTtsReady) {
            textToSpeech?.language = locale
        }
    }

    fun speak(text: String, language: String = "Japanese", slow: Boolean = false) {
        if (isDestroyed) return
        if (_isPlayingRecording.value) {
            pcmBuffer.stopPlayback()
            _isPlayingRecording.value = false
        }
        setLanguage(language)
        textToSpeech?.setSpeechRate(if (slow) 0.72f else 1.0f)
        val rateFactor = if (slow) 1.35f else 1.0f
        val estimatedDuration = ((text.length * 75L) * rateFactor).toLong().coerceIn(1500L, 10000L)
        onSpeakStart(estimatedDuration, text)
        val utteranceId = "sensei_${System.currentTimeMillis()}"
        val speakResult = if (isTtsReady) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            TextToSpeech.ERROR
        }
        if (speakResult == TextToSpeech.ERROR || speakResult == null) {
            mainHandler.postDelayed({
                if (!isDestroyed) {
                    _speakingEpoch.value = _speakingEpoch.value + 1L
                    onSpeakEnd()
                }
            }, estimatedDuration.coerceAtMost(3500L))
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        if (!isDestroyed) {
            onSpeakEnd()
        }
    }

    fun startListening(
        language: String = "Japanese",
        onFinalResult: (String) -> Unit
    ) {
        if (isDestroyed) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition is not available on this device")
            showUnavailableToast()
            return
        }

        releaseRecognizer()
        _spokenText.value = ""
        _waveformSamples.value = List(32) { 0.08f }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (isDestroyed) return
                    _isListening.value = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    if (isDestroyed) return
                    val safeRms = rmsdB.coerceAtLeast(0f)
                    _audioRms.value = safeRms
                    val normalized = AudioPitchAnalyzer.normalizeRmsDb(safeRms)
                    _waveformSamples.value = AudioPitchAnalyzer.smoothWaveformUpdate(
                        _waveformSamples.value,
                        normalized
                    )
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    if (isDestroyed) return
                    _isListening.value = false
                    _audioRms.value = 0f
                }
                override fun onError(error: Int) {
                    Log.w(TAG, "SpeechRecognizer error: $error")
                    releaseRecognizer()
                }
                override fun onResults(results: Bundle?) {
                    if (isDestroyed) return
                    _isListening.value = false
                    _audioRms.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    _spokenText.value = text
                    if (text.isNotBlank()) {
                        onFinalResult(text)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (isDestroyed) return
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull().orEmpty()
                    if (partial.isNotBlank()) {
                        _spokenText.value = partial
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val langTag = when (language.lowercase()) {
            "japanese", "ja" -> "ja-JP"
            "french", "fr" -> "fr-FR"
            "spanish", "es" -> "es-ES"
            "german", "de" -> "de-DE"
            "mandarin", "chinese", "zh" -> "zh-CN"
            "korean", "ko" -> "ko-KR"
            "italian", "it" -> "it-IT"
            else -> "en-US"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognition", e)
            _isListening.value = false
            _audioRms.value = 0f
            showUnavailableToast()
        }
    }

    fun stopListening() {
        try {
            // Only signal end of audio so pending onResults callback can still deliver recognized text.
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping recognizer", e)
        } finally {
            _isListening.value = false
            _audioRms.value = 0f
        }
    }

    private fun releaseRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing recognizer", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _audioRms.value = 0f
        }
    }

    private fun showUnavailableToast() {
        mainHandler.post {
            Toast.makeText(
                context.applicationContext,
                context.getString(R.string.speech_recognizer_unavailable),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun startPcmCompareCapture(onCaptureReady: (List<Int>) -> Unit = {}) {
        if (isDestroyed) return
        stopListening()
        stopSpeaking()
        if (_isPcmRecording.value) return
        _isPcmRecording.value = true
        _waveformSamples.value = List(32) { 0.08f }

        pcmBuffer.startRecording(
            onWaveformNormalized = { normalized ->
                if (!isDestroyed) {
                    _audioRms.value = normalized * 12f
                    _waveformSamples.value = AudioPitchAnalyzer.smoothWaveformUpdate(
                        _waveformSamples.value,
                        normalized
                    )
                }
            },
            onFinished = { _, contour ->
                if (!isDestroyed) {
                    _isPcmRecording.value = false
                    _audioRms.value = 0f
                    _hasRecordedAudio.value = pcmBuffer.hasAudio()
                    _lastUserPitchContour.value = contour
                    onCaptureReady(contour)
                }
            },
            onUnavailable = {
                if (!isDestroyed) {
                    _isPcmRecording.value = false
                    _audioRms.value = 0f
                }
            }
        )
    }

    fun stopPcmCompareCapture() {
        pcmBuffer.stopRecording()
        _isPcmRecording.value = false
        _audioRms.value = 0f
    }

    fun togglePlayRecordedAudio() {
        if (isDestroyed) return
        if (_isPlayingRecording.value) {
            pcmBuffer.stopPlayback()
            _isPlayingRecording.value = false
            return
        }
        if (!pcmBuffer.hasAudio()) return
        stopSpeaking()
        _isPlayingRecording.value = true
        pcmBuffer.playCapturedAudio {
            if (!isDestroyed) {
                _isPlayingRecording.value = false
            }
        }
    }

    fun seedRecordedAudioForTesting(samples: ShortArray) {
        pcmBuffer.setSamplesForTesting(samples)
        _hasRecordedAudio.value = pcmBuffer.hasAudio()
        if (samples.isNotEmpty()) {
            _lastUserPitchContour.value = AudioPitchAnalyzer.extractNormalizedContour(samples)
        }
    }

    fun destroy() {
        isDestroyed = true
        isTtsReady = false
        mainHandler.removeCallbacksAndMessages(null)
        pcmBuffer.release()
        releaseRecognizer()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
