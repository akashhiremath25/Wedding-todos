package com.shradhaabhishek.weddingtodos

import android.Manifest
import android.app.AlarmManager
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.shradhaabhishek.weddingtodos.model.Task
import com.shradhaabhishek.weddingtodos.ui.AuthScreen
import com.shradhaabhishek.weddingtodos.ui.DashboardScreen
import com.shradhaabhishek.weddingtodos.ui.TaskEditor
import com.shradhaabhishek.weddingtodos.viewmodel.TaskViewModel
import com.shradhaabhishek.weddingtodos.viewmodel.AuthState
import com.shradhaabhishek.weddingtodos.ui.theme.WeddingTodosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications are disabled. You won't receive task reminders.", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        checkExactAlarmPermission()
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Precise Reminders")
                    .setMessage("To receive reminders exactly at the right time, the app needs the 'Alarms & Reminders' permission.")
                    .setPositiveButton("Settings") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("Later", null)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestNotificationPermission()

        setContent {
            WeddingTodosTheme {
                val authState by viewModel.authState.collectAsState()
                var editingTask by remember { mutableStateOf<Task?>(null) }
                val context = LocalContext.current
                
                // Handle tab selection from intent
                var initialTab by remember { 
                    mutableIntStateOf(if (intent?.getStringExtra("OPEN_TAB") == "BROADCASTS") 1 else 0) 
                }

                AnimatedContent(
                    targetState = authState,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { state ->
                    when (state) {
                        is AuthState.Initial, is AuthState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is AuthState.Authenticated -> {
                            Box {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onEditTask = { editingTask = it },
                                    initialTab = initialTab
                                )

                                editingTask?.let { task ->
                                    TaskEditor(
                                        task = task,
                                        onDismiss = { editingTask = null },
                                        onSave = {
                                            viewModel.saveTask(it)
                                            editingTask = null
                                        }
                                    )
                                }
                            }
                        }
                        is AuthState.Unauthenticated -> {
                            AuthScreen(viewModel)
                        }
                        is AuthState.AccessDenied -> {
                            AccessDeniedScreen(onLogOut = { viewModel.signOut() })
                        }
                        is AuthState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            AuthScreen(viewModel)
                        }
                    }
                }
            }
        }

        checkForUpdates()
    }

    @androidx.compose.runtime.Composable
    private fun AccessDeniedScreen(onLogOut: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Access Denied",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Your email is not on the guest list. Please contact the organizers if you believe this is a mistake.",
                modifier = Modifier.padding(24.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onLogOut,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text("Log Out", modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    private fun checkForUpdates() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/akashhiremath25/Wedding-todos/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "WeddingTodos-App") // Required by GitHub API
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonObject = JSONObject(response)

                    val latestVersion = jsonObject.getString("tag_name")
                        .replace("v", "", ignoreCase = true).trim()
                    val currentVersion = BuildConfig.VERSION_NAME
                        .replace("v", "", ignoreCase = true).trim()

                    android.util.Log.d("UpdateCheck", "Current: $currentVersion, Latest: $latestVersion")

                    if (isNewerVersion(currentVersion, latestVersion)) {
                        val assets = jsonObject.getJSONArray("assets")
                        if (assets.length() > 0) {
                            val apkUrl = assets.getJSONObject(0).getString("browser_download_url")
                            withContext(Dispatchers.Main) {
                                showUpdateDialog(apkUrl)
                            }
                        } else {
                            android.util.Log.w("UpdateCheck", "New version found but no assets attached to release")
                        }
                    }
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "No error message"
                    android.util.Log.e("UpdateCheck", "Failed to check updates: ${connection.responseCode} - $errorResponse")
                }
            } catch (e: Exception) {
                android.util.Log.e("UpdateCheck", "Error checking for updates", e)
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        return try {
            val currParts = current.split(".").map { it.toInt() }
            val lateParts = latest.split(".").map { it.toInt() }
            for (i in 0 until minOf(currParts.size, lateParts.size)) {
                if (lateParts[i] > currParts[i]) return true
                if (lateParts[i] < currParts[i]) return false
            }
            lateParts.size > currParts.size
        } catch (e: Exception) {
            latest != current
        }
    }

    private fun showUpdateDialog(apkUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("Update Available")
            .setMessage("A new version of the app is available. Would you like to install it?")
            .setPositiveButton("Update") { _, _ -> downloadAndInstallUpdate(apkUrl) }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadAndInstallUpdate(apkUrl: String) {
        val fileName = "wedding-app-update.apk"
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Downloading Update")
            .setMimeType("application/vnd.android.package-archive")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val oldFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (oldFile.exists()) oldFile.delete()

        val downloadId = manager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(fileName)
                    context?.unregisterReceiver(this)
                }
            }
        }

        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun installApk(fileName: String) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        }
    }
}
