package com.example.github

object WorkflowGenerator {

    private const val D = "$"

    val DROIDIDE_APP_WORKFLOW = """
name: Build & Release DroidIDE APK

on:
  push:
    branches: [ "main", "master" ]
    tags:
      - 'v*'
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch:
    inputs:
      build_type:
        description: 'Build Type (debug or release)'
        required: true
        default: 'debug'
        type: choice
        options:
          - debug
          - release

permissions:
  contents: write

jobs:
  build-apk:
    name: Build Android APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Prepare Debug Keystore
        run: |
          if [ -f "debug.keystore.base64" ]; then
            echo "Decoding debug.keystore from base64..."
            base64 -d debug.keystore.base64 > debug.keystore
          elif [ ! -f "debug.keystore" ]; then
            echo "Generating fresh debug keystore for CI..."
            keytool -genkey -v \
              -keystore debug.keystore \
              -alias androiddebugkey \
              -keyalg RSA \
              -keysize 2048 \
              -validity 10000 \
              -storepass android \
              -keypass android \
              -dname "CN=Android Debug,O=Android,C=US"
          fi
          touch .env || true

      - name: Make Gradle Wrapper Executable
        run: chmod +x gradlew

      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest --no-daemon --stacktrace

      - name: Build Debug APK
        if: "${D}{{ github.event.inputs.build_type != 'release' }}"
        run: ./gradlew assembleDebug --no-daemon --stacktrace

      - name: Build Release APK
        if: "${D}{{ github.event.inputs.build_type == 'release' }}"
        run: |
          export STORE_PASSWORD="android"
          export KEY_PASSWORD="android"
          export KEYSTORE_PATH="${D}(pwd)/debug.keystore"
          ./gradlew assembleRelease --no-daemon --stacktrace

      - name: Locate Output APK
        id: find_apk
        run: |
          mkdir -p artifacts
          APK_PATH=${D}(find app/build/outputs/apk -name "*.apk" | head -n 1)
          if [ -z "${D}APK_PATH" ]; then
            echo "Error: No APK found in app/build/outputs/apk"
            exit 1
          fi
          echo "Found APK: ${D}APK_PATH"
          cp "${D}APK_PATH" artifacts/DroidIDE-app.apk
          echo "apk_path=artifacts/DroidIDE-app.apk" >> "${D}GITHUB_OUTPUT"

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: DroidIDE-apk
          path: artifacts/DroidIDE-app.apk
          if-no-files-found: error
          retention-days: 30

      - name: Create GitHub Release
        if: "startsWith(github.ref, 'refs/tags/v') || (github.event_name == 'workflow_dispatch' && github.ref == 'refs/heads/main')"
        uses: softprops/action-gh-release@v2
        with:
          files: artifacts/DroidIDE-app.apk
          name: "DroidIDE ${D}{{ github.ref_name }}"
          body: |
            ## 🚀 DroidIDE Native Android App
            
            Install directly on your Android phone or tablet:
            1. Download `DroidIDE-app.apk` below directly on your mobile browser.
            2. Tap the downloaded APK and select **Install** (allow *Install from Unknown Sources* if prompted).
            3. Open DroidIDE and start building Android apps on the go!
          draft: false
          prerelease: false
        env:
          GITHUB_TOKEN: ${D}{{ secrets.GITHUB_TOKEN }}
""".trimIndent()

    fun getProjectWorkflow(projectName: String): String {
        return """
name: Build & Release $projectName APK

on:
  push:
    branches: [ "main", "master" ]
    tags:
      - 'v*'
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build:
    name: Build $projectName APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Make Gradle Wrapper Executable
        run: chmod +x gradlew || true

      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon --stacktrace

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: $projectName-Debug-APK
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 30

      - name: Create GitHub Release
        if: "startsWith(github.ref, 'refs/tags/v')"
        uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/apk/debug/*.apk
          name: "$projectName Release ${D}{{ github.ref_name }}"
          body: "Direct APK installation build from GitHub Actions workflow for $projectName."
        env:
          GITHUB_TOKEN: ${D}{{ secrets.GITHUB_TOKEN }}
""".trimIndent()
    }
}
