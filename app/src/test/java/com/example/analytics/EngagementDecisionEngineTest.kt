package com.example.analytics

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class EngagementDecisionEngineTest {

    private val baseTime = 1_700_000_000_000L
    private val fiveMinutes = TimeUnit.MINUTES.toMillis(5)
    private val twoDays = TimeUnit.HOURS.toMillis(48)
    private val eightDays = TimeUnit.DAYS.toMillis(8)

    @Test
    fun neverShowsPromptAtAppStart() {
        val snapshot = EngagementSnapshot(totalSessions = 5, positiveMilestones = 10)
        val prompt = EngagementDecisionEngine.selectBestPrompt(
            snapshot = snapshot,
            triggerMoment = TriggerMoment.APP_START,
            nowMs = baseTime + fiveMinutes,
            sessionStartMs = baseTime,
            isUserBusy = false,
            currentVersionCode = 1,
            availableUpdate = AppUpdateInfo(2, "1.1", "Fixes", isMandatory = false)
        )
        assertNull(prompt)
    }

    @Test
    fun neverShowsRateOrShareBefore90SecondsSessionWarmup() {
        val snapshot = EngagementSnapshot(totalSessions = 5, positiveMilestones = 6)
        val prompt = EngagementDecisionEngine.selectBestPrompt(
            snapshot = snapshot,
            triggerMoment = TriggerMoment.CELEBRATORY_MILESTONE,
            nowMs = baseTime + TimeUnit.SECONDS.toMillis(30),
            sessionStartMs = baseTime,
            isUserBusy = false,
            currentVersionCode = 1
        )
        assertNull(prompt)
    }

    @Test
    fun neverInterruptsWhenUserIsBusy() {
        val snapshot = EngagementSnapshot(totalSessions = 5, positiveMilestones = 6)
        val prompt = EngagementDecisionEngine.selectBestPrompt(
            snapshot = snapshot,
            triggerMoment = TriggerMoment.CELEBRATORY_MILESTONE,
            nowMs = baseTime + fiveMinutes,
            sessionStartMs = baseTime,
            isUserBusy = true,
            currentVersionCode = 1
        )
        assertNull(prompt)
    }

    @Test
    fun enforcesOnePromptPerSessionCap() {
        val snapshot = EngagementSnapshot(
            totalSessions = 5,
            positiveMilestones = 6,
            promptsShownThisSession = 1
        )
        val prompt = EngagementDecisionEngine.selectBestPrompt(
            snapshot = snapshot,
            triggerMoment = TriggerMoment.CELEBRATORY_MILESTONE,
            nowMs = baseTime + fiveMinutes,
            sessionStartMs = baseTime,
            isUserBusy = false,
            currentVersionCode = 1
        )
        assertNull(prompt)
    }

    @Test
    fun enforces48HourCrossPromptCooldown() {
        val snapshot = EngagementSnapshot(
            totalSessions = 5,
            positiveMilestones = 6,
            lastAnyPromptShownMs = baseTime - TimeUnit.HOURS.toMillis(12),
            lastSharePromptShownMs = baseTime - TimeUnit.HOURS.toMillis(12),
            shareDismissCount = 1
        )
        val prompt = EngagementDecisionEngine.selectBestPrompt(
            snapshot = snapshot,
            triggerMoment = TriggerMoment.CELEBRATORY_MILESTONE,
            nowMs = baseTime + fiveMinutes,
            sessionStartMs = baseTime,
            isUserBusy = false,
            currentVersionCode = 1
        )
        assertNull(prompt)
    }

    @Test
    fun selectsRatePromptForLoyalUserAfterWarmupAndCooldown() {
        val snapshot = EngagementSnapshot(
            totalSessions = 3,
            positiveMilestones = 3,
            lastAnyPromptShownMs = baseTime - eightDays
        )
        val prompt = EngagementDecisionEngine.selectBestPrompt(
            snapshot = snapshot,
            triggerMoment = TriggerMoment.CELEBRATORY_MILESTONE,
            nowMs = baseTime + fiveMinutes,
            sessionStartMs = baseTime,
            isUserBusy = false,
            currentVersionCode = 1
        )
        assertTrue(prompt is EngagementPrompt.RatePrompt)
    }

    @Test
    fun respectsRateOptOutAndFallsBackToShareAfterCooldown() {
        val snapshot = EngagementSnapshot(
            totalSessions = 4,
            positiveMilestones = 5,
            rateCompletedOrOptedOut = true,
            lastAnyPromptShownMs = baseTime - twoDays - 1000L
        )
        val prompt = EngagementDecisionEngine.selectBestPrompt(
            snapshot = snapshot,
            triggerMoment = TriggerMoment.CELEBRATORY_MILESTONE,
            nowMs = baseTime + fiveMinutes,
            sessionStartMs = baseTime,
            isUserBusy = false,
            currentVersionCode = 1,
            wordsMastered = 12L,
            latestScore = 95
        )
        assertTrue(prompt is EngagementPrompt.SharePrompt)
    }

    @Test
    fun prioritizesGenuineUpdateOverRateAndRespectsVersionSnooze() {
        val update = AppUpdateInfo(latestVersionCode = 2, latestVersionName = "1.1.0", releaseNotes = "Live improvements")
        val freshSnapshot = EngagementSnapshot(totalSessions = 4, positiveMilestones = 5)
        val prompt = EngagementDecisionEngine.selectBestPrompt(
            snapshot = freshSnapshot,
            triggerMoment = TriggerMoment.POST_PRACTICE_IDLE,
            nowMs = baseTime + fiveMinutes,
            sessionStartMs = baseTime,
            isUserBusy = false,
            currentVersionCode = 1,
            availableUpdate = update
        )
        assertTrue(prompt is EngagementPrompt.UpdatePrompt)

        val snoozedSnapshot = EngagementSnapshot(
            totalSessions = 4,
            positiveMilestones = 5,
            lastAnyPromptShownMs = baseTime - TimeUnit.HOURS.toMillis(49),
            lastUpdatePromptShownMs = baseTime - TimeUnit.HOURS.toMillis(49),
            snoozedUpdateVersionCode = 2,
            updateDismissCountForVersion = 1
        )
        val afterSnoozePrompt = EngagementDecisionEngine.selectBestPrompt(
            snapshot = snoozedSnapshot,
            triggerMoment = TriggerMoment.POST_PRACTICE_IDLE,
            nowMs = baseTime + fiveMinutes,
            sessionStartMs = baseTime,
            isUserBusy = false,
            currentVersionCode = 1,
            availableUpdate = update
        )
        // Update is still snoozed (< 3 days), so eligible RatePrompt is selected instead
        assertTrue(afterSnoozePrompt is EngagementPrompt.RatePrompt)
    }

    @Test
    fun dailyDojoGoalIncrementsSameDayAndResetsOnNewDay() {
        org.junit.Assert.assertEquals(
            0,
            EngagementDecisionEngine.computeDailyGoalReadState(null, 0, "2026-11-13")
        )
        org.junit.Assert.assertEquals(
            "2026-11-13" to 1,
            EngagementDecisionEngine.computeNextDailyGoalState(null, 0, "2026-11-13")
        )
        org.junit.Assert.assertEquals(
            "2026-11-13" to 3,
            EngagementDecisionEngine.computeNextDailyGoalState("2026-11-13", 2, "2026-11-13")
        )
        // New day resets read count to 0 and increments cleanly to 1
        org.junit.Assert.assertEquals(
            0,
            EngagementDecisionEngine.computeDailyGoalReadState("2026-11-12", 5, "2026-11-13")
        )
        org.junit.Assert.assertEquals(
            "2026-11-13" to 1,
            EngagementDecisionEngine.computeNextDailyGoalState("2026-11-12", 5, "2026-11-13")
        )
    }

    @Test
    fun dailyStreakStartsIncrementsPreservesBestAndResetsAfterMissedDay() {
        // Brand new user starts at 0 before practicing today
        val freshRead = EngagementDecisionEngine.computeDailyStreakReadState(
            lastPracticeDay = null,
            storedStreak = 0,
            storedBestStreak = 0,
            todayDay = "2026-11-13"
        )
        org.junit.Assert.assertEquals(0, freshRead.currentStreak)
        org.junit.Assert.assertEquals(false, freshRead.practicedToday)

        // First practice ignites day 1 and locks today in
        val (day1State, day1Locked) = EngagementDecisionEngine.computeNextDailyStreakState(
            lastPracticeDay = null,
            storedStreak = 0,
            storedBestStreak = 0,
            todayDay = "2026-11-13"
        )
        assertTrue(day1Locked)
        org.junit.Assert.assertEquals(1, day1State.currentStreak)
        org.junit.Assert.assertEquals(1, day1State.bestStreak)
        assertTrue(day1State.practicedToday)

        // Repeat practice same day does not double increment
        val (repeatSameDay, repeatLocked) = EngagementDecisionEngine.computeNextDailyStreakState(
            lastPracticeDay = "2026-11-13",
            storedStreak = 1,
            storedBestStreak = 1,
            todayDay = "2026-11-13"
        )
        org.junit.Assert.assertFalse(repeatLocked)
        org.junit.Assert.assertEquals(1, repeatSameDay.currentStreak)

        // Next consecutive calendar day keeps streak alive on read (pending today) and increments on practice
        val nextDayRead = EngagementDecisionEngine.computeDailyStreakReadState(
            lastPracticeDay = "2026-11-13",
            storedStreak = 4,
            storedBestStreak = 4,
            todayDay = "2026-11-14"
        )
        org.junit.Assert.assertEquals(4, nextDayRead.currentStreak)
        org.junit.Assert.assertFalse(nextDayRead.practicedToday)

        val (day5State, day5Locked) = EngagementDecisionEngine.computeNextDailyStreakState(
            lastPracticeDay = "2026-11-13",
            storedStreak = 4,
            storedBestStreak = 4,
            todayDay = "2026-11-14"
        )
        assertTrue(day5Locked)
        org.junit.Assert.assertEquals(5, day5State.currentStreak)
        org.junit.Assert.assertEquals(5, day5State.bestStreak)

        // Missing a day resets read streak to 0 and restarts at 1 on practice while preserving bestStreak
        val gappedRead = EngagementDecisionEngine.computeDailyStreakReadState(
            lastPracticeDay = "2026-11-10",
            storedStreak = 5,
            storedBestStreak = 5,
            todayDay = "2026-11-13"
        )
        org.junit.Assert.assertEquals(0, gappedRead.currentStreak)
        org.junit.Assert.assertEquals(5, gappedRead.bestStreak)

        val (restartedState, restartedLocked) = EngagementDecisionEngine.computeNextDailyStreakState(
            lastPracticeDay = "2026-11-10",
            storedStreak = 5,
            storedBestStreak = 5,
            todayDay = "2026-11-13"
        )
        assertTrue(restartedLocked)
        org.junit.Assert.assertEquals(1, restartedState.currentStreak)
        org.junit.Assert.assertEquals(5, restartedState.bestStreak)
    }

    @Test
    fun streakFreezeProtectsOneMissedDayAndEarnsBonusEverySeventhDay() {
        // Missed 1 day (deltaDays == 2) with 1 freeze available preserves streak on read
        val protectedRead = EngagementDecisionEngine.computeDailyStreakReadState(
            lastPracticeDay = "2026-11-11",
            storedStreak = 5,
            storedBestStreak = 5,
            todayDay = "2026-11-13",
            storedFreezes = 1
        )
        org.junit.Assert.assertEquals(5, protectedRead.currentStreak)
        assertTrue(protectedRead.freezeSavedStreak)
        org.junit.Assert.assertEquals(1, protectedRead.freezesAvailable)

        // Practicing after 1 missed day consumes 1 freeze and advances streak to 6
        val (day6State, day6Locked) = EngagementDecisionEngine.computeNextDailyStreakState(
            lastPracticeDay = "2026-11-11",
            storedStreak = 5,
            storedBestStreak = 5,
            todayDay = "2026-11-13",
            storedFreezes = 1
        )
        assertTrue(day6Locked)
        org.junit.Assert.assertEquals(6, day6State.currentStreak)
        org.junit.Assert.assertEquals(0, day6State.freezesAvailable)
        assertTrue(day6State.freezeSavedStreak)

        // Reaching day 7 awards +1 freeze up to MAX_STREAK_FREEZES (3)
        val (day7State, day7Locked) = EngagementDecisionEngine.computeNextDailyStreakState(
            lastPracticeDay = "2026-11-13",
            storedStreak = 6,
            storedBestStreak = 6,
            todayDay = "2026-11-14",
            storedFreezes = 0
        )
        assertTrue(day7Locked)
        org.junit.Assert.assertEquals(7, day7State.currentStreak)
        org.junit.Assert.assertEquals(1, day7State.freezesAvailable)
    }
}
