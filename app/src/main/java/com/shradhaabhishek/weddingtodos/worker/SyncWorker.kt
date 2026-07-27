package com.shradhaabhishek.weddingtodos.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.shradhaabhishek.weddingtodos.model.Task
import com.shradhaabhishek.weddingtodos.util.NotificationScheduler
import com.shradhaabhishek.weddingtodos.util.TaskStorage
import kotlinx.coroutines.tasks.await

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncWorker", "Starting sync from Firebase")
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("wedding_tasks").get().await()
            val tasks = snapshot.toObjects(Task::class.java)

            // Schedule alarms for all future tasks
            NotificationScheduler.scheduleAllTasks(applicationContext, tasks)

            // Group tasks by date part of dueDate
            val groupedTasks = tasks.groupBy { it.dueDate?.take(10) ?: "TBD" }

            groupedTasks.forEach { (date, tasksForDate) ->
                if (date != "TBD") {
                    Log.d("SyncWorker", "Saving tasks for $date: ${tasksForDate.joinToString { "${it.id}(${it.dueDate})" }}")
                    TaskStorage.saveTasksForDate(applicationContext, date, tasksForDate)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error syncing tasks", e)
            Result.retry()
        }
    }
}
