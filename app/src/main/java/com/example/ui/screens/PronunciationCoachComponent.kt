package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.SpeechManager
import java.util.Locale

data class SpeechPatternSummary(
    val peakAmplitude: Float,
    val averageAmplitude: Float,
    val activeSpeechRatio: Float
)

fun analyzeSpeechPattern(samples: List<Float>): SpeechPatternSummary {
    if (samples.isEmpty()) {
        return SpeechPatternSummary(peakAmplitude = 0f, averageAmplitude = 0f, activeSpeechRatio = 0f)
    }
    var peak = 0f
    var sum = 0f
    var activeCount = 0
    for (raw in samples) {
        val v = raw.coerceIn(0f, 1f)
        if (v > peak) peak = v
        sum += v
        if (v >= 0.25f) activeCount++
    }
    return SpeechPatternSummary(
        peakAmplitude = peak,
        averageAmplitude = sum / samples.size,
        activeSpeechRatio = activeCount.toFloat() / samples.size.toFloat()
    )
}

/**
 * Self-contained Pronunciation Coach audio capture & real-time Canvas speech pattern component.
 * Connects directly to [SpeechManager] for live microphone RMS/waveform frames and speech transcripts.
 */
@Composable
fun PronunciationCoachPanel(
    speechManager: SpeechManager,
    targetLanguage: String,
    targetPhrase: String,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onOpenManualInput: () -> Unit,
    onTranscriptReady: (String, List<Int>?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening by speechManager.isListening.collectAsState()
    val isPcmRecording by speechManager.isPcmRecording.collectAsState()
    val isPlayingRecording by speechManager.isPlayingRecording.collectAsState()
    val hasRecordedAudio by speechManager.hasRecordedAudio.collectAsState()
    val lastUserPitchContour by speechManager.lastUserPitchContour.collectAsState()
    val spokenText by speechManager.spokenText.collectAsState()
    val waveformSamples by speechManager.waveformSamples.collectAsState()
    val audioRms by speechManager.audioRms.collectAsState()

    PronunciationCoachCard(
        waveformSamples = waveformSamples,
        isListening = isListening || isPcmRecording,
        spokenText = spokenText,
        audioRmsDb = audioRms,
        onMicToggle = {
            if (!hasMicPermission) {
                onRequestMicPermission()
            } else if (isListening) {
                speechManager.stopListening()
            } else {
                speechManager.startListening(targetLanguage) { resultText ->
                    onTranscriptReady(resultText, speechManager.lastUserPitchContour.value)
                }
            }
        },
        onOpenManualInput = onOpenManualInput,
        modifier = modifier,
        isPcmRecording = isPcmRecording,
        hasRecordedAudio = hasRecordedAudio,
        isPlayingRecording = isPlayingRecording,
        onRecordMineToggle = {
            if (!hasMicPermission) {
                onRequestMicPermission()
            } else if (isPcmRecording) {
                speechManager.stopPcmCompareCapture()
            } else {
                speechManager.startPcmCompareCapture()
            }
        },
        onPlayMineToggle = {
            speechManager.togglePlayRecordedAudio()
        }
    )
}

@Composable
fun PronunciationCoachCard(
    waveformSamples: List<Float>,
    isListening: Boolean,
    spokenText: String,
    audioRmsDb: Float,
    onMicToggle: () -> Unit,
    onOpenManualInput: () -> Unit,
    modifier: Modifier = Modifier,
    isPcmRecording: Boolean = false,
    hasRecordedAudio: Boolean = false,
    isPlayingRecording: Boolean = false,
    onRecordMineToggle: () -> Unit = {},
    onPlayMineToggle: () -> Unit = {}
) {
    val dbFormatted = remember(audioRmsDb) {
        String.format(Locale.US, "%.1f", audioRmsDb.coerceAtLeast(0f))
    }
    val speechPattern = remember(waveformSamples) {
        analyzeSpeechPattern(waveformSamples)
    }
    val peakPercent = (speechPattern.peakAmplitude * 100f).toInt().coerceIn(0, 100)
    val cadencePercent = (speechPattern.activeSpeechRatio * 100f).toInt().coerceIn(0, 100)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pronunciation_coach_panel"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191642)),
        border = BorderStroke(
            width = 1.dp,
            color = if (isListening) Color(0xFF34D399).copy(alpha = 0.7f) else Color(0xFF312E81)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with live status badge & dB meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isListening) Color(0xFF34D399) else Color(0xFF818CF8))
                    )
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (isListening) Color(0xFF34D399) else Color(0xFFA5B4FC),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.coach_waveform_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isListening) Color(0xFF064E3B).copy(alpha = 0.8f)
                            else Color(0xFF312E81).copy(alpha = 0.7f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isListening) {
                            stringResource(R.string.coach_waveform_db_format, dbFormatted)
                        } else {
                            stringResource(R.string.coach_waveform_ready)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isListening) Color(0xFF6EE7B7) else Color(0xFFA5B4FC),
                        modifier = Modifier.testTag("coach_waveform_status")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-time Canvas Waveform & Speech Cadence Envelope
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F0E26))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                CanvasWaveformVisualizer(
                    samples = waveformSamples,
                    isListening = isListening,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Real-time Speech Pattern Telemetry Strip (Projection Peak & Active Cadence Ratio)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coach_speech_pattern_metrics"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (peakPercent >= 65) Color(0xFF34D399)
                                    else if (peakPercent >= 30) Color(0xFFFBBF24)
                                    else Color(0xFF64748B)
                                )
                        )
                        Text(
                            text = "${stringResource(R.string.result_phonetic_precision)}: ${stringResource(R.string.percent_format, peakPercent)}",
                            fontSize = 10.sp,
                            color = Color(0xFFC7D2FE),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("coach_pattern_peak_metric")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (cadencePercent in 35..85) Color(0xFF38BDF8)
                                    else Color(0xFF818CF8)
                                )
                        )
                        Text(
                            text = "${stringResource(R.string.result_fluency_rhythm)}: ${stringResource(R.string.percent_format, cadencePercent)}",
                            fontSize = 10.sp,
                            color = Color(0xFF93C5FD),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("coach_pattern_cadence_metric")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Speech Transcript or Coaching Hint
            if (isListening || spokenText.isNotBlank()) {
                Text(
                    text = if (isListening) {
                        stringResource(R.string.coach_listening_format, spokenText)
                    } else {
                        stringResource(R.string.coach_transcribed_format, spokenText)
                    },
                    color = Color(0xFF34D399),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .testTag("coach_live_transcript")
                )
            } else {
                Text(
                    text = stringResource(R.string.coach_waveform_hint),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            // Capture Controls: Pulsing Mic Button + Manual Fallback
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = if (isListening) 1.16f else 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "micPulseScale"
                )

                FloatingActionButton(
                    onClick = onMicToggle,
                    modifier = Modifier
                        .size(72.dp)
                        .scale(if (isListening) pulseScale else 1f)
                        .testTag("microphone_button"),
                    shape = CircleShape,
                    containerColor = if (isListening) Color(0xFFEF4444) else Color(0xFF4F46E5)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = stringResource(R.string.cd_speak_to_sensei),
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                OutlinedButton(
                    onClick = onOpenManualInput,
                    modifier = Modifier.testTag("manual_input_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5B4FC))
                ) {
                    Text(stringResource(R.string.coach_type_input), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PCM Pitch Capture & Hear Yourself Compare Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRecordMineToggle,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("record_mine_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isPcmRecording) Color(0xFFEF4444) else Color(0xFF4F46E5)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isPcmRecording) Color(0xFFFCA5A5) else Color(0xFFC7D2FE)
                    )
                ) {
                    Icon(
                        imageVector = if (isPcmRecording) Icons.Default.Stop else Icons.Default.Hearing,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (isPcmRecording) R.string.coach_stop_pitch_rec else R.string.coach_record_pitch_sample
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (hasRecordedAudio) {
                    OutlinedButton(
                        onClick = onPlayMineToggle,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("play_mine_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF34D399)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF34D399)
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlayingRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                if (isPlayingRecording) R.string.coach_stop_mine else R.string.coach_play_mine
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
