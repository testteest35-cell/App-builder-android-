package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.LogLevel
import com.example.ui.components.*
import com.example.ui.theme.IdeBackgroundDark
import com.example.ui.theme.IdeCyan
import com.example.ui.theme.IdeGreen
import com.example.ui.viewmodel.BottomPanelTab
import com.example.ui.viewmodel.IdeViewModel
import kotlinx.coroutines.launch

@Composable
fun IdeMainScreen(
    viewModel: IdeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val projectsList by viewModel.projectsList.collectAsStateWithLifecycle()
    val fileTree by viewModel.fileTree.collectAsStateWithLifecycle()
    val openTabs by viewModel.openTabs.collectAsStateWithLifecycle()
    val activeTabIndex by viewModel.activeTabIndex.collectAsStateWithLifecycle()
    val settings by viewModel.ideSettings.collectAsStateWithLifecycle()

    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val replaceQuery by viewModel.replaceQuery.collectAsStateWithLifecycle()

    val buildSteps by viewModel.buildSteps.collectAsStateWithLifecycle()
    val buildResult by viewModel.buildResult.collectAsStateWithLifecycle()
    val isBuilding by viewModel.isBuilding.collectAsStateWithLifecycle()

    val logEntries by viewModel.logEntries.collectAsStateWithLifecycle()
    val logFilterLevel by viewModel.logFilterLevel.collectAsStateWithLifecycle()
    val logSearchQuery by viewModel.logSearchQuery.collectAsStateWithLifecycle()

    val terminalLines by viewModel.terminalLines.collectAsStateWithLifecycle()
    val gitStatusList by viewModel.gitStatusList.collectAsStateWithLifecycle()
    val gitCommits by viewModel.gitCommits.collectAsStateWithLifecycle()

    val activeBottomPanel by viewModel.activeBottomPanel.collectAsStateWithLifecycle()

    val isLivePreviewOpen by viewModel.isLivePreviewOpen.collectAsStateWithLifecycle()
    val isProjectWizardOpen by viewModel.isProjectWizardOpen.collectAsStateWithLifecycle()
    val isDependencyManagerOpen by viewModel.isDependencyManagerOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val isBuildStatusDialogOpen by viewModel.isBuildStatusDialogOpen.collectAsStateWithLifecycle()

    BackHandler(enabled = drawerState.isOpen || activeBottomPanel != null) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (activeBottomPanel != null) {
            viewModel.closeBottomPanel()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                FileExplorerDrawer(
                    currentProject = currentProject,
                    fileTree = fileTree,
                    activeFilePath = openTabs.getOrNull(activeTabIndex)?.file?.absolutePath,
                    onSelectFile = { file ->
                        viewModel.openFileInTab(file)
                        scope.launch { drawerState.close() }
                    },
                    onToggleFolder = { viewModel.toggleFolder(it) },
                    onCreateFile = { parent, name, isFolder -> viewModel.createFile(parent, name, isFolder) },
                    onRenameFile = { file, newName -> viewModel.renameFile(file, newName) },
                    onDeleteFile = { viewModel.deleteFile(it) },
                    onRefresh = { viewModel.refreshFileTree() }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                IdeTopBar(
                    currentProject = currentProject,
                    projects = projectsList,
                    isBuilding = isBuilding,
                    onSelectProject = { viewModel.openProject(it) },
                    onNewProject = { viewModel.setProjectWizardOpen(true) },
                    onRun = { viewModel.runApp() },
                    onBuild = { viewModel.startBuild(openDialog = true) },
                    onInstallApk = { viewModel.installApk(context) },
                    onOpenDependencies = { viewModel.setDependencyManagerOpen(true) },
                    onOpenGit = { viewModel.toggleBottomPanel(BottomPanelTab.GIT) },
                    onOpenSettings = { viewModel.setSettingsOpen(true) },
                    onToggleDrawer = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    }
                )
            },
            bottomBar = {
                // Bottom Status & Panel Trigger Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Column {
                        // Bottom Drawer (if open)
                        if (activeBottomPanel != null) {
                            BottomConsolePane(
                                activeTab = activeBottomPanel,
                                buildSteps = buildSteps,
                                buildResult = buildResult,
                                isBuilding = isBuilding,
                                logEntries = logEntries,
                                logFilterLevel = logFilterLevel,
                                logSearchQuery = logSearchQuery,
                                terminalLines = terminalLines,
                                gitStatusList = gitStatusList,
                                gitCommits = gitCommits,
                                openTabs = openTabs,
                                onSelectTab = { viewModel.toggleBottomPanel(it) },
                                onClosePanel = { viewModel.closeBottomPanel() },
                                onInstallApk = { viewModel.installApk(context) },
                                onShareApk = { viewModel.shareApk(context) },
                                onSendTerminalCommand = { viewModel.sendTerminalCommand(it) },
                                onClearTerminal = { viewModel.clearTerminal() },
                                onSetLogLevel = { viewModel.setLogFilterLevel(it) },
                                onSetLogSearch = { viewModel.setLogSearchQuery(it) },
                                onClearLogs = { viewModel.clearLogs() },
                                onCommitGit = { viewModel.commitGit(it) }
                            )
                        }

                        // Bottom Navigation Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BottomPanelTab.values().forEach { tab ->
                                    val isSelected = activeBottomPanel == tab
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.clickable { viewModel.toggleBottomPanel(tab) }
                                    ) {
                                        Text(
                                            text = tab.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Quick Run / Preview Button on the bottom strip
                            FilledTonalButton(
                                onClick = { viewModel.setLivePreviewOpen(true) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) { innerPadding ->
            CodeEditorPane(
                openTabs = openTabs,
                activeTabIndex = activeTabIndex,
                settings = settings,
                isSearchOpen = isSearchOpen,
                searchQuery = searchQuery,
                replaceQuery = replaceQuery,
                onSelectTab = { viewModel.selectTab(it) },
                onCloseTab = { viewModel.closeTab(it) },
                onContentChange = { viewModel.updateEditorContent(it) },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onSave = { viewModel.saveCurrentFile() },
                onToggleSearch = { viewModel.setSearchOpen(!isSearchOpen) },
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onReplaceQueryChange = { viewModel.setReplaceQuery(it) },
                onReplace = { viewModel.replaceInCurrentFile(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    // Modal Dialogs
    if (isLivePreviewOpen) {
        val activeCode = openTabs.getOrNull(activeTabIndex)?.content ?: ""
        LivePreviewDialog(
            project = currentProject,
            codeContent = activeCode,
            onEmitLog = { level, tag, msg -> viewModel.addLog(level, tag, msg) },
            onDismiss = { viewModel.setLivePreviewOpen(false) }
        )
    }

    if (isProjectWizardOpen) {
        ProjectWizardDialog(
            onDismiss = { viewModel.setProjectWizardOpen(false) },
            onCreateProject = { name, pkg, tmpl, sdk ->
                viewModel.createNewProject(name, pkg, tmpl, sdk)
            }
        )
    }

    if (isDependencyManagerOpen) {
        DependencyManagerDialog(
            onDismiss = { viewModel.setDependencyManagerOpen(false) },
            onAddDependency = { viewModel.addDependencyToActiveProject(it) }
        )
    }

    if (isSettingsOpen) {
        SettingsDialog(
            settings = settings,
            onUpdateSettings = { viewModel.updateSettings(it) },
            onDismiss = { viewModel.setSettingsOpen(false) }
        )
    }

    if (isBuildStatusDialogOpen) {
        BuildStatusDialog(
            steps = buildSteps,
            result = buildResult,
            isBuilding = isBuilding,
            onInstallApk = { viewModel.installApk(context) },
            onShareApk = { viewModel.shareApk(context) },
            onDismiss = { viewModel.setBuildStatusDialogOpen(false) }
        )
    }
}
