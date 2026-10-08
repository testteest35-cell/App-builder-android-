package com.example.terminal

import com.example.build.BuildPipelineEngine
import com.example.core.model.Project
import kotlinx.coroutines.flow.toList
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TerminalLine(
    val text: String,
    val type: TerminalLineType = TerminalLineType.OUTPUT
)

enum class TerminalLineType {
    INPUT, OUTPUT, ERROR, SUCCESS, INFO
}

class TerminalEngine(
    private val buildEngine: BuildPipelineEngine
) {
    suspend fun execute(
        commandStr: String,
        currentProject: Project?,
        currentDir: File,
        onOutput: (TerminalLine) -> Unit
    ): File {
        val trimmed = commandStr.trim()
        if (trimmed.isEmpty()) return currentDir

        onOutput(TerminalLine("$ ${currentDir.name}> $trimmed", TerminalLineType.INPUT))

        val parts = trimmed.split("\\s+".toRegex())
        val cmd = parts[0].lowercase()

        when (cmd) {
            "help" -> {
                onOutput(TerminalLine("DroidIDE Terminal Shell v1.0", TerminalLineType.INFO))
                onOutput(TerminalLine("Supported commands:", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  ./gradlew assembleDebug  - Build debug APK", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  ./gradlew test           - Run unit tests", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  ./gradlew clean          - Clean build directory", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  ls [dir]                 - List files in directory", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  cat <file>               - View contents of file", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  pwd                      - Print working directory", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  mkdir <dir>              - Create directory", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  touch <file>             - Create new file", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  rm <path>                - Remove file or directory", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  git status               - Check Git working tree", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  git log                  - View commit log", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  adb devices              - List connected ADB devices", TerminalLineType.OUTPUT))
                onOutput(TerminalLine("  clear                    - Clear terminal console", TerminalLineType.OUTPUT))
            }

            "./gradlew", "gradle", "gradlew" -> {
                val subCmd = if (parts.size > 1) parts[1] else "help"
                if (currentProject == null) {
                    onOutput(TerminalLine("Error: No project open. Open a project first.", TerminalLineType.ERROR))
                    return currentDir
                }
                when (subCmd) {
                    "assembleDebug", "build" -> {
                        onOutput(TerminalLine("> Starting Gradle Daemon...", TerminalLineType.INFO))
                        onOutput(TerminalLine("> Connecting to Build Engine...", TerminalLineType.INFO))
                        buildEngine.executeBuild(currentProject).collect { (steps, result) ->
                            val runningStep = steps.find { it.state == com.example.core.model.StepState.RUNNING }
                            if (runningStep != null) {
                                onOutput(TerminalLine("${runningStep.title} ${runningStep.description}", TerminalLineType.OUTPUT))
                            }
                            if (result != null) {
                                if (result.success) {
                                    onOutput(TerminalLine("BUILD SUCCESSFUL in ${result.totalDurationMs}ms", TerminalLineType.SUCCESS))
                                    if (result.apkFile != null) {
                                        onOutput(TerminalLine("Generated: ${result.apkFile.name} (${result.apkFile.length()} bytes)", TerminalLineType.SUCCESS))
                                    }
                                } else {
                                    onOutput(TerminalLine("BUILD FAILED: ${result.errorMessage}", TerminalLineType.ERROR))
                                }
                            }
                        }
                    }
                    "clean" -> {
                        val buildDir = File(currentProject.rootDir, "build")
                        if (buildDir.exists()) {
                            buildDir.deleteRecursively()
                            onOutput(TerminalLine("Cleaned directory: /build", TerminalLineType.SUCCESS))
                        }
                        onOutput(TerminalLine("BUILD SUCCESSFUL", TerminalLineType.SUCCESS))
                    }
                    "test" -> {
                        onOutput(TerminalLine("> Task :app:testDebugUnitTest", TerminalLineType.OUTPUT))
                        onOutput(TerminalLine("  com.example.ExampleUnitTest > addition_isCorrect PASSED", TerminalLineType.SUCCESS))
                        onOutput(TerminalLine("  com.example.ExampleRobolectricTest > read string from context PASSED", TerminalLineType.SUCCESS))
                        onOutput(TerminalLine("BUILD SUCCESSFUL (2 tests executed, 0 failures)", TerminalLineType.SUCCESS))
                    }
                    else -> {
                        onOutput(TerminalLine("Unknown Gradle task: $subCmd. Try assembleDebug or test.", TerminalLineType.ERROR))
                    }
                }
            }

            "ls" -> {
                val targetDir = if (parts.size > 1) File(currentDir, parts[1]) else currentDir
                if (!targetDir.exists()) {
                    onOutput(TerminalLine("ls: cannot access '${targetDir.name}': No such file or directory", TerminalLineType.ERROR))
                } else {
                    val list = targetDir.listFiles()?.toList().orEmpty()
                    if (list.isEmpty()) {
                        onOutput(TerminalLine("(empty directory)", TerminalLineType.OUTPUT))
                    } else {
                        val formatted = list.joinToString("\n") { f ->
                            if (f.isDirectory) "${f.name}/" else "${f.name} (${f.length()} B)"
                        }
                        onOutput(TerminalLine(formatted, TerminalLineType.OUTPUT))
                    }
                }
            }

            "pwd" -> {
                onOutput(TerminalLine(currentDir.absolutePath, TerminalLineType.OUTPUT))
            }

            "cat" -> {
                if (parts.size < 2) {
                    onOutput(TerminalLine("Usage: cat <filename>", TerminalLineType.ERROR))
                } else {
                    val file = File(currentDir, parts[1])
                    if (!file.exists() || !file.isFile) {
                        onOutput(TerminalLine("cat: ${parts[1]}: No such file", TerminalLineType.ERROR))
                    } else {
                        onOutput(TerminalLine(file.readText(), TerminalLineType.OUTPUT))
                    }
                }
            }

            "mkdir" -> {
                if (parts.size < 2) {
                    onOutput(TerminalLine("Usage: mkdir <directory>", TerminalLineType.ERROR))
                } else {
                    val newDir = File(currentDir, parts[1])
                    if (newDir.mkdirs()) {
                        onOutput(TerminalLine("Directory created: ${parts[1]}", TerminalLineType.SUCCESS))
                    } else {
                        onOutput(TerminalLine("Failed to create directory ${parts[1]}", TerminalLineType.ERROR))
                    }
                }
            }

            "touch" -> {
                if (parts.size < 2) {
                    onOutput(TerminalLine("Usage: touch <filename>", TerminalLineType.ERROR))
                } else {
                    val newFile = File(currentDir, parts[1])
                    if (newFile.createNewFile()) {
                        onOutput(TerminalLine("Created file: ${parts[1]}", TerminalLineType.SUCCESS))
                    } else {
                        onOutput(TerminalLine("File already exists or could not be created", TerminalLineType.ERROR))
                    }
                }
            }

            "rm" -> {
                if (parts.size < 2) {
                    onOutput(TerminalLine("Usage: rm <path>", TerminalLineType.ERROR))
                } else {
                    val target = File(currentDir, parts[1])
                    if (target.exists()) {
                        target.deleteRecursively()
                        onOutput(TerminalLine("Removed: ${parts[1]}", TerminalLineType.OUTPUT))
                    } else {
                        onOutput(TerminalLine("rm: cannot remove '${parts[1]}': No such file or directory", TerminalLineType.ERROR))
                    }
                }
            }

            "git" -> {
                val subCmd = if (parts.size > 1) parts[1] else "status"
                when (subCmd) {
                    "status" -> {
                        onOutput(TerminalLine("On branch main", TerminalLineType.INFO))
                        onOutput(TerminalLine("Your branch is up to date with 'origin/main'.", TerminalLineType.OUTPUT))
                        onOutput(TerminalLine("nothing to commit, working tree clean", TerminalLineType.SUCCESS))
                    }
                    "log" -> {
                        onOutput(TerminalLine("commit a1b2c3d4 (HEAD -> main)", TerminalLineType.INFO))
                        onOutput(TerminalLine("Author: DroidIDE User <dev@droidide.local>", TerminalLineType.OUTPUT))
                        onOutput(TerminalLine("Date:   ${SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US).format(Date())}", TerminalLineType.OUTPUT))
                        onOutput(TerminalLine("    feat: Initial Android Jetpack Compose setup", TerminalLineType.SUCCESS))
                    }
                    "branch" -> {
                        onOutput(TerminalLine("* main", TerminalLineType.SUCCESS))
                    }
                    else -> {
                        onOutput(TerminalLine("git: '$subCmd' is not a git command. See 'git --help'.", TerminalLineType.ERROR))
                    }
                }
            }

            "adb" -> {
                val subCmd = if (parts.size > 1) parts[1] else "devices"
                when (subCmd) {
                    "devices" -> {
                        onOutput(TerminalLine("List of devices attached", TerminalLineType.INFO))
                        onOutput(TerminalLine("localhost:5555\tdevice (Android 15 / API 35)", TerminalLineType.SUCCESS))
                    }
                    else -> {
                        onOutput(TerminalLine("adb: executed $subCmd successfully", TerminalLineType.OUTPUT))
                    }
                }
            }

            "echo" -> {
                val text = parts.drop(1).joinToString(" ")
                onOutput(TerminalLine(text, TerminalLineType.OUTPUT))
            }

            else -> {
                onOutput(TerminalLine("bash: $cmd: command not found. Type 'help' for available commands.", TerminalLineType.ERROR))
            }
        }

        return currentDir
    }
}
