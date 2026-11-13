package com.example.notifications

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val TAG = "ReminderScheduler"

object ReminderScheduler {

    const val UNIQUE_WORK_NAME = "pocket_sensei_daily_streak_reminder"

    fun computeInitialDelayMs(
        targetHour: Int,
        targetMinute: Int = 0,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val nextCal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, targetHour.coerceIn(0, 23))
            set(Calendar.MINUTE, targetMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowCal.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (nextCal.timeInMillis - nowCal.timeInMillis).coerceAtLeast(60_000L)
    }

    fun scheduleEveningReminder(
        context: Context,
        hourOfDay: Int = StreakReminderPreferences.getReminderHour(context),
        minute: Int = StreakReminderPreferences.getReminderMinute(context)
    ) {
        StreakReminderPreferences.setEnabled(context, true)
        StreakReminderPreferences.setReminderTime(context, hourOfDay, minute)

        val delayMs = computeInitialDelayMs(hourOfDay, minute)
        val request = PeriodicWorkRequestBuilder<StreakReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        try {
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to schedule WorkManager streak reminder", e)
        }
    }

    fun cancelEveningReminder(context: Context) {
        StreakReminderPreferences.setEnabled(context, false)
        try {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to cancel WorkManager streak reminder", e)
        }
    }
}
