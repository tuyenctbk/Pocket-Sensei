package com.example.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AiCoachingMode(val label: String, val description: String) {
    HYBRID_FREE(
        label = "Smart Hybrid",
        description = "Room cache + daily free cloud AI, with instant offline fallback"
    ),
    LOCAL_OFFLINE_ONLY(
        label = "100% Offline ($0)",
        description = "Pure on-device phonetic & pitch matching with zero network usage"
    ),
    BYOK_USER_KEY(
        label = "My Gemini Key (BYOK)",
        description = "Unlimited AI coaching funded by your own personal Gemini API key"
    )
}

data class AiSettingsSnapshot(
    val mode: AiCoachingMode = AiCoachingMode.HYBRID_FREE,
    val hasByokKey: Boolean = false,
    val dailyCloudCallsUsed: Int = 0,
    val dailyFreeCloudCap: Int = AiSettingsManager.DEFAULT_DAILY_FREE_CAP,
    val ecoLiteMode: Boolean = false,
    val graceCredits: Int = 0,
    val currentTier: AiModelConfig.AiCapabilityTier = AiModelConfig.AiCapabilityTier.TIER_1_PRIMARY
)

class AiSettingsManager(
    context: Context,
    val quotaManager: UsageQuotaManager? = null
) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val usageQuotaManager: UsageQuotaManager by lazy {
        quotaManager ?: UsageQuotaManager(appContext)
    }

    private val _settings = MutableStateFlow(readSnapshot())
    val settings: StateFlow<AiSettingsSnapshot> = _settings.asStateFlow()

    fun setMode(mode: AiCoachingMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _settings.value = readSnapshot()
    }

    fun saveUserApiKey(rawKey: String) {
        val trimmed = rawKey.trim()
        val editor = prefs.edit()
        if (trimmed.isEmpty()) {
            editor.remove(KEY_USER_API_KEY)
            if (getCurrentMode() == AiCoachingMode.BYOK_USER_KEY) {
                editor.putString(KEY_MODE, AiCoachingMode.HYBRID_FREE.name)
            }
        } else {
            editor.putString(KEY_USER_API_KEY, trimmed)
            editor.putString(KEY_MODE, AiCoachingMode.BYOK_USER_KEY.name)
        }
        editor.apply()
        _settings.value = readSnapshot()
    }

    fun getUserApiKey(): String = prefs.getString(KEY_USER_API_KEY, "").orEmpty()

    fun getCurrentMode(): AiCoachingMode {
        val saved = prefs.getString(KEY_MODE, AiCoachingMode.HYBRID_FREE.name)
        return runCatching { AiCoachingMode.valueOf(saved ?: AiCoachingMode.HYBRID_FREE.name) }
            .getOrDefault(AiCoachingMode.HYBRID_FREE)
    }

    fun canUseFreeCloudQuota(): Boolean {
        if (getCurrentMode() == AiCoachingMode.LOCAL_OFFLINE_ONLY) return false
        return usageQuotaManager.canConsume(QuotaFeature.PRONUNCIATION_EVALUATION)
    }

    @Synchronized
    fun tryConsumeCloudQuota(feature: QuotaFeature = QuotaFeature.PRONUNCIATION_EVALUATION): Boolean {
        return when (getCurrentMode()) {
            AiCoachingMode.LOCAL_OFFLINE_ONLY -> false
            AiCoachingMode.BYOK_USER_KEY -> true
            AiCoachingMode.HYBRID_FREE -> {
                val deducted = usageQuotaManager.deductInteraction(feature)
                if (deducted && feature == QuotaFeature.PRONUNCIATION_EVALUATION) {
                    val dayKey = currentDayKey()
                    val updated = getDailyCloudCount() + 1
                    prefs.edit()
                        .putString(KEY_CLOUD_USAGE_DAY, dayKey)
                        .putInt(KEY_CLOUD_USAGE_COUNT, updated)
                        .apply()
                }
                _settings.value = readSnapshot()
                deducted
            }
        }
    }

    @Synchronized
    fun refundCloudQuota(feature: QuotaFeature = QuotaFeature.PRONUNCIATION_EVALUATION) {
        if (getCurrentMode() != AiCoachingMode.HYBRID_FREE) return
        if (feature == QuotaFeature.PRONUNCIATION_EVALUATION) {
            val count = getDailyCloudCount()
            if (count > 0) {
                prefs.edit().putInt(KEY_CLOUD_USAGE_COUNT, count - 1).apply()
            }
        }
        usageQuotaManager.refundInteraction(feature)
        _settings.value = readSnapshot()
    }

    fun incrementDailyCloudUsage() {
        val dayKey = currentDayKey()
        val updated = getDailyCloudCount() + 1
        prefs.edit()
            .putString(KEY_CLOUD_USAGE_DAY, dayKey)
            .putInt(KEY_CLOUD_USAGE_COUNT, updated)
            .apply()
        usageQuotaManager.deductInteraction(QuotaFeature.PRONUNCIATION_EVALUATION)
        _settings.value = readSnapshot()
    }

    fun refundDailyCloudUsage() {
        val count = getDailyCloudCount()
        if (count > 0) {
            prefs.edit().putInt(KEY_CLOUD_USAGE_COUNT, count - 1).apply()
        }
        usageQuotaManager.refundInteraction(QuotaFeature.PRONUNCIATION_EVALUATION)
        _settings.value = readSnapshot()
    }

    fun toggleEcoLiteMode(enabled: Boolean) {
        usageQuotaManager.toggleEcoLiteMode(enabled)
        _settings.value = readSnapshot()
    }

    fun claimGraceCredits(): Boolean {
        val success = usageQuotaManager.claimDailyGraceCredits()
        if (success) {
            _settings.value = readSnapshot()
        }
        return success
    }

    fun refreshSettings() {
        _settings.value = readSnapshot()
    }

    private fun getDailyCloudCount(): Int {
        val savedDay = prefs.getString(KEY_CLOUD_USAGE_DAY, "")
        return if (savedDay == currentDayKey()) {
            prefs.getInt(KEY_CLOUD_USAGE_COUNT, 0)
        } else {
            0
        }
    }

    private fun currentDayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun readSnapshot(): AiSettingsSnapshot {
        val quotaSnap = usageQuotaManager.quotaState.value
        val activeBaseCap = if (quotaSnap.isSignedIn) {
            UsageQuotaManager.SIGNED_IN_CAP_EVALUATIONS
        } else {
            UsageQuotaManager.GUEST_CAP_EVALUATIONS
        }
        val tier = when (getCurrentMode()) {
            AiCoachingMode.LOCAL_OFFLINE_ONLY -> AiModelConfig.AiCapabilityTier.TIER_3_ZERO_COST_LOCAL
            AiCoachingMode.BYOK_USER_KEY -> AiModelConfig.AiCapabilityTier.TIER_1_PRIMARY
            AiCoachingMode.HYBRID_FREE -> usageQuotaManager.resolveCurrentTier(QuotaFeature.PRONUNCIATION_EVALUATION)
        }
        return AiSettingsSnapshot(
            mode = getCurrentMode(),
            hasByokKey = getUserApiKey().isNotBlank(),
            dailyCloudCallsUsed = maxOf(getDailyCloudCount(), quotaSnap.evaluationsUsed),
            dailyFreeCloudCap = activeBaseCap + quotaSnap.bonusGraceCredits,
            ecoLiteMode = quotaSnap.ecoLiteMode,
            graceCredits = quotaSnap.bonusGraceCredits,
            currentTier = tier
        )
    }

    companion object {
        const val DEFAULT_DAILY_FREE_CAP = 15
        private const val PREFS_NAME = "pocket_sensei_ai_settings"
        private const val KEY_MODE = "ai_coaching_mode"
        private const val KEY_USER_API_KEY = "user_byok_gemini_key"
        private const val KEY_CLOUD_USAGE_DAY = "cloud_usage_day"
        private const val KEY_CLOUD_USAGE_COUNT = "cloud_usage_count"
    }
}
