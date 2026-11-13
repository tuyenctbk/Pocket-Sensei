package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.SpeechManager
import com.example.data.local.SpacedRepetitionScheduler
import com.example.data.local.VocabularyWord
import com.example.ui.MainViewModel

enum class MasteryFilter {
    ALL,
    DUE_TODAY,
    NEEDS_REVIEW,
    LEARNING,
    MASTERED
}

enum class MasteryStatus {
    NEEDS_REVIEW,
    LEARNING,
    MASTERED
}

data class MasteryCounts(
    val total: Int,
    val dueToday: Int,
    val needsReview: Int,
    val learning: Int,
    val mastered: Int
)

fun masteryStatusFor(masteryLevel: Int): MasteryStatus = when {
    masteryLevel >= 4 -> MasteryStatus.MASTERED
    masteryLevel == 3 -> MasteryStatus.LEARNING
    else -> MasteryStatus.NEEDS_REVIEW
}

fun computeMasteryCounts(
    words: List<VocabularyWord>,
    nowMs: Long = System.currentTimeMillis()
): MasteryCounts {
    var dueToday = 0
    var needsReview = 0
    var learning = 0
    var mastered = 0
    for (word in words) {
        if (SpacedRepetitionScheduler.isDueForReview(word, nowMs)) {
            dueToday++
        }
        when (masteryStatusFor(word.masteryLevel)) {
            MasteryStatus.NEEDS_REVIEW -> needsReview++
            MasteryStatus.LEARNING -> learning++
            MasteryStatus.MASTERED -> mastered++
        }
    }
    return MasteryCounts(
        total = words.size,
        dueToday = dueToday,
        needsReview = needsReview,
        learning = learning,
        mastered = mastered
    )
}

fun filterVocabularyWords(
    words: List<VocabularyWord>,
    languageFilter: String,
    masteryFilter: MasteryFilter,
    nowMs: Long = System.currentTimeMillis()
): List<VocabularyWord> {
    val languageMatched = if (languageFilter == "All") {
        words
    } else {
        words.filter { it.language.equals(languageFilter, ignoreCase = true) }
    }

    val masteryMatched = when (masteryFilter) {
        MasteryFilter.ALL -> languageMatched
        MasteryFilter.DUE_TODAY -> languageMatched.filter { SpacedRepetitionScheduler.isDueForReview(it, nowMs) }
        MasteryFilter.NEEDS_REVIEW -> languageMatched.filter { masteryStatusFor(it.masteryLevel) == MasteryStatus.NEEDS_REVIEW }
        MasteryFilter.LEARNING -> languageMatched.filter { masteryStatusFor(it.masteryLevel) == MasteryStatus.LEARNING }
        MasteryFilter.MASTERED -> languageMatched.filter { masteryStatusFor(it.masteryLevel) == MasteryStatus.MASTERED }
    }

    return when (masteryFilter) {
        MasteryFilter.DUE_TODAY -> masteryMatched.sortedWith(
            compareBy<VocabularyWord> { it.nextReviewAt }
                .thenBy { it.masteryLevel }
                .thenBy { it.pronunciationScore }
        )
        MasteryFilter.NEEDS_REVIEW -> masteryMatched.sortedWith(
            compareBy<VocabularyWord> { it.masteryLevel }
                .thenBy { it.pronunciationScore }
                .thenByDescending { it.lastPracticedAt }
        )
        else -> masteryMatched.sortedByDescending { it.lastPracticedAt }
    }
}

fun weakestWordForReview(words: List<VocabularyWord>): VocabularyWord? {
    return words.minWithOrNull(
        compareBy<VocabularyWord> { it.masteryLevel }
            .thenBy { it.pronunciationScore }
            .thenBy { it.lastPracticedAt }
    )
}

@Composable
fun VocabMasteryScreen(
    viewModel: MainViewModel,
    speechManager: SpeechManager
) {
    // Keep Firestore -> Room listener active while displaying Room as offline-first source of truth.
    viewModel.vocabListState.collectAsState()
    val localWords by viewModel.localVocabularyState.collectAsState()

    VocabMasteryContent(
        words = localWords,
        onPlayWord = { word, slow ->
            speechManager.speak(word.word, word.language, slow = slow)
        },
        onPracticeInCoach = { word ->
            viewModel.practiceLocalWord(word)
        },
        onReviewMastery = { word, remembered ->
            viewModel.reviewLocalWordMastery(word, remembered)
        },
        onTrackFlashcardOpened = { word ->
            viewModel.trackEvent(
                eventName = "flashcard_drilled",
                category = "vocab",
                metadata = mapOf(
                    "word" to word.word,
                    "language" to word.language,
                    "mastery_level" to word.masteryLevel
                )
            )
        }
    )
}

