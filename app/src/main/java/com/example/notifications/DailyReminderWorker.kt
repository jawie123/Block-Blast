package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.GamePreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DailyReminderScheduler {
    const val CHANNEL_ID = "block_blast_daily_reminders"
    const val WORK_NAME = "block_blast_daily_streak_reminder"
    const val NOTIFICATION_ID = 4104

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Streak & Spin Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle daily reminders when your Free Lucky Spin or Daily Gift is ready."
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminder(context: Context): Boolean {
        return runCatching {
            createNotificationChannel(context)
            val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(18, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            true
        }.onFailure { err ->
            Log.w("DailyReminder", "Unable to schedule WorkManager reminder", err)
        }.getOrDefault(false)
    }

    fun cancelDailyReminder(context: Context) {
        runCatching {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val prefs = GamePreferences(applicationContext)
        if (!prefs.isDailyRemindersEnabled) return Result.success()
        if (!DailyReminderScheduler.hasNotificationPermission(applicationContext)) return Result.success()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val needsSpin = prefs.canClaimFreeSpinToday(today)
        val needsGift = prefs.lastLoginClaimDate != today
        val needsDaily = !prefs.isDailyDateCompleted(today)

        if (!needsSpin && !needsGift && !needsDaily) {
            return Result.success()
        }

        DailyReminderScheduler.createNotificationChannel(applicationContext)

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = applicationContext.getString(R.string.reminder_notification_title)
        val body = applicationContext.getString(R.string.reminder_notification_body)

        val notification = NotificationCompat.Builder(applicationContext, DailyReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext)
                .notify(DailyReminderScheduler.NOTIFICATION_ID, notification)
        }

        return Result.success()
    }
}
