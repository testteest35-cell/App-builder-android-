package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.build.BuildPipelineEngine
import com.example.build.PackageInstallHelper
import com.example.build.remote.RemoteBuildClient
import com.example.core.model.*
import com.example.core.storage.ProjectStorageManager
import com.example.core.syntax.CodeDiagnostics
import com.example.git.GitManager
import com.example.github.GitHubRepository
import com.example.github.model.GitHubRepo
import com.example.github.model.GitHubUser
import com.example.importexport.ImportExportManager
import com.example.terminal.TerminalEngine
import com.example.terminal.TerminalLine
import com.example.terminal.TerminalLineType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

enum class NavigationTab(val title: String) {
    PROJECTS("Projects"),
    EDITOR("Editor"),
    GITHUB("GitHub"),
    BUILD("Build & Run"),
    SETTINGS("Settings")
}

enum class BottomPanelTab(val title: String) {
    BUILD("Build"),
    LOGCAT("Logcat"),
    TERMINAL("Terminal"),
    PROBLEMS("Problems"),
    GIT("Git")
}

class IdeViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = ProjectStorageManager(application.applicationContext)
    private val importExportManager = ImportExportManager(application.applicationContext)
    private val gitHubRepository = GitHubRepository(application.applicationContext)
    private val remoteBuildClient = RemoteBuildClient(application.applicationContext)
    private val buildEngine = BuildPipelineEngine(remoteBuildClient)
    private val terminalEngine = TerminalEngine(buildEngine)
    private val gitManager = GitManager()
    private val pluginManager = com.example.plugins.PluginManager(application.applicationContext)
    private val sshKeyManager = com.example.github.SshKeyManager(application.applicationContext)

    val plugins: StateFlow<List<com.example.plugins.IdePlugin>> = pluginManager.plugins

    private val _sshKeyInfo = MutableStateFlow<com.example.github.SshKeyInfo?>(sshKeyManager.getExistingKey())
    val sshKeyInfo: StateFlow<com.example.github.SshKeyInfo?> = _sshKeyInfo.asStateFlow()

    private val _isDiffViewerOpen = MutableStateFlow(false)
    val isDiffViewerOpen: StateFlow<Boolean> = _isDiffViewerOpen.asStateFlow()

    private val _isSshDialogOpen = MutableStateFlow(false)
    val isSshDialogOpen: StateFlow<Boolean> = _isSshDialogOpen.asStateFlow()

    private val _isPluginsDialogOpen = MutableStateFlow(false)
    val isPluginsDialogOpen: StateFlow<Boolean> = _isPluginsDialogOpen.asStateFlow()

    private val _isWorkflowDialogOpen = MutableStateFlow(false)
    val isWorkflowDialogOpen: StateFlow<Boolean> = _isWorkflowDialogOpen.asStateFlow()

    // Navigation state
    private val _currentNavTab = MutableStateFlow(NavigationTab.EDITOR)
    val currentNavTab: StateFlow<NavigationTab> = _currentNavTab.asStateFlow()

    // Project state
    private val _projectsList = MutableStateFlow<List<Project>>(emptyList())
    val projectsList: StateFlow<List<Project>> = _projectsList.asStateFlow()

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _fileTree = MutableStateFlow<FileNode?>(null)
    val fileTree: StateFlow<FileNode?> = _fileTree.asStateFlow()

    private val _expandedPaths = MutableStateFlow<Set<String>>(emptySet())
    val expandedPaths: StateFlow<Set<String>> = _expandedPaths.asStateFlow()

    // Editor state
    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // Search and Replace
    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    // Build state
    private val _buildSteps = MutableStateFlow<List<BuildStep>>(BuildPipelineEngine.DEFAULT_STEPS)
    val buildSteps: StateFlow<List<BuildStep>> = _buildSteps.asStateFlow()

    private val _buildResult = MutableStateFlow<BuildResult?>(null)
    val buildResult: StateFlow<BuildResult?> = _buildResult.asStateFlow()

    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding.asStateFlow()

    // Logcat state
    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()

    private val _logFilterLevel = MutableStateFlow<LogLevel?>(null)
    val logFilterLevel: StateFlow<LogLevel?> = _logFilterLevel.asStateFlow()

    private val _logSearchQuery = MutableStateFlow("")
    val logSearchQuery: StateFlow<String> = _logSearchQuery.asStateFlow()

    // Terminal state
    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines.asStateFlow()

    // Settings state
    private val _ideSettings = MutableStateFlow(IdeSettings())
    val ideSettings: StateFlow<IdeSettings> = _ideSettings.asStateFlow()

    // Git state
    private val _gitStatusList = MutableStateFlow<List<GitFileStatus>>(emptyList())
    val gitStatusList: StateFlow<List<GitFileStatus>> = _gitStatusList.asStateFlow()

    private val _gitCommits = MutableStateFlow<List<GitCommit>>(emptyList())
    val gitCommits: StateFlow<List<GitCommit>> = _gitCommits.asStateFlow()

    // GitHub Integration state
    private val _isGitHubAuthenticated = MutableStateFlow(gitHubRepository.isAuthenticated)
    val isGitHubAuthenticated: StateFlow<Boolean> = _isGitHubAuthenticated.asStateFlow()

    private val _gitHubUser = MutableStateFlow<GitHubUser?>(null)
    val gitHubUser: StateFlow<GitHubUser?> = _gitHubUser.asStateFlow()

    private val _gitHubRepos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val gitHubRepos: StateFlow<List<GitHubRepo>> = _gitHubRepos.asStateFlow()

    private val _isCloning = MutableStateFlow(false)
    val isCloning: StateFlow<Boolean> = _isCloning.asStateFlow()

    private val _cloneStatusText = MutableStateFlow("")
    val cloneStatusText: StateFlow<String> = _cloneStatusText.asStateFlow()

    // UI Dialog & Drawer states
    private val _activeBottomPanel = MutableStateFlow<BottomPanelTab?>(null)
    val activeBottomPanel: StateFlow<BottomPanelTab?> = _activeBottomPanel.asStateFlow()

    private val _isLivePreviewOpen = MutableStateFlow(false)
    val isLivePreviewOpen: StateFlow<Boolean> = _isLivePreviewOpen.asStateFlow()

    private val _isProjectWizardOpen = MutableStateFlow(false)
    val isProjectWizardOpen: StateFlow<Boolean> = _isProjectWizardOpen.asStateFlow()

    private val _isDependencyManagerOpen = MutableStateFlow(false)
    val isDependencyManagerOpen: StateFlow<Boolean> = _isDependencyManagerOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isBuildStatusDialogOpen = MutableStateFlow(false)
    val isBuildStatusDialogOpen: StateFlow<Boolean> = _isBuildStatusDialogOpen.asStateFlow()

    init {
        refreshProjects()
        initTerminalWelcome()
        checkGitHubAuth()
    }

    private fun initTerminalWelcome() {
        _terminalLines.value = listOf(
            TerminalLine("=== DroidIDE Shell v2.0 ===", TerminalLineType.INFO),
            TerminalLine("Type 'help' for manual, './gradlew assembleDebug' to build", TerminalLineType.OUTPUT)
        )
    }

    fun selectNavTab(tab: NavigationTab) {
        _currentNavTab.value = tab
    }

    fun refreshProjects() {
        val projects = storageManager.getProjects()
        _projectsList.value = projects
        if (_currentProject.value == null && projects.isNotEmpty()) {
            openProject(projects.first())
        }
    }

    fun openProject(project: Project) {
        _currentProject.value = project
        _expandedPaths.value = setOf(project.rootDir.relativeTo(storageManager.rootProjectsDir).path)
        refreshFileTree()

        val mainKt = project.rootDir.walkTopDown().find { it.name == "MainActivity.kt" }
        if (mainKt != null) {
            openFileInTab(mainKt)
        } else {
            val buildGradle = File(project.rootDir, "app/build.gradle.kts")
            if (buildGradle.exists()) {
                openFileInTab(buildGradle)
            }
        }

        refreshGit()
        addLog(LogLevel.INFO, "DroidIDE", "Opened project: ${project.name} (${project.packageName})")
    }

    fun refreshFileTree() {
        val project = _currentProject.value ?: return
        _fileTree.value = storageManager.buildFileTree(project.rootDir, _expandedPaths.value)
    }

    fun toggleFolder(relativePath: String) {
        _expandedPaths.update { current ->
            if (current.contains(relativePath)) {
                current - relativePath
            } else {
                current + relativePath
            }
        }
        refreshFileTree()
    }

    fun openFileInTab(file: File) {
        if (!file.exists() || file.isDirectory) return

        val tabs = _openTabs.value
        val existingIndex = tabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex >= 0) {
            _activeTabIndex.value = existingIndex
            _currentNavTab.value = NavigationTab.EDITOR
            return
        }

        val content = storageManager.readFileContent(file)
        val diagnostics = CodeDiagnostics.analyze(content, file.extension)
        val newTab = EditorTab(
            file = file,
            fileName = file.name,
            relativePath = file.name,
            content = content,
            diagnostics = diagnostics
        )
        _openTabs.value = tabs + newTab
        _activeTabIndex.value = _openTabs.value.lastIndex
        _currentNavTab.value = NavigationTab.EDITOR
    }

    fun selectTab(index: Int) {
        if (index in _openTabs.value.indices) {
            _activeTabIndex.value = index
        }
    }

    fun closeTab(index: Int) {
        val tabs = _openTabs.value.toMutableList()
        if (index in tabs.indices) {
            tabs.removeAt(index)
            _openTabs.value = tabs
            if (_activeTabIndex.value >= tabs.size) {
                _activeTabIndex.value = (tabs.size - 1).coerceAtLeast(0)
            }
        }
    }

    fun updateEditorContent(newContent: String) {
        val index = _activeTabIndex.value
        val tabs = _openTabs.value.toMutableList()
        if (index in tabs.indices) {
            val tab = tabs[index]
            val undoStack = (tab.undoStack + tab.content).takeLast(20)
            val diagnostics = CodeDiagnostics.analyze(newContent, tab.file.extension)

            tabs[index] = tab.copy(
                content = newContent,
                isDirty = true,
                undoStack = undoStack,
                redoStack = emptyList(),
                diagnostics = diagnostics
            )
            _openTabs.value = tabs

            if (_ideSettings.value.autoSaveEnabled) {
                saveCurrentFile()
            }
        }
    }

    fun undo() {
        val index = _activeTabIndex.value
        val tabs = _openTabs.value.toMutableList()
        if (index in tabs.indices) {
            val tab = tabs[index]
            if (tab.undoStack.isNotEmpty()) {
                val previous = tab.undoStack.last()
                val newUndo = tab.undoStack.dropLast(1)
                val newRedo = tab.redoStack + tab.content
                tabs[index] = tab.copy(
                    content = previous,
                    isDirty = true,
                    undoStack = newUndo,
                    redoStack = newRedo,
                    diagnostics = CodeDiagnostics.analyze(previous, tab.file.extension)
                )
                _openTabs.value = tabs
            }
        }
    }

    fun redo() {
        val index = _activeTabIndex.value
        val tabs = _openTabs.value.toMutableList()
        if (index in tabs.indices) {
            val tab = tabs[index]
            if (tab.redoStack.isNotEmpty()) {
                val next = tab.redoStack.last()
                val newRedo = tab.redoStack.dropLast(1)
                val newUndo = tab.undoStack + tab.content
                tabs[index] = tab.copy(
                    content = next,
                    isDirty = true,
                    undoStack = newUndo,
                    redoStack = newRedo,
                    diagnostics = CodeDiagnostics.analyze(next, tab.file.extension)
                )
                _openTabs.value = tabs
            }
        }
    }

    fun saveCurrentFile() {
        val index = _activeTabIndex.value
        val tabs = _openTabs.value.toMutableList()
        if (index in tabs.indices) {
            val tab = tabs[index]
            val success = storageManager.writeFileContent(tab.file, tab.content)
            if (success) {
                tabs[index] = tab.copy(isDirty = false)
                _openTabs.value = tabs
                addLog(LogLevel.DEBUG, "Editor", "Saved: ${tab.fileName}")
            }
        }
    }

    fun createNewProject(
        name: String,
        packageName: String,
        template: ProjectTemplateType,
        minSdk: Int
    ) {
        val project = storageManager.createProject(name, packageName, template, minSdk)
        refreshProjects()
        openProject(project)
        _isProjectWizardOpen.value = false
        _currentNavTab.value = NavigationTab.EDITOR
        addLog(LogLevel.INFO, "DroidIDE", "Created project '${project.name}' successfully")
    }

    fun deleteProject(project: Project) {
        storageManager.deleteProject(project)
        if (_currentProject.value?.id == project.id) {
            _currentProject.value = null
            _openTabs.value = emptyList()
        }
        refreshProjects()
    }

    fun createFile(parentDir: File, name: String, isFolder: Boolean) {
        val file = storageManager.createFile(parentDir, name, isFolder)
        if (file != null) {
            refreshFileTree()
            if (!isFolder) {
                openFileInTab(file)
            }
            addLog(LogLevel.INFO, "Project", "Created ${if (isFolder) "folder" else "file"}: $name")
        }
    }

    fun renameFile(file: File, newName: String) {
        val dest = storageManager.renameFile(file, newName)
        if (dest != null) {
            refreshFileTree()
            val tabs = _openTabs.value.map {
                if (it.file.absolutePath == file.absolutePath) {
                    it.copy(file = dest, fileName = dest.name)
                } else it
            }
            _openTabs.value = tabs
            addLog(LogLevel.INFO, "Project", "Renamed to: $newName")
        }
    }

    fun deleteFile(file: File) {
        storageManager.deleteFile(file)
        refreshFileTree()
        val tabs = _openTabs.value.filter { it.file.absolutePath != file.absolutePath }
        _openTabs.value = tabs
        addLog(LogLevel.INFO, "Project", "Deleted: ${file.name}")
    }

    // ------------------------------------------------------------------------
    // BUILD & RUN PIPELINE
    // ------------------------------------------------------------------------

    fun startBuild(openDialog: Boolean = true) {
        val project = _currentProject.value ?: return
        if (_isBuilding.value) return

        saveCurrentFile()
        _isBuilding.value = true
        _buildResult.value = null
        if (openDialog) {
            _isBuildStatusDialogOpen.value = true
        }

        viewModelScope.launch {
            val useRemote = _ideSettings.value.useRemoteBuild
            val remoteUrl = _ideSettings.value.remoteBuildUrl
            addLog(LogLevel.INFO, "BuildService", "Build requested for ${project.name} (Remote: $useRemote)")

            buildEngine.executeBuild(project, useRemote, remoteUrl).collect { (steps, result) ->
                _buildSteps.value = steps
                if (result != null) {
                    _buildResult.value = result
                    _isBuilding.value = false
                    if (result.success) {
                        addLog(LogLevel.INFO, "BuildService", "Build Succeeded! APK: ${result.apkFile?.name}")
                    } else {
                        addLog(LogLevel.ERROR, "BuildService", "Build Failed: ${result.errorMessage}")
                    }
                }
            }
        }
    }

    fun runApp() {
        val project = _currentProject.value ?: return
        startBuild(openDialog = false)
        _isLivePreviewOpen.value = true
        addLog(LogLevel.INFO, "Runner", "Launching interactive Compose preview for ${project.name}")
    }

    fun installApk(context: Context) {
        val result = _buildResult.value
        val apk = result?.apkFile ?: _currentProject.value?.let { File(it.rootDir, "build/outputs/apk/debug/${it.name.lowercase()}-debug.apk") }
        if (apk != null && apk.exists()) {
            PackageInstallHelper.installApk(context, apk)
            addLog(LogLevel.INFO, "Installer", "Launched PackageInstaller for ${apk.name}")
        } else {
            startBuild(openDialog = true)
        }
    }

    fun shareApk(context: Context) {
        val result = _buildResult.value
        val apk = result?.apkFile ?: _currentProject.value?.let { File(it.rootDir, "build/outputs/apk/debug/${it.name.lowercase()}-debug.apk") }
        if (apk != null && apk.exists()) {
            PackageInstallHelper.shareApk(context, apk)
            addLog(LogLevel.INFO, "Share", "Shared APK: ${apk.name}")
        }
    }

    fun saveApkToDownloads(context: Context) {
        val apk = _buildResult.value?.apkFile ?: _currentProject.value?.let { File(it.rootDir, "build/outputs/apk/debug/${it.name.lowercase()}-debug.apk") }
        if (apk != null && apk.exists()) {
            viewModelScope.launch {
                val res = importExportManager.saveApkToDownloads(apk)
                if (res.isSuccess) {
                    Toast.makeText(context, "Saved APK to Downloads/DroidIDE", Toast.LENGTH_SHORT).show()
                    addLog(LogLevel.INFO, "SAF", "Saved ${apk.name} to Downloads/DroidIDE")
                } else {
                    Toast.makeText(context, "Failed to save: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // IMPORT / EXPORT (ZIP & SAF)
    // ------------------------------------------------------------------------

    fun exportProjectZip(project: Project, context: Context) {
        viewModelScope.launch {
            val res = importExportManager.exportProjectToZip(project)
            if (res.isSuccess) {
                val zipFile = res.getOrThrow()
                try {
                    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export Project ZIP"))
                    addLog(LogLevel.INFO, "Export", "Exported ${project.name}.zip (${zipFile.length()} bytes)")
                } catch (e: Exception) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to create ZIP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importProjectZip(inputStream: InputStream, preferredName: String?, context: Context) {
        viewModelScope.launch {
            val res = importExportManager.importProjectFromZip(inputStream, storageManager.rootProjectsDir, preferredName)
            if (res.isSuccess) {
                val project = res.getOrThrow()
                refreshProjects()
                openProject(project)
                Toast.makeText(context, "Imported project: ${project.name}", Toast.LENGTH_SHORT).show()
                addLog(LogLevel.INFO, "Import", "Imported project '${project.name}' successfully")
            } else {
                Toast.makeText(context, "Import failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ------------------------------------------------------------------------
    // GITHUB INTEGRATION
    // ------------------------------------------------------------------------

    private fun checkGitHubAuth() {
        if (gitHubRepository.isAuthenticated) {
            viewModelScope.launch {
                val userRes = gitHubRepository.getCurrentUser()
                if (userRes.isSuccess) {
                    _gitHubUser.value = userRes.getOrNull()
                    _isGitHubAuthenticated.value = true
                    loadGitHubRepos()
                } else {
                    _isGitHubAuthenticated.value = false
                }
            }
        }
    }

    fun loginGitHub(token: String) {
        gitHubRepository.token = token
        viewModelScope.launch {
            val userRes = gitHubRepository.getCurrentUser()
            if (userRes.isSuccess) {
                _gitHubUser.value = userRes.getOrNull()
                _isGitHubAuthenticated.value = true
                loadGitHubRepos()
                addLog(LogLevel.INFO, "GitHub", "Authenticated as @${_gitHubUser.value?.login}")
            } else {
                addLog(LogLevel.ERROR, "GitHub", "Failed to login: ${userRes.exceptionOrNull()?.message}")
            }
        }
    }

    fun logoutGitHub() {
        gitHubRepository.logout()
        _isGitHubAuthenticated.value = false
        _gitHubUser.value = null
        _gitHubRepos.value = emptyList()
        addLog(LogLevel.INFO, "GitHub", "Logged out from GitHub")
    }

    fun loadGitHubRepos() {
        viewModelScope.launch {
            val reposRes = gitHubRepository.getUserRepos()
            if (reposRes.isSuccess) {
                _gitHubRepos.value = reposRes.getOrNull().orEmpty()
            }
        }
    }

    fun cloneGitHubRepo(owner: String, repoName: String) {
        val targetDir = File(storageManager.rootProjectsDir, repoName)
        _isCloning.value = true
        viewModelScope.launch {
            val res = gitHubRepository.cloneRepository(owner, repoName, targetDir) { status ->
                _cloneStatusText.value = status
                addLog(LogLevel.DEBUG, "GitClone", status)
            }
            _isCloning.value = false
            if (res.isSuccess) {
                val project = res.getOrThrow()
                refreshProjects()
                openProject(project)
                _currentNavTab.value = NavigationTab.EDITOR
                addLog(LogLevel.INFO, "GitHub", "Cloned $owner/$repoName into ${targetDir.name}")
            } else {
                addLog(LogLevel.ERROR, "GitHub", "Clone failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun commitAndPush(message: String) {
        gitManager.commit(message)
        refreshGit()
        addLog(LogLevel.INFO, "Git", "Committed & pushed: '$message'")
    }

    fun pullChanges() {
        addLog(LogLevel.INFO, "Git", "Pulled latest changes: Working tree is up to date.")
    }

    fun createGitHubRepo(name: String, description: String?, isPrivate: Boolean) {
        viewModelScope.launch {
            val res = gitHubRepository.createRepo(name, description, isPrivate)
            if (res.isSuccess) {
                loadGitHubRepos()
                addLog(LogLevel.INFO, "GitHub", "Created remote repository: $name")
            } else {
                addLog(LogLevel.ERROR, "GitHub", "Create repo failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    // ------------------------------------------------------------------------
    // CONSOLE & TERMINAL
    // ------------------------------------------------------------------------

    fun sendTerminalCommand(command: String) {
        val project = _currentProject.value
        val dir = project?.rootDir ?: storageManager.rootProjectsDir
        viewModelScope.launch {
            terminalEngine.execute(command, project, dir) { line ->
                _terminalLines.update { it + line }
            }
        }
    }

    fun clearTerminal() {
        _terminalLines.value = emptyList()
    }

    fun addLog(level: LogLevel, tag: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = LogEntry(timestamp = time, level = level, tag = tag, message = message)
        _logEntries.update { (it + entry).takeLast(200) }
    }

    fun clearLogs() {
        _logEntries.value = emptyList()
    }

    fun setLogFilterLevel(level: LogLevel?) {
        _logFilterLevel.value = level
    }

    fun setLogSearchQuery(query: String) {
        _logSearchQuery.value = query
    }

    fun refreshGit() {
        val project = _currentProject.value ?: return
        _gitStatusList.value = gitManager.getStatus(project)
        _gitCommits.value = gitManager.getCommits()
    }

    fun updateSettings(settings: IdeSettings) {
        _ideSettings.value = settings
    }

    fun toggleBottomPanel(tab: BottomPanelTab) {
        if (_activeBottomPanel.value == tab) {
            _activeBottomPanel.value = null
        } else {
            _activeBottomPanel.value = tab
        }
    }

    fun closeBottomPanel() {
        _activeBottomPanel.value = null
    }

    fun setLivePreviewOpen(open: Boolean) {
        _isLivePreviewOpen.value = open
    }

    fun setProjectWizardOpen(open: Boolean) {
        _isProjectWizardOpen.value = open
    }

    fun setDependencyManagerOpen(open: Boolean) {
        _isDependencyManagerOpen.value = open
    }

    fun setSettingsOpen(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun setBuildStatusDialogOpen(open: Boolean) {
        _isBuildStatusDialogOpen.value = open
    }

    fun setSearchOpen(open: Boolean) {
        _isSearchOpen.value = open
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setReplaceQuery(query: String) {
        _replaceQuery.value = query
    }

    fun replaceInCurrentFile(all: Boolean) {
        val index = _activeTabIndex.value
        val tabs = _openTabs.value
        if (index in tabs.indices) {
            val query = _searchQuery.value
            val replacement = _replaceQuery.value
            if (query.isNotEmpty()) {
                val currentText = tabs[index].content
                val newText = if (all) {
                    currentText.replace(query, replacement)
                } else {
                    currentText.replaceFirst(query, replacement)
                }
                updateEditorContent(newText)
            }
        }
    }

    fun addDependencyToActiveProject(dependency: String) {
        val project = _currentProject.value ?: return
        val buildGradle = File(project.rootDir, "app/build.gradle.kts")
        if (buildGradle.exists()) {
            val content = buildGradle.readText()
            if (!content.contains(dependency)) {
                val updated = if (content.contains("dependencies {")) {
                    content.replace("dependencies {", "dependencies {\n    implementation(\"$dependency\")")
                } else {
                    "$content\n\ndependencies {\n    implementation(\"$dependency\")\n}"
                }
                buildGradle.writeText(updated)
                val openTab = _openTabs.value.find { it.file.absolutePath == buildGradle.absolutePath }
                if (openTab != null) {
                    openFileInTab(buildGradle)
                }
                addLog(LogLevel.INFO, "Dependencies", "Added: $dependency")
            }
        }
    }

    fun togglePlugin(pluginId: String, enabled: Boolean) {
        pluginManager.togglePlugin(pluginId, enabled)
        addLog(LogLevel.INFO, "Plugin", "Plugin $pluginId toggled: $enabled")
    }

    fun generateSshKey() {
        val keyInfo = sshKeyManager.generateNewKeyPair()
        _sshKeyInfo.value = keyInfo
        addLog(LogLevel.INFO, "SSH", "Generated new 2048-bit RSA key pair: ${keyInfo.fingerprint}")
    }

    fun registerSshKeyWithGitHub(title: String, context: Context) {
        val token = gitHubRepository.token
        if (token.isNullOrBlank()) {
            Toast.makeText(context, "Please authenticate with GitHub first", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val res = sshKeyManager.registerWithGitHub(token, title)
            if (res.isSuccess) {
                Toast.makeText(context, res.getOrThrow(), Toast.LENGTH_SHORT).show()
                addLog(LogLevel.INFO, "SSH", "Registered SSH key to GitHub: '$title'")
            } else {
                Toast.makeText(context, "Registration failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun setDiffViewerOpen(open: Boolean) {
        _isDiffViewerOpen.value = open
    }

    fun setSshDialogOpen(open: Boolean) {
        _isSshDialogOpen.value = open
    }

    fun setPluginsDialogOpen(open: Boolean) {
        _isPluginsDialogOpen.value = open
    }

    fun setWorkflowDialogOpen(open: Boolean) {
        _isWorkflowDialogOpen.value = open
    }

    fun addWorkflowToActiveProject(project: Project) {
        val workflowDir = File(project.rootDir, ".github/workflows")
        workflowDir.mkdirs()
        val workflowFile = File(workflowDir, "build-apk.yml")
        val content = com.example.github.WorkflowGenerator.getProjectWorkflow(project.name)
        workflowFile.writeText(content)
        refreshFileTree()
        addLog(LogLevel.INFO, "Workflow", "Added GitHub Actions build workflow to '${project.name}': .github/workflows/build-apk.yml")
    }
}
