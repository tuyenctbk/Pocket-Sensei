package com.example.analytics

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class DailyStreakState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastPracticeDay: String? = null,
    val practicedToday: Boolean = false,
    val freezesAvailable: Int = 0,
    val freezeSavedStreak: Boolean = false
)

/**
 * Pure, deterministic scheduling algorithm that decides whether an auto-popup
 * (Update, Rate, or Share) should be displayed at a given moment while preventing
 * spam, repeated nagging, cold-start interruptions, and busy-state interruptions.
 */
object EngagementDecisionEngine {

    val MIN_SESSION_ELAPSED_FOR_RATE_SHARE_MS: Long = TimeUnit.SECONDS.toMillis(90)
    val MIN_SESSION_ELAPSED_FOR_UPDATE_MS: Long = TimeUnit.SECONDS.toMillis(15)
    val GLOBAL_CROSS_PROMPT_COOLDOWN_MS: Long = TimeUnit.HOURS.toMillis(48)

    private val RATE_BACKOFF_MS = listOf(
        TimeUnit.DAYS.toMillis(7),
        TimeUnit.DAYS.toMillis(21)
    )
    private val SHARE_BACKOFF_MS = listOf(
        TimeUnit.DAYS.toMillis(5),
        TimeUnit.DAYS.toMillis(14)
    )
    private val SHARE_POST_ACCEPT_COOLDOWN_MS = TimeUnit.DAYS.toMillis(30)
    private val UPDATE_SNOOZE_COOLDOWN_MS = TimeUnit.DAYS.toMillis(3)

    val MIN_DAYS_SINCE_INSTALL_FOR_DONATION_MS: Long = TimeUnit.DAYS.toMillis(2)
    val DONATION_BANNER_DISMISS_COOLDOWN_MS: Long = TimeUnit.DAYS.toMillis(14)
    val DONATION_AUTO_PROMPT_COOLDOWN_MS: Long = TimeUnit.DAYS.toMillis(30)

    private const val MIN_OPS_FOR_DONATION_BANNER = 3
    private const val MIN_SESSIONS_FOR_DONATION = 3
    private const val MIN_OPS_FOR_DONATION_AUTO_PROMPT = 7
    private const val MAX_DONATION_AUTO_DISMISSALS = 3

    private const val MAX_PROMPTS_PER_SESSION = 1
    private const val MAX_RATE_DISMISSALS = 2
    private const val MAX_SHARE_DISMISSALS = 3
    private const val MAX_UPDATE_DISMISSALS_PER_VERSION = 2

    const val DEFAULT_DAILY_DOJO_GOAL = 3

    fun computeDailyGoalReadState(
        storedDay: String?,
        storedCount: Int,
        todayDay: String
    ): Int {
        return if (storedDay == todayDay) storedCount.coerceAtLeast(0) else 0
    }

    fun computeNextDailyGoalState(
        storedDay: String?,
        storedCount: Int,
        todayDay: String
    ): Pair<String, Int> {
        val currentForToday = computeDailyGoalReadState(storedDay, storedCount, todayDay)
        return todayDay to (currentForToday + 1)
    }

    const val MAX_STREAK_FREEZES = 3

    fun computeDailyStreakReadState(
        lastPracticeDay: String?,
        storedStreak: Int,
        storedBestStreak: Int,
        todayDay: String,
        storedFreezes: Int = 0
    ): DailyStreakState {
        val safeStreak = storedStreak.coerceAtLeast(0)
        val safeBest = maxOf(storedBestStreak.coerceAtLeast(0), safeStreak)
        val safeFreezes = storedFreezes.coerceIn(0, MAX_STREAK_FREEZES)
        if (lastPracticeDay.isNullOrBlank() || safeStreak == 0) {
            return DailyStreakState(
                currentStreak = 0,
                bestStreak = safeBest,
                lastPracticeDay = lastPracticeDay,
                practicedToday = false,
                freezesAvailable = safeFreezes,
                freezeSavedStreak = false
            )
        }
        val deltaDays = daysBetweenDayStamps(lastPracticeDay, todayDay)
        return when {
            deltaDays == 0L -> DailyStreakState(
                currentStreak = safeStreak,
                bestStreak = safeBest,
                lastPracticeDay = lastPracticeDay,
                practicedToday = true,
                freezesAvailable = safeFreezes,
                freezeSavedStreak = false
            )
            deltaDays == 1L -> DailyStreakState(
                currentStreak = safeStreak,
                bestStreak = safeBest,
                lastPracticeDay = lastPracticeDay,
                practicedToday = false,
                freezesAvailable = safeFreezes,
                freezeSavedStreak = false
            )
            deltaDays == 2L && safeFreezes > 0 -> DailyStreakState(
                currentStreak = safeStreak,
                bestStreak = safeBest,
                lastPracticeDay = lastPracticeDay,
                practicedToday = false,
                freezesAvailable = safeFreezes,
                freezeSavedStreak = true
            )
            deltaDays != null && deltaDays < 0L -> DailyStreakState(
                currentStreak = safeStreak,
                bestStreak = safeBest,
                lastPracticeDay = lastPracticeDay,
                practicedToday = false,
                freezesAvailable = safeFreezes,
                freezeSavedStreak = false
            )
            else -> DailyStreakState(
                currentStreak = 0,
                bestStreak = safeBest,
                lastPracticeDay = lastPracticeDay,
                practicedToday = false,
                freezesAvailable = safeFreezes,
                freezeSavedStreak = false
            )
        }
    }

