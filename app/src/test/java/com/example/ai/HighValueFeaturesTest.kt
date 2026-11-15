package com.example.ai

import android.content.Context
import android.content.res.Configuration
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.audio.AudioPitchAnalyzer
import com.example.audio.SpeechManager
import com.example.data.local.CachedEvaluationDao
import com.example.data.local.CachedEvaluationEntity
import com.example.data.local.SenseiLocalDatabase
import com.example.ui.screens.OnboardingPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HighValueFeaturesTest {

    private lateinit var context: Context
    private lateinit var database: SenseiLocalDatabase
    private lateinit var cacheDao: CachedEvaluationDao
    private lateinit var settingsManager: AiSettingsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("pocket_sensei_ai_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("pocket_sensei_onboarding_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("pocket_sensei_usage_quota", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        database = Room.inMemoryDatabaseBuilder(context, SenseiLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cacheDao = database.cachedEvaluationDao()
        settingsManager = AiSettingsManager(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun audioPitchAnalyzer_detects200HzSineWaveAndRejectsSilence() {
        val sampleRate = 16_000
        val count = 1600 // 100 ms
        val toneSamples = ShortArray(count) { i ->
            val angle = 2.0 * PI * 200.0 * i / sampleRate
            (sin(angle) * 8000.0).toInt().toShort()
        }
        val detected = AudioPitchAnalyzer.estimatePitchHz(toneSamples, sampleRate)
        assertNotNull(detected)
        assertTrue("Expected pitch near 200 Hz, got $detected", detected!! in 190f..210f)

        val silence = ShortArray(count) { 0 }
        assertNull(AudioPitchAnalyzer.estimatePitchHz(silence, sampleRate))
    }

    @Test
    fun localPronunciationEvaluator_scoresExactMatchAndGeneratesRoleplayTurn() {
        val exact = LocalPronunciationEvaluator.evaluate(
            targetLanguage = "Spanish",
            targetPhrase = "La cuenta, por favor.",
            spokenTranscript = "La cuenta por favor"
        )
        assertTrue("Expected score >= 95 for exact normalized match, got ${exact.score}", exact.score >= 95)

        val partial = LocalPronunciationEvaluator.evaluate(
            targetLanguage = "Spanish",
            targetPhrase = "Una mesa para dos, por favor.",
            spokenTranscript = "Una mesa"
        )
        assertTrue("Expected lower score for partial match, got ${partial.score}", partial.score < exact.score)
        assertTrue("Expected missed words to be detected", partial.mispronouncedParts.isNotEmpty())

        val roleplay = LocalPronunciationEvaluator.conductLocalRoleplayTurn(
            language = "Japanese",
            scenarioTitle = "Tokyo Café Order",
            userSpokenMessage = "Matcha latte o kudasai"
        )
        assertTrue(roleplay.senseiReply.isNotBlank())
        assertTrue(roleplay.suggestedUserReplies.isNotEmpty())
    }

    @Test
    fun aiSenseiCoach_hitsRoomCacheAtZeroCostBeforeCloud() = runBlocking {
        val key = AiSenseiCoach.buildCacheKey("Japanese", "こんにちは", "こんにちは")
        cacheDao.insertOrReplace(
            CachedEvaluationEntity(
                cacheKey = key,
                language = "Japanese",
                targetPhrase = "こんにちは",
                spokenTranscript = "こんにちは",
                score = 96,
                phoneticAccuracy = 96,
                intonationPitch = 94,
                fluency = 95,
                phoneticBreakdown = "Kon-ni-chi-wa",
                mispronouncedJson = "[]",
                coachingTip = "Cached crisp vowels",
                nativePhrasingsJson = "[\"こんにちは\"]",
                encouragement = "Cached praise!",
                pitchContourType = "Flat",
                targetPitchJson = "[50,70,70,65]",
                learnerPitchJson = "[50,68,70,64]",
                pitchGuidance = "Even pitch"
            )
        )
        assertEquals(1, cacheDao.observeCacheCount().first())

        val coach = AiSenseiCoach(cacheDao = cacheDao, aiSettingsManager = settingsManager)
        val result = coach.evaluatePronunciation("Japanese", "こんにちは", "こんにちは").getOrThrow()

        assertEquals(96, result.score)
        assertEquals("Room Cache ($0)", result.engineSource)
        assertEquals("Cached praise!", result.encouragement)
    }

    @Test
    fun aiSenseiCoach_respectsLocalOfflineOnlyMode() = runBlocking {
        settingsManager.setMode(AiCoachingMode.LOCAL_OFFLINE_ONLY)
        val coach = AiSenseiCoach(cacheDao = cacheDao, aiSettingsManager = settingsManager)

        val result = coach.evaluatePronunciation(
            targetLanguage = "French",
            promptOrWord = "Bonjour",
            spokenTranscript = "Bonjour"
        ).getOrThrow()

        assertEquals("Offline Coach ($0)", result.engineSource)
        assertTrue(result.score >= 95)

        val turn = coach.conductRoleplayTurn(
            language = "French",
            scenarioTitle = "Parisian Boulangerie",
            conversationHistory = emptyList(),
            userSpokenMessage = "Un croissant s'il vous plaît"
        ).getOrThrow()

        assertTrue(turn.accentAndGrammarNote.contains("Offline Coach ($0 mode)"))
    }

    @Test
    fun aiSettingsManager_tracksQuotaAndByokLifecycle() {
        assertTrue(settingsManager.canUseFreeCloudQuota())
        repeat(AiSettingsManager.DEFAULT_DAILY_FREE_CAP) {
            settingsManager.incrementDailyCloudUsage()
        }
        assertFalse(settingsManager.canUseFreeCloudQuota())
        assertEquals(AiSettingsManager.DEFAULT_DAILY_FREE_CAP, settingsManager.settings.value.dailyCloudCallsUsed)

        settingsManager.saveUserApiKey("  test-user-gemini-key-123  ")
        assertEquals(AiCoachingMode.BYOK_USER_KEY, settingsManager.getCurrentMode())
        assertEquals("test-user-gemini-key-123", settingsManager.getUserApiKey())
        assertTrue(settingsManager.settings.value.hasByokKey)

        settingsManager.saveUserApiKey("")
        assertEquals(AiCoachingMode.HYBRID_FREE, settingsManager.getCurrentMode())
        assertFalse(settingsManager.settings.value.hasByokKey)

        // Claim +3 Grace Credits unlocks cloud quota even after reaching base cap
        assertTrue(settingsManager.claimGraceCredits())
        assertTrue(settingsManager.canUseFreeCloudQuota())

        // Signed-in users receive higher allowance (25 vs guest 10)
        settingsManager.usageQuotaManager.updateAuthStatus(isSignedIn = true, isPro = false)
        settingsManager.refreshSettings()
        assertEquals(UsageQuotaManager.SIGNED_IN_CAP_EVALUATIONS + 3, settingsManager.settings.value.dailyFreeCloudCap)
        assertTrue(settingsManager.canUseFreeCloudQuota())
    }

    @Test
    fun localPronunciationEvaluator_resolvesLocalizedStringsAndSupportsGermanItalianRoleplay() {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.JAPANESE)
        val jaContext = context.createConfigurationContext(config)

        val localizedEval = LocalPronunciationEvaluator.evaluate(
            targetLanguage = "Japanese",
            targetPhrase = "こんにちは",
            spokenTranscript = "こんにちは",
            context = jaContext
        )
        assertEquals(jaContext.getString(R.string.eval_tip_clean), localizedEval.coachingTip)
        assertEquals(jaContext.getString(R.string.eval_encourage_high), localizedEval.encouragement)

        val deTurn = LocalPronunciationEvaluator.conductLocalRoleplayTurn(
            language = "German",
            scenarioTitle = "Berlin Tech Meetup",
            userSpokenMessage = "Ich arbeite an einer KI-App",
            context = jaContext
        )
        assertTrue(deTurn.senseiReply.contains("Ausgezeichnet"))
        assertEquals(jaContext.getString(R.string.roleplay_offline_reply_de_translation), deTurn.englishTranslation)
        assertTrue(deTurn.suggestedUserReplies.isNotEmpty())

        val itTurn = LocalPronunciationEvaluator.conductLocalRoleplayTurn(
            language = "Italian",
            scenarioTitle = "Florence Gelateria",
            userSpokenMessage = "Un cono al pistacchio per favore",
            context = jaContext
        )
        assertTrue(itTurn.senseiReply.contains("Perfetto"))
        assertEquals(jaContext.getString(R.string.roleplay_offline_reply_it_translation), itTurn.englishTranslation)
        assertTrue(itTurn.suggestedUserReplies.isNotEmpty())
    }

    @Test
    fun onboardingPreferences_persistsCompletionAndPreferredLanguage() {
        assertFalse(OnboardingPreferences.hasCompletedOnboarding(context))
        assertNull(OnboardingPreferences.getPreferredLanguage(context))

        OnboardingPreferences.setOnboardingCompleted(
            context = context,
            completed = true,
            preferredLanguage = "Spanish"
        )

        assertTrue(OnboardingPreferences.hasCompletedOnboarding(context))
        assertEquals("Spanish", OnboardingPreferences.getPreferredLanguage(context))
    }

    @Test
    fun speechManager_lifecycleMethodsAreSafeWithoutHardwareRecognizer() {
        val manager = SpeechManager(
            context = context,
            onSpeakStart = { _, _ -> },
            onSpeakEnd = {}
        )
        manager.setLanguage("Spanish")
        manager.stopListening()
        assertFalse(manager.isListening.value)
        manager.destroy()
        manager.stopListening()
    }

    @Test
    fun aiSenseiCoach_byokModeDoesNotLeakIntoHybridQuotaAndTopicPackRespectsOfflineMode() = runBlocking {
        settingsManager.setMode(AiCoachingMode.LOCAL_OFFLINE_ONLY)
        val coach = AiSenseiCoach(
            cacheDao = cacheDao,
            aiSettingsManager = settingsManager,
            appContext = context
        )

        val offlinePack = coach.generateByokTopicPack("Japanese", "Ramen Shop").getOrThrow()
        assertFalse(offlinePack.usedByokCloud)
        assertEquals(3, offlinePack.phrases.size)
        assertEquals(0, settingsManager.usageQuotaManager.quotaState.value.topicPacksUsed)

        // Switch to BYOK mode without an active key (simulating cleared/unusable BYOK path)
        settingsManager.setMode(AiCoachingMode.BYOK_USER_KEY)
        val evalFallback = coach.evaluatePronunciation(
            targetLanguage = "Spanish",
            promptOrWord = "Hola",
            spokenTranscript = "Hola"
        ).getOrThrow()
        assertEquals("Local Sensei ($0 • Streak Preserved)", evalFallback.engineSource)

        val roleplayFallback = coach.conductRoleplayTurn(
            language = "Spanish",
            scenarioTitle = "Madrid Tapas Bar",
            conversationHistory = emptyList(),
            userSpokenMessage = "Una mesa por favor"
        ).getOrThrow()
        assertTrue(roleplayFallback.senseiReply.isNotBlank())

        val packFallback = coach.generateByokTopicPack("Spanish", "Airport").getOrThrow()
        assertFalse(packFallback.usedByokCloud)
        assertEquals(3, packFallback.phrases.size)

        // Ensure developer Hybrid Free quota counters were never deducted by BYOK fallback
        val quota = settingsManager.usageQuotaManager.quotaState.value
        assertEquals(0, quota.evaluationsUsed)
        assertEquals(0, quota.chatTurnsUsed)
        assertEquals(0, quota.topicPacksUsed)
        assertTrue(quota.fallbackCascadeCount >= 3)
    }
}
