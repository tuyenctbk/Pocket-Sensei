package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.analytics.EngagementDecisionEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StreakReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!StreakReminderPreferences.isEnabled(applicationContext)) {
            return Result.success()
        }

        val prefs = applicationContext.getSharedPreferences(
            "pocket_sensei_engagement_prefs",
            Context.MODE_PRIVATE
        )
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val streakState = EngagementDecisionEngine.computeDailyStreakReadState(
            lastPracticeDay = prefs.getString("daily_streak_last_day", null),
            storedStreak = prefs.getInt("daily_streak_count", 0),
            storedBestStreak = prefs.getInt("daily_streak_best", 0),
            todayDay = today,
            storedFreezes = prefs.getInt("daily_streak_freezes", 0)
        )

        // Never nag if the user already locked in practice today.
        if (streakState.practicedToday) {
            return Result.success()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                return Result.success()
            }
        }

        ensureChannel(applicationContext)

        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = applicationContext.getString(R.string.reminder_notif_title)
        val body = applicationContext.getString(
            R.string.reminder_notif_body_format,
            streakState.currentStreak
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "sensei_streak_reminders"
        const val NOTIFICATION_ID = 4012

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.reminder_channel_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