    fun computeNextDailyStreakState(
        lastPracticeDay: String?,
        storedStreak: Int,
        storedBestStreak: Int,
        todayDay: String,
        storedFreezes: Int = 0
    ): Pair<DailyStreakState, Boolean> {
        val safeStreak = storedStreak.coerceAtLeast(0)
        val safeBest = maxOf(storedBestStreak.coerceAtLeast(0), safeStreak)
        val safeFreezes = storedFreezes.coerceIn(0, MAX_STREAK_FREEZES)
        if (lastPracticeDay.isNullOrBlank() || safeStreak == 0) {
            val nextBest = maxOf(safeBest, 1)
            return DailyStreakState(
                currentStreak = 1,
                bestStreak = nextBest,
                lastPracticeDay = todayDay,
                practicedToday = true,
                freezesAvailable = safeFreezes,
                freezeSavedStreak = false
            ) to true
        }
        val deltaDays = daysBetweenDayStamps(lastPracticeDay, todayDay)
        return when {
            deltaDays == 0L -> DailyStreakState(
                currentStreak = safeStreak,
                bestStreak = safeBest,
                lastPracticeDay = todayDay,
                practicedToday = true,
                freezesAvailable = safeFreezes,
                freezeSavedStreak = false
            ) to false
            deltaDays == 1L -> {
                val nextStreak = safeStreak + 1
                val nextBest = maxOf(safeBest, nextStreak)
                val earnedFreeze = if (nextStreak % 7 == 0) 1 else 0
                DailyStreakState(
                    currentStreak = nextStreak,
                    bestStreak = nextBest,
                    lastPracticeDay = todayDay,
                    practicedToday = true,
                    freezesAvailable = (safeFreezes + earnedFreeze).coerceAtMost(MAX_STREAK_FREEZES),
                    freezeSavedStreak = false
                ) to true
            }
            deltaDays == 2L && safeFreezes > 0 -> {
                val nextStreak = safeStreak + 1
                val nextBest = maxOf(safeBest, nextStreak)
                val consumed = safeFreezes - 1
                val earnedFreeze = if (nextStreak % 7 == 0) 1 else 0
                DailyStreakState(
                    currentStreak = nextStreak,
                    bestStreak = nextBest,
                    lastPracticeDay = todayDay,
                    practicedToday = true,
                    freezesAvailable = (consumed + earnedFreeze).coerceAtMost(MAX_STREAK_FREEZES),
                    freezeSavedStreak = true
                ) to true
            }
            deltaDays != null && deltaDays < 0L -> DailyStreakState(
                currentStreak = safeStreak,
                bestStreak = safeBest,
                lastPracticeDay = lastPracticeDay,
                practicedToday = false,
                freezesAvailable = safeFreezes,
                freezeSavedStreak = false
            ) to false
            else -> {
                val nextBest = maxOf(safeBest, 1)
                DailyStreakState(
                    currentStreak = 1,
                    bestStreak = nextBest,
                    lastPracticeDay = todayDay,
                    practicedToday = true,
                    freezesAvailable = safeFreezes,
                    freezeSavedStreak = false
                ) to true
            }
        }
    }

