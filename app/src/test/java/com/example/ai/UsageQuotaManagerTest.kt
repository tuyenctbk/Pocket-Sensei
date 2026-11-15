package com.example.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UsageQuotaManagerTest {

    private lateinit var context: Context
    private lateinit var quotaManager: UsageQuotaManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("pocket_sensei_usage_quota", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        quotaManager = UsageQuotaManager(context)
    }

    @Test
    fun modelAgnosticTiers_resolveProperlyAccordingToQuotaAndEcoMode() {
        // Tier 1: Healthy quota and Eco Mode off
        val tier1 = AiModelConfig.resolveTier(
            usagePercent = 0.5f,
            ecoLiteMode = false,
            isRateLimited = false,
            isOfflineOrZeroQuota = false
        )
        assertEquals(AiModelConfig.AiCapabilityTier.TIER_1_PRIMARY, tier1)

        // Tier 2: 80% threshold reached
        val tier2Threshold = AiModelConfig.resolveTier(
            usagePercent = 0.82f,
            ecoLiteMode = false,
            isRateLimited = false,
            isOfflineOrZeroQuota = false
        )
        assertEquals(AiModelConfig.AiCapabilityTier.TIER_2_ECONOMY_LITE, tier2Threshold)

        // Tier 2: Eco Lite Mode explicitly enabled
        val tier2Eco = AiModelConfig.resolveTier(
            usagePercent = 0.2f,
            ecoLiteMode = true,
            isRateLimited = false,
            isOfflineOrZeroQuota = false
        )
        assertEquals(AiModelConfig.AiCapabilityTier.TIER_2_ECONOMY_LITE, tier2Eco)

        // Tier 2: Rate limited (429)
        val tier2RateLimit = AiModelConfig.resolveTier(
            usagePercent = 0.4f,
            ecoLiteMode = false,
            isRateLimited = true,
            isOfflineOrZeroQuota = false
        )
        assertEquals(AiModelConfig.AiCapabilityTier.TIER_2_ECONOMY_LITE, tier2RateLimit)

        // Tier 3: Zero quota / offline
        val tier3 = AiModelConfig.resolveTier(
            usagePercent = 1.0f,
            ecoLiteMode = false,
            isRateLimited = false,
            isOfflineOrZeroQuota = true
        )
        assertEquals(AiModelConfig.AiCapabilityTier.TIER_3_ZERO_COST_LOCAL, tier3)
    }

    @Test
    fun atomicDeductAndRefund_preservesCreditsOnNetworkError() {
        val initial = quotaManager.getRemainingAllowance(QuotaFeature.PRONUNCIATION_EVALUATION)
        assertTrue(initial > 0)

        // Deduct 1 interaction
        assertTrue(quotaManager.deductInteraction(QuotaFeature.PRONUNCIATION_EVALUATION))
        assertEquals(initial - 1, quotaManager.getRemainingAllowance(QuotaFeature.PRONUNCIATION_EVALUATION))

        // Refund 1 interaction (e.g. timeout or 5xx)
        quotaManager.refundInteraction(QuotaFeature.PRONUNCIATION_EVALUATION)
        assertEquals(initial, quotaManager.getRemainingAllowance(QuotaFeature.PRONUNCIATION_EVALUATION))
    }

    @Test
    fun graceCredits_grantsBonusThreeAndCannotBeClaimedTwiceInSameDay() {
        val initial = quotaManager.getRemainingAllowance(QuotaFeature.PRONUNCIATION_EVALUATION)
        assertFalse(quotaManager.quotaState.value.graceClaimed)

        // Claim grace credits
        val claimed = quotaManager.claimDailyGraceCredits()
        assertTrue(claimed)
        assertTrue(quotaManager.quotaState.value.graceClaimed)
        assertEquals(3, quotaManager.quotaState.value.bonusGraceCredits)
        assertEquals(initial + 3, quotaManager.getRemainingAllowance(QuotaFeature.PRONUNCIATION_EVALUATION))

        // Second claim attempt on same day must be rejected
        val secondClaim = quotaManager.claimDailyGraceCredits()
        assertFalse(secondClaim)
        assertEquals(3, quotaManager.quotaState.value.bonusGraceCredits)

        // Deducting while grace credits are available consumes grace credit, and refund restores it
        assertTrue(quotaManager.deductInteraction(QuotaFeature.PRONUNCIATION_EVALUATION))
        assertEquals(2, quotaManager.quotaState.value.bonusGraceCredits)
        quotaManager.refundInteraction(QuotaFeature.PRONUNCIATION_EVALUATION)
        assertEquals(3, quotaManager.quotaState.value.bonusGraceCredits)
    }

    @Test
    fun midnightRollover_resetsDailyUsageAndGraceWhilePreservingEcoMode() {
        // Toggle Eco Lite mode
        quotaManager.toggleEcoLiteMode(true)
        assertTrue(quotaManager.quotaState.value.ecoLiteMode)

        // Deduct some quota and claim grace
        quotaManager.deductInteraction(QuotaFeature.PRONUNCIATION_EVALUATION)
        quotaManager.claimDailyGraceCredits()
        assertTrue(quotaManager.quotaState.value.graceClaimed)
        assertTrue(quotaManager.quotaState.value.evaluationsUsed > 0)

        // Simulate midnight date change by setting an older date in SharedPreferences
        context.getSharedPreferences("pocket_sensei_usage_quota", Context.MODE_PRIVATE)
            .edit()
            .putString("date_iso", "2026-01-01")
            .commit()

        // Rollover check
        val didRollover = quotaManager.checkAndApplyMidnightRollover()
        assertTrue(didRollover)

        // Usage and grace reset to 0 / false
        assertEquals(0, quotaManager.quotaState.value.evaluationsUsed)
        assertEquals(0, quotaManager.quotaState.value.bonusGraceCredits)
        assertFalse(quotaManager.quotaState.value.graceClaimed)

        // Eco Lite mode is preserved across rollover
        assertTrue(quotaManager.quotaState.value.ecoLiteMode)
    }

    @Test
    fun guestVsSignedIn_unlocksHigherAllowanceForSignedInUsers() {
        // Default is Guest
        assertEquals(UsageQuotaManager.GUEST_CAP_EVALUATIONS, quotaManager.getRemainingAllowance(QuotaFeature.PRONUNCIATION_EVALUATION))

        // Sign in with Google
        quotaManager.updateAuthStatus(isSignedIn = true, isPro = false)
        assertEquals(UsageQuotaManager.SIGNED_IN_CAP_EVALUATIONS, quotaManager.getRemainingAllowance(QuotaFeature.PRONUNCIATION_EVALUATION))

        // Pro user has unlimited
        quotaManager.updateAuthStatus(isSignedIn = true, isPro = true)
        assertTrue(quotaManager.getRemainingAllowance(QuotaFeature.PRONUNCIATION_EVALUATION) >= 500)
    }

    @Test
    fun valueReceipt_accumulatesDailyPracticeStatsAndEstimatedTokens() {
        quotaManager.deductInteraction(QuotaFeature.PRONUNCIATION_EVALUATION)
        quotaManager.deductInteraction(QuotaFeature.PRONUNCIATION_EVALUATION)
        quotaManager.deductInteraction(QuotaFeature.ROLEPLAY_CHAT)

        val receipt = quotaManager.getValueReceipt()
        assertEquals(2, receipt.evaluationsCount)
        assertEquals(1, receipt.chatTurnsCount)
        assertEquals(0, receipt.topicPacksCount)
        assertTrue(receipt.estimatedTokensSaved > 0)
    }

    @Test
    fun softNudgeThreshold_triggersAtEightyPercent() {
        // Guest has 10 cap. 0 to 7 used -> < 80%
        repeat(7) {
            quotaManager.deductInteraction(QuotaFeature.PRONUNCIATION_EVALUATION)
        }
        assertFalse(quotaManager.isApproachingLimit(QuotaFeature.PRONUNCIATION_EVALUATION))

        // 8th used -> 8/10 = 80% -> approaching limit soft nudge banner triggered!
        quotaManager.deductInteraction(QuotaFeature.PRONUNCIATION_EVALUATION)
        assertTrue(quotaManager.isApproachingLimit(QuotaFeature.PRONUNCIATION_EVALUATION))
        assertEquals(2, quotaManager.getRemainingAllowance(QuotaFeature.PRONUNCIATION_EVALUATION))
    }

    @Test
    fun withAtomicQuotaOrFallback_deductsOnSuccessAndRefundsOnFailure() = kotlinx.coroutines.runBlocking {
        val initial = quotaManager.getRemainingAllowance(QuotaFeature.ROLEPLAY_CHAT)

        val successValue = quotaManager.withAtomicQuotaOrFallback(
            feature = QuotaFeature.ROLEPLAY_CHAT,
            onQuotaExhausted = { "exhausted" },
            onErrorFallback = { "error" },
            cloudAction = { "cloud_ok" }
        )
        assertEquals("cloud_ok", successValue)
        assertEquals(initial - 1, quotaManager.getRemainingAllowance(QuotaFeature.ROLEPLAY_CHAT))

        val failedValue = quotaManager.withAtomicQuotaOrFallback(
            feature = QuotaFeature.ROLEPLAY_CHAT,
            onQuotaExhausted = { "exhausted" },
            onErrorFallback = { "local_fallback" },
            cloudAction = { throw IllegalStateException("503 upstream failure") }
        )
        assertEquals("local_fallback", failedValue)
        assertEquals(initial - 1, quotaManager.getRemainingAllowance(QuotaFeature.ROLEPLAY_CHAT))
        assertEquals(1, quotaManager.quotaState.value.fallbackCascadeCount)
    }

    @Test
    fun multipleGraceDeductionsAndRefunds_restoreExactBonusBalance() {
        assertTrue(quotaManager.claimDailyGraceCredits())
        assertEquals(3, quotaManager.quotaState.value.bonusGraceCredits)

        assertTrue(quotaManager.deductInteraction(QuotaFeature.PRONUNCIATION_EVALUATION))
        assertTrue(quotaManager.deductInteraction(QuotaFeature.PRONUNCIATION_EVALUATION))
        assertEquals(1, quotaManager.quotaState.value.bonusGraceCredits)
        assertEquals(2, quotaManager.getValueReceipt().bonusCreditsUsed)

        quotaManager.refundInteraction(QuotaFeature.PRONUNCIATION_EVALUATION)
        quotaManager.refundInteraction(QuotaFeature.PRONUNCIATION_EVALUATION)
        assertEquals(3, quotaManager.quotaState.value.bonusGraceCredits)
        assertEquals(0, quotaManager.getValueReceipt().bonusCreditsUsed)
    }

    @Test
    fun aiModelConfig_usesSupportedGemini3Models() {
        assertEquals("gemini-3.8-flash", AiModelConfig.PRIMARY_REASONING_MODEL)
        assertEquals("gemini-3.5-flash-lite", AiModelConfig.ECONOMY_LITE_MODEL)
    }

    @Test
    fun topicPackSnapshot_matchesGetRemainingAllowanceWithBonusGraceCredits() {
        assertEquals(
            quotaManager.getRemainingAllowance(QuotaFeature.TOPIC_PACK),
            quotaManager.quotaState.value.remainingTopicPacks
        )

        assertTrue(quotaManager.claimDailyGraceCredits())
        assertEquals(
            UsageQuotaManager.GUEST_CAP_TOPICS + 3,
            quotaManager.quotaState.value.remainingTopicPacks
        )
        assertEquals(
            quotaManager.getRemainingAllowance(QuotaFeature.TOPIC_PACK),
            quotaManager.quotaState.value.remainingTopicPacks
        )

        assertTrue(quotaManager.deductInteraction(QuotaFeature.TOPIC_PACK))
        assertEquals(
            UsageQuotaManager.GUEST_CAP_TOPICS + 2,
            quotaManager.quotaState.value.remainingTopicPacks
        )
        quotaManager.refundInteraction(QuotaFeature.TOPIC_PACK)
        assertEquals(
            UsageQuotaManager.GUEST_CAP_TOPICS + 3,
            quotaManager.quotaState.value.remainingTopicPacks
        )
    }
}
