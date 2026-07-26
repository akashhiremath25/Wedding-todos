package com.shradhaabhishek.weddingtodos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shradhaabhishek.weddingtodos.worker.NotificationWorker
import com.shradhaabhishek.weddingtodos.worker.SyncWorker
import java.util.concurrent.TimeUnit

class WeddingApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleDailySync()
        scheduleNotificationCheck()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Wedding Task Reminders"
            val descriptionText = "Notifications for upcoming wedding events"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleDailySync() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailySyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun scheduleNotificationCheck() {
        val notifyRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NotificationCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            notifyRequest
        )
    }

    companion object {
        const val CHANNEL_ID = "wedding_tasks_channel"
    }
}
