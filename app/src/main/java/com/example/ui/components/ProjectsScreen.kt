package com.example.ui.components

import androidx.compose.foundation.clickable
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
import com.example.core.model.Project
import com.example.ui.theme.IdeCyan
import com.example.ui.theme.IdeGreen
import com.example.ui.theme.IdeYellow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectsScreen(
    projects: List<Project>,
    currentProject: Project?,
    onSelectProject: (Project) -> Unit,
    onNewProject: () -> Unit,
    onImportZip: () -> Unit,
    onCloneGitHub: () -> Unit,
    onExportProject: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onOpenWorkflows: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Android Projects",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${projects.size} projects available on device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onNewProject,
                colors = ButtonDefaults.buttonColors(containerColor = IdeCyan)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Import Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onImportZip,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import ZIP", fontSize = 12.sp)
            }
            FilledTonalButton(
                onClick = onCloneGitHub,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clone Git", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Projects List
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No projects yet. Create or import one!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    val isActive = project.id == currentProject?.id
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProject(project) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Android,
                                        contentDescription = null,
                                        tint = if (isActive) IdeCyan else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = project.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Badge(containerColor = IdeGreen) {
                                            Text("Active", fontSize = 10.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = project.packageName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(project.template.displayName, fontSize = 10.sp) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Min SDK ${project.minSdk}", fontSize = 10.sp) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }

                            // Project Actions Menu
                            var menuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Project menu")
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Open in Editor") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onSelectProject(project)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export as ZIP") },
                                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onExportProject(project)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("GitHub Actions CI/CD") },
                                        leadingIcon = { Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = IdeCyan) },
                                        onClick = {
                                            menuExpanded = false
                                            onOpenWorkflows()
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuExpanded = false
                                            projectToDelete = project
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    projectToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete ${target.name}?") },
            text = { Text("This will remove all project source files from device storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(target)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
