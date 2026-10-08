package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Project
import com.example.github.WorkflowGenerator
import com.example.ui.theme.IdeCyan
import com.example.ui.theme.IdeGreen
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun GitHubWorkflowDialog(
    project: Project?,
    onDismiss: () -> Unit,
    onAddWorkflowToProject: (Project) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedWorkflowTab by remember { mutableIntStateOf(0) } // 0: DroidIDE CI/CD Installer, 1: Active Project CI/CD
    val scrollState = rememberScrollState()

    val currentContent = if (selectedWorkflowTab == 0) {
        WorkflowGenerator.DROIDIDE_APP_WORKFLOW
    } else {
        WorkflowGenerator.getProjectWorkflow(project?.name ?: "MyAndroidApp")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = IdeCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GitHub Actions Workflow Setup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Automatically compile, package, and install this Android app using GitHub Actions workflows. Runs on every push or tag release.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(selectedTabIndex = selectedWorkflowTab) {
                    Tab(
                        selected = selectedWorkflowTab == 0,
                        onClick = { selectedWorkflowTab = 0 },
                        text = { Text("DroidIDE App Installer", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedWorkflowTab == 1,
                        onClick = { selectedWorkflowTab = 1 },
                        text = { Text("Active Project CI", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // How to install guide
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (selectedWorkflowTab == 0) "📱 How to Install via GitHub Actions:" else "🚀 How it Works:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (selectedWorkflowTab == 0) {
                            Text(
                                text = "1. Push this codebase to your GitHub repository.\n" +
                                       "2. Go to the 'Actions' tab in GitHub or create a git tag (e.g. 'v1.0.0').\n" +
                                       "3. GitHub Actions builds the APK and publishes it under 'Releases' & 'Artifacts'.\n" +
                                       "4. Download DroidIDE-app.apk on your mobile phone to install immediately!",
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        } else {
                            Text(
                                text = "Adds '.github/workflows/build-apk.yml' to your project root. When pushed to GitHub, it automatically creates APK releases on GitHub.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ".github/workflows/build-apk.yml",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentContent))
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy YAML", fontSize = 11.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = currentContent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedWorkflowTab == 1 && project != null) {
                Button(
                    onClick = {
                        onAddWorkflowToProject(project)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IdeGreen)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add to Project")
                }
            } else {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(currentContent))
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IdeCyan)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy & Close")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
