package com.example.core.model

import java.io.File

/**
 * Representation of an Android Project inside the IDE
 */
data class Project(
    val id: String,
    val name: String,
    val packageName: String,
    val template: ProjectTemplateType,
    val minSdk: Int = 26,
    val targetSdk: Int = 36,
    val rootDir: File,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

enum class ProjectTemplateType(val displayName: String, val description: String) {
    COMPOSE_EMPTY("Empty Compose App", "Minimal Jetpack Compose application with Material 3"),
    COMPOSE_NOTES("TaskCraft Notes", "Full Compose app with ViewModel, State & Task list"),
    COMPOSE_COUNTER("Interactive Counter", "Jetpack Compose sample with reactive state & animations"),
    BASIC_VIEWS("Basic Views & Navigation", "Traditional XML layout app with ViewBinding"),
    EMPTY_LIBRARY("Android Library", "Reusable Android library module with sample tests")
}

/**
 * Node in the project file explorer tree
 */
data class FileNode(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val relativePath: String,
    val children: List<FileNode> = emptyList(),
    val isExpanded: Boolean = false,
    val depth: Int = 0
) {
    val extension: String
        get() = if (isDirectory) "" else file.extension.lowercase()
}

/**
 * Represents an open tab in the code editor
 */
data class EditorTab(
    val file: File,
    val fileName: String = file.name,
    val relativePath: String,
    val content: String,
    val isDirty: Boolean = false,
    val undoStack: List<String> = emptyList(),
    val redoStack: List<String> = emptyList(),
    val cursorPosition: Int = 0,
    val diagnostics: List<DiagnosticItem> = emptyList()
)

/**
 * Diagnostic error/warning
 */
data class DiagnosticItem(
    val line: Int,
    val message: String,
    val severity: DiagnosticSeverity
)

enum class DiagnosticSeverity {
    ERROR, WARNING, INFO
}

/**
 * Build pipeline execution status & progress
 */
enum class BuildState {
    IDLE, IN_PROGRESS, SUCCESS, FAILED
}

data class BuildStep(
    val id: String,
    val title: String,
    val description: String,
    val state: StepState = StepState.PENDING,
    val durationMs: Long = 0L,
    val errorOutput: String? = null
)

enum class StepState {
    PENDING, RUNNING, SUCCESS, FAILED
}

data class BuildResult(
    val success: Boolean,
    val apkFile: File? = null,
    val totalDurationMs: Long = 0L,
    val logs: List<String> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Logcat entries
 */
data class LogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class LogLevel(val letter: String) {
    VERBOSE("V"),
    DEBUG("D"),
    INFO("I"),
    WARN("W"),
    ERROR("E")
}

/**
 * Editor settings
 */
enum class EditorThemeType(val displayName: String) {
    DARCULA("Darcula (Android Studio)"),
    ONE_DARK("One Dark Pro"),
    MONOKAI("Monokai"),
    CYBERPUNK("Cyberpunk Neon")
}

data class IdeSettings(
    val fontSizeSp: Int = 13,
    val theme: EditorThemeType = EditorThemeType.DARCULA,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val autoSaveEnabled: Boolean = true,
    val indentSpaces: Int = 4,
    val remoteBuildUrl: String = "https://build.droidide.dev/api/v1/build",
    val useRemoteBuild: Boolean = false
)

/**
 * Autocomplete item
 */
data class AutocompleteSuggestion(
    val label: String,
    val insertText: String,
    val detail: String,
    val category: SuggestionCategory
)

enum class SuggestionCategory {
    KEYWORD,
    COMPOSABLE,
    MODIFIER,
    TYPE,
    SNIPPET
}

/**
 * Git models
 */
data class GitFileStatus(
    val file: File,
    val relativePath: String,
    val status: GitStatusType
)

enum class GitStatusType {
    MODIFIED, UNTRACKED, STAGED
}

data class GitCommit(
    val hash: String,
    val author: String,
    val message: String,
    val timestamp: String
)
