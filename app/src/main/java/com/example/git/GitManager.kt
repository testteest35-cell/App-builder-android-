package com.example.git

import com.example.core.model.GitCommit
import com.example.core.model.GitFileStatus
import com.example.core.model.GitStatusType
import com.example.core.model.Project
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GitManager {

    private val commitHistory = mutableListOf(
        GitCommit(
            hash = "7f2a89c",
            author = "Developer <dev@droidide.io>",
            message = "feat: Setup project architecture with Jetpack Compose",
            timestamp = "10 mins ago"
        ),
        GitCommit(
            hash = "3e1b04d",
            author = "Developer <dev@droidide.io>",
            message = "chore: Configure Gradle Kotlin DSL & dependencies",
            timestamp = "25 mins ago"
        )
    )

    fun getStatus(project: Project): List<GitFileStatus> {
        val list = mutableListOf<GitFileStatus>()
        val files = project.rootDir.walkTopDown()
            .filter { it.isFile && !it.path.contains("/build/") && !it.path.contains("/.gradle/") }
            .toList()

        for (file in files) {
            val relative = file.relativeTo(project.rootDir).path
            // If file was modified in the last 15 minutes, show as modified
            if (System.currentTimeMillis() - file.lastModified() < 900_000) {
                list.add(GitFileStatus(file, relative, GitStatusType.MODIFIED))
            }
        }

        if (list.isEmpty() && files.isNotEmpty()) {
            val first = files.first()
            list.add(GitFileStatus(first, first.relativeTo(project.rootDir).path, GitStatusType.MODIFIED))
        }

        return list
    }

    fun getCommits(): List<GitCommit> = commitHistory.toList()

    fun commit(message: String): GitCommit {
        val newCommit = GitCommit(
            hash = UUID.randomUUID().toString().substring(0, 7),
            author = "Developer <dev@droidide.io>",
            message = message,
            timestamp = "Just now"
        )
        commitHistory.add(0, newCommit)
        return newCommit
    }
}
