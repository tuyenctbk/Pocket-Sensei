package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.AiCoachingMode
import com.example.ai.AiSettingsSnapshot
import com.example.ai.LocalPronunciationEvaluator
import com.example.audio.SpeechManager
import com.example.ui.PracticePhrase
import kotlinx.coroutines.delay

/**
 * High-resolution 60fps Compose Canvas scrolling audio waveform visualizer ($0 local DSP).
 */
@Composable
fun CanvasWaveformVisualizer(
    samples: List<Float>,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val activeBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF34D399), Color(0xFF38BDF8), Color(0xFF818CF8))
        )
    }
    val highEnergyBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFBBF24), Color(0xFF34D399), Color(0xFF38BDF8))
        )
    }
    val idleBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF4F46E5).copy(alpha = 0.55f), Color(0xFF312E81))
        )
    }
    val areaFillBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF34D399).copy(alpha = 0.18f),
                Color(0xFF38BDF8).copy(alpha = 0.05f),
                Color(0xFF34D399).copy(alpha = 0.18f)
            )
        )
    }
    val upperEnvelopePath = remember { androidx.compose.ui.graphics.Path() }
    val lowerEnvelopePath = remember { androidx.compose.ui.graphics.Path() }
    val areaEnvelopePath = remember { androidx.compose.ui.graphics.Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .testTag("canvas_waveform_visualizer")
    ) {
        val barCount = samples.size.coerceAtLeast(16)
        val gapPx = 4.dp.toPx()
        val totalGap = gapPx * (barCount - 1)
        val barWidth = ((size.width - totalGap) / barCount).coerceAtLeast(2.dp.toPx())
        val centerY = size.height / 2f

        // Subtle amplitude guide lines (25% and 75% height) + center axis
        val guideColor = if (isListening) Color(0xFF38BDF8).copy(alpha = 0.16f) else Color(0xFF334155).copy(alpha = 0.45f)
        drawLine(
            color = guideColor,
            start = Offset(0f, size.height * 0.25f),
            end = Offset(size.width, size.height * 0.25f),
            strokeWidth = 0.8.dp.toPx()
        )
        drawLine(
            color = guideColor,
            start = Offset(0f, size.height * 0.75f),
            end = Offset(size.width, size.height * 0.75f),
            strokeWidth = 0.8.dp.toPx()
        )
        drawLine(
            color = if (isListening) Color(0xFF34D399).copy(alpha = 0.35f) else Color(0xFF475569).copy(alpha = 0.7f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx()
        )

        val upperPoints = ArrayList<Offset>(barCount)
        val lowerPoints = ArrayList<Offset>(barCount)

        for (i in 0 until barCount) {
            val amp = samples.getOrElse(i) { 0.08f }.coerceIn(0.05f, 1f)
            val effectiveAmp = if (isListening) amp else (0.08f + (i % 3) * 0.03f)
            val barHeight = (size.height * 0.86f * effectiveAmp).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + gapPx)
            val top = centerY - (barHeight / 2f)
            val bottom = centerY + (barHeight / 2f)
            val peakCenterX = x + barWidth / 2f

            upperPoints.add(Offset(peakCenterX, top))
            lowerPoints.add(Offset(peakCenterX, bottom))
        }

        upperEnvelopePath.reset()
        lowerEnvelopePath.reset()
        areaEnvelopePath.reset()

        if (upperPoints.isNotEmpty()) {
            upperEnvelopePath.moveTo(upperPoints.first().x, upperPoints.first().y)
            lowerEnvelopePath.moveTo(lowerPoints.first().x, lowerPoints.first().y)
            areaEnvelopePath.moveTo(upperPoints.first().x, upperPoints.first().y)

            for (i in 0 until upperPoints.size - 1) {
                val u0 = upperPoints[i]
                val u1 = upperPoints[i + 1]
                val uMidX = (u0.x + u1.x) / 2f
                upperEnvelopePath.cubicTo(uMidX, u0.y, uMidX, u1.y, u1.x, u1.y)
                areaEnvelopePath.cubicTo(uMidX, u0.y, uMidX, u1.y, u1.x, u1.y)

                val l0 = lowerPoints[i]
                val l1 = lowerPoints[i + 1]
                val lMidX = (l0.x + l1.x) / 2f
                lowerEnvelopePath.cubicTo(lMidX, l0.y, lMidX, l1.y, l1.x, l1.y)
            }

            for (i in lowerPoints.size - 1 downTo 0) {
                val pt = lowerPoints[i]
                areaEnvelopePath.lineTo(pt.x, pt.y)
            }
            areaEnvelopePath.close()
        }

        if (isListening) {
            drawPath(
                path = areaEnvelopePath,
                brush = areaFillBrush
            )
        }

        for (i in 0 until barCount) {
            val amp = samples.getOrElse(i) { 0.08f }.coerceIn(0.05f, 1f)
            val effectiveAmp = if (isListening) amp else (0.08f + (i % 3) * 0.03f)
            val barHeight = (size.height * 0.86f * effectiveAmp).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + gapPx)
            val top = centerY - (barHeight / 2f)

            val barBrush = when {
                !isListening -> idleBrush
                amp >= 0.72f -> highEnergyBrush
                else -> activeBrush
            }

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )

            // Highlight stressed syllable peaks with an accent dot above the bar
            if (isListening && amp >= 0.65f) {
                drawCircle(
                    color = Color(0xFFFDE68A),
                    radius = (barWidth * 0.42f).coerceAtLeast(1.8.dp.toPx()),
                    center = Offset(x + barWidth / 2f, (top - 3.dp.toPx()).coerceAtLeast(2.dp.toPx()))
                )
            }
        }

        if (isListening) {
            drawPath(
                path = upperEnvelopePath,
                color = Color(0xFF6EE7B7).copy(alpha = 0.85f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.8.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
            drawPath(
                path = lowerEnvelopePath,
                color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * AI Cost-Control & BYOK Settings Card inside ProfileScreen.
 * Supports Hybrid Free (cache + daily cap), 100% Offline ($0), and User-Funded BYOK Gemini Key.
 */
@Composable
fun AiModeSettingsCard(
    settings: AiSettingsSnapshot,
    cachedEvalCount: Int,
    onModeSelected: (AiCoachingMode) -> Unit,
    onSaveByokKey: (String) -> Unit
) {
    var keyDraft by remember { mutableStateOf("") }
    var showKeyEditor by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_mode_settings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, Color(0xFF312E81))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.ai_card_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = stringResource(R.string.ai_cached_count, cachedEvalCount),
                    fontSize = 11.sp,
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = stringResource(
                    R.string.ai_cloud_quota_summary,
                    settings.dailyCloudCallsUsed,
                    settings.dailyFreeCloudCap
                ),
                fontSize = 11.sp,
                color = Color(0xFFA5B4FC)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiCoachingMode.entries.forEach { mode ->
                    val label = when (mode) {
                        AiCoachingMode.HYBRID_FREE -> stringResource(R.string.ai_mode_chip_hybrid)
                        AiCoachingMode.LOCAL_OFFLINE_ONLY -> stringResource(R.string.ai_mode_chip_offline)
                        AiCoachingMode.BYOK_USER_KEY -> stringResource(R.string.ai_mode_chip_byok)
                    }
                    FilterChip(
                        selected = settings.mode == mode,
                        onClick = {
                            if (mode == AiCoachingMode.BYOK_USER_KEY && !settings.hasByokKey) {
                                showKeyEditor = true
                            } else {
                                onModeSelected(mode)
                            }
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("ai_mode_chip_${mode.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF141233),
                            labelColor = Color(0xFFC7D2FE)
                        )
                    )
                }
            }

            Text(
                text = when (settings.mode) {
                    AiCoachingMode.HYBRID_FREE -> stringResource(R.string.ai_mode_desc_hybrid)
                    AiCoachingMode.LOCAL_OFFLINE_ONLY -> stringResource(R.string.ai_mode_desc_offline)
                    AiCoachingMode.BYOK_USER_KEY -> stringResource(R.string.ai_mode_desc_byok)
                },
                fontSize = 11.sp,
                color = Color(0xFFC7D2FE)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showKeyEditor = !showKeyEditor },
                    modifier = Modifier.testTag("toggle_byok_editor_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (settings.hasByokKey) {
                            stringResource(R.string.ai_byok_edit_saved)
                        } else {
                            stringResource(R.string.ai_byok_add_prompt)
                        },
                        fontSize = 11.sp,
                        color = Color(0xFFFDE68A)
                    )
                }

                if (settings.hasByokKey) {
                    TextButton(onClick = { onSaveByokKey("") }) {
                        Text(stringResource(R.string.ai_byok_clear), fontSize = 11.sp, color = Color(0xFFF87171))
                    }
                }
            }

            AnimatedVisibility(visible = showKeyEditor) {
                val context = LocalContext.current
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = keyDraft,
                        onValueChange = { keyDraft = it },
                        label = { Text(stringResource(R.string.ai_byok_input_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("byok_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { openAiStudioApiKeyPage(context) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("open_aistudio_key_url_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.ai_byok_get_key_btn),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = {
                                onSaveByokKey(keyDraft)
                                keyDraft = ""
                                showKeyEditor = false
                            },
                            enabled = keyDraft.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399)),
                            modifier = Modifier.testTag("save_byok_key_button")
                        ) {
                            Text(
                                text = stringResource(R.string.ai_byok_save_btn),
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val AI_STUDIO_KEY_URL = "https://aistudio.google.com/app/apikey"

internal fun openAiStudioApiKeyPage(context: android.content.Context) {
    try {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(AI_STUDIO_KEY_URL)
        ).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        clipboard?.setPrimaryClip(
            android.content.ClipData.newPlainText(
                context.getString(R.string.ai_byok_get_key_btn),
                AI_STUDIO_KEY_URL
            )
        )
    }
}

/**
 * Guided Quick-Setup Dialog so users can plug in their own personal Gemini API key (BYOK)
 * when they reach the daily free cloud cap or want unlimited AI roleplay & topic generation.
 */
@Composable
fun ByokQuickSetupDialog(
    onSaveByokKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var keyDraft by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("byok_quick_setup_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24)
                )
                Text(
                    text = stringResource(R.string.ai_byok_add_prompt),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.ai_mode_desc_byok),
                    fontSize = 12.sp,
                    color = Color(0xFFC7D2FE)
                )
                OutlinedButton(
                    onClick = { openAiStudioApiKeyPage(context) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("byok_dialog_open_url_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ai_byok_get_key_btn),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedTextField(
                    value = keyDraft,
                    onValueChange = { keyDraft = it },
                    label = { Text(stringResource(R.string.ai_byok_input_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("byok_dialog_key_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF34D399),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveByokKey(keyDraft.trim())
                    onDismiss()
                },
                enabled = keyDraft.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399)),
                modifier = Modifier.testTag("byok_dialog_save_btn")
            ) {
                Text(
                    text = stringResource(R.string.ai_byok_save_btn),
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Color(0xFFA5B4FC))
            }
        },
        containerColor = Color(0xFF1E1B4B)
    )
}

/**
 * Zero-Cost ($0 API) 60-Second Voice Speaking & Pitch Blitz Micro-Game.
 * Uses LocalPronunciationEvaluator for instant scoring and awards Dojo League XP.
 */
@Composable
fun DojoBlitzGameDialog(
    language: String,
    phrases: List<PracticePhrase>,
    speechManager: SpeechManager,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onPraiseSensei: () -> Unit,
    onGameFinished: (totalPoints: Int, phrasesCleared: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isListening by speechManager.isListening.collectAsState()
    val waveformSamples by speechManager.waveformSamples.collectAsState()

    val deck = remember(phrases) {
        phrases.ifEmpty {
            listOf(
                PracticePhrase(
                    id = "fallback",
                    targetText = "Hello",
                    phonetic = "Hello",
                    translation = context.getString(R.string.lang_english),
                    difficulty = "Core",
                    accentFocus = context.getString(R.string.coach_default_accent_focus),
                    accentFocusRes = R.string.coach_default_accent_focus
                )
            )
        }
    }
    var currentIndex by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(45) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var clearedCount by remember { mutableIntStateOf(0) }
    var gameFinished by remember { mutableStateOf(false) }
    val initialPrompt = stringResource(R.string.blitz_initial_feedback)
    var lastFeedback by remember { mutableStateOf(initialPrompt) }

    LaunchedEffect(gameFinished) {
        if (!gameFinished) {
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft -= 1
            }
            gameFinished = true
            onGameFinished(score, clearedCount)
        }
    }

    val currentPhrase = deck[currentIndex % deck.size]

    fun submitAttempt(spoken: String) {
        val eval = LocalPronunciationEvaluator.evaluate(
            targetLanguage = language,
            targetPhrase = currentPhrase.targetText,
            spokenTranscript = spoken,
            context = context
        )
        if (eval.score >= 70) {
            combo += 1
            clearedCount += 1
            val mult = combo.coerceAtMost(4)
            val gained = 25 * mult
            score += gained
            lastFeedback = context.getString(R.string.blitz_hit_feedback, eval.score, gained, mult)
            onPraiseSensei()
            currentIndex = (currentIndex + 1) % deck.size
        } else {
            combo = 0
            lastFeedback = context.getString(R.string.blitz_miss_feedback, eval.score)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFBBF24))
                    Text(stringResource(R.string.blitz_title), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_close), tint = Color(0xFFA5B4FC))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!gameFinished) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.blitz_timer_label, secondsLeft), color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(stringResource(R.string.blitz_score_label, score), color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(stringResource(R.string.blitz_combo_label, combo.coerceAtLeast(1)), color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    LinearProgressIndicator(
                        progress = { secondsLeft / 45f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF34D399),
                        trackColor = Color(0xFF312E81)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141233)),
                        border = BorderStroke(1.dp, Color(0xFF4F46E5))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentPhrase.targetText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = currentPhrase.phonetic,
                                fontSize = 12.sp,
                                color = Color(0xFF34D399),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (currentPhrase.translationRes != 0) {
                                    stringResource(currentPhrase.translationRes)
                                } else {
                                    currentPhrase.translation
                                },
                                fontSize = 11.sp,
                                color = Color(0xFFA5B4FC),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    CanvasWaveformVisualizer(samples = waveformSamples, isListening = isListening)

                    Text(
                        text = lastFeedback,
                        fontSize = 11.sp,
                        color = Color(0xFFC7D2FE),
                        textAlign = TextAlign.Center
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { speechManager.speak(currentPhrase.targetText, language) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = stringResource(R.string.cd_hear_phrase), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.blitz_btn_hear), fontSize = 12.sp)
                        }

                        FloatingActionButton(
                            onClick = {
                                if (!hasMicPermission) {
                                    onRequestMicPermission()
                                } else if (isListening) {
                                    speechManager.stopListening()
                                } else {
                                    speechManager.startListening(language) { spoken ->
                                        submitAttempt(spoken)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("blitz_mic_button"),
                            shape = CircleShape,
                            containerColor = if (isListening) Color(0xFFEF4444) else Color(0xFF4F46E5)
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = stringResource(R.string.cd_blitz_mic),
                                tint = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                combo = 0
                                currentIndex = (currentIndex + 1) % deck.size
                                lastFeedback = context.getString(R.string.blitz_skip_feedback)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.blitz_btn_skip), fontSize = 12.sp, color = Color(0xFFA5B4FC))
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = stringResource(R.string.blitz_times_up, score),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.blitz_summary, clearedCount),
                        fontSize = 12.sp,
                        color = Color(0xFFC7D2FE),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            if (gameFinished) {
                Button(
                    onClick = {
                        currentIndex = 0
                        secondsLeft = 45
                        score = 0
                        combo = 0
                        clearedCount = 0
                        gameFinished = false
                        lastFeedback = initialPrompt
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text(stringResource(R.string.blitz_play_again), color = Color.White)
                }
            } else {
                TextButton(
                    onClick = {
                        gameFinished = true
                        onGameFinished(score, clearedCount)
                    }
                ) {
                    Text(stringResource(R.string.blitz_finish_early), color = Color(0xFFA5B4FC))
                }
            }
        },
        containerColor = Color(0xFF1E1B4B)
    )
}
