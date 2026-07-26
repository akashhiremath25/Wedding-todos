package com.shradhaabhishek.weddingtodos.worker

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shradhaabhishek.weddingtodos.MainActivity
import com.shradhaabhishek.weddingtodos.R
import com.shradhaabhishek.weddingtodos.WeddingApplication
import com.shradhaabhishek.weddingtodos.util.TaskStorage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.w("NotificationWorker", "Aborting: POST_NOTIFICATIONS permission not granted")
                return Result.success()
            }
        }
        
        if (!notificationManager.areNotificationsEnabled()) {
            Log.w("NotificationWorker", "Aborting: System-wide notifications are disabled for this app")
            return Result.success()
        }

        val now = LocalDateTime.now()
        val todayStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val currentDateTimeStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        
        Log.d("NotificationWorker", "Checking tasks for $todayStr at $currentDateTimeStr")
        val tasks = TaskStorage.getTasksForDate(applicationContext, todayStr)
        Log.d("NotificationWorker", "Found ${tasks.size} tasks for today: ${tasks.joinToString { "${it.id}(${it.dueDate})" }}")
        
        val sharedPrefs = applicationContext.getSharedPreferences("notifications", Context.MODE_PRIVATE)
        val notifiedKeys = sharedPrefs.getStringSet("notified_keys", emptySet()) ?: emptySet()
        val newNotifiedKeys = notifiedKeys.toMutableSet()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        tasks.forEach { task ->
            val trackingKey = "${task.id}_${task.dueDate}"
            
            if (task.completed) {
                Log.d("NotificationWorker", "Task ${task.id} (${task.task}) is completed, skipping")
                return@forEach
            }
            if (task.dueDate == null) {
                Log.d("NotificationWorker", "Task ${task.id} (${task.task}) has no due date, skipping")
                return@forEach
            }
            if (trackingKey in notifiedKeys) {
                Log.d("NotificationWorker", "Task ${task.id} (${task.task}) with due date ${task.dueDate} was already notified, skipping")
                return@forEach
            }

            try {
                val taskDateTime = LocalDateTime.parse(task.dueDate, formatter)
                
                // If the task is due now or was due in the past
                if (now.isAfter(taskDateTime) || now.isEqual(taskDateTime)) {
                    Log.d("NotificationWorker", "MATCH: Sending notification for task ${task.id} (${task.task})")
                    sendNotification(task.id.hashCode(), "Upcoming Task", task.task)
                    newNotifiedKeys.add(trackingKey)
                } else {
                    Log.d("NotificationWorker", "FUTURE: Task ${task.id} (${task.task}) due at ${task.dueDate} is in the future")
                }
            } catch (e: Exception) {
                Log.e("NotificationWorker", "Error parsing date '${task.dueDate}' for task ${task.id}", e)
            }
        }

        sharedPrefs.edit()
            .putStringSet("notified_keys", newNotifiedKeys)
            .apply()

        return Result.success()
    }

    private fun sendNotification(id: Int, title: String, content: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, WeddingApplication.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(id, builder.build())
    }
}
