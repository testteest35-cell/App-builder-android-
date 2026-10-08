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
import com.example.core.model.FileNode
import com.example.core.model.Project
import com.example.ui.theme.*
import java.io.File

@Composable
fun FileExplorerDrawer(
    currentProject: Project?,
    fileTree: FileNode?,
    activeFilePath: String?,
    onSelectFile: (File) -> Unit,
    onToggleFolder: (String) -> Unit,
    onCreateFile: (parentDir: File, name: String, isFolder: Boolean) -> Unit,
    onRenameFile: (file: File, newName: String) -> Unit,
    onDeleteFile: (file: File) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var selectedParentDir by remember { mutableStateOf<File?>(null) }

    var itemToRename by remember { mutableStateOf<File?>(null) }
    var itemToDelete by remember { mutableStateOf<File?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PROJECT FILES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Row {
                    IconButton(
                        onClick = {
                            selectedParentDir = currentProject?.rootDir
                            isCreatingFolder = false
                            showCreateDialog = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.NoteAdd,
                            contentDescription = "New File",
                            modifier = Modifier.size(16.dp),
                            tint = IdeCyan
                        )
                    }
                    IconButton(
                        onClick = {
                            selectedParentDir = currentProject?.rootDir
                            isCreatingFolder = true
                            showCreateDialog = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            modifier = Modifier.size(16.dp),
                            tint = IdeYellow
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Files",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Tree View
            if (fileTree != null) {
                val flatNodes = remember(fileTree) { flattenFileTree(fileTree) }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    items(flatNodes, key = { it.file.absolutePath }) { node ->
                        FileTreeRow(
                            node = node,
                            isSelected = activeFilePath == node.file.absolutePath,
                            onItemClick = {
                                if (node.isDirectory) {
                                    onToggleFolder(node.relativePath)
                                } else {
                                    onSelectFile(node.file)
                                }
                            },
                            onAddChild = { isFolder ->
                                selectedParentDir = if (node.isDirectory) node.file else node.file.parentFile
                                isCreatingFolder = isFolder
                                showCreateDialog = true
                            },
                            onRename = { itemToRename = node.file },
                            onDelete = { itemToDelete = node.file }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No project loaded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // Dialog: Create File / Folder
    if (showCreateDialog) {
        var inputName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (isCreatingFolder) "New Directory" else "New File") },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text(if (isCreatingFolder) "Folder Name" else "File Name (e.g. Screen.kt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parent = selectedParentDir ?: currentProject?.rootDir
                        if (parent != null && inputName.isNotBlank()) {
                            onCreateFile(parent, inputName.trim(), isCreatingFolder)
                        }
                        showCreateDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Rename
    itemToRename?.let { target ->
        var newName by remember { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Rename ${target.name}") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newName != target.name) {
                            onRenameFile(target, newName.trim())
                        }
                        itemToRename = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Delete
    itemToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete ${target.name}?") },
            text = { Text("Are you sure you want to permanently delete '${target.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(target)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FileTreeRow(
    node: FileNode,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onAddChild: (isFolder: Boolean) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (node.depth * 14).dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (node.isDirectory) {
                Icon(
                    imageVector = if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = IdeYellow,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                FileExtensionIcon(node.extension)
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = node.name,
                fontSize = 13.sp,
                color = if (isSelected) IdeCyan else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected || node.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            // Context Menu Button
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (node.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("New File") },
                            leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null, tint = IdeCyan) },
                            onClick = {
                                menuExpanded = false
                                onAddChild(false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Folder") },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = IdeYellow) },
                            onClick = {
                                menuExpanded = false
                                onAddChild(true)
                            }
                        )
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileExtensionIcon(extension: String) {
    val (icon, color) = when (extension) {
        "kt" -> Icons.Default.Code to IdePurple
        "java" -> Icons.Default.Code to IdeOrange
        "xml" -> Icons.Default.Terminal to IdeOrange
        "kts", "gradle" -> Icons.Default.Settings to IdeGreen
        "json" -> Icons.Default.DataObject to IdeYellow
        "md", "txt" -> Icons.Default.Description to IdeCyan
        else -> Icons.Default.InsertDriveFile to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(16.dp)
    )
}

private fun flattenFileTree(root: FileNode): List<FileNode> {
    val list = mutableListOf<FileNode>()
    // Skip root directory itself from being a row if desired, or include its children
    for (child in root.children) {
        list.add(child)
        if (child.isDirectory && child.isExpanded) {
            list.addAll(flattenChildren(child))
        }
    }
    return list
}

private fun flattenChildren(node: FileNode): List<FileNode> {
    val list = mutableListOf<FileNode>()
    for (child in node.children) {
        list.add(child)
        if (child.isDirectory && child.isExpanded) {
            list.addAll(flattenChildren(child))
        }
    }
    return list
}
