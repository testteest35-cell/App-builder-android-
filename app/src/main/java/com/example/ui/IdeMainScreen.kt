package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.theme.IdeCyan
import com.example.ui.viewmodel.BottomPanelTab
import com.example.ui.viewmodel.IdeViewModel
import com.example.ui.viewmodel.NavigationTab
import kotlinx.coroutines.launch

@Composable
fun IdeMainScreen(
    viewModel: IdeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentNavTab by viewModel.currentNavTab.collectAsStateWithLifecycle()
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

    val isGitHubAuthenticated by viewModel.isGitHubAuthenticated.collectAsStateWithLifecycle()
    val gitHubUser by viewModel.gitHubUser.collectAsStateWithLifecycle()
    val gitHubRepos by viewModel.gitHubRepos.collectAsStateWithLifecycle()
    val isCloning by viewModel.isCloning.collectAsStateWithLifecycle()
    val cloneStatusText by viewModel.cloneStatusText.collectAsStateWithLifecycle()

    val activeBottomPanel by viewModel.activeBottomPanel.collectAsStateWithLifecycle()
    val isLivePreviewOpen by viewModel.isLivePreviewOpen.collectAsStateWithLifecycle()
    val isProjectWizardOpen by viewModel.isProjectWizardOpen.collectAsStateWithLifecycle()
    val isBuildStatusDialogOpen by viewModel.isBuildStatusDialogOpen.collectAsStateWithLifecycle()
    val isDependencyManagerOpen by viewModel.isDependencyManagerOpen.collectAsStateWithLifecycle()

    val plugins by viewModel.plugins.collectAsStateWithLifecycle()
    val sshKeyInfo by viewModel.sshKeyInfo.collectAsStateWithLifecycle()
    val isDiffViewerOpen by viewModel.isDiffViewerOpen.collectAsStateWithLifecycle()
    val isSshDialogOpen by viewModel.isSshDialogOpen.collectAsStateWithLifecycle()
    val isPluginsDialogOpen by viewModel.isPluginsDialogOpen.collectAsStateWithLifecycle()
    val isWorkflowDialogOpen by viewModel.isWorkflowDialogOpen.collectAsStateWithLifecycle()

    // SAF Zip Import Launcher
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.importProjectZip(inputStream, null, context)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not read ZIP file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = drawerState.isOpen || activeBottomPanel != null || currentNavTab != NavigationTab.EDITOR) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (activeBottomPanel != null) {
            viewModel.closeBottomPanel()
        } else if (currentNavTab != NavigationTab.EDITOR) {
            viewModel.selectNavTab(NavigationTab.EDITOR)
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
                    onOpenGit = { viewModel.selectNavTab(NavigationTab.GITHUB) },
                    onOpenSettings = { viewModel.selectNavTab(NavigationTab.SETTINGS) },
                    onToggleDrawer = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Bottom Dock Panel (when expanded in Editor mode)
                    if (currentNavTab == NavigationTab.EDITOR && activeBottomPanel != null) {
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
                            onCommitGit = { viewModel.commitAndPush(it) }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        tonalElevation = 8.dp,
                        modifier = Modifier.height(64.dp)
                    ) {
                        NavigationBarItem(
                            selected = currentNavTab == NavigationTab.PROJECTS,
                            onClick = { viewModel.selectNavTab(NavigationTab.PROJECTS) },
                            icon = { Icon(Icons.Default.Folder, contentDescription = "Projects") },
                            label = { Text("Projects", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentNavTab == NavigationTab.EDITOR,
                            onClick = { viewModel.selectNavTab(NavigationTab.EDITOR) },
                            icon = { Icon(Icons.Default.Code, contentDescription = "Editor") },
                            label = { Text("Editor", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentNavTab == NavigationTab.GITHUB,
                            onClick = { viewModel.selectNavTab(NavigationTab.GITHUB) },
                            icon = { Icon(Icons.Default.Cloud, contentDescription = "GitHub") },
                            label = { Text("GitHub", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentNavTab == NavigationTab.BUILD,
                            onClick = { viewModel.selectNavTab(NavigationTab.BUILD) },
                            icon = { Icon(Icons.Default.Build, contentDescription = "Build & Run") },
                            label = { Text("Build", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentNavTab == NavigationTab.SETTINGS,
                            onClick = { viewModel.selectNavTab(NavigationTab.SETTINGS) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontSize = 11.sp) }
                        )
                    }
                }
            },
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentNavTab) {
                    NavigationTab.PROJECTS -> {
                        ProjectsScreen(
                            projects = projectsList,
                            currentProject = currentProject,
                            onSelectProject = {
                                viewModel.openProject(it)
                                viewModel.selectNavTab(NavigationTab.EDITOR)
                            },
                            onNewProject = { viewModel.setProjectWizardOpen(true) },
                            onImportZip = { zipPickerLauncher.launch("application/zip") },
                            onCloneGitHub = { viewModel.selectNavTab(NavigationTab.GITHUB) },
                            onExportProject = { viewModel.exportProjectZip(it, context) },
                            onDeleteProject = { viewModel.deleteProject(it) },
                            onOpenWorkflows = { viewModel.setWorkflowDialogOpen(true) }
                        )
                    }

                    NavigationTab.EDITOR -> {
                        Column(modifier = Modifier.fillMaxSize()) {
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
                                modifier = Modifier.weight(1f)
                            )

                            // Quick Dock Strip (Bottom of Editor)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = { viewModel.setLivePreviewOpen(true) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Preview", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    NavigationTab.GITHUB -> {
                        GitHubScreen(
                            isAuthenticated = isGitHubAuthenticated,
                            currentUser = gitHubUser,
                            userRepos = gitHubRepos,
                            currentProject = currentProject,
                            gitStatusList = gitStatusList,
                            gitCommits = gitCommits,
                            isCloning = isCloning,
                            cloneStatusText = cloneStatusText,
                            onLoginWithToken = { viewModel.loginGitHub(it) },
                            onLogout = { viewModel.logoutGitHub() },
                            onCloneRepo = { owner, repo -> viewModel.cloneGitHubRepo(owner, repo) },
                            onCommitAndPush = { viewModel.commitAndPush(it) },
                            onPullChanges = { viewModel.pullChanges() },
                            onCreateRepo = { name, desc, priv -> viewModel.createGitHubRepo(name, desc, priv) },
                            onOpenSshKeys = { viewModel.setSshDialogOpen(true) },
                            onOpenDiffViewer = { viewModel.setDiffViewerOpen(true) },
                            onOpenWorkflows = { viewModel.setWorkflowDialogOpen(true) }
                        )
                    }

                    NavigationTab.BUILD -> {
                        BuildScreen(
                            currentProject = currentProject,
                            buildSteps = buildSteps,
                            buildResult = buildResult,
                            isBuilding = isBuilding,
                            useRemoteBuild = settings.useRemoteBuild,
                            remoteServerUrl = settings.remoteBuildUrl,
                            onToggleRemoteBuild = { viewModel.updateSettings(settings.copy(useRemoteBuild = it)) },
                            onStartBuild = { viewModel.startBuild(openDialog = false) },
                            onInstallApk = { viewModel.installApk(context) },
                            onSaveApkToDownloads = { viewModel.saveApkToDownloads(context) },
                            onShareApk = { viewModel.shareApk(context) },
                            onLaunchPreview = { viewModel.setLivePreviewOpen(true) },
                            onOpenWorkflows = { viewModel.setWorkflowDialogOpen(true) }
                        )
                    }

                    NavigationTab.SETTINGS -> {
                        SettingsScreen(
                            settings = settings,
                            isGitHubAuthenticated = isGitHubAuthenticated,
                            onUpdateSettings = { viewModel.updateSettings(it) },
                            onOpenGitHubTab = { viewModel.selectNavTab(NavigationTab.GITHUB) },
                            onResetDefaultProjects = { viewModel.refreshProjects() },
                            onOpenSshKeys = { viewModel.setSshDialogOpen(true) },
                            onOpenPlugins = { viewModel.setPluginsDialogOpen(true) },
                            onOpenWorkflows = { viewModel.setWorkflowDialogOpen(true) }
                        )
                    }
                }
            }
        }
    }

    // Modal Overlays
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

    if (isWorkflowDialogOpen) {
        GitHubWorkflowDialog(
            project = currentProject,
            onDismiss = { viewModel.setWorkflowDialogOpen(false) },
            onAddWorkflowToProject = { viewModel.addWorkflowToActiveProject(it) }
        )
    }
}
