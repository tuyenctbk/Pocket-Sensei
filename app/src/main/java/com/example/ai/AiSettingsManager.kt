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
    val dailyFreeCloudCap: Int = AiSettingsManager.DEFAULT_DAILY_FREE_CAP
)

class AiSettingsManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        return getDailyCloudCount() < DEFAULT_DAILY_FREE_CAP
    }

    fun incrementDailyCloudUsage() {
        val dayKey = currentDayKey()
        val updated = getDailyCloudCount() + 1
        prefs.edit()
            .putString(KEY_CLOUD_USAGE_DAY, dayKey)
            .putInt(KEY_CLOUD_USAGE_COUNT, updated)
            .apply()
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
        return AiSettingsSnapshot(
            mode = getCurrentMode(),
            hasByokKey = getUserApiKey().isNotBlank(),
            dailyCloudCallsUsed = getDailyCloudCount(),
            dailyFreeCloudCap = DEFAULT_DAILY_FREE_CAP
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
