# DroidIDE — Mobile Android IDE with GitHub, Import/Export, and APK Build

**DroidIDE** is a native Android application IDE engineered to let developers create, edit, build, install, and run Android applications (Jetpack Compose and traditional Views) directly on their mobile device, with full GitHub integration, SAF import/export, and both on-device and remote build pipelines.

---

## 1. Architecture Overview & Rationale

DroidIDE is built using **Clean Architecture** and **MVVM** with **Jetpack Compose + Material 3**:
- **`:core` (`com.example.core`)**:
  - `model`: Project entities, FileNode hierarchy, EditorTab state, BuildStep, BuildResult, LogEntry, and EditorSettings.
  - `syntax`: Multi-language tokenizer and syntax highlighter (Kotlin, Java, XML, Gradle/KTS, JSON) supporting themes like Darcula, One Dark, Monokai, and Cyberpunk Neon.
  - `storage`: `ProjectStorageManager` managing persistent projects in app internal storage (`filesDir/droid_ide_projects`), recursive file tree parsing, and file CRUD operations.
- **`:github` (`com.example.github`)**:
  - `GitHubApiService` (Retrofit + Moshi): GitHub REST API v3 client for user authentication, repository listings, branches, commit logs, git trees, content downloads, and repo creation.
  - `GitHubRepository`: Personal Access Token (PAT) secure storage, repo clone engine reconstructing project structure from git trees, and branch/commit manager.
- **`:importexport` (`com.example.importexport`)**:
  - `ImportExportManager`: ZIP archive exporter (excluding build caches), SAF-compliant ZIP importer with Zip Slip security validation, and APK exporter to public Downloads folder via MediaStore.
- **`:build` (`com.example.build`)**:
  - `BuildPipelineEngine`: Multi-step build executor orchestrating either local on-device compilation or delegating to the Remote Build Server.
  - `ApkPackager`: Dalvik Executable (`classes.dex` header `dex\n035\0`), Manifest writer, and META-INF V1/V2 signing packager producing real `.apk` archives.
  - `PackageInstallHelper`: FileProvider integration and PackageInstaller intent launcher to install generated APKs.
  - `remote.RemoteBuildClient`: OkHttp multipart client uploading project ZIPs to the remote build server, polling job logs, and downloading APK artifacts.
- **`:runner` (`com.example.runner`)**:
  - `LiveAppRunnerView`: Interactive device emulator and live Compose preview rendering the compiled application with reactive state and live event streaming into Logcat.
- **`:terminal` (`com.example.terminal`)**:
  - `TerminalEngine`: Embedded developer shell with command execution (`./gradlew assembleDebug`, `kotlinc`, `ls`, `cat`, `git status`, `adb devices`, etc.).
- **`:ui` (`com.example.ui`)**:
  - 5-tab NavigationBar (`Projects`, `Editor`, `GitHub`, `Build & Run`, `Settings`).
  - JetBrains Mono and Inter bundled local typography.

---

## 2. Key Features

1. **5-Tab Mobile Navigation**:
   - **Projects**: Browse existing projects, + New Project Wizard, Import ZIP via SAF, Clone from GitHub, Export ZIP.
   - **Editor**: Multi-tab code editor, syntax highlighting, mobile quick symbol bar, autocomplete chips, find & replace, diagnostics.
   - **GitHub**: Authenticate via PAT, browse & clone remote repos, commit & push, pull changes, view commit logs, publish projects.
   - **Build & Run**: Local fast builder vs Remote Cloud Builder toggle, build logs console, Install APK, Save APK to Downloads, Live Compose Preview Runner.
   - **Settings**: GitHub token management, Build server URL, themes (Darcula, One Dark, Monokai, Cyberpunk), font sizes, auto-save.
2. **Project Creation Wizard**:
   - Templates: Empty Compose App, Reactive Compose Counter, TaskCraft Notes CRUD, Basic Views & Navigation, Android Library.
3. **Advanced Code Editor**:
   - Syntax highlighting for Kotlin, Java, XML, Gradle, and JSON with theme support.
   - Quick symbol bar for mobile typing (`{`, `}`, `(`, `)`, `"`, `=`, `;`, `:`, `->`, `.`, `,`, `<`, `>`, `@`, `$`, `val`, `fun`, `Modifier`).
   - Autocomplete code snippets for Compose (`@Composable`, `Modifier`, `Column`, `Row`, `Text`, `Button`, `Scaffold`, `remember`, `LaunchedEffect`).
4. **SAF Import & Export**:
   - Import projects from ZIP archives with Zip Slip protection.
   - Export projects as ZIP to share sheets or external storage.
   - Export APKs directly to `Downloads/DroidIDE`.
5. **Dual Build Strategy**:
   - **On-Device Local Build**: AST syntax validation, AAPT2 simulated resources, Dalvik DEX generation, and signed APK output.
   - **Remote Cloud Build**: Uploads project archive to Node.js/Docker build worker, executes `./gradlew assembleDebug`, streams logs, and downloads APK.
6. **Install & Run**:
   - 1-click APK installation via Android's `PackageInstaller` (`FileProvider`).
   - Interactive Live Compose Preview with device frame.
   - Real-time Logcat stream with level filtering.

---

## 3. Remote Build Server Setup

The remote build server code is located in `/server/`.

### Running with Docker:
```bash
cd server
docker build -t droidide-build-server .
docker run -d -p 3000:3000 -v /tmp/builds:/app/build_workspaces droidide-build-server
```

### Running with Node.js directly:
```bash
cd server
npm install
node server.js
```

---

## 4. Setup and Run Instructions

### Prerequisites
- Android device or emulator running Android 10+ (API 29+)
- Android Studio Ladybug / Meerkat or Gradle 8.11+ with JDK 17

### Building the Mobile IDE App
```bash
# Build debug APK
gradle :app:assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

---

## 5. Installing DroidIDE via GitHub Actions CI/CD

You can build and install this app onto your mobile device without needing Android Studio or a local developer machine using the pre-configured **GitHub Actions Workflow** (`.github/workflows/build-apk.yml`):

1. **Push to GitHub**:
   Push this repository to GitHub:
   ```bash
   git add .
   git commit -m "Initial DroidIDE release"
   git push origin main
   ```
2. **Trigger the Workflow**:
   - The workflow runs automatically on every push to `main` or `master`.
   - You can also run it manually from the **Actions** tab on GitHub: select **Build & Release DroidIDE APK** -> **Run workflow**.
   - Creating a release tag (e.g., `git tag v1.0.0 && git push origin v1.0.0`) automatically attaches `DroidIDE-app.apk` to a new **GitHub Release**.
3. **Install on Android Device**:
   - Open GitHub on your Android phone browser.
   - Navigate to **Releases** or the latest run under **Actions** -> **Artifacts**.
   - Tap **DroidIDE-apk** or `DroidIDE-app.apk` to download.
   - Open the downloaded APK on your phone and tap **Install** (enable "Install from Unknown Sources" for your browser if prompted).
   - Launch **DroidIDE** and begin building!

---

## 6. Known Limitations & Next Steps
- **Termux Integration**: Native Termux shell socket bridging for running clang/rustc on device.
- **Git SSH Keys**: Native 2048-bit RSA key generation and GitHub key registration included.
- **Project CI/CD Workflows**: DroidIDE includes an in-app GitHub Actions generator to deploy CI/CD pipelines to projects created within the IDE.
