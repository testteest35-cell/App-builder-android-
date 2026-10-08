package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.core.model.*
import com.example.terminal.TerminalLine
import com.example.terminal.TerminalLineType
import com.example.ui.theme.*
import com.example.ui.viewmodel.BottomPanelTab

@Composable
fun BottomConsolePane(
    activeTab: BottomPanelTab?,
    buildSteps: List<BuildStep>,
    buildResult: BuildResult?,
    isBuilding: Boolean,
    logEntries: List<LogEntry>,
    logFilterLevel: LogLevel?,
    logSearchQuery: String,
    terminalLines: List<TerminalLine>,
    gitStatusList: List<GitFileStatus>,
    gitCommits: List<GitCommit>,
    openTabs: List<EditorTab>,
    onSelectTab: (BottomPanelTab) -> Unit,
    onClosePanel: () -> Unit,
    onInstallApk: () -> Unit,
    onShareApk: () -> Unit,
    onSendTerminalCommand: (String) -> Unit,
    onClearTerminal: () -> Unit,
    onSetLogLevel: (LogLevel?) -> Unit,
    onSetLogSearch: (String) -> Unit,
    onClearLogs: () -> Unit,
    onCommitGit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeTab == null) return

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomPanelTab.values().forEach { tab ->
                        val isSelected = tab == activeTab
                        TextButton(
                            onClick = { onSelectTab(tab) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isSelected) IdeCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onClosePanel,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close console",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            HorizontalDivider()

            // Panel Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(IdeBackgroundDark)
            ) {
                when (activeTab) {
                    BottomPanelTab.BUILD -> {
                        BuildConsoleView(
                            steps = buildSteps,
                            result = buildResult,
                            isBuilding = isBuilding,
                            onInstallApk = onInstallApk,
                            onShareApk = onShareApk
                        )
                    }
                    BottomPanelTab.LOGCAT -> {
                        LogcatConsoleView(
                            logs = logEntries,
                            filterLevel = logFilterLevel,
                            searchQuery = logSearchQuery,
                            onSetLevel = onSetLogLevel,
                            onSetSearch = onSetLogSearch,
                            onClear = onClearLogs
                        )
                    }
                    BottomPanelTab.TERMINAL -> {
                        TerminalConsoleView(
                            lines = terminalLines,
                            onSendCommand = onSendTerminalCommand,
                            onClear = onClearTerminal
                        )
                    }
                    BottomPanelTab.PROBLEMS -> {
                        ProblemsConsoleView(openTabs = openTabs)
                    }
                    BottomPanelTab.GIT -> {
                        GitConsoleView(
                            statusList = gitStatusList,
                            commits = gitCommits,
                            onCommit = onCommitGit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildConsoleView(
    steps: List<BuildStep>,
    result: BuildResult?,
    isBuilding: Boolean,
    onInstallApk: () -> Unit,
    onShareApk: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        if (result != null && result.apkFile != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onInstallApk,
                    colors = ButtonDefaults.buttonColors(containerColor = IdeGreen),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Install APK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                FilledTonalButton(
                    onClick = onShareApk,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share APK", fontSize = 11.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(steps, key = { it.id }) { step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (step.state) {
                        StepState.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = IdeCyan)
                        StepState.SUCCESS -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IdeGreen, modifier = Modifier.size(14.dp))
                        StepState.FAILED -> Icon(Icons.Default.Cancel, contentDescription = null, tint = IdeRed, modifier = Modifier.size(14.dp))
                        StepState.PENDING -> Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${step.title} - ${step.description}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = when (step.state) {
                            StepState.RUNNING -> IdeCyan
                            StepState.SUCCESS -> IdeTextPrimary
                            StepState.FAILED -> IdeRed
                            StepState.PENDING -> IdeTextMuted
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (step.durationMs > 0) {
                        Text(
                            text = "${step.durationMs}ms",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = IdeTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogcatConsoleView(
    logs: List<LogEntry>,
    filterLevel: LogLevel?,
    searchQuery: String,
    onSetLevel: (LogLevel?) -> Unit,
    onSetSearch: (String) -> Unit,
    onClear: () -> Unit
) {
    val filteredLogs = remember(logs, filterLevel, searchQuery) {
        logs.filter { entry ->
            val matchLevel = filterLevel == null || entry.level == filterLevel
            val matchSearch = searchQuery.isBlank() || entry.message.contains(searchQuery, ignoreCase = true) || entry.tag.contains(searchQuery, ignoreCase = true)
            matchLevel && matchSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Level Filter Chips
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    FilterChip(
                        selected = filterLevel == null,
                        onClick = { onSetLevel(null) },
                        label = { Text("ALL", fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                }
                items(LogLevel.values()) { level ->
                    FilterChip(
                        selected = filterLevel == level,
                        onClick = { onSetLevel(if (filterLevel == level) null else level) },
                        label = { Text(level.letter, fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear logs", modifier = Modifier.size(16.dp))
            }
        }

        HorizontalDivider(color = Color.DarkGray)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(filteredLogs, key = { it.id }) { log ->
                val color = when (log.level) {
                    LogLevel.VERBOSE -> IdeTextMuted
                    LogLevel.DEBUG -> IdeCyan
                    LogLevel.INFO -> IdeGreen
                    LogLevel.WARN -> IdeYellow
                    LogLevel.ERROR -> IdeRed
                }
                Text(
                    text = "${log.timestamp} ${log.level.letter}/${log.tag}: ${log.message}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun TerminalConsoleView(
    lines: List<TerminalLine>,
    onSendCommand: (String) -> Unit,
    onClear: () -> Unit
) {
    var inputCmd by remember { mutableStateOf("") }
    val quickCmds = listOf("./gradlew assembleDebug", "./gradlew test", "ls", "git status", "adb devices", "clear")

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Action Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(quickCmds) { cmd ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .clickable {
                            if (cmd == "clear") onClear() else onSendCommand(cmd)
                        }
                ) {
                    Text(
                        text = cmd,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Lines stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(lines) { line ->
                val color = when (line.type) {
                    TerminalLineType.INPUT -> IdeCyan
                    TerminalLineType.OUTPUT -> IdeTextPrimary
                    TerminalLineType.ERROR -> IdeRed
                    TerminalLineType.SUCCESS -> IdeGreen
                    TerminalLineType.INFO -> IdeYellow
                }
                Text(
                    text = line.text,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = color
                )
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$ ", fontFamily = JetBrainsMonoFontFamily, color = IdeCyan, fontSize = 12.sp)
            OutlinedTextField(
                value = inputCmd,
                onValueChange = { inputCmd = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("type command...", fontSize = 11.sp) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = {
                    if (inputCmd.isNotBlank()) {
                        onSendCommand(inputCmd.trim())
                        inputCmd = ""
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Run", tint = IdeCyan, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ProblemsConsoleView(openTabs: List<EditorTab>) {
    val allDiagnostics = remember(openTabs) {
        openTabs.flatMap { tab -> tab.diagnostics.map { tab to it } }
    }

    if (allDiagnostics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No problems found in open files.", color = IdeGreen, fontSize = 12.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(allDiagnostics) { (tab, diag) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (diag.severity == DiagnosticSeverity.ERROR) Icons.Default.Cancel else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (diag.severity == DiagnosticSeverity.ERROR) IdeRed else IdeYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${tab.fileName}:${diag.line} - ${diag.message}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun GitConsoleView(
    statusList: List<GitFileStatus>,
    commits: List<GitCommit>,
    onCommit: (String) -> Unit
) {
    var commitMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Commit Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = commitMessage,
                onValueChange = { commitMessage = it },
                placeholder = { Text("Commit message...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (commitMessage.isNotBlank()) {
                        onCommit(commitMessage.trim())
                        commitMessage = ""
                    }
                },
                modifier = Modifier.height(36.dp)
            ) {
                Text("Commit", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Modified files & commit history
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text("Working Changes (${statusList.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = IdeCyan)
            }
            items(statusList) { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "M",
                        fontFamily = JetBrainsMonoFontFamily,
                        color = IdeOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = item.relativePath, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Recent Commits", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = IdePurple)
            }
            items(commits) { commit ->
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    Row {
                        Text(commit.hash, fontFamily = JetBrainsMonoFontFamily, color = IdeYellow, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(commit.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("${commit.author} • ${commit.timestamp}", fontSize = 9.sp, color = IdeTextMuted)
                }
            }
        }
    }
}
