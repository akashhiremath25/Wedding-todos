package com.shradhaabhishek.weddingtodos.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shradhaabhishek.weddingtodos.model.Task
import java.io.File

object TaskStorage {
    private val gson = Gson()

    fun saveTasksForDate(context: Context, date: String, tasks: List<Task>) {
        val fileName = "$date.json"
        val file = File(context.filesDir, fileName)
        try {
            val json = gson.toJson(tasks)
            file.writeText(json)
            Log.d("TaskStorage", "Successfully saved ${tasks.size} tasks to $fileName")
        } catch (e: Exception) {
            Log.e("TaskStorage", "Error saving tasks to $fileName", e)
        }
    }

    fun getTasksForDate(context: Context, date: String): List<Task> {
        val fileName = "$date.json"
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()

        return try {
            val json = file.readText()
            val type = object : TypeToken<List<Task>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun hasFileForDate(context: Context, date: String): Boolean {
        return File(context.filesDir, "$date.json").exists()
    }
}
