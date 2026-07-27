package com.shradhaabhishek.weddingtodos.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.shradhaabhishek.weddingtodos.model.BroadcastMessage
import com.shradhaabhishek.weddingtodos.model.Task
import com.shradhaabhishek.weddingtodos.util.NotificationHelper
import com.shradhaabhishek.weddingtodos.util.NotificationScheduler
import com.shradhaabhishek.weddingtodos.util.TaskStorage
import com.shradhaabhishek.weddingtodos.worker.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser, val isAdmin: Boolean, val isGuest: Boolean) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
    object AccessDenied : AuthState()
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _messages = MutableStateFlow<List<BroadcastMessage>>(emptyList())
    val messages: StateFlow<List<BroadcastMessage>> = _messages.asStateFlow()

    private var tasksListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    private var lastKnownMessageId: String? = null
    private var isFirstMessageSync = true

    init {
        Log.d("TaskViewModel", "Initializing...")
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.d("TaskViewModel", "AuthState changed: user=${user?.email}")
            
            if (user != null) {
                checkPermissions(user)
                startListeningForTasks()
                startListeningForMessages()
            } else {
                _authState.value = AuthState.Unauthenticated
                _isAdmin.value = false
                stopListeningForTasks()
                stopListeningForMessages()
                _tasks.value = emptyList()
                _messages.value = emptyList()
            }
        }
    }

    private fun checkPermissions(user: FirebaseUser) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                Log.d("TaskViewModel", "Checking permissions for ${user.email} (uid=${user.uid})")
                
                val adminDoc = db.collection("admins").document(user.uid).get().await()
                val isAdminResult = adminDoc.exists()
                _isAdmin.value = isAdminResult
                
                var isGuestResult = false
                if (!isAdminResult && user.email != null) {
                    val lowercaseEmail = user.email!!.lowercase()
                    Log.d("TaskViewModel", "Checking guest permission for $lowercaseEmail")
                    val guestDoc = db.collection("allowed_guests").document(lowercaseEmail).get().await()
                    isGuestResult = guestDoc.exists()
                }

                if (isAdminResult || isGuestResult) {
                    Log.d("TaskViewModel", "User authenticated: Admin=$isAdminResult, Guest=$isGuestResult")
                    _authState.value = AuthState.Authenticated(user, isAdminResult, isGuestResult)
                } else {
                    Log.w("TaskViewModel", "Access Denied for email: ${user.email}")
                    _authState.value = AuthState.AccessDenied
                }
                
                Log.d("TaskViewModel", "Permissions check complete. Admin: $isAdminResult, Guest: $isGuestResult")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error checking permissions", e)
                _authState.value = AuthState.Error("Permission check failed: ${e.message}")
            }
        }
    }

    private fun startListeningForTasks() {
        Log.d("TaskViewModel", "Starting to listen for tasks")
        tasksListener?.remove()
        _isLoading.value = true
        tasksListener = db.collection("wedding_tasks")
            .orderBy("dueDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    Log.e("TaskViewModel", "Firestore error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val taskList = snapshot.toObjects(Task::class.java)
                    Log.d("TaskViewModel", "Received ${taskList.size} tasks")
                    _tasks.value = taskList
                    
                    // Schedule precision alarms
                    NotificationScheduler.scheduleAllTasks(getApplication(), taskList)
                    
                    // Also save to local storage for offline/notifications
                    viewModelScope.launch {
                        val groupedTasks = taskList.groupBy { it.dueDate?.take(10) ?: "TBD" }
                        groupedTasks.forEach { (date, tasksForDate) ->
                            if (date != "TBD") {
                                Log.d("TaskViewModel", "Real-time save for $date: ${tasksForDate.joinToString { "${it.id}(${it.dueDate})" }}")
                                TaskStorage.saveTasksForDate(getApplication(), date, tasksForDate)
                            }
                        }
                    }
                }
            }
    }

    private fun stopListeningForTasks() {
        tasksListener?.remove()
        tasksListener = null
    }

    private fun startListeningForMessages() {
        Log.d("TaskViewModel", "Starting to listen for messages")
        messagesListener?.remove()
        messagesListener = db.collection("broadcast_messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TaskViewModel", "Messages listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messageList = snapshot.toObjects(BroadcastMessage::class.java)
                    Log.d("TaskViewModel", "Received ${messageList.size} messages")
                    
                    // Trigger notification for new messages
                    if (!isFirstMessageSync && messageList.isNotEmpty()) {
                        val latestMessage = messageList.first()
                        if (latestMessage.id != lastKnownMessageId && latestMessage.senderId != auth.currentUser?.uid) {
                            NotificationHelper.showChatNotification(getApplication(), latestMessage)
                        }
                    }
                    
                    if (messageList.isNotEmpty()) {
                        lastKnownMessageId = messageList.first().id
                        isFirstMessageSync = false
                    }
                    
                    _messages.value = messageList
                }
            }
    }

    private fun stopListeningForMessages() {
        messagesListener?.remove()
        messagesListener = null
    }

    fun sendBroadcastMessage(content: String) {
        val user = auth.currentUser ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                val message = BroadcastMessage(
                    content = content,
                    senderId = user.uid,
                    senderName = user.displayName ?: user.email?.substringBefore("@") ?: "Guest"
                )
                db.collection("broadcast_messages").add(message).await()
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error sending message", e)
            }
        }
    }

    fun deleteBroadcastMessage(messageId: String) {
        val user = auth.currentUser ?: return
        // Only Akash can delete
        if (user.email?.lowercase() != "akash.hiremath25@gmail.com") return

        viewModelScope.launch {
            try {
                db.collection("broadcast_messages").document(messageId).delete().await()
                Log.d("TaskViewModel", "Message deleted by admin: $messageId")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error deleting message", e)
            }
        }
    }

    fun saveTask(task: Task) {
        viewModelScope.launch {
            try {
                if (task.id.isEmpty()) {
                    db.collection("wedding_tasks").add(task).await()
                } else {
                    db.collection("wedding_tasks").document(task.id).set(task).await()
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error saving task", e)
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                db.collection("wedding_tasks").document(taskId).delete().await()
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error deleting task", e)
            }
        }
    }

    fun signOut() {
        Log.d("TaskViewModel", "Signing out")
        auth.signOut()
    }

    fun syncManual() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(getApplication()).enqueue(syncRequest)
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                Log.d("TaskViewModel", "Signing in with Google credential")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                Log.d("TaskViewModel", "Sign in successful")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Sign in failed", e)
                _authState.value = AuthState.Error("Sign in failed: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningForTasks()
        stopListeningForMessages()
    }
}
