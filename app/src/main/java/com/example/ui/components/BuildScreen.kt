package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.BuildResult
import com.example.core.model.BuildStep
import com.example.core.model.Project
import com.example.core.model.StepState
import com.example.ui.theme.*

@Composable
fun BuildScreen(
    currentProject: Project?,
    buildSteps: List<BuildStep>,
    buildResult: BuildResult?,
    isBuilding: Boolean,
    useRemoteBuild: Boolean,
    remoteServerUrl: String,
    onToggleRemoteBuild: (Boolean) -> Unit,
    onStartBuild: () -> Unit,
    onInstallApk: () -> Unit,
    onSaveApkToDownloads: () -> Unit,
    onShareApk: () -> Unit,
    onLaunchPreview: () -> Unit,
    onOpenWorkflows: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Build Configuration Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Build Pipeline Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Target: ${currentProject?.name ?: "No project selected"} (${currentProject?.packageName ?: ""})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Build mode selection toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (useRemoteBuild) "Remote Cloud Build Server" else "On-Device Local Build Engine",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (useRemoteBuild) "Offloads compilation to server via Gradle" else "Fast local syntax verification, AAPT2, & DEX packaging",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useRemoteBuild,
                        onCheckedChange = onToggleRemoteBuild
                    )
                }

                if (useRemoteBuild) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Endpoint: $remoteServerUrl",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = IdeCyan
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Build Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartBuild,
                        enabled = !isBuilding && currentProject != null,
                        colors = ButtonDefaults.buttonColors(containerColor = IdeCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isBuilding) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Building...")
                        } else {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Build APK", fontWeight = FontWeight.Bold)
                        }
                    }

                    FilledTonalButton(
                        onClick = onLaunchPreview,
                        enabled = currentProject != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Preview")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOpenWorkflows,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = IdeCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GitHub Actions CI/CD & Cloud Install", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Build Artifact Success Card
        if (buildResult != null && buildResult.success && buildResult.apkFile != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IdeGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APK Artifact Ready!",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "${buildResult.apkFile.name} • ${buildResult.apkFile.length()} bytes • ${buildResult.totalDurationMs}ms",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onInstallApk,
                            colors = ButtonDefaults.buttonColors(containerColor = IdeGreen),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install APK", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = onSaveApkToDownloads,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save to Downloads", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = onShareApk,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share APK")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Build Steps Timeline
        Text(
            text = "Pipeline Execution Tasks",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(buildSteps, key = { it.id }) { step ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (step.state) {
                            StepState.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = IdeCyan)
                            StepState.SUCCESS -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IdeGreen, modifier = Modifier.size(18.dp))
                            StepState.FAILED -> Icon(Icons.Default.Cancel, contentDescription = null, tint = IdeRed, modifier = Modifier.size(18.dp))
                            StepState.PENDING -> Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(step.title, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(step.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (step.durationMs > 0) {
                            Text("${step.durationMs}ms", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
