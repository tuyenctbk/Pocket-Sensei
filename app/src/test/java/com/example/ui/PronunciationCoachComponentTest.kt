package com.example.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.screens.DailyDojoGoalCard
import com.example.ui.screens.DailyStreakDialog
import com.example.ui.screens.HeaderStreakBadge
import com.example.ui.screens.PronunciationCoachCard
import com.example.ui.screens.analyzeSpeechPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PronunciationCoachComponentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `analyzeSpeechPattern computes peak average and active ratio accurately`() {
        val empty = analyzeSpeechPattern(emptyList())
        assertEquals(0f, empty.peakAmplitude, 0.001f)

        val pattern = analyzeSpeechPattern(listOf(0.1f, 0.2f, 0.8f, 0.6f))
        assertEquals(0.8f, pattern.peakAmplitude, 0.001f)
        assertEquals(0.425f, pattern.averageAmplitude, 0.001f)
        assertEquals(0.5f, pattern.activeSpeechRatio, 0.001f)
    }

    @Test
    fun `pronunciation coach component displays live canvas waveform transcript and capture triggers`() {
        var micClicks = 0
        var manualClicks = 0
        var recordSampleClicks = 0
        var playMineClicks = 0

        composeRule.setContent {
            MaterialTheme {
                PronunciationCoachCard(
                    waveformSamples = List(32) { idx -> if (idx % 4 == 0) 0.78f else 0.16f },
                    isListening = true,
                    spokenText = "Ohayou gozaimasu",
                    audioRmsDb = 8.4f,
                    onMicToggle = { micClicks++ },
                    onOpenManualInput = { manualClicks++ },
                    hasRecordedAudio = true,
                    onRecordMineToggle = { recordSampleClicks++ },
                    onPlayMineToggle = { playMineClicks++ }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pronunciation_coach_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("canvas_waveform_visualizer").assertIsDisplayed()
        composeRule.onNodeWithTag("coach_speech_pattern_metrics").assertIsDisplayed()
        composeRule.onNodeWithTag("coach_pattern_peak_metric").assertIsDisplayed()
        composeRule.onNodeWithTag("coach_pattern_cadence_metric").assertIsDisplayed()
        composeRule.onNodeWithTag("coach_waveform_status").assertIsDisplayed()
        composeRule.onNodeWithText("Level 8.4 dB").assertIsDisplayed()
        composeRule.onNodeWithTag("coach_live_transcript").assertIsDisplayed()

        composeRule.onNodeWithTag("microphone_button").performClick()
        composeRule.onNodeWithTag("manual_input_button").performClick()
        composeRule.onNodeWithTag("record_mine_btn").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("play_mine_btn").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertTrue(micClicks == 1)
        assertTrue(manualClicks == 1)
        assertTrue(recordSampleClicks == 1)
        assertTrue(playMineClicks == 1)
    }

    @Test
    fun `header streak badge and coach card render streak status and open streak dialog`() {
        var headerClicked = false
        var practiceNowClicked = false

        composeRule.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    HeaderStreakBadge(
                        streakCount = 6,
                        practicedToday = false,
                        freezesAvailable = 2,
                        freezeSavedStreak = true,
                        onClick = { headerClicked = true }
                    )
                    DailyDojoGoalCard(
                        doneCount = 1,
                        targetCount = 3,
                        streakCount = 6,
                        practicedToday = true,
                        justUnlockedStreak = true
                    )
                    DailyStreakDialog(
                        streakCount = 6,
                        bestStreak = 12,
                        practicedToday = false,
                        freezesAvailable = 2,
                        freezeSavedStreak = true,
                        onShareStreak = {},
                        onPracticeNow = { practiceNowClicked = true },
                        onDismiss = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("header_streak_badge").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("header_streak_fire_icon", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("header_streak_freeze_icon", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("coach_streak_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("coach_streak_unlocked_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_streak_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_freeze_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_freeze_saved_text").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_share_card_btn").assertIsDisplayed()
        composeRule.onNodeWithText("Best streak: 12 days").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_dialog_primary_btn").performClick()
        composeRule.waitForIdle()

        assertTrue(headerClicked)
        assertTrue(practiceNowClicked)
    }

    @Test
    fun `byok quick setup dialog and custom topic pack generator work at zero developer cost`() {
        val coach = com.example.ai.AiSenseiCoach()
        val pack = coach.buildLocalTopicPackFallback("Japanese", "Fukuoka Ramen")
        assertEquals(3, pack.phrases.size)
        assertTrue(pack.scenarioTitle.contains("Fukuoka Ramen"))

        var savedKey = ""
        var generatedTopic = ""

        composeRule.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    com.example.ui.screens.ByokQuickSetupDialog(
                        onSaveByokKey = { savedKey = it },
                        onDismiss = {}
                    )
                    com.example.ui.screens.CustomPhraseDialog(
                        language = "Japanese",
                        isGeneratingTopicPack = false,
                        onGenerateTopicPack = { generatedTopic = it },
                        onSave = { _, _, _, _ -> },
                        onDismiss = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("byok_quick_setup_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("byok_dialog_open_url_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("byok_dialog_key_input").assertIsDisplayed()
        composeRule.onNodeWithTag("generate_topic_pack_btn").assertIsDisplayed()
    }
}
