package com.shradhaabhishek.weddingtodos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager

class WeddingApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Task reminders channel
            val taskName = "Wedding Task Reminders"
            val taskDescriptionText = "Notifications for upcoming wedding events"
            val taskImportance = NotificationManager.IMPORTANCE_HIGH
            val taskChannel = NotificationChannel(CHANNEL_ID, taskName, taskImportance).apply {
                description = taskDescriptionText
            }
            notificationManager.createNotificationChannel(taskChannel)

            // Chat channel
            val chatName = "Wedding Broadcasts"
            val chatDescriptionText = "Notifications for new broadcast messages"
            val chatImportance = NotificationManager.IMPORTANCE_HIGH
            val chatChannel = NotificationChannel(CHAT_CHANNEL_ID, chatName, chatImportance).apply {
                description = chatDescriptionText
            }
            notificationManager.createNotificationChannel(chatChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "wedding_tasks_channel"
        const val CHAT_CHANNEL_ID = "wedding_chat_channel"
    }
}
