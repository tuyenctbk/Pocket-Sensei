package com.example.donation

import android.app.Activity
import android.content.ContextWrapper
import com.android.billingclient.api.Purchase
import com.example.analytics.EngagementDecisionEngine
import com.example.analytics.EngagementPrompt
import com.example.analytics.EngagementSnapshot
import com.example.analytics.TriggerMoment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DonationSystemTest {

    private val baseTime = 1_700_000_000_000L
    private val threeDays = TimeUnit.DAYS.toMillis(3)
    private val fiveMinutes = TimeUnit.MINUTES.toMillis(5)

    @Test
    fun findActivity_unwrapsNestedContextWrappers() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val wrapped = ContextWrapper(ContextWrapper(activity))
        assertSame(activity, wrapped.findActivity())
    }

    @Test
    fun buildPayPalUrl_formatsFixedAndCustomAmounts() {
        assertEquals(
            "https://paypal.me/${DonationConfig.PAYPAL_ME_USERNAME}/5USD",
            DonationConfig.buildPayPalUrl("5")
        )
        assertEquals(
            "https://paypal.me/${DonationConfig.PAYPAL_ME_USERNAME}/12.50USD",
            DonationConfig.buildPayPalUrl("12.5")
        )
        assertEquals(
            "https://paypal.me/${DonationConfig.PAYPAL_ME_USERNAME}",
            DonationConfig.buildPayPalUrl(null)
        )
        assertEquals(
            "https://paypal.me/${DonationConfig.PAYPAL_ME_USERNAME}",
            DonationConfig.buildPayPalUrl("")
        )
    }

    @Test
    fun qrCodeGenerator_rendersValidBitmapForPayPalUrl() {
        val url = DonationConfig.buildPayPalUrl("5")
        val bitmap = QrCodeGenerator.generateQrBitmap(url, sizePx = 256)
        assertNotNull(bitmap)
        assertEquals(256, bitmap!!.width)
        assertEquals(256, bitmap.height)
    }

    @Test
    fun validatePlayReceipt_acceptsValidPurchasedTierAndRejectsInvalid() {
        assertTrue(
            DonationBillingManager.validatePlayReceipt(
                purchaseToken = "tok_valid_123",
                productIds = listOf("pocket_sensei_tip_tier_2"),
                purchaseState = Purchase.PurchaseState.PURCHASED
            )
        )
        assertFalse(
            DonationBillingManager.validatePlayReceipt(
                purchaseToken = "",
                productIds = listOf("pocket_sensei_tip_tier_2"),
                purchaseState = Purchase.PurchaseState.PURCHASED
            )
        )
        assertFalse(
            DonationBillingManager.validatePlayReceipt(
                purchaseToken = "tok_pending",
                productIds = listOf("pocket_sensei_tip_tier_2"),
                purchaseState = Purchase.PurchaseState.PENDING
            )
        )
        assertFalse(
            DonationBillingManager.validatePlayReceipt(
                purchaseToken = "tok_unknown",
                productIds = listOf("unrelated_sku"),
                purchaseState = Purchase.PurchaseState.PURCHASED
            )
        )
    }

    @Test
    fun homeDonationBanner_enforcesSatisfactionSafeguardAnd14DayDismissCooldown() {
        val installMs = baseTime - threeDays
        val tooFewOps = EngagementSnapshot(
            firstInstallMs = installMs,
            totalSessions = 3,
            successfulOperations = 2
        )
        assertFalse(EngagementDecisionEngine.shouldShowHomeDonationBanner(tooFewOps, baseTime))

        val tooRecentInstall = EngagementSnapshot(
            firstInstallMs = baseTime - TimeUnit.HOURS.toMillis(12),
            totalSessions = 5,
            successfulOperations = 5
        )
        assertFalse(EngagementDecisionEngine.shouldShowHomeDonationBanner(tooRecentInstall, baseTime))

        val satisfied = EngagementSnapshot(
            firstInstallMs = installMs,
            totalSessions = 3,
            successfulOperations = 3
        )
        assertTrue(EngagementDecisionEngine.shouldShowHomeDonationBanner(satisfied, baseTime))

        val dismissedRecently = satisfied.copy(
            lastDonationBannerDismissMs = baseTime - TimeUnit.DAYS.toMillis(5)
        )
        assertFalse(EngagementDecisionEngine.shouldShowHomeDonationBanner(dismissedRecently, baseTime))

        val activeSupporter = satisfied.copy(isActiveSupporter = true)
        assertFalse(EngagementDecisionEngine.shouldShowHomeDonationBanner(activeSupporter, baseTime))
    }

    @Test
    fun donationAutoPrompt_requires7Operations_30DayCooldown_andStopsForSupporter() {
        val installMs = baseTime - TimeUnit.DAYS.toMillis(10)
        val eligibleSnapshot = EngagementSnapshot(
            firstInstallMs = installMs,
            totalSessions = 5,
            positiveMilestones = 8,
            successfulOperations = 7,
            rateCompletedOrOptedOut = true,
            shareOptedOut = true
        )

        // Never at APP_START
        assertNull(
            EngagementDecisionEngine.selectBestPrompt(
                snapshot = eligibleSnapshot,
                triggerMoment = TriggerMoment.APP_START,
                nowMs = baseTime + fiveMinutes,
                sessionStartMs = baseTime,
                isUserBusy = false,
                currentVersionCode = 1
            )
        )

        // Shows after warmup & 7+ ops on celebratory milestone
        val selected = EngagementDecisionEngine.selectBestPrompt(
            snapshot = eligibleSnapshot,
            triggerMoment = TriggerMoment.CELEBRATORY_MILESTONE,
            nowMs = baseTime + fiveMinutes,
            sessionStartMs = baseTime,
            isUserBusy = false,
            currentVersionCode = 1
        )
        assertTrue(selected is EngagementPrompt.DonationPrompt)

        // Suppressed during 30-day cooldown
        val cooledDown = eligibleSnapshot.copy(
            lastDonationPromptShownMs = baseTime - TimeUnit.DAYS.toMillis(15),
            lastAnyPromptShownMs = baseTime - TimeUnit.DAYS.toMillis(15),
            donationPromptDismissCount = 1
        )
        assertNull(
            EngagementDecisionEngine.selectBestPrompt(
                snapshot = cooledDown,
                triggerMoment = TriggerMoment.CELEBRATORY_MILESTONE,
                nowMs = baseTime + fiveMinutes,
                sessionStartMs = baseTime,
                isUserBusy = false,
                currentVersionCode = 1
            )
        )

        // Permanently suppressed for Active Supporters
        val supporter = eligibleSnapshot.copy(isActiveSupporter = true)
        assertNull(
            EngagementDecisionEngine.selectBestPrompt(
                snapshot = supporter,
                triggerMoment = TriggerMoment.CELEBRATORY_MILESTONE,
                nowMs = baseTime + fiveMinutes,
                sessionStartMs = baseTime,
                isUserBusy = false,
                currentVersionCode = 1
            )
        )
    }
}
