package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.BuildResult
import com.example.core.model.BuildStep
import com.example.core.model.StepState
import com.example.ui.theme.*

@Composable
fun BuildStatusDialog(
    steps: List<BuildStep>,
    result: BuildResult?,
    isBuilding: Boolean,
    onInstallApk: () -> Unit,
    onShareApk: () -> Unit,
    onSaveToDownloads: () -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isBuilding) onDismiss()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isBuilding) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = IdeCyan)
                    Text("Building Application...", fontWeight = FontWeight.Bold)
                } else if (result?.success == true) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IdeGreen)
                    Text("Build Succeeded!", fontWeight = FontWeight.Bold, color = IdeGreen)
                } else {
                    Icon(Icons.Default.Error, contentDescription = null, tint = IdeRed)
                    Text("Build Failed", fontWeight = FontWeight.Bold, color = IdeRed)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(steps, key = { it.id }) { step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (step.state) {
                                StepState.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = IdeCyan)
                                StepState.SUCCESS -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IdeGreen, modifier = Modifier.size(16.dp))
                                StepState.FAILED -> Icon(Icons.Default.Cancel, contentDescription = null, tint = IdeRed, modifier = Modifier.size(16.dp))
                                StepState.PENDING -> Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.title,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = step.description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (step.durationMs > 0) {
                                Text(
                                    text = "${step.durationMs}ms",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (result != null && result.success && result.apkFile != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Artifact: ${result.apkFile.name}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Size: ${result.apkFile.length()} bytes • Signed with Debug Key",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (result != null && !result.success) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Build Failure Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.errorMessage ?: "Unknown error occurred during build",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (result != null && result.success && result.apkFile != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onInstallApk,
                        colors = ButtonDefaults.buttonColors(containerColor = IdeGreen)
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Install APK")
                    }
                    FilledTonalButton(onClick = onSaveToDownloads) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                    OutlinedButton(onClick = onShareApk) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            } else if (!isBuilding) {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (!isBuilding) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
