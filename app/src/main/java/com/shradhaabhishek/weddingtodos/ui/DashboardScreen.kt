package com.shradhaabhishek.weddingtodos.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shradhaabhishek.weddingtodos.model.Task
import com.shradhaabhishek.weddingtodos.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    onEditTask: (Task) -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showQuickLinks by remember { mutableStateOf(false) }

    val filteredTasks = remember(tasks, searchQuery) {
        tasks.filter {
            it.task.contains(searchQuery, ignoreCase = true) ||
                    (it.username?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    }

    val groupedTasks = remember(filteredTasks) {
        filteredTasks.groupBy { task ->
            task.dueDate?.take(10) ?: "TBD"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { WeddingLogo() },
                actions = {
                    Box {
                        TextButton(onClick = { showQuickLinks = true }) {
                            Text(
                                "Quick Links ▾", 
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        DropdownMenu(
                            expanded = showQuickLinks,
                            onDismissRequest = { showQuickLinks = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sangeet Registration") },
                                onClick = { 
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://sangeet-registration.netlify.app/")))
                                    showQuickLinks = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Registration") },
                                onClick = { 
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://sangeet-registration.netlify.app/registers")))
                                    showQuickLinks = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Download Invitations") },
                                onClick = { 
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://sangeet-registration.netlify.app/gallery")))
                                    showQuickLinks = false 
                                }
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.signOut() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout, 
                            "Sign Out", 
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { onEditTask(Task()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, "Add Event")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Wedding Itinerary",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "The complete schedule for Shradha & Abhishek.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    SearchTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it }
                    )
                }

                groupedTasks.forEach { (dateKey, tasksForDate) ->
                    stickyHeader(key = dateKey) {
                        DateHeader(dateKey)
                    }

                    items(tasksForDate, key = { it.id }) { task ->
                        Box(Modifier.animateItem(
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )) {
                            TaskCard(
                                task = task,
                                isAdmin = isAdmin,
                                onEdit = { onEditTask(task) },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun SearchTextField(value: String, onValueChange: (String) -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Search events or people", style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = if (value.isNotEmpty()) {
                { IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Close, null) } }
            } else null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true,
            modifier = Modifier.fillMaxSize(),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun DateHeader(dateKey: String) {
    val displayDate = remember(dateKey) {
        if (dateKey == "TBD") "Unscheduled Events"
        else {
            try {
                val date = LocalDate.parse(dateKey)
                val today = LocalDate.now()
                when (date) {
                    today -> "Today"
                    today.plusDays(1) -> "Tomorrow"
                    else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH))
                }
            } catch (e: Exception) {
                dateKey
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = displayDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
        )
    }
}

@Composable
fun TaskCard(
    task: Task,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = if (task.completed) 
        MaterialTheme.colorScheme.surface 
    else 
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    
    val outlineColor = if (task.completed)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    else
        MaterialTheme.colorScheme.outlineVariant
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.task, 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (task.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                    
                    if (!task.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description, 
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                if (task.completed) {
                    Icon(
                        Icons.Default.CheckCircle, 
                        null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).padding(start = 8.dp)
                    )
                } else if (task.dueDate != null && task.dueDate.length > 10) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = task.dueDate.substring(11),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (!task.location.isNullOrEmpty() || !task.username.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!task.location.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                task.location, 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!task.username.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                task.username, 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!task.remark.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info, 
                            null, 
                            modifier = Modifier.size(16.dp).padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.remark,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp), 
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onEdit) { 
                        Text("Modify", style = MaterialTheme.typography.labelLarge) 
                    }
                    IconButton(onClick = onDelete) { 
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) 
                    }
                }
            }
        }
    }
}
