package com.example.notifications

import android.content.Context

object StreakReminderPreferences {
    private const val PREFS_NAME = "pocket_sensei_engagement_prefs"
    private const val KEY_REMINDER_ENABLED = "streak_reminder_enabled"
    private const val KEY_REMINDER_HOUR = "streak_reminder_hour"
    private const val KEY_REMINDER_MINUTE = "streak_reminder_minute"

    fun isEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REMINDER_ENABLED, false)
    }

    fun getReminderHour(context: Context): Int {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_REMINDER_HOUR, 20)
            .coerceIn(6, 22)
    }

    fun getReminderMinute(context: Context): Int {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_REMINDER_MINUTE, 0)
            .coerceIn(0, 59)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REMINDER_ENABLED, enabled)
            .apply()
    }

    fun setReminderTime(context: Context, hour: Int, minute: Int = 0) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REMINDER_HOUR, hour.coerceIn(6, 22))
            .putInt(KEY_REMINDER_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }
}
