package com.example.analytics

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.example.data.local.AppEventDao
import com.example.data.local.AppEventLog
import com.example.data.local.SenseiLocalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "EngagementManager"
private const val PREFS_NAME = "pocket_sensei_engagement_prefs"

class EngagementManager(
    context: Context,
    private val eventDao: AppEventDao = SenseiLocalDatabase.getInstance(context).appEventDao()
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val sessionId: String = UUID.randomUUID().toString().take(12)
    private val sessionStartMs: Long = System.currentTimeMillis()
    private var promptsShownThisSession: Int = 0

    private val _activePrompt = MutableStateFlow<EngagementPrompt?>(null)
    val activePrompt: StateFlow<EngagementPrompt?> = _activePrompt.asStateFlow()

    private val _isActiveSupporter = MutableStateFlow(false)
    val isActiveSupporter: StateFlow<Boolean> = _isActiveSupporter.asStateFlow()

    private val _shouldShowHomeDonationBanner = MutableStateFlow(false)
    val shouldShowHomeDonationBanner: StateFlow<Boolean> = _shouldShowHomeDonationBanner.asStateFlow()

    private val _successfulOperationsCount = MutableStateFlow(0)
    val successfulOperationsCount: StateFlow<Int> = _successfulOperationsCount.asStateFlow()

    private val _openDonationSheetRequested = MutableStateFlow(false)
    val openDonationSheetRequested: StateFlow<Boolean> = _openDonationSheetRequested.asStateFlow()

    private val _dailyGoalProgress = MutableStateFlow(
        readDailyGoalPair(todayDayStamp())
    )
    val dailyGoalProgress: StateFlow<Pair<Int, Int>> = _dailyGoalProgress.asStateFlow()

    private val _dailyStreakState = MutableStateFlow(
        readDailyStreakState(todayDayStamp())
    )
    val dailyStreakState: StateFlow<DailyStreakState> = _dailyStreakState.asStateFlow()

    // Populated only if remote config / Play update metadata reports a newer versionCode
    private var availableUpdate: AppUpdateInfo? = null

    init {
        val nowMs = System.currentTimeMillis()
        val editor = prefs.edit()
        if (!prefs.contains(KEY_FIRST_INSTALL_MS)) {
            editor.putLong(KEY_FIRST_INSTALL_MS, nowMs)
        }
        if (!prefs.contains(KEY_STREAK_FREEZES)) {
            editor.putInt(KEY_STREAK_FREEZES, 1)
        }
        val sessions = prefs.getInt(KEY_TOTAL_SESSIONS, 0) + 1
        editor.putInt(KEY_TOTAL_SESSIONS, sessions)
        editor.apply()

        _dailyStreakState.value = readDailyStreakState(todayDayStamp())
        refreshDonationBannerState()

        trackEvent(
            eventName = "app_session_start",
            category = "lifecycle",
            metadata = mapOf(
                "session_number" to sessions,
                "version_code" to BuildConfig.VERSION_CODE
            )
        )
    }

    fun observeRecentEvents(limit: Int = 20): Flow<List<AppEventLog>> =
        eventDao.observeRecentEvents(limit)

    fun observeTotalEventCount(): Flow<Int> =
        eventDao.observeTotalEventCount()

    fun setAvailableUpdate(updateInfo: AppUpdateInfo?) {
        availableUpdate = updateInfo
    }

    fun trackEvent(
        eventName: String,
        category: String,
        metadata: Map<String, Any?> = emptyMap()
    ) {
        val json = JSONObject().apply {
            metadata.forEach { (k, v) ->
                if (v != null) put(k, v)
            }
        }.toString()

        scope.launch {
            try {
                eventDao.insertEvent(
                    AppEventLog(
                        eventName = eventName,
                        category = category,
                        metadataJson = json,
                        sessionId = sessionId,
                        timestampMs = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to log event $eventName", e)
            }
        }
    }

    fun recordSuccessfulCoreOperation(operationName: String, score: Int? = null): Int {
        val updatedOps = prefs.getInt(KEY_SUCCESSFUL_OPERATIONS, 0) + 1
        val today = todayDayStamp()
        val (nextDay, nextGoalCount) = EngagementDecisionEngine.computeNextDailyGoalState(
            storedDay = prefs.getString(KEY_DAILY_GOAL_DAY, null),
            storedCount = prefs.getInt(KEY_DAILY_GOAL_COUNT, 0),
            todayDay = today
        )
        prefs.edit()
            .putInt(KEY_SUCCESSFUL_OPERATIONS, updatedOps)
            .putString(KEY_DAILY_GOAL_DAY, nextDay)
            .putInt(KEY_DAILY_GOAL_COUNT, nextGoalCount)
            .apply()
        _dailyGoalProgress.value = nextGoalCount to EngagementDecisionEngine.DEFAULT_DAILY_DOJO_GOAL
        refreshDonationBannerState()

        if (nextGoalCount == EngagementDecisionEngine.DEFAULT_DAILY_DOJO_GOAL) {
            trackEvent(
                eventName = "daily_goal_completed",
                category = "retention",
                metadata = mapOf(
                    "operation" to operationName,
                    "day" to nextDay,
                    "goal_target" to EngagementDecisionEngine.DEFAULT_DAILY_DOJO_GOAL
                )
            )
        }

        if (updatedOps in listOf(3, 7, 10, 25, 50, 100)) {
            trackEvent(
                eventName = "core_operations_milestone",
                category = "milestone",
                metadata = mapOf(
                    "operation" to operationName,
                    "count" to updatedOps,
                    "score" to score
                )
            )
        }
        return updatedOps
    }

    fun recordDailyStreakPractice(source: String): Pair<DailyStreakState, Boolean> {
        val today = todayDayStamp()
        val (nextState, lockedInToday) = EngagementDecisionEngine.computeNextDailyStreakState(
            lastPracticeDay = prefs.getString(KEY_STREAK_LAST_DAY, null),
            storedStreak = prefs.getInt(KEY_STREAK_COUNT, 0),
            storedBestStreak = prefs.getInt(KEY_STREAK_BEST, 0),
            todayDay = today,
            storedFreezes = prefs.getInt(KEY_STREAK_FREEZES, 0)
        )
        prefs.edit()
            .putInt(KEY_STREAK_COUNT, nextState.currentStreak)
            .putInt(KEY_STREAK_BEST, nextState.bestStreak)
            .putString(KEY_STREAK_LAST_DAY, nextState.lastPracticeDay)
            .putInt(KEY_STREAK_FREEZES, nextState.freezesAvailable)
            .apply()
        _dailyStreakState.value = nextState

        if (lockedInToday) {
            trackEvent(
                eventName = "daily_streak_updated",
                category = "retention",
                metadata = mapOf(
                    "current_streak" to nextState.currentStreak,
                    "best_streak" to nextState.bestStreak,
                    "freezes_left" to nextState.freezesAvailable,
                    "freeze_used" to nextState.freezeSavedStreak,
                    "source" to source
                )
            )
        }
        return nextState to lockedInToday
    }

    fun refreshDailyRetentionState() {
        val today = todayDayStamp()
        _dailyGoalProgress.value = readDailyGoalPair(today)
        _dailyStreakState.value = readDailyStreakState(today)
    }

    fun syncRemoteStreakIfHigher(remoteStreak: Int, remoteUpdatedAtMs: Long?): DailyStreakState {
        if (remoteStreak <= 0) return _dailyStreakState.value
        val today = todayDayStamp()
        val remoteDayStamp = remoteUpdatedAtMs?.takeIf { it > 0L }?.let { ms ->
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))
        }
        val currentRead = readDailyStreakState(today)
        val storedBest = prefs.getInt(KEY_STREAK_BEST, 0)
        val mergedBest = maxOf(storedBest, currentRead.bestStreak, remoteStreak)
        if (remoteStreak > currentRead.currentStreak) {
            val effectiveLastDay = when {
                !remoteDayStamp.isNullOrBlank() -> remoteDayStamp
                currentRead.lastPracticeDay != null -> currentRead.lastPracticeDay
                else -> today
            }
            prefs.edit()
                .putInt(KEY_STREAK_COUNT, remoteStreak)
                .putInt(KEY_STREAK_BEST, mergedBest)
                .putString(KEY_STREAK_LAST_DAY, effectiveLastDay)
                .apply()
        } else if (mergedBest > storedBest) {
            prefs.edit().putInt(KEY_STREAK_BEST, mergedBest).apply()
        }
        val updated = readDailyStreakState(today)
        _dailyStreakState.value = updated
        return updated
    }

    fun dismissHomeDonationBanner() {
        prefs.edit()
            .putLong(KEY_LAST_DONATION_BANNER_DISMISS_MS, System.currentTimeMillis())
            .apply()
        _shouldShowHomeDonationBanner.value = false
        trackEvent("donation_banner_dismissed", "donation")
    }

    fun markActiveSupporter(source: String, detail: String = "") {
        val updatedFreezes = (prefs.getInt(KEY_STREAK_FREEZES, 0) + 1)
            .coerceAtMost(EngagementDecisionEngine.MAX_STREAK_FREEZES)
        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE_SUPPORTER, true)
            .putInt(KEY_STREAK_FREEZES, updatedFreezes)
            .apply()
        _dailyStreakState.value = readDailyStreakState(todayDayStamp())
        refreshDonationBannerState()
        trackEvent(
            eventName = "active_supporter_unlocked",
            category = "donation",
            metadata = mapOf("source" to source, "detail" to detail)
        )
    }

    fun consumeOpenDonationSheetRequest() {
        _openDonationSheetRequested.value = false
    }

    fun canShowPostOperationSupportCard(score: Int): Boolean {
        return EngagementDecisionEngine.shouldShowPostOperationSupportCard(
            snapshot = readSnapshot(),
            nowMs = System.currentTimeMillis(),
            operationScore = score
        )
    }

    fun refreshDonationBannerState() {
        val snapshot = readSnapshot()
        _isActiveSupporter.value = snapshot.isActiveSupporter
        _successfulOperationsCount.value = snapshot.successfulOperations
        _shouldShowHomeDonationBanner.value =
            EngagementDecisionEngine.shouldShowHomeDonationBanner(snapshot, System.currentTimeMillis())
    }

    fun recordPositiveMilestone(
        milestoneName: String,
        metadata: Map<String, Any?> = emptyMap(),
        isCelebratory: Boolean = true,
        isUserBusy: Boolean = false,
        wordsMastered: Long = 0L,
        latestScore: Int? = null
    ) {
        val updatedCount = prefs.getInt(KEY_POSITIVE_MILESTONES, 0) + 1
        prefs.edit().putInt(KEY_POSITIVE_MILESTONES, updatedCount).apply()
        refreshDonationBannerState()

        trackEvent(
            eventName = milestoneName,
            category = "milestone",
            metadata = metadata + mapOf("total_milestones" to updatedCount)
        )

        val moment = if (isCelebratory) {
            TriggerMoment.CELEBRATORY_MILESTONE
        } else {
            TriggerMoment.POST_PRACTICE_IDLE
        }

        evaluatePromptOpportunity(
            triggerMoment = moment,
            isUserBusy = isUserBusy,
            wordsMastered = wordsMastered,
            latestScore = latestScore
        )
    }

    fun evaluatePromptOpportunity(
        triggerMoment: TriggerMoment,
        isUserBusy: Boolean,
        wordsMastered: Long = 0L,
        latestScore: Int? = null
    ) {
        if (_activePrompt.value != null) return

        val nowMs = System.currentTimeMillis()
        val snapshot = readSnapshot()
        val selected = EngagementDecisionEngine.selectBestPrompt(
            snapshot = snapshot,
            triggerMoment = triggerMoment,
            nowMs = nowMs,
            sessionStartMs = sessionStartMs,
            isUserBusy = isUserBusy,
            currentVersionCode = BuildConfig.VERSION_CODE,
            availableUpdate = availableUpdate,
            wordsMastered = wordsMastered,
            latestScore = latestScore
        ) ?: return

        promptsShownThisSession += 1
        markPromptShown(selected, nowMs)
        _activePrompt.value = selected

        trackEvent(
            eventName = "prompt_shown",
            category = "engagement",
            metadata = mapOf(
                "prompt_type" to selected.type.name,
                "trigger_moment" to triggerMoment.name,
                "session_count" to snapshot.totalSessions,
                "milestones" to snapshot.positiveMilestones
            )
        )
    }

    fun handlePromptAction(prompt: EngagementPrompt, action: PromptAction) {
        val nowMs = System.currentTimeMillis()
        val editor = prefs.edit()

        when (prompt.type) {
            PromptType.RATE -> {
                when (action) {
                    PromptAction.ACCEPTED -> {
                        editor.putBoolean(KEY_RATE_COMPLETED_OR_OPTED_OUT, true)
                        openPlayStorePage()
                    }
                    PromptAction.LATER -> {
                        val count = prefs.getInt(KEY_RATE_DISMISS_COUNT, 0) + 1
                        editor.putInt(KEY_RATE_DISMISS_COUNT, count)
                    }
                    PromptAction.NEVER -> {
                        editor.putBoolean(KEY_RATE_COMPLETED_OR_OPTED_OUT, true)
                    }
                }
            }
            PromptType.SHARE -> {
                when (action) {
                    PromptAction.ACCEPTED -> {
                        editor.putLong(KEY_LAST_SHARE_COMPLETED_MS, nowMs)
                        launchShareIntent()
                    }
                    PromptAction.LATER -> {
                        val count = prefs.getInt(KEY_SHARE_DISMISS_COUNT, 0) + 1
                        editor.putInt(KEY_SHARE_DISMISS_COUNT, count)
                    }
                    PromptAction.NEVER -> {
                        editor.putBoolean(KEY_SHARE_OPTED_OUT, true)
                    }
                }
            }
            PromptType.UPDATE -> {
                val updatePrompt = prompt as? EngagementPrompt.UpdatePrompt
                when (action) {
                    PromptAction.ACCEPTED -> {
                        openPlayStorePage()
                    }
                    PromptAction.LATER, PromptAction.NEVER -> {
                        if (updatePrompt != null) {
                            val prevVersion = prefs.getInt(KEY_SNOOZED_UPDATE_VERSION, 0)
                            val count = if (prevVersion == updatePrompt.versionCode) {
                                prefs.getInt(KEY_UPDATE_DISMISS_COUNT, 0) + 1
                            } else {
                                1
                            }
                            editor.putInt(KEY_SNOOZED_UPDATE_VERSION, updatePrompt.versionCode)
                            editor.putInt(KEY_UPDATE_DISMISS_COUNT, if (action == PromptAction.NEVER) 99 else count)
                        }
                    }
                }
            }
            PromptType.DONATION -> {
                when (action) {
                    PromptAction.ACCEPTED -> {
                        _openDonationSheetRequested.value = true
                    }
                    PromptAction.LATER -> {
                        val count = prefs.getInt(KEY_DONATION_DISMISS_COUNT, 0) + 1
                        editor.putInt(KEY_DONATION_DISMISS_COUNT, count)
                    }
                    PromptAction.NEVER -> {
                        editor.putBoolean(KEY_DONATION_OPTED_OUT, true)
                    }
                }
            }
        }

        editor.apply()
        _activePrompt.value = null
        refreshDonationBannerState()

        trackEvent(
            eventName = "prompt_action",
            category = "engagement",
            metadata = mapOf(
                "prompt_type" to prompt.type.name,
                "action" to action.name
            )
        )
    }

    fun launchShareIntent() {
        trackEvent(eventName = "share_intent_launched", category = "engagement")
        val pkg = appContext.packageName
        val url = "https://play.google.com/store/apps/details?id=$pkg"
        val shareText = appContext.getString(R.string.share_app_text, url)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appContext.getString(R.string.share_app_subject))
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(intent, appContext.getString(R.string.share_app_chooser)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(chooser)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to launch share chooser", e)
        }
    }

    fun launchMilestoneShareCard(
        headline: String,
        phraseText: String,
        phoneticText: String,
        score: Int?
    ) {
        val streakDays = _dailyStreakState.value.currentStreak
        trackEvent(
            eventName = "milestone_share_card_launched",
            category = "engagement",
            metadata = mapOf("score" to score, "streak" to streakDays)
        )
        val pkg = appContext.packageName
        val url = "https://play.google.com/store/apps/details?id=$pkg"
        val shareText = appContext.getString(R.string.share_app_text, url)
        val imageUri = MilestoneShareCardGenerator.generateAndGetUri(
            context = appContext,
            headline = headline,
            phraseText = phraseText,
            phoneticText = phoneticText,
            score = score,
            streakDays = streakDays
        )
        val sendIntent = if (imageUri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_SUBJECT, appContext.getString(R.string.share_app_subject))
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, appContext.getString(R.string.share_app_subject))
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
        }
        val chooser = Intent.createChooser(
            sendIntent,
            appContext.getString(R.string.share_card_chooser)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(chooser)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to launch milestone card share chooser", e)
        }
    }

    fun openPlayStorePage() {
        trackEvent(eventName = "play_store_opened", category = "engagement")
        val pkg = appContext.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(marketIntent)
        } catch (_: Exception) {
            try {
                appContext.startActivity(webIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to open Play Store listing", e)
            }
        }
    }

    private fun markPromptShown(prompt: EngagementPrompt, nowMs: Long) {
        val editor = prefs.edit().putLong(KEY_LAST_ANY_PROMPT_MS, nowMs)
        when (prompt.type) {
            PromptType.RATE -> editor.putLong(KEY_LAST_RATE_PROMPT_MS, nowMs)
            PromptType.SHARE -> editor.putLong(KEY_LAST_SHARE_PROMPT_MS, nowMs)
            PromptType.UPDATE -> editor.putLong(KEY_LAST_UPDATE_PROMPT_MS, nowMs)
            PromptType.DONATION -> editor.putLong(KEY_LAST_DONATION_PROMPT_MS, nowMs)
        }
        editor.apply()
    }

    private fun readSnapshot(): EngagementSnapshot {
        return EngagementSnapshot(
            totalSessions = prefs.getInt(KEY_TOTAL_SESSIONS, 1),
            positiveMilestones = prefs.getInt(KEY_POSITIVE_MILESTONES, 0),
            promptsShownThisSession = promptsShownThisSession,
            lastAnyPromptShownMs = prefs.getLong(KEY_LAST_ANY_PROMPT_MS, 0L),
            lastRatePromptShownMs = prefs.getLong(KEY_LAST_RATE_PROMPT_MS, 0L),
            rateDismissCount = prefs.getInt(KEY_RATE_DISMISS_COUNT, 0),
            rateCompletedOrOptedOut = prefs.getBoolean(KEY_RATE_COMPLETED_OR_OPTED_OUT, false),
            lastSharePromptShownMs = prefs.getLong(KEY_LAST_SHARE_PROMPT_MS, 0L),
            shareDismissCount = prefs.getInt(KEY_SHARE_DISMISS_COUNT, 0),
            lastShareCompletedMs = prefs.getLong(KEY_LAST_SHARE_COMPLETED_MS, 0L),
            shareOptedOut = prefs.getBoolean(KEY_SHARE_OPTED_OUT, false),
            lastUpdatePromptShownMs = prefs.getLong(KEY_LAST_UPDATE_PROMPT_MS, 0L),
            snoozedUpdateVersionCode = prefs.getInt(KEY_SNOOZED_UPDATE_VERSION, 0),
            updateDismissCountForVersion = prefs.getInt(KEY_UPDATE_DISMISS_COUNT, 0),
            firstInstallMs = prefs.getLong(KEY_FIRST_INSTALL_MS, System.currentTimeMillis()),
            successfulOperations = prefs.getInt(KEY_SUCCESSFUL_OPERATIONS, 0),
            isActiveSupporter = prefs.getBoolean(KEY_IS_ACTIVE_SUPPORTER, false),
            lastDonationPromptShownMs = prefs.getLong(KEY_LAST_DONATION_PROMPT_MS, 0L),
            donationPromptDismissCount = prefs.getInt(KEY_DONATION_DISMISS_COUNT, 0),
            donationPromptOptedOut = prefs.getBoolean(KEY_DONATION_OPTED_OUT, false),
            lastDonationBannerDismissMs = prefs.getLong(KEY_LAST_DONATION_BANNER_DISMISS_MS, 0L)
        )
    }

    private companion object {
        const val KEY_TOTAL_SESSIONS = "total_sessions"
        const val KEY_POSITIVE_MILESTONES = "positive_milestones"
        const val KEY_LAST_ANY_PROMPT_MS = "last_any_prompt_ms"
        const val KEY_LAST_RATE_PROMPT_MS = "last_rate_prompt_ms"
        const val KEY_RATE_DISMISS_COUNT = "rate_dismiss_count"
        const val KEY_RATE_COMPLETED_OR_OPTED_OUT = "rate_completed_or_opted_out"
        const val KEY_LAST_SHARE_PROMPT_MS = "last_share_prompt_ms"
        const val KEY_SHARE_DISMISS_COUNT = "share_dismiss_count"
        const val KEY_LAST_SHARE_COMPLETED_MS = "last_share_completed_ms"
        const val KEY_SHARE_OPTED_OUT = "share_opted_out"
        const val KEY_LAST_UPDATE_PROMPT_MS = "last_update_prompt_ms"
        const val KEY_SNOOZED_UPDATE_VERSION = "snoozed_update_version"
        const val KEY_UPDATE_DISMISS_COUNT = "update_dismiss_count"
        const val KEY_FIRST_INSTALL_MS = "first_install_ms"
        const val KEY_SUCCESSFUL_OPERATIONS = "successful_operations"
        const val KEY_IS_ACTIVE_SUPPORTER = "is_active_supporter"
        const val KEY_LAST_DONATION_PROMPT_MS = "last_donation_prompt_ms"
        const val KEY_DONATION_DISMISS_COUNT = "donation_dismiss_count"
        const val KEY_DONATION_OPTED_OUT = "donation_opted_out"
        const val KEY_LAST_DONATION_BANNER_DISMISS_MS = "last_donation_banner_dismiss_ms"
        const val KEY_DAILY_GOAL_DAY = "daily_goal_day"
        const val KEY_DAILY_GOAL_COUNT = "daily_goal_count"
        const val KEY_STREAK_COUNT = "daily_streak_count"
        const val KEY_STREAK_BEST = "daily_streak_best"
        const val KEY_STREAK_LAST_DAY = "daily_streak_last_day"
        const val KEY_STREAK_FREEZES = "daily_streak_freezes"
    }

    private fun todayDayStamp(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun readDailyGoalPair(todayDay: String): Pair<Int, Int> {
        val count = EngagementDecisionEngine.computeDailyGoalReadState(
            storedDay = prefs.getString(KEY_DAILY_GOAL_DAY, null),
            storedCount = prefs.getInt(KEY_DAILY_GOAL_COUNT, 0),
            todayDay = todayDay
        )
        return count to EngagementDecisionEngine.DEFAULT_DAILY_DOJO_GOAL
    }

    private fun readDailyStreakState(todayDay: String): DailyStreakState {
        return EngagementDecisionEngine.computeDailyStreakReadState(
            lastPracticeDay = prefs.getString(KEY_STREAK_LAST_DAY, null),
            storedStreak = prefs.getInt(KEY_STREAK_COUNT, 0),
            storedBestStreak = prefs.getInt(KEY_STREAK_BEST, 0),
            todayDay = todayDay,
            storedFreezes = prefs.getInt(KEY_STREAK_FREEZES, 0)
        )
    }
}
