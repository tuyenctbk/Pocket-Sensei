package com.example.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.screens.OnboardingScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `onboarding advances across 3 slides lets user pick language and completes`() {
        var completedLanguage: String? = null

        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen(
                    initialLanguage = "Japanese",
                    onComplete = { completedLanguage = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Step 1 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_back_button").assertDoesNotExist()

        // Advance to slide 2
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Step 2 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_back_button").assertIsDisplayed()

        // Advance to slide 3 and pick Spanish
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Step 3 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_language_picker_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_lang_spanish")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        // Complete onboarding
        composeRule.onNodeWithTag("onboarding_get_started_button").performClick()
        composeRule.waitForIdle()
        assertEquals("Spanish", completedLanguage)
    }

    @Test
    fun `onboarding skip invokes completion with current initial language`() {
        var skippedWithLanguage: String? = null

        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen(
                    initialLanguage = "French",
                    onComplete = { skippedWithLanguage = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("onboarding_skip_button").performClick()
        composeRule.waitForIdle()

        assertEquals("French", skippedWithLanguage)
    }
}
