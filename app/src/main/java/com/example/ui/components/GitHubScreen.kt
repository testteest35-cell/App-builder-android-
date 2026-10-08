package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.core.model.GitCommit
import com.example.core.model.GitFileStatus
import com.example.core.model.Project
import com.example.github.model.GitHubRepo
import com.example.github.model.GitHubUser
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubScreen(
    isAuthenticated: Boolean,
    currentUser: GitHubUser?,
    userRepos: List<GitHubRepo>,
    currentProject: Project?,
    gitStatusList: List<GitFileStatus>,
    gitCommits: List<GitCommit>,
    isCloning: Boolean,
    cloneStatusText: String,
    onLoginWithToken: (String) -> Unit,
    onLogout: () -> Unit,
    onCloneRepo: (owner: String, repo: String) -> Unit,
    onCommitAndPush: (message: String) -> Unit,
    onPullChanges: () -> Unit,
    onCreateRepo: (name: String, desc: String?, isPrivate: Boolean) -> Unit,
    onOpenSshKeys: () -> Unit = {},
    onOpenDiffViewer: () -> Unit = {},
    onOpenWorkflows: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var patInput by remember { mutableStateOf("") }
    var commitMessage by remember { mutableStateOf("") }
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Project Git, 1: My Repositories
    var showCreateRepoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header & User Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isAuthenticated) {
                    Text(
                        text = "Connect GitHub Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter a Personal Access Token (PAT) with repo scope permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = patInput,
                        onValueChange = { patInput = it },
                        placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx") },
                        label = { Text("Personal Access Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (patInput.isNotBlank()) onLoginWithToken(patInput.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IdePurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Authenticate")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(IdePurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentUser?.name ?: currentUser?.login ?: "GitHub User",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "@${currentUser?.login ?: "authenticated"} • ${currentUser?.publicRepos ?: userRepos.size} repos",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar during repo clone
        if (isCloning) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(cloneStatusText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Sub Navigation Tabs
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Active Project Git") }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Remote Repositories") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedSubTab == 0) {
            // Active Project Git Working Tree & History
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Project: ${currentProject?.name ?: "None"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row {
                            TextButton(onClick = onOpenDiffViewer) {
                                Icon(Icons.Default.Difference, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Diff")
                            }
                            TextButton(onClick = onOpenSshKeys) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SSH")
                            }
                            TextButton(onClick = onPullChanges) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pull")
                            }
                            TextButton(onClick = { showCreateRepoDialog = true }) {
                                Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Publish")
                            }
                            TextButton(onClick = onOpenWorkflows) {
                                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, modifier = Modifier.size(14.dp), tint = IdeCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Actions")
                            }
                        }
                    }
                }

                // Commit Input
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Commit Changes", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = commitMessage,
                                onValueChange = { commitMessage = it },
                                placeholder = { Text("e.g. feat: Add Compose UI components") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (commitMessage.isNotBlank()) {
                                        onCommitAndPush(commitMessage.trim())
                                        commitMessage = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CallMerge, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Commit & Push to Main")
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Working Tree Changes (${gitStatusList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = IdeCyan
                    )
                }

                items(gitStatusList) { fileStatus ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("MODIFIED", fontFamily = JetBrainsMonoFontFamily, color = IdeOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(fileStatus.relativePath, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Commit Log History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = IdePurple
                    )
                }

                items(gitCommits) { commit ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(commit.message, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(commit.hash, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = IdeYellow)
                            }
                            Text("${commit.author} • ${commit.timestamp}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            // Remote Repositories List
            if (userRepos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No repositories found or not authenticated.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(userRepos, key = { it.id }) { repo ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(repo.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (repo.private) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge { Text("Private", fontSize = 9.sp) }
                                        }
                                    }
                                    if (!repo.description.isNullOrBlank()) {
                                        Text(repo.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("Default branch: ${repo.defaultBranch}", fontSize = 10.sp, color = IdeCyan)
                                }

                                Button(
                                    onClick = {
                                        val parts = repo.fullName.split("/")
                                        val owner = if (parts.size > 1) parts[0] else repo.name
                                        onCloneRepo(owner, repo.name)
                                    },
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clone", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateRepoDialog) {
        var newRepoName by remember { mutableStateOf(currentProject?.name ?: "MyAndroidApp") }
        var newRepoDesc by remember { mutableStateOf("Android application created with DroidIDE") }
        var isPrivate by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreateRepoDialog = false },
            title = { Text("Publish Project to GitHub") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newRepoName,
                        onValueChange = { newRepoName = it },
                        label = { Text("Repository Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRepoDesc,
                        onValueChange = { newRepoDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                        Text("Private Repository")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRepoName.isNotBlank()) {
                            onCreateRepo(newRepoName.trim(), newRepoDesc.trim(), isPrivate)
                        }
                        showCreateRepoDialog = false
                    }
                ) {
                    Text("Create & Publish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRepoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
