package com.shradhaabhishek.weddingtodos.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.shradhaabhishek.weddingtodos.MainActivity
import com.shradhaabhishek.weddingtodos.R
import com.shradhaabhishek.weddingtodos.WeddingApplication
import com.shradhaabhishek.weddingtodos.worker.SyncWorker

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("TaskAlarmReceiver", "Device rebooted, triggering sync to restore alarms")
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(context).enqueue(syncRequest)
            return
        }

        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Wedding Event"

        Log.d("TaskAlarmReceiver", "Alarm triggered for task: $taskTitle ($taskId)")
        showNotification(context, taskId, taskTitle)
    }

    private fun showNotification(context: Context, taskId: String, title: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            mainIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, WeddingApplication.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Wedding Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(taskId.hashCode(), builder.build())
    }
}
