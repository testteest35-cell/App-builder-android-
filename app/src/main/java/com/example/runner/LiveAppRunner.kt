package com.example.runner

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.LogLevel
import com.example.core.model.Project
import com.example.core.model.ProjectTemplateType

data class TaskDemoItem(
    val id: String,
    val title: String,
    val isDone: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveAppRunnerView(
    project: Project,
    codeContent: String,
    onEmitLog: (LogLevel, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine the template mode
    val template = project.template

    LaunchedEffect(project.id) {
        onEmitLog(
            LogLevel.INFO,
            "ActivityManager",
            "START u0 {act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] cmp=${project.packageName}/.MainActivity}"
        )
        onEmitLog(LogLevel.DEBUG, "ComposeRuntime", "Initial composition completed for ${project.name}")
    }

    // Realistic Android Device Mockup Frame
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111315)),
        border = CardDefaults.outlinedCardBorder().copy(width = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Android Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "09:41",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                // Center Camera Hole
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Icon(
                        Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Screen Viewport Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 380.dp, max = 520.dp)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (template) {
                    ProjectTemplateType.COMPOSE_COUNTER -> {
                        CounterInteractiveScreen(
                            projectName = project.name,
                            codeContent = codeContent,
                            onEmitLog = onEmitLog
                        )
                    }
                    ProjectTemplateType.COMPOSE_NOTES -> {
                        TaskCraftInteractiveScreen(
                            projectName = project.name,
                            onEmitLog = onEmitLog
                        )
                    }
                    else -> {
                        EmptyComposeInteractiveScreen(
                            projectName = project.name,
                            codeContent = codeContent,
                            onEmitLog = onEmitLog
                        )
                    }
                }
            }

            // Android Navigation Bar (Gesture Pill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                )
            }
        }
    }
}

@Composable
private fun CounterInteractiveScreen(
    projectName: String,
    codeContent: String,
    onEmitLog: (LogLevel, String, String) -> Unit
) {
    var count by remember { mutableIntStateOf(0) }
    var taps by remember { mutableIntStateOf(0) }

    // Parse customized title if edited in code
    val customTitle = remember(codeContent) {
        val match = Regex("Text\\(\\s*text\\s*=\\s*\"([^\"]+)\"").find(codeContent)
        match?.groupValues?.get(1) ?: projectName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = customTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Jetpack Compose Live Runtime",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(targetState = count, label = "counter") { value ->
                Text(
                    text = "$value",
                    fontSize = 48.sp,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Interaction Events: $taps taps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    count--
                    taps++
                    onEmitLog(LogLevel.DEBUG, "CounterApp", "Decremented count to $count (taps: $taps)")
                }
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Decrement")
            }
            Button(
                onClick = {
                    count++
                    taps++
                    onEmitLog(LogLevel.DEBUG, "CounterApp", "Incremented count to $count (taps: $taps)")
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Increment")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                count = 0
                taps = 0
                onEmitLog(LogLevel.INFO, "CounterApp", "Counter state reset to 0")
            }
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset State")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCraftInteractiveScreen(
    projectName: String,
    onEmitLog: (LogLevel, String, String) -> Unit
) {
    var tasks by remember {
        mutableStateOf(
            listOf(
                TaskDemoItem("1", "Setup Android IDE Architecture", true),
                TaskDemoItem("2", "Test Compose on device emulator", true),
                TaskDemoItem("3", "Compile D8 Dalvik Executable", false),
                TaskDemoItem("4", "Generate signed release APK", false)
            )
        )
    }
    var newTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Badge {
                    Text("${tasks.count { !it.isDone }} open")
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    placeholder = { Text("Add task...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            val item = TaskDemoItem(System.currentTimeMillis().toString(), newTitle.trim(), false)
                            tasks = listOf(item) + tasks
                            onEmitLog(LogLevel.DEBUG, "TaskCraft", "Created task: '${item.title}'")
                            newTitle = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tasks, key = { it.id }) { task ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = task.isDone,
                                onCheckedChange = { isChecked ->
                                    tasks = tasks.map { if (it.id == task.id) it.copy(isDone = isChecked) else it }
                                    onEmitLog(LogLevel.DEBUG, "TaskCraft", "Task '${task.title}' updated isDone=$isChecked")
                                }
                            )
                            IconButton(
                                onClick = {
                                    tasks = tasks.filter { it.id != task.id }
                                    onEmitLog(LogLevel.INFO, "TaskCraft", "Deleted task '${task.title}'")
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyComposeInteractiveScreen(
    projectName: String,
    codeContent: String,
    onEmitLog: (LogLevel, String, String) -> Unit
) {
    var clickCount by remember { mutableIntStateOf(0) }

    val displayText = remember(codeContent) {
        val match = Regex("Text\\(\\s*text\\s*=\\s*\"([^\"]+)\"").find(codeContent)
        match?.groupValues?.get(1) ?: "Hello from $projectName!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Android,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = displayText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Compose screen compiled & active",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                clickCount++
                onEmitLog(LogLevel.INFO, "LiveApp", "Button tapped $clickCount times")
            }
        ) {
            Text("Test Button ($clickCount)")
        }
    }
}
