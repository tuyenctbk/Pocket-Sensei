package com.example.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.local.VocabularyWord
import com.example.ui.screens.MasteryFilter
import com.example.ui.screens.MasteryStatus
import com.example.ui.screens.VocabMasteryContent
import com.example.ui.screens.computeMasteryCounts
import com.example.ui.screens.filterVocabularyWords
import com.example.ui.screens.masteryStatusFor
import com.example.ui.screens.weakestWordForReview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VocabMasteryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleWords = listOf(
        VocabularyWord(
            id = 1,
            word = "おはようございます",
            translation = "Good morning",
            phonetics = "o-ha-you go-za-i-ma-su",
            masteryLevel = 5,
            language = "Japanese",
            pronunciationScore = 96,
            practiceCount = 7,
            lastPracticedAt = 3000L
        ),
        VocabularyWord(
            id = 2,
            word = "Sumimasen",
            translation = "Excuse me",
            phonetics = "su-mi-ma-sen",
            masteryLevel = 3,
            language = "Japanese",
            pronunciationScore = 79,
            practiceCount = 3,
            lastPracticedAt = 2000L
        ),
        VocabularyWord(
            id = 3,
            word = "Je voudrais un café",
            translation = "I would like a coffee",
            phonetics = "zhuh voo-dray uhn ka-fay",
            masteryLevel = 1,
            language = "French",
            pronunciationScore = 54,
            practiceCount = 1,
            lastPracticedAt = 1000L
        )
    )

    @Test
    fun `mastery helpers compute bucket counts and filter accurately`() {
        assertEquals(MasteryStatus.NEEDS_REVIEW, masteryStatusFor(1))
        assertEquals(MasteryStatus.NEEDS_REVIEW, masteryStatusFor(2))
        assertEquals(MasteryStatus.LEARNING, masteryStatusFor(3))
        assertEquals(MasteryStatus.MASTERED, masteryStatusFor(4))
        assertEquals(MasteryStatus.MASTERED, masteryStatusFor(5))

        val counts = computeMasteryCounts(sampleWords)
        assertEquals(3, counts.total)
        assertEquals(1, counts.needsReview)
        assertEquals(1, counts.learning)
        assertEquals(1, counts.mastered)

        val reviewOnly = filterVocabularyWords(sampleWords, "All", MasteryFilter.NEEDS_REVIEW)
        assertEquals(listOf("Je voudrais un café"), reviewOnly.map { it.word })

        val japaneseMastered = filterVocabularyWords(sampleWords, "Japanese", MasteryFilter.MASTERED)
        assertEquals(listOf("おはようございます"), japaneseMastered.map { it.word })

        assertEquals("Je voudrais un café", weakestWordForReview(sampleWords)?.word)
    }

    @Test
    fun `vocab mastery screen displays status badges filters by level and launches weakest drill`() {
        var practicedWord: VocabularyWord? = null
        var reviewedWord: Pair<VocabularyWord, Boolean>? = null

        composeRule.setContent {
            MaterialTheme {
                VocabMasteryContent(
                    words = sampleWords,
                    onPlayWord = { _, _ -> },
                    onPracticeInCoach = { practicedWord = it },
                    onReviewMastery = { word, remembered -> reviewedWord = word to remembered },
                    onTrackFlashcardOpened = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vocab_mastery_summary_card").assertIsDisplayed()
        composeRule.onNodeWithText("Vocabulary Mastery").assertIsDisplayed()
        composeRule.onNodeWithText("おはようございます").assertIsDisplayed()
        composeRule.onNodeWithTag("vocab_mastery_list").performScrollToNode(hasTestTag("vocab_card_3"))
        composeRule.onNodeWithText("Je voudrais un café").assertIsDisplayed()

        // Filter down to Mastered (Lv 4-5)
        composeRule.onNodeWithTag("mastery_filter_mastered").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("おはようございます").assertIsDisplayed()
        composeRule.onNodeWithText("Je voudrais un café").assertDoesNotExist()

        // Filter down to Learning (Lv 3)
        composeRule.onNodeWithTag("mastery_filter_learning").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sumimasen").assertIsDisplayed()
        composeRule.onNodeWithText("おはようございます").assertDoesNotExist()

        // Filter down to Needs Review (Lv 1-2)
        composeRule.onNodeWithTag("mastery_filter_needs_review").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Je voudrais un café").assertIsDisplayed()
        composeRule.onNodeWithText("おはようございます").assertDoesNotExist()

        // Open flashcard drill and click Got It to promote mastery
        composeRule.onNodeWithTag("vocab_drill_btn_3").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("flashcard_got_it_btn").performClick()
        composeRule.waitForIdle()
        assertEquals("Je voudrais un café", reviewedWord?.first?.word)
        assertEquals(true, reviewedWord?.second)

        // Click Drill Weakest CTA on summary header
        composeRule.onNodeWithTag("vocab_drill_weakest_btn").performClick()
        composeRule.waitForIdle()
        assertEquals("Je voudrais un café", practicedWord?.word)
    }
}
