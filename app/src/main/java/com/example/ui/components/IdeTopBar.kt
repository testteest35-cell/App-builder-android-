package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.core.model.Project
import com.example.ui.theme.IdeCyan
import com.example.ui.theme.IdeGreen
import com.example.ui.theme.IdePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeTopBar(
    currentProject: Project?,
    projects: List<Project>,
    isBuilding: Boolean,
    onSelectProject: (Project) -> Unit,
    onNewProject: () -> Unit,
    onRun: () -> Unit,
    onBuild: () -> Unit,
    onInstallApk: () -> Unit,
    onOpenDependencies: () -> Unit,
    onOpenGit: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showProjectDropdown by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Drawer menu & Project selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onToggleDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Toggle Files Explorer",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Project Dropdown Button
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showProjectDropdown = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(IdeGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentProject?.name ?: "Select Project",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showProjectDropdown,
                        onDismissRequest = { showProjectDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("+ New Project", fontWeight = FontWeight.SemiBold, color = IdeCyan) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = IdeCyan) },
                            onClick = {
                                showProjectDropdown = false
                                onNewProject()
                            }
                        )
                        HorizontalDivider()
                        projects.forEach { p ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(p.name, fontWeight = FontWeight.Medium)
                                        Text(p.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (p.id == currentProject?.id) IdeCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showProjectDropdown = false
                                    onSelectProject(p)
                                }
                            )
                        }
                    }
                }
            }

            // Right: Build & Run Action controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Run Button (Primary)
                FilledTonalButton(
                    onClick = onRun,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = IdeGreen.copy(alpha = 0.2f),
                        contentColor = IdeGreen
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run App",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Build Button
                IconButton(
                    onClick = onBuild,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isBuilding) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                            color = IdeCyan
                        )
                    } else {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = "Build Project",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Install APK Quick Action
                IconButton(
                    onClick = onInstallApk,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.InstallMobile,
                        contentDescription = "Install APK",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Dependencies Manager
                IconButton(
                    onClick = onOpenDependencies,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Extension,
                        contentDescription = "Dependencies",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Git
                IconButton(
                    onClick = onOpenGit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.CallMerge,
                        contentDescription = "Git Version Control",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "IDE Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
