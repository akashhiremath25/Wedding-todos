package com.shradhaabhishek.weddingtodos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.shradhaabhishek.weddingtodos.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser, val isAdmin: Boolean, val isGuest: Boolean) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
    object AccessDenied : AuthState()
}

class TaskViewModel : ViewModel() {
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

    private var tasksListener: ListenerRegistration? = null

    init {
        Log.d("TaskViewModel", "Initializing...")
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.d("TaskViewModel", "AuthState changed: user=${user?.email}")
            
            if (user != null) {
                checkPermissions(user)
                startListeningForTasks()
            } else {
                _authState.value = AuthState.Unauthenticated
                _isAdmin.value = false
                stopListeningForTasks()
                _tasks.value = emptyList()
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
                    _authState.value = AuthState.Authenticated(user, isAdminResult, isGuestResult)
                } else {
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
                }
            }
    }

    private fun stopListeningForTasks() {
        tasksListener?.remove()
        tasksListener = null
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
    }
}