    internal fun daysBetweenDayStamps(startDay: String, endDay: String): Long? {
        return try {
            val utc = TimeZone.getTimeZone("UTC")
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
                timeZone = utc
            }
            val startDate = parser.parse(startDay) ?: return null
            val endDate = parser.parse(endDay) ?: return null
            val startCal = Calendar.getInstance(utc).apply {
                time = startDate
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance(utc).apply {
                time = endDate
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMs = endCal.timeInMillis - startCal.timeInMillis
            TimeUnit.MILLISECONDS.toDays(diffMs)
        } catch (_: Exception) {
            null
        }
    }

    fun selectBestPrompt(
        snapshot: EngagementSnapshot,
        triggerMoment: TriggerMoment,
        nowMs: Long,
        sessionStartMs: Long,
        isUserBusy: Boolean,
        currentVersionCode: Int,
        availableUpdate: AppUpdateInfo? = null,
        wordsMastered: Long = 0L,
        latestScore: Int? = null
    ): EngagementPrompt? {
        // 1. Never interrupt active speaking, recording, AI thinking, or open dialogs
        if (isUserBusy) return null

        // 2. Strictly enforce 1 auto-popup per session
        if (snapshot.promptsShownThisSession >= MAX_PROMPTS_PER_SESSION) return null

        // 3. Never show prompts immediately on cold app start
        if (triggerMoment == TriggerMoment.APP_START) return null

        val elapsedSessionMs = (nowMs - sessionStartMs).coerceAtLeast(0L)
        val sinceAnyPromptMs = if (snapshot.lastAnyPromptShownMs > 0L) {
            (nowMs - snapshot.lastAnyPromptShownMs).coerceAtLeast(0L)
        } else {
            Long.MAX_VALUE
        }

        // Priority 1: Genuine App Update (only when server/config indicates a newer version exists)
        if (availableUpdate != null && availableUpdate.latestVersionCode > currentVersionCode) {
            val canShowUpdate = isUpdateEligible(
                snapshot = snapshot,
                update = availableUpdate,
                elapsedSessionMs = elapsedSessionMs,
                sinceAnyPromptMs = sinceAnyPromptMs,
                nowMs = nowMs
            )
            if (canShowUpdate) {
                return EngagementPrompt.UpdatePrompt(
                    versionName = availableUpdate.latestVersionName,
                    versionCode = availableUpdate.latestVersionCode,
                    releaseNotes = availableUpdate.releaseNotes,
                    isMandatory = availableUpdate.isMandatory
                )
            }
        }

        // Enforce 48h global cooldown and 90s session warm-up for Rate, Share & Donation
        if (elapsedSessionMs < MIN_SESSION_ELAPSED_FOR_RATE_SHARE_MS) return null
        if (sinceAnyPromptMs < GLOBAL_CROSS_PROMPT_COOLDOWN_MS) return null

        // Priority 2: Rate App (high-intent, loyal learners after a positive milestone)
        if (isRateEligible(snapshot, triggerMoment, nowMs)) {
            return EngagementPrompt.RatePrompt(
                sessionsCompleted = snapshot.totalSessions,
                milestonesReached = snapshot.positiveMilestones
            )
        }

        // Priority 3: Share App (triggered specifically on celebratory moments)
        if (isShareEligible(snapshot, triggerMoment, nowMs)) {
            return EngagementPrompt.SharePrompt(
                wordsMastered = wordsMastered,
                latestScore = latestScore
            )
        }

        // Priority 4: Voluntary Supporter Donation Prompt (7+ operations, 2+ days installed, 30d cooldown)
        if (isDonationPromptEligible(snapshot, triggerMoment, nowMs)) {
            val daysInstalled = TimeUnit.MILLISECONDS.toDays((nowMs - snapshot.firstInstallMs).coerceAtLeast(0L))
            return EngagementPrompt.DonationPrompt(
                successfulOperations = snapshot.successfulOperations,
                daysSinceInstall = daysInstalled
            )
        }

        return null
    }

    /**
     * Compact Home Banner safeguard:
     * Only visible after 3+ successful operations, 3+ app sessions, and 2+ days since install.
     * Silenced for 14 days when dismissed via "X", and permanently hidden for Active Supporters.
     */
    fun shouldShowHomeDonationBanner(
        snapshot: EngagementSnapshot,
        nowMs: Long
    ): Boolean {
        if (snapshot.isActiveSupporter || snapshot.donationPromptOptedOut) return false
        if (snapshot.successfulOperations < MIN_OPS_FOR_DONATION_BANNER) return false
        if (snapshot.totalSessions < MIN_SESSIONS_FOR_DONATION) return false
        if (snapshot.firstInstallMs <= 0L) return false
        if ((nowMs - snapshot.firstInstallMs) < MIN_DAYS_SINCE_INSTALL_FOR_DONATION_MS) return false
        if (snapshot.lastDonationBannerDismissMs > 0L &&
            (nowMs - snapshot.lastDonationBannerDismissMs) < DONATION_BANNER_DISMISS_COOLDOWN_MS
        ) {
            return false
        }
        return true
    }

    /**
     * Subtle post-operation support footer shown inside high-scoring practice results
     * (Value Realization Moment) only for satisfied, non-supporter learners.
     */
    fun shouldShowPostOperationSupportCard(
        snapshot: EngagementSnapshot,
        nowMs: Long,
        operationScore: Int
    ): Boolean {
        if (operationScore < 85) return false
        return shouldShowHomeDonationBanner(snapshot, nowMs)
    }

    private fun isDonationPromptEligible(
        snapshot: EngagementSnapshot,
        triggerMoment: TriggerMoment,
        nowMs: Long
    ): Boolean {
        if (snapshot.isActiveSupporter || snapshot.donationPromptOptedOut) return false
        if (snapshot.donationPromptDismissCount >= MAX_DONATION_AUTO_DISMISSALS) return false
        if (snapshot.successfulOperations < MIN_OPS_FOR_DONATION_AUTO_PROMPT) return false
        if (snapshot.totalSessions < MIN_SESSIONS_FOR_DONATION) return false
        if (snapshot.firstInstallMs <= 0L) return false
        if ((nowMs - snapshot.firstInstallMs) < MIN_DAYS_SINCE_INSTALL_FOR_DONATION_MS) return false
        if (triggerMoment != TriggerMoment.CELEBRATORY_MILESTONE) return false

        if (snapshot.lastDonationPromptShownMs > 0L &&
            (nowMs - snapshot.lastDonationPromptShownMs) < DONATION_AUTO_PROMPT_COOLDOWN_MS
        ) {
            return false
        }
        return true
    }

    private fun isUpdateEligible(
        snapshot: EngagementSnapshot,
        update: AppUpdateInfo,
        elapsedSessionMs: Long,
        sinceAnyPromptMs: Long,
        nowMs: Long
    ): Boolean {
        if (elapsedSessionMs < MIN_SESSION_ELAPSED_FOR_UPDATE_MS) return false
        if (update.isMandatory) return true
        if (sinceAnyPromptMs < GLOBAL_CROSS_PROMPT_COOLDOWN_MS) return false

        val sameSnoozedVersion = snapshot.snoozedUpdateVersionCode == update.latestVersionCode
        if (sameSnoozedVersion) {
            if (snapshot.updateDismissCountForVersion >= MAX_UPDATE_DISMISSALS_PER_VERSION) {
                return false
            }
            val sinceUpdatePrompt = (nowMs - snapshot.lastUpdatePromptShownMs).coerceAtLeast(0L)
            if (sinceUpdatePrompt < UPDATE_SNOOZE_COOLDOWN_MS) {
                return false
            }
        }
        return true
    }

    private fun isRateEligible(
        snapshot: EngagementSnapshot,
        triggerMoment: TriggerMoment,
        nowMs: Long
    ): Boolean {
        if (snapshot.rateCompletedOrOptedOut) return false
        if (snapshot.rateDismissCount >= MAX_RATE_DISMISSALS) return false
        if (snapshot.totalSessions < 3 || snapshot.positiveMilestones < 3) return false
        if (triggerMoment != TriggerMoment.CELEBRATORY_MILESTONE &&
            triggerMoment != TriggerMoment.POST_PRACTICE_IDLE
        ) {
            return false
        }

        if (snapshot.lastRatePromptShownMs > 0L) {
            val backoffIndex = (snapshot.rateDismissCount - 1).coerceIn(0, RATE_BACKOFF_MS.lastIndex)
            val requiredCooldown = RATE_BACKOFF_MS[backoffIndex]
            if ((nowMs - snapshot.lastRatePromptShownMs) < requiredCooldown) {
                return false
            }
        }
        return true
    }

    private fun isShareEligible(
        snapshot: EngagementSnapshot,
        triggerMoment: TriggerMoment,
        nowMs: Long
    ): Boolean {
        if (snapshot.shareOptedOut) return false
        if (snapshot.shareDismissCount >= MAX_SHARE_DISMISSALS) return false
        if (snapshot.totalSessions < 2 || snapshot.positiveMilestones < 2) return false
        if (triggerMoment != TriggerMoment.CELEBRATORY_MILESTONE) return false

        if (snapshot.lastShareCompletedMs > 0L &&
            (nowMs - snapshot.lastShareCompletedMs) < SHARE_POST_ACCEPT_COOLDOWN_MS
        ) {
            return false
        }

        if (snapshot.lastSharePromptShownMs > 0L) {
            val backoffIndex = (snapshot.shareDismissCount - 1).coerceIn(0, SHARE_BACKOFF_MS.lastIndex)
            val requiredCooldown = SHARE_BACKOFF_MS[backoffIndex]
            if ((nowMs - snapshot.lastSharePromptShownMs) < requiredCooldown) {
                return false
            }
        }
        return true
    }
}