@Composable
fun VocabMasteryContent(
    words: List<VocabularyWord>,
    onPlayWord: (VocabularyWord, slow: Boolean) -> Unit,
    onPracticeInCoach: (VocabularyWord) -> Unit,
    onReviewMastery: (VocabularyWord, remembered: Boolean) -> Unit,
    onTrackFlashcardOpened: (VocabularyWord) -> Unit
) {
    var filterLanguage by remember { mutableStateOf("All") }
    var filterMastery by remember { mutableStateOf(MasteryFilter.ALL) }
    var selectedDrillWord by remember { mutableStateOf<VocabularyWord?>(null) }

    val languageWords = remember(words, filterLanguage) {
        if (filterLanguage == "All") {
            words
        } else {
            words.filter { it.language.equals(filterLanguage, ignoreCase = true) }
        }
    }
    val masteryCounts = remember(languageWords) { computeMasteryCounts(languageWords) }
    val filteredWords = remember(words, filterLanguage, filterMastery) {
        filterVocabularyWords(words, filterLanguage, filterMastery)
    }
    val weakestWord = remember(languageWords) { weakestWordForReview(languageWords) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(16.dp)
        ) {
            MasterySummaryHeaderCard(
                counts = masteryCounts,
                hasWeakestWord = weakestWord != null,
                onDrillWeakest = {
                    weakestWord?.let { onPracticeInCoach(it) }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Mastery Level & Spaced-Repetition Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    MasteryFilterChip(
                        selected = filterMastery == MasteryFilter.ALL,
                        label = stringResource(R.string.vocab_filter_mastery_all),
                        dotColor = MaterialTheme.colorScheme.primary,
                        testTag = "mastery_filter_all",
                        onClick = { filterMastery = MasteryFilter.ALL }
                    )
                }
                item {
                    MasteryFilterChip(
                        selected = filterMastery == MasteryFilter.DUE_TODAY,
                        label = stringResource(R.string.vocab_filter_due_today, masteryCounts.dueToday),
                        dotColor = Color(0xFFF43F5E),
                        testTag = "mastery_filter_due_today",
                        onClick = { filterMastery = MasteryFilter.DUE_TODAY }
                    )
                }
                item {
                    MasteryFilterChip(
                        selected = filterMastery == MasteryFilter.NEEDS_REVIEW,
                        label = stringResource(R.string.vocab_filter_needs_review, masteryCounts.needsReview),
                        dotColor = Color(0xFFF59E0B),
                        testTag = "mastery_filter_needs_review",
                        onClick = { filterMastery = MasteryFilter.NEEDS_REVIEW }
                    )
                }
                item {
                    MasteryFilterChip(
                        selected = filterMastery == MasteryFilter.LEARNING,
                        label = stringResource(R.string.vocab_filter_learning, masteryCounts.learning),
                        dotColor = Color(0xFF38BDF8),
                        testTag = "mastery_filter_learning",
                        onClick = { filterMastery = MasteryFilter.LEARNING }
                    )
                }
                item {
                    MasteryFilterChip(
                        selected = filterMastery == MasteryFilter.MASTERED,
                        label = stringResource(R.string.vocab_filter_mastered, masteryCounts.mastered),
                        dotColor = Color(0xFF34D399),
                        testTag = "mastery_filter_mastered",
                        onClick = { filterMastery = MasteryFilter.MASTERED }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Language Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val filters = listOf(
                    "All",
                    "Japanese",
                    "French",
                    "Spanish",
                    "German",
                    "Korean",
                    "Mandarin",
                    "Italian",
                    "English"
                )
                items(filters) { lang ->
                    FilterChip(
                        selected = filterLanguage == lang,
                        onClick = { filterLanguage = lang },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("vocab_lang_filter_${lang.lowercase()}"),
                        label = { Text(localizedLanguageName(lang), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E1B4B),
                            labelColor = Color(0xFFC7D2FE)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredWords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.vocab_empty_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(
                                if (words.isEmpty()) R.string.vocab_empty_subtitle
                                else R.string.vocab_empty_filtered_subtitle
                            ),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("vocab_mastery_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(filteredWords, key = { "${it.id}_${it.word}_${it.language}" }) { word ->
                        VocabMasteryCard(
                            word = word,
                            onPlay = { slow -> onPlayWord(word, slow) },
                            onDrill = {
                                selectedDrillWord = word
                                onTrackFlashcardOpened(word)
                            }
                        )
                    }
                }
            }
        }
    }

    selectedDrillWord?.let { wordItem ->
        VocabularyWordFlashcardDialog(
            word = wordItem,
            onDismiss = { selectedDrillWord = null },
            onPlay = { slow -> onPlayWord(wordItem, slow) },
            onNeedsPractice = {
                selectedDrillWord = null
                onReviewMastery(wordItem, false)
            },
            onGotIt = {
                selectedDrillWord = null
                onReviewMastery(wordItem, true)
            },
            onOpenInCoach = {
                selectedDrillWord = null
                onPracticeInCoach(wordItem)
            }
        )
    }
}

@Composable
private fun MasterySummaryHeaderCard(
    counts: MasteryCounts,
    hasWeakestWord: Boolean,
    onDrillWeakest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vocab_mastery_summary_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, Color(0xFF312E81))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF272262))
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.vocab_bank_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(
                            R.string.vocab_mastery_summary_format,
                            counts.total,
                            counts.mastered
                        ),
                        fontSize = 12.sp,
                        color = Color(0xFFA5B4FC),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (hasWeakestWord) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDrillWeakest,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("vocab_drill_weakest_btn"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.vocab_btn_drill_weakest),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (counts.total > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                val masteredRatio by animateFloatAsState(
                    targetValue = counts.mastered.toFloat() / counts.total.toFloat(),
                    animationSpec = tween(450, easing = FastOutSlowInEasing),
                    label = "masteredRatio"
                )
                val learningRatio by animateFloatAsState(
                    targetValue = counts.learning.toFloat() / counts.total.toFloat(),
                    animationSpec = tween(450, easing = FastOutSlowInEasing),
                    label = "learningRatio"
                )
                val reviewRatio by animateFloatAsState(
                    targetValue = counts.needsReview.toFloat() / counts.total.toFloat(),
                    animationSpec = tween(450, easing = FastOutSlowInEasing),
                    label = "reviewRatio"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF0F0E26))
                ) {
                    if (masteredRatio > 0.001f) {
                        Box(
                            modifier = Modifier
                                .weight(masteredRatio.coerceAtLeast(0.001f))
                                .fillMaxHeight()
                                .background(Color(0xFF34D399))
                        )
                    }
                    if (learningRatio > 0.001f) {
                        Box(
                            modifier = Modifier
                                .weight(learningRatio.coerceAtLeast(0.001f))
                                .fillMaxHeight()
                                .background(Color(0xFF38BDF8))
                        )
                    }
                    if (reviewRatio > 0.001f) {
                        Box(
                            modifier = Modifier
                                .weight(reviewRatio.coerceAtLeast(0.001f))
                                .fillMaxHeight()
                                .background(Color(0xFFF59E0B))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MasteryFilterChip(
    selected: Boolean,
    label: String,
    dotColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else dotColor)
            )
        },
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = dotColor.copy(alpha = 0.25f),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF1E1B4B),
            labelColor = Color(0xFFC7D2FE)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color(0xFF312E81),
            selectedBorderColor = dotColor
        )
    )
}

