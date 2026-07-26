package com.shradhaabhishek.weddingtodos.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.shradhaabhishek.weddingtodos.model.Task
import com.shradhaabhishek.weddingtodos.receiver.TaskAlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationScheduler {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun scheduleAllTasks(context: Context, tasks: List<Task>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Can we schedule exact alarms?
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val now = LocalDateTime.now()

        tasks.forEach { task ->
            if (task.completed || task.dueDate == null) {
                cancelAlarm(context, task)
                return@forEach
            }

            try {
                val taskDateTime = LocalDateTime.parse(task.dueDate, formatter)
                
                // Only schedule future tasks
                if (taskDateTime.isAfter(now)) {
                    val triggerTime = taskDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    
                    val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
                        putExtra("TASK_ID", task.id)
                        putExtra("TASK_TITLE", task.task)
                    }
                    
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        task.id.hashCode(),
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    if (canScheduleExact) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        Log.d("NotificationScheduler", "Scheduled EXACT alarm for task: ${task.task} at ${task.dueDate}")
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        Log.w("NotificationScheduler", "Scheduled INEXACT alarm for task: ${task.task} at ${task.dueDate} (Permission missing)")
                    }
                } else {
                    // Task is in the past, ensure no alarm is pending
                    cancelAlarm(context, task)
                }
            } catch (e: Exception) {
                Log.e("NotificationScheduler", "Error parsing date for task ${task.id}", e)
            }
        }
    }

    private fun cancelAlarm(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d("NotificationScheduler", "Cancelled alarm for task: ${task.task}")
        }
    }
}
