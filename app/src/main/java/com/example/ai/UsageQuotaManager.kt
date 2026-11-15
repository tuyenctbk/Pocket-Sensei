package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "UsageQuotaManager"

enum class QuotaFeature {
    PRONUNCIATION_EVALUATION,
    ROLEPLAY_CHAT,
    TOPIC_PACK
}

data class QuotaSnapshot(
    val evaluationsUsed: Int = 0,
    val chatTurnsUsed: Int = 0,
    val topicPacksUsed: Int = 0,
    val bonusGraceCredits: Int = 0,
    val graceClaimed: Boolean = false,
    val ecoLiteMode: Boolean = false,
    val fallbackCascadeCount: Int = 0,
    val dateIso: String = "",
    val isSignedIn: Boolean = false,
    val isPro: Boolean = false,
    val remainingEvaluations: Int = 10,
    val remainingChatTurns: Int = 6,
    val remainingTopicPacks: Int = 1,
    val evaluationUsagePercent: Float = 0f,
    val resetCountdownFormatted: String = ""
)

data class ValueReceiptSnapshot(
    val evaluationsCount: Int,
    val chatTurnsCount: Int,
    val topicPacksCount: Int,
    val bonusCreditsUsed: Int,
    val estimatedTokensSaved: Long
)

class UsageQuotaManager(
    context: Context,
    private val firestore: FirebaseFirestore? = null,
    private val currentUserIdProvider: (() -> String?)? = null
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _quotaState = MutableStateFlow(loadLocalSnapshot())
    val quotaState: StateFlow<QuotaSnapshot> = _quotaState.asStateFlow()

    init {
        checkAndApplyMidnightRollover()
        if (currentUserIdProvider?.invoke() != null) {
            refreshFromFirestore()
        }
    }

    /**
     * Checks if current calendar date differs from stored dateIso.
     * Resets daily counters and graceClaimed while preserving user preferences like ecoLiteMode.
     */
    @Synchronized
    fun checkAndApplyMidnightRollover(): Boolean {
        val today = currentDayIso()
        val storedDay = prefs.getString(KEY_DATE_ISO, "")
        if (storedDay != today) {
            val preservedEcoMode = prefs.getBoolean(KEY_ECO_LITE_MODE, false)
            prefs.edit()
                .putString(KEY_DATE_ISO, today)
                .putInt(KEY_EVALUATIONS_USED, 0)
                .putInt(KEY_CHAT_USED, 0)
                .putInt(KEY_TOPIC_USED, 0)
                .putInt(KEY_BONUS_GRACE_CREDITS, 0)
                .putInt(KEY_GRACE_CREDITS_SPENT_TODAY, 0)
                .putBoolean(KEY_GRACE_CLAIMED, false)
                .putInt(KEY_FALLBACK_CASCADE_COUNT, 0)
                .putBoolean(KEY_ECO_LITE_MODE, preservedEcoMode)
                .apply()
            val fresh = loadLocalSnapshot()
            _quotaState.value = fresh
            syncToFirestoreAsync(fresh)
            return true
        }
        return false
    }

    fun updateAuthStatus(isSignedIn: Boolean, isPro: Boolean) {
        val current = _quotaState.value
        val wasSignedIn = current.isSignedIn
        if (current.isSignedIn != isSignedIn || current.isPro != isPro) {
            prefs.edit()
                .putBoolean(KEY_IS_SIGNED_IN, isSignedIn)
                .putBoolean(KEY_IS_PRO, isPro)
                .apply()
            _quotaState.value = loadLocalSnapshot()
            if (isSignedIn && !wasSignedIn) {
                refreshFromFirestore()
            }
        }
    }

    fun refreshFromFirestore() {
        val uid = currentUserIdProvider?.invoke() ?: return
        val db = firestore ?: return
        scope.launch {
            try {
                val doc = db.collection("users")
                    .document(uid)
                    .collection("usageQuota")
                    .document("daily")
                    .get()
                    .await()
                if (!doc.exists()) {
                    syncToFirestoreAsync(_quotaState.value)
                    return@launch
                }
                val remoteDate = doc.getString("dateIso") ?: ""
                val today = currentDayIso()
                val editor = prefs.edit()
                if (doc.contains("ecoLiteMode")) {
                    val remoteEco = doc.getBoolean("ecoLiteMode") ?: false
                    if (remoteEco || !prefs.contains(KEY_ECO_LITE_MODE)) {
                        editor.putBoolean(KEY_ECO_LITE_MODE, remoteEco)
                    }
                }
                if (remoteDate == today) {
                    val remoteEval = (doc.getLong("evaluationsUsed") ?: 0L).toInt().coerceAtLeast(0)
                    val remoteChat = (doc.getLong("chatTurnsUsed") ?: 0L).toInt().coerceAtLeast(0)
                    val remoteTopic = (doc.getLong("topicPacksUsed") ?: 0L).toInt().coerceAtLeast(0)
                    val remoteCascades = (doc.getLong("fallbackCascadeCount") ?: 0L).toInt().coerceAtLeast(0)
                    val remoteGraceClaimed = doc.getBoolean("graceClaimed") ?: false
                    val remoteBonus = (doc.getLong("bonusGraceCredits") ?: 0L).toInt().coerceIn(0, 5)

                    val localGraceClaimed = prefs.getBoolean(KEY_GRACE_CLAIMED, false)
                    val mergedGraceClaimed = localGraceClaimed || remoteGraceClaimed
                    val mergedBonus = when {
                        localGraceClaimed && remoteGraceClaimed ->
                            minOf(prefs.getInt(KEY_BONUS_GRACE_CREDITS, 0), remoteBonus)
                        remoteGraceClaimed -> remoteBonus
                        else -> prefs.getInt(KEY_BONUS_GRACE_CREDITS, 0)
                    }

                    editor.putString(KEY_DATE_ISO, today)
                        .putInt(KEY_EVALUATIONS_USED, maxOf(prefs.getInt(KEY_EVALUATIONS_USED, 0), remoteEval))
                        .putInt(KEY_CHAT_USED, maxOf(prefs.getInt(KEY_CHAT_USED, 0), remoteChat))
                        .putInt(KEY_TOPIC_USED, maxOf(prefs.getInt(KEY_TOPIC_USED, 0), remoteTopic))
                        .putInt(KEY_FALLBACK_CASCADE_COUNT, maxOf(prefs.getInt(KEY_FALLBACK_CASCADE_COUNT, 0), remoteCascades))
                        .putBoolean(KEY_GRACE_CLAIMED, mergedGraceClaimed)
                        .putInt(KEY_BONUS_GRACE_CREDITS, mergedBonus)
                }
                editor.apply()
                _quotaState.value = loadLocalSnapshot()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, "users/$uid/usageQuota/daily")
            }
        }
    }

    fun canConsume(feature: QuotaFeature): Boolean {
        checkAndApplyMidnightRollover()
        val snapshot = _quotaState.value
        if (snapshot.isPro) return true
        return getRemainingAllowance(feature) > 0
    }

    @Synchronized
    fun deductInteraction(feature: QuotaFeature): Boolean {
        checkAndApplyMidnightRollover()
        val snapshot = _quotaState.value
        if (snapshot.isPro) {
            // Pro users have unlimited access, still record for value receipt
            incrementCounterOnly(feature)
            return true
        }

        val remaining = getRemainingAllowance(feature)
        if (remaining <= 0) {
            return false
        }

        val editor = prefs.edit()
        var grace = prefs.getInt(KEY_BONUS_GRACE_CREDITS, 0)
        if (grace > 0) {
            grace -= 1
            editor.putInt(KEY_BONUS_GRACE_CREDITS, grace)
            editor.putInt(
                KEY_GRACE_CREDITS_SPENT_TODAY,
                prefs.getInt(KEY_GRACE_CREDITS_SPENT_TODAY, 0) + 1
            )
        } else {
            when (feature) {
                QuotaFeature.PRONUNCIATION_EVALUATION -> {
                    val count = prefs.getInt(KEY_EVALUATIONS_USED, 0) + 1
                    editor.putInt(KEY_EVALUATIONS_USED, count)
                }
                QuotaFeature.ROLEPLAY_CHAT -> {
                    val count = prefs.getInt(KEY_CHAT_USED, 0) + 1
                    editor.putInt(KEY_CHAT_USED, count)
                }
                QuotaFeature.TOPIC_PACK -> {
                    val count = prefs.getInt(KEY_TOPIC_USED, 0) + 1
                    editor.putInt(KEY_TOPIC_USED, count)
                }
            }
        }
        editor.apply()

        val updated = loadLocalSnapshot()
        _quotaState.value = updated
        syncToFirestoreAsync(updated)
        return true
    }

    /**
     * Refunds quota when an AI interaction times out or fails with a 5xx/network error.
     * Prevents users from losing free credits to transient network issues.
     */
    @Synchronized
    fun refundInteraction(feature: QuotaFeature) {
        checkAndApplyMidnightRollover()
        val editor = prefs.edit()
        val graceSpent = prefs.getInt(KEY_GRACE_CREDITS_SPENT_TODAY, 0)
        val featureCount = when (feature) {
            QuotaFeature.PRONUNCIATION_EVALUATION -> prefs.getInt(KEY_EVALUATIONS_USED, 0)
            QuotaFeature.ROLEPLAY_CHAT -> prefs.getInt(KEY_CHAT_USED, 0)
            QuotaFeature.TOPIC_PACK -> prefs.getInt(KEY_TOPIC_USED, 0)
        }
        if (graceSpent > 0) {
            val currentGrace = prefs.getInt(KEY_BONUS_GRACE_CREDITS, 0)
            editor.putInt(KEY_BONUS_GRACE_CREDITS, (currentGrace + 1).coerceAtMost(5))
            editor.putInt(KEY_GRACE_CREDITS_SPENT_TODAY, (graceSpent - 1).coerceAtLeast(0))
        } else if (featureCount > 0) {
            when (feature) {
                QuotaFeature.PRONUNCIATION_EVALUATION -> editor.putInt(KEY_EVALUATIONS_USED, featureCount - 1)
                QuotaFeature.ROLEPLAY_CHAT -> editor.putInt(KEY_CHAT_USED, featureCount - 1)
                QuotaFeature.TOPIC_PACK -> editor.putInt(KEY_TOPIC_USED, featureCount - 1)
            }
        }
        editor.apply()
        val updated = loadLocalSnapshot()
        _quotaState.value = updated
        syncToFirestoreAsync(updated)
    }

    /**
     * Atomic deduct-with-refund execution wrapper:
     * Reserves 1 quota credit before calling [cloudAction] to prevent concurrent double-tap bypasses,
     * and automatically refunds the credit if [cloudAction] fails, times out, or is cancelled.
     */
    suspend fun <T> withAtomicQuotaOrFallback(
        feature: QuotaFeature,
        onQuotaExhausted: suspend () -> T,
        onErrorFallback: suspend (Throwable) -> T,
        cloudAction: suspend () -> T
    ): T {
        if (!deductInteraction(feature)) {
            recordFallbackCascade()
            return onQuotaExhausted()
        }
        return try {
            cloudAction()
        } catch (e: CancellationException) {
            refundInteraction(feature)
            if (e is TimeoutCancellationException) {
                recordFallbackCascade()
                onErrorFallback(e)
            } else {
                throw e
            }
        } catch (e: Exception) {
            refundInteraction(feature)
            recordFallbackCascade()
            onErrorFallback(e)
        }
    }

    /**
     * Claim +3 Free Grace Credits (once per day) to allow the user to finish mid-task practice.
     */
    @Synchronized
    fun claimDailyGraceCredits(): Boolean {
        checkAndApplyMidnightRollover()
        val alreadyClaimed = prefs.getBoolean(KEY_GRACE_CLAIMED, false)
        if (alreadyClaimed) return false

        val currentGrace = prefs.getInt(KEY_BONUS_GRACE_CREDITS, 0)
        prefs.edit()
            .putBoolean(KEY_GRACE_CLAIMED, true)
            .putInt(KEY_BONUS_GRACE_CREDITS, (currentGrace + 3).coerceAtMost(5))
            .apply()

        val updated = loadLocalSnapshot()
        _quotaState.value = updated
        syncToFirestoreAsync(updated)
        return true
    }

    fun toggleEcoLiteMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ECO_LITE_MODE, enabled).apply()
        val updated = loadLocalSnapshot()
        _quotaState.value = updated
        syncToFirestoreAsync(updated)
    }

    fun recordFallbackCascade() {
        val current = prefs.getInt(KEY_FALLBACK_CASCADE_COUNT, 0)
        prefs.edit().putInt(KEY_FALLBACK_CASCADE_COUNT, current + 1).apply()
        val updated = loadLocalSnapshot()
        _quotaState.value = updated
        syncToFirestoreAsync(updated)
    }

    fun getRemainingAllowance(feature: QuotaFeature): Int {
        val snapshot = _quotaState.value
        if (snapshot.isPro) return 999
        val grace = snapshot.bonusGraceCredits
        val (cap, used) = when (feature) {
            QuotaFeature.PRONUNCIATION_EVALUATION -> getCapForEval(snapshot) to snapshot.evaluationsUsed
            QuotaFeature.ROLEPLAY_CHAT -> getCapForChat(snapshot) to snapshot.chatTurnsUsed
            QuotaFeature.TOPIC_PACK -> getCapForTopic(snapshot) to snapshot.topicPacksUsed
        }
        val remainingStandard = (cap - used).coerceAtLeast(0)
        return remainingStandard + grace
    }

    fun getUsagePercent(feature: QuotaFeature): Float {
        val snapshot = _quotaState.value
        if (snapshot.isPro) return 0.1f
        val (cap, used) = when (feature) {
            QuotaFeature.PRONUNCIATION_EVALUATION -> getCapForEval(snapshot) to snapshot.evaluationsUsed
            QuotaFeature.ROLEPLAY_CHAT -> getCapForChat(snapshot) to snapshot.chatTurnsUsed
            QuotaFeature.TOPIC_PACK -> getCapForTopic(snapshot) to snapshot.topicPacksUsed
        }
        if (cap <= 0) return 1f
        return (used.toFloat() / cap.toFloat()).coerceIn(0f, 1f)
    }

    fun isApproachingLimit(feature: QuotaFeature = QuotaFeature.PRONUNCIATION_EVALUATION): Boolean {
        val pct = getUsagePercent(feature)
        val remaining = getRemainingAllowance(feature)
        return pct >= AiModelConfig.ECO_LITE_THRESHOLD_PERCENT && remaining > 0
    }

    fun isLimitReached(feature: QuotaFeature = QuotaFeature.PRONUNCIATION_EVALUATION): Boolean {
        return getRemainingAllowance(feature) <= 0
    }

    fun resolveCurrentTier(feature: QuotaFeature = QuotaFeature.PRONUNCIATION_EVALUATION): AiModelConfig.AiCapabilityTier {
        val snapshot = _quotaState.value
        val usagePct = getUsagePercent(feature)
        val remaining = getRemainingAllowance(feature)
        return AiModelConfig.resolveTier(
            usagePercent = usagePct,
            ecoLiteMode = snapshot.ecoLiteMode,
            isRateLimited = false,
            isOfflineOrZeroQuota = remaining <= 0
        )
    }

    fun getValueReceipt(): ValueReceiptSnapshot {
        val snapshot = _quotaState.value
        val estimatedTokens = (snapshot.evaluationsUsed * 850L) +
                (snapshot.chatTurnsUsed * 600L) +
                (snapshot.topicPacksUsed * 1400L)
        val graceSpent = prefs.getInt(KEY_GRACE_CREDITS_SPENT_TODAY, 0)
        return ValueReceiptSnapshot(
            evaluationsCount = snapshot.evaluationsUsed,
            chatTurnsCount = snapshot.chatTurnsUsed,
            topicPacksCount = snapshot.topicPacksUsed,
            bonusCreditsUsed = graceSpent.coerceAtLeast(
                if (snapshot.graceClaimed) (3 - snapshot.bonusGraceCredits).coerceAtLeast(0) else 0
            ),
            estimatedTokensSaved = estimatedTokens
        )
    }

    fun getResetCountdownMillis(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (cal.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun getResetCountdownFormatted(): String {
        val millis = getResetCountdownMillis()
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun incrementCounterOnly(feature: QuotaFeature) {
        val editor = prefs.edit()
        when (feature) {
            QuotaFeature.PRONUNCIATION_EVALUATION -> editor.putInt(KEY_EVALUATIONS_USED, prefs.getInt(KEY_EVALUATIONS_USED, 0) + 1)
            QuotaFeature.ROLEPLAY_CHAT -> editor.putInt(KEY_CHAT_USED, prefs.getInt(KEY_CHAT_USED, 0) + 1)
            QuotaFeature.TOPIC_PACK -> editor.putInt(KEY_TOPIC_USED, prefs.getInt(KEY_TOPIC_USED, 0) + 1)
        }
        editor.apply()
        val updated = loadLocalSnapshot()
        _quotaState.value = updated
        syncToFirestoreAsync(updated)
    }

    private fun loadLocalSnapshot(): QuotaSnapshot {
        val today = currentDayIso()
        val storedDate = prefs.getString(KEY_DATE_ISO, today) ?: today
        val isToday = storedDate == today
        val evalUsed = if (isToday) prefs.getInt(KEY_EVALUATIONS_USED, 0) else 0
        val chatUsed = if (isToday) prefs.getInt(KEY_CHAT_USED, 0) else 0
        val topicUsed = if (isToday) prefs.getInt(KEY_TOPIC_USED, 0) else 0
        val bonus = if (isToday) prefs.getInt(KEY_BONUS_GRACE_CREDITS, 0) else 0
        val graceClaimed = if (isToday) prefs.getBoolean(KEY_GRACE_CLAIMED, false) else false
        val cascades = if (isToday) prefs.getInt(KEY_FALLBACK_CASCADE_COUNT, 0) else 0
        val eco = prefs.getBoolean(KEY_ECO_LITE_MODE, false)
        val signedIn = prefs.getBoolean(KEY_IS_SIGNED_IN, false)
        val pro = prefs.getBoolean(KEY_IS_PRO, false)

        val capEval = if (signedIn) SIGNED_IN_CAP_EVALUATIONS else GUEST_CAP_EVALUATIONS
        val capChat = if (signedIn) SIGNED_IN_CAP_CHAT else GUEST_CAP_CHAT
        val capTopic = if (signedIn) SIGNED_IN_CAP_TOPICS else GUEST_CAP_TOPICS

        val remEval = if (pro) 999 else (capEval - evalUsed).coerceAtLeast(0) + bonus
        val remChat = if (pro) 999 else (capChat - chatUsed).coerceAtLeast(0) + bonus
        val remTopic = if (pro) 999 else (capTopic - topicUsed).coerceAtLeast(0) + bonus

        val usagePct = if (capEval > 0) (evalUsed.toFloat() / capEval.toFloat()).coerceIn(0f, 1f) else 0f

        return QuotaSnapshot(
            evaluationsUsed = evalUsed,
            chatTurnsUsed = chatUsed,
            topicPacksUsed = topicUsed,
            bonusGraceCredits = bonus,
            graceClaimed = graceClaimed,
            ecoLiteMode = eco,
            fallbackCascadeCount = cascades,
            dateIso = today,
            isSignedIn = signedIn,
            isPro = pro,
            remainingEvaluations = remEval,
            remainingChatTurns = remChat,
            remainingTopicPacks = remTopic,
            evaluationUsagePercent = usagePct,
            resetCountdownFormatted = getResetCountdownFormatted()
        )
    }

    suspend fun syncNowToFirestore(snapshot: QuotaSnapshot = _quotaState.value): Result<Unit> {
        val uid = currentUserIdProvider?.invoke()
            ?: return Result.failure(IllegalStateException("User must be signed in to sync quota."))
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore instance not configured."))
        val path = "users/$uid/usageQuota/daily"
        return try {
            val data = mapOf(
                "userId" to uid,
                "evaluationsUsed" to snapshot.evaluationsUsed,
                "chatTurnsUsed" to snapshot.chatTurnsUsed,
                "topicPacksUsed" to snapshot.topicPacksUsed,
                "bonusGraceCredits" to snapshot.bonusGraceCredits,
                "graceClaimed" to snapshot.graceClaimed,
                "ecoLiteMode" to snapshot.ecoLiteMode,
                "fallbackCascadeCount" to snapshot.fallbackCascadeCount,
                "dateIso" to snapshot.dateIso,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users")
                .document(uid)
                .collection("usageQuota")
                .document("daily")
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun syncToFirestoreAsync(snapshot: QuotaSnapshot) {
        if (currentUserIdProvider?.invoke() == null || firestore == null) return
        scope.launch {
            syncNowToFirestore(snapshot)
        }
    }

    private fun getCapForEval(snapshot: QuotaSnapshot) = if (snapshot.isSignedIn) SIGNED_IN_CAP_EVALUATIONS else GUEST_CAP_EVALUATIONS
    private fun getCapForChat(snapshot: QuotaSnapshot) = if (snapshot.isSignedIn) SIGNED_IN_CAP_CHAT else GUEST_CAP_CHAT
    private fun getCapForTopic(snapshot: QuotaSnapshot) = if (snapshot.isSignedIn) SIGNED_IN_CAP_TOPICS else GUEST_CAP_TOPICS

    private fun currentDayIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    companion object {
        const val GUEST_CAP_EVALUATIONS = 10
        const val GUEST_CAP_CHAT = 6
        const val GUEST_CAP_TOPICS = 1

        const val SIGNED_IN_CAP_EVALUATIONS = 25
        const val SIGNED_IN_CAP_CHAT = 15
        const val SIGNED_IN_CAP_TOPICS = 3

        private const val PREFS_NAME = "pocket_sensei_usage_quota"
        private const val KEY_DATE_ISO = "date_iso"
        private const val KEY_EVALUATIONS_USED = "evaluations_used"
        private const val KEY_CHAT_USED = "chat_used"
        private const val KEY_TOPIC_USED = "topic_used"
        private const val KEY_BONUS_GRACE_CREDITS = "bonus_grace_credits"
        private const val KEY_GRACE_CLAIMED = "grace_claimed"
        private const val KEY_ECO_LITE_MODE = "eco_lite_mode"
        private const val KEY_FALLBACK_CASCADE_COUNT = "fallback_cascade_count"
        private const val KEY_IS_SIGNED_IN = "is_signed_in"
        private const val KEY_IS_PRO = "is_pro"
        private const val KEY_GRACE_CREDITS_SPENT_TODAY = "grace_credits_spent_today"
    }
}