@Composable
private fun VocabMasteryCard(
    word: VocabularyWord,
    onPlay: (slow: Boolean) -> Unit,
    onDrill: () -> Unit
) {
    val status = masteryStatusFor(word.masteryLevel)
    val statusVisuals = statusVisualsFor(status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vocab_card_${word.id}")
            .clickable(onClick = onDrill),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, statusVisuals.accentColor.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Semantic status edge indicator
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(statusVisuals.accentColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusBadgePill(visuals = statusVisuals)

                        if (SpacedRepetitionScheduler.isDueForReview(word)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF881337).copy(alpha = 0.7f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.vocab_due_now_badge),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDA4AF)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF064E3B).copy(alpha = 0.55f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.vocab_next_interval_format, word.intervalDays),
                                    fontSize = 9.sp,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF312E81))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${getFlagForLanguage(word.language)} ${localizedLanguageName(word.language)}",
                                fontSize = 9.sp,
                                color = Color(0xFFA5B4FC)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = word.word,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (word.phonetics.isNotBlank()) {
                        Text(
                            text = word.phonetics,
                            fontSize = 12.sp,
                            color = Color(0xFFA5B4FC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = word.translation,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mastery stars, level, accuracy, and practice count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for (i in 1..5) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (i <= word.masteryLevel) Color(0xFFFBBF24) else Color(0xFF475569),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.vocab_level_format, word.masteryLevel.coerceIn(1, 5)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = stringResource(R.string.vocab_accuracy_format, word.pronunciationScore),
                            fontSize = 10.sp,
                            color = statusVisuals.accentColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.vocab_practice_count_format, word.practiceCount),
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onPlay(false) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.cd_play_normal),
                            tint = Color(0xFF818CF8)
                        )
                    }

                    Button(
                        onClick = onDrill,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("vocab_drill_btn_${word.id}"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text(stringResource(R.string.vocab_btn_drill), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private data class MasteryStatusVisuals(
    val label: String,
    val accentColor: Color,
    val containerColor: Color,
    val icon: ImageVector
)

@Composable
private fun statusVisualsFor(status: MasteryStatus): MasteryStatusVisuals {
    return when (status) {
        MasteryStatus.MASTERED -> MasteryStatusVisuals(
            label = stringResource(R.string.vocab_status_mastered),
            accentColor = Color(0xFF34D399),
            containerColor = Color(0xFF064E3B).copy(alpha = 0.7f),
            icon = Icons.Default.Verified
        )
        MasteryStatus.LEARNING -> MasteryStatusVisuals(
            label = stringResource(R.string.vocab_status_learning),
            accentColor = Color(0xFF38BDF8),
            containerColor = Color(0xFF0C4A6E).copy(alpha = 0.7f),
            icon = Icons.AutoMirrored.Filled.TrendingUp
        )
        MasteryStatus.NEEDS_REVIEW -> MasteryStatusVisuals(
            label = stringResource(R.string.vocab_status_needs_review),
            accentColor = Color(0xFFF59E0B),
            containerColor = Color(0xFF78350F).copy(alpha = 0.65f),
            icon = Icons.Default.Refresh
        )
    }
}

@Composable
private fun StatusBadgePill(visuals: MasteryStatusVisuals) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(visuals.containerColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = visuals.icon,
            contentDescription = visuals.label,
            tint = visuals.accentColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = visuals.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = visuals.accentColor
        )
    }
}

@Composable
private fun VocabularyWordFlashcardDialog(
    word: VocabularyWord,
    onDismiss: () -> Unit,
    onPlay: (slow: Boolean) -> Unit,
    onNeedsPractice: () -> Unit,
    onGotIt: () -> Unit,
    onOpenInCoach: () -> Unit
) {
    var isFlipped by remember(word.id, word.word) { mutableStateOf(false) }
    val visuals = statusVisualsFor(masteryStatusFor(word.masteryLevel))
    val nextRememberedSchedule = remember(word.masteryLevel, word.intervalDays) {
        SpacedRepetitionScheduler.computeNextSchedule(
            currentMastery = word.masteryLevel,
            currentIntervalDays = word.intervalDays,
            remembered = true
        )
    }
    val flipScale by animateFloatAsState(
        targetValue = if (isFlipped) 1.02f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "flashcardFlipScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.vocab_flashcard_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                StatusBadgePill(visuals = visuals)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = flipScale
                            scaleY = flipScale
                        }
                        .clickable { isFlipped = !isFlipped },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) Color(0xFF1A1748) else Color(0xFF141233)
                    ),
                    border = BorderStroke(
                        width = if (isFlipped) 1.5.dp else 1.dp,
                        color = if (isFlipped) Color(0xFFFBBF24) else Color(0xFF4F46E5)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${getFlagForLanguage(word.language)} ${localizedLanguageName(word.language)}",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B4FC)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = word.word,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        if (word.phonetics.isNotBlank()) {
                            Text(
                                text = word.phonetics,
                                fontSize = 14.sp,
                                color = Color(0xFFA5B4FC),
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        AnimatedVisibility(visible = isFlipped) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.vocab_meaning_label),
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = word.translation,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCD34D),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        if (!isFlipped) {
                            Text(
                                text = stringResource(R.string.vocab_tap_to_reveal),
                                fontSize = 11.sp,
                                color = Color(0xFF818CF8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onPlay(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.vocab_btn_listen), fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onPlay(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.SlowMotionVideo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.vocab_btn_slow), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.vocab_next_review_hint_format, nextRememberedSchedule.nextIntervalDays),
                    fontSize = 10.sp,
                    color = Color(0xFF6EE7B7)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onNeedsPractice,
                    modifier = Modifier.testTag("flashcard_repeat_btn"),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24))
                ) {
                    Text(stringResource(R.string.vocab_btn_repeat), fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onGotIt,
                    modifier = Modifier.testTag("flashcard_got_it_btn"),
                    border = BorderStroke(1.dp, Color(0xFF34D399)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.vocab_btn_got_it), fontSize = 11.sp)
                }

                Button(
                    onClick = onOpenInCoach,
                    modifier = Modifier.testTag("flashcard_coach_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text(stringResource(R.string.vocab_btn_practice_coach), fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        },
        containerColor = Color(0xFF1E1B4B)
    )
}
