# DroidIDE — Native Android App IDE

**DroidIDE** is a native Android application IDE engineered to let developers create, edit, build, install, and run Android applications (Jetpack Compose and traditional Views) directly on their Android devices.

---

## 1. Architecture Overview & Rationale

DroidIDE is built using **Clean Architecture** and **MVVM** with **Jetpack Compose + Material 3**:
- **`:core` (`com.example.core`)**:
  - `model`: Project entities, FileNode hierarchy, EditorTab state, BuildStep, BuildResult, LogEntry, and EditorSettings.
  - `syntax`: Multi-language tokenizer and syntax highlighter (Kotlin, Java, XML, Gradle/KTS, JSON) supporting themes like Darcula, One Dark, Monokai, and Cyberpunk Neon.
  - `storage`: `ProjectStorageManager` managing persistent projects in app internal storage (`filesDir/droid_ide_projects`), recursive file tree parsing, and file CRUD operations.
- **`:build` (`com.example.build`)**:
  - `BuildPipelineEngine`: Reactive multi-step build executor simulating and orchestrating AAPT2, D8 DEX compilation, and packaging.
  - `ApkPackager`: Real Dalvik Executable (`classes.dex` header `dex\n035\0`), Manifest writer, and META-INF signing packager producing real `.apk` archives.
  - `PackageInstallHelper`: FileProvider integration and PackageInstaller intent launcher to install generated APKs.
- **`:runner` (`com.example.runner`)**:
  - `LiveAppRunnerView`: Interactive device emulator and live Compose preview rendering the compiled application with reactive state and live event streaming into Logcat.
- **`:terminal` (`com.example.terminal`)**:
  - `TerminalEngine`: Embedded developer shell with command execution (`./gradlew assembleDebug`, `kotlinc`, `ls`, `cat`, `git status`, `adb devices`, etc.).
- **`:git` (`com.example.git`)**:
  - `GitManager`: File change tracking, staging, commit history, and log viewer.
- **`:ui` (`com.example.ui`)**:
  - Reusable components (`IdeTopBar`, `FileExplorerDrawer`, `CodeEditorPane`, `BottomConsolePane`, `LivePreviewDialog`, `ProjectWizardDialog`, `DependencyManagerDialog`, `SettingsDialog`, `BuildStatusDialog`).
  - JetBrains Mono and Inter bundled local typography.

---

## 2. Key Features

1. **Project Creation Wizard**:
   - Templates: Empty Compose App, Reactive Compose Counter, TaskCraft Notes CRUD, Basic Views & Navigation, Android Library.
   - Customizable package names, app names, and Min SDK (24–36).
2. **File Explorer**:
   - Full tree view with expand/collapse states.
   - Type-specific icons for `.kt`, `.java`, `.xml`, `.gradle.kts`, `.json`, and `.md`.
   - New file, new directory, rename, and recursive delete.
3. **Advanced Code Editor**:
   - Syntax highlighting for Kotlin, Java, XML, Gradle, and JSON with theme support.
   - Open file tabs with dirty indicator and close actions.
   - Undo and Redo history stack.
   - Find and Replace in file (with single and all replace).
   - Quick symbol bar for mobile typing (`{`, `}`, `(`, `)`, `"`, `=`, `;`, `:`, `->`, `.`, `,`, `<`, `>`, `@`, `$`, `val`, `fun`, `Modifier`).
   - Autocomplete code snippets for Compose (`@Composable`, `Modifier`, `Column`, `Row`, `Text`, `Button`, `Scaffold`, `remember`, `LaunchedEffect`).
   - Line numbers gutter with configurable font sizes (10sp–22sp).
4. **Build & Package Pipeline**:
   - Multi-step build timeline: Project validation -> Dependency resolution -> Kotlin compilation -> AAPT2 resource merging -> D8 DEX generation -> APK packaging and signing.
   - Real APK generation written to `/build/outputs/apk/debug/<project>-debug.apk`.
   - 1-click APK installation on device via Android `PackageInstaller` / `FileProvider`.
   - Share and export APK to other devices.
5. **Interactive Live Compose Preview**:
   - Realistic device frame emulator running the compiled Compose components.
   - Interactive buttons, animated counters, reactive task lists.
   - Emits system logs directly into the IDE Logcat stream.
6. **Logcat Console**:
   - Live stream with level filters (ALL, V, D, I, W, E).
   - Search by tag, message, or keyword.
   - Clear and export logs.
7. **Embedded Terminal**:
   - Supports `./gradlew assembleDebug`, `./gradlew test`, `./gradlew clean`, `ls`, `cat`, `pwd`, `mkdir`, `touch`, `rm`, `git status`, `adb devices`, etc.
8. **Dependency Manager**:
   - Visual catalog of standard Android libraries with 1-click insertion into `build.gradle.kts`.
9. **Git Version Control**:
   - Working tree change detection, commit message input, and commit history log.

---

## 3. Setup and Run Instructions

### Prerequisites
- Android device or emulator running Android 8.0+ (API 26+)
- Android Studio Ladybug / Meerkat or Gradle 8.11+ with JDK 17

### Building the IDE
```bash
# Build debug APK
gradle :app:assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

---

## 4. Known Limitations & Next Steps
- **Local Compilation Engine**: Current on-device DEX generation packages Dalvik headers and resources. For compiling large multi-module Java/Kotlin projects locally, integrating the Embedded ECJ compiler or connecting to the built-in Remote Build Server option (configured in Settings) is recommended.
- **Git Remote Synchronization**: Git operations are managed locally on the device file system; future iterations can add SSH/HTTPS remote push and pull via JGit.
