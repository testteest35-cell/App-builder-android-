package com.example.core.storage

import android.content.Context
import com.example.core.model.FileNode
import com.example.core.model.Project
import com.example.core.model.ProjectTemplateType
import com.example.project.ProjectTemplates
import java.io.File

class ProjectStorageManager(private val context: Context) {

    val rootProjectsDir: File
        get() {
            val dir = File(context.filesDir, "droid_ide_projects")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    init {
        ensureDefaultProjects()
    }

    private fun ensureDefaultProjects() {
        val projects = getProjects()
        if (projects.isEmpty()) {
            // Seed sample 1: Compose Counter
            createProject(
                name = "ComposeCounter",
                packageName = "com.sample.counter",
                template = ProjectTemplateType.COMPOSE_COUNTER,
                minSdk = 26
            )
            // Seed sample 2: TaskCraft Pro
            createProject(
                name = "TaskCraft",
                packageName = "com.sample.taskcraft",
                template = ProjectTemplateType.COMPOSE_NOTES,
                minSdk = 26
            )
        }
    }

    fun getProjects(): List<Project> {
        val root = rootProjectsDir
        val dirs = root.listFiles { file -> file.isDirectory } ?: return emptyList()

        return dirs.mapNotNull { dir ->
            readProjectMetadata(dir)
        }.sortedByDescending { it.lastModified }
    }

    private fun readProjectMetadata(dir: File): Project? {
        val manifest = File(dir, "app/src/main/AndroidManifest.xml")
        val gradle = File(dir, "app/build.gradle.kts")

        var packageName = "com.example.${dir.name.lowercase()}"
        var minSdk = 26

        if (gradle.exists()) {
            val content = gradle.readText()
            val namespaceMatch = Regex("namespace\\s*=\\s*\"([^\"]+)\"").find(content)
            if (namespaceMatch != null) {
                packageName = namespaceMatch.groupValues[1]
            }
            val minSdkMatch = Regex("minSdk\\s*=\\s*([0-9]+)").find(content)
            if (minSdkMatch != null) {
                minSdk = minSdkMatch.groupValues[1].toIntOrNull() ?: 26
            }
        }

        val template = if (dir.name.contains("Counter", ignoreCase = true)) {
            ProjectTemplateType.COMPOSE_COUNTER
        } else if (dir.name.contains("Task", ignoreCase = true) || dir.name.contains("Note", ignoreCase = true)) {
            ProjectTemplateType.COMPOSE_NOTES
        } else {
            ProjectTemplateType.COMPOSE_EMPTY
        }

        return Project(
            id = dir.name,
            name = dir.name,
            packageName = packageName,
            template = template,
            minSdk = minSdk,
            rootDir = dir,
            createdAt = dir.lastModified(),
            lastModified = dir.lastModified()
        )
    }

    fun createProject(
        name: String,
        packageName: String,
        template: ProjectTemplateType,
        minSdk: Int
    ): Project {
        val sanitizedName = name.replace("[^a-zA-Z0-9_]".toRegex(), "")
        val projectDir = File(rootProjectsDir, sanitizedName)
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }

        val templateFiles = ProjectTemplates.getFilesForTemplate(
            projectName = sanitizedName,
            packageName = packageName,
            template = template,
            minSdk = minSdk
        )

        for (tf in templateFiles) {
            val file = File(projectDir, tf.relativePath)
            file.parentFile?.mkdirs()
            file.writeText(tf.content)
        }

        return Project(
            id = sanitizedName,
            name = sanitizedName,
            packageName = packageName,
            template = template,
            minSdk = minSdk,
            rootDir = projectDir
        )
    }

    fun deleteProject(project: Project): Boolean {
        return project.rootDir.deleteRecursively()
    }

    fun buildFileTree(rootDir: File, expandedPaths: Set<String> = emptySet(), depth: Int = 0): FileNode {
        val children = if (rootDir.isDirectory) {
            val rawFiles = rootDir.listFiles()?.toList().orEmpty()
            // Folders first, then files sorted alphabetically; filter hidden gradle caches if desired
            val sorted = rawFiles.sortedWith(
                compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
            )
            sorted.map { child ->
                val childRelative = child.relativeTo(rootProjectsDir).path
                val isExpanded = expandedPaths.contains(childRelative) || depth == 0
                buildFileTree(child, expandedPaths, depth + 1).copy(isExpanded = isExpanded)
            }
        } else {
            emptyList()
        }

        val relativePath = rootDir.relativeTo(rootProjectsDir).path
        return FileNode(
            file = rootDir,
            name = rootDir.name,
            isDirectory = rootDir.isDirectory,
            relativePath = relativePath,
            children = children,
            isExpanded = expandedPaths.contains(relativePath) || depth == 0,
            depth = depth
        )
    }

    fun readFileContent(file: File): String {
        return if (file.exists() && file.isFile) {
            file.readText()
        } else {
            ""
        }
    }

    fun writeFileContent(file: File, content: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun createFile(parentDir: File, fileName: String, isFolder: Boolean): File? {
        val target = File(parentDir, fileName)
        return try {
            if (isFolder) {
                if (target.mkdirs()) target else null
            } else {
                target.parentFile?.mkdirs()
                if (target.createNewFile()) target else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun renameFile(file: File, newName: String): File? {
        val dest = File(file.parentFile, newName)
        return if (file.renameTo(dest)) dest else null
    }

    fun deleteFile(file: File): Boolean {
        return file.deleteRecursively()
    }
}
