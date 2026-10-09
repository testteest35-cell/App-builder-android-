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

      - name: Setup Android SDK & Pre-accept Licenses
        run: |
          ANDROID_SDK_ROOT="${D}{ANDROID_HOME:-/usr/local/lib/android/sdk}"
          export PATH="${D}ANDROID_SDK_ROOT/cmdline-tools/latest/bin:${D}ANDROID_SDK_ROOT/platform-tools:${D}PATH"
          mkdir -p "${D}ANDROID_SDK_ROOT/licenses" || true
          printf "\n24333f8a63b6825ea9c5514f83c2829b004d1fee\nd56f5187479451eabf01fb78af6dfcb131a6481e\n84831b9409646a3e80e4b2e74ba07519965a7f76" > "${D}ANDROID_SDK_ROOT/licenses/android-sdk-license"
          printf "\n84831b9409646a3e80e4b2e74ba07519965a7f76" > "${D}ANDROID_SDK_ROOT/licenses/android-sdk-preview-license"
          yes | sdkmanager --licenses || true
          sdkmanager "platforms;android-36" "platforms;android-35" "build-tools;36.0.0" "build-tools;35.0.0" || true

      - name: Prepare Signing Keystores & Environment
        run: |
          touch .env || true

          if [ -f "debug.keystore.base64" ]; then
            echo "Decoding debug.keystore from base64..."
            base64 -d debug.keystore.base64 > debug.keystore
          elif [ ! -f "debug.keystore" ]; then
            echo "Generating debug keystore for CI..."
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

          if [ ! -f "my-upload-key.jks" ]; then
            echo "Generating release upload keystore with alias 'upload'..."
            keytool -genkey -v \
              -keystore my-upload-key.jks \
              -alias upload \
              -keyalg RSA \
              -keysize 2048 \
              -validity 10000 \
              -storepass android \
              -keypass android \
              -dname "CN=DroidIDE Release,O=DroidIDE,C=US"
          fi

      - name: Make Gradle Wrapper Executable
        run: chmod +x gradlew || true

      - name: Build Debug APK
        if: github.event.inputs.build_type != 'release'
        run: ./gradlew assembleDebug --no-daemon --stacktrace --no-configuration-cache

      - name: Build Release APK
        if: github.event.inputs.build_type == 'release'
        run: |
          export STORE_PASSWORD="android"
          export KEY_PASSWORD="android"
          export KEYSTORE_PATH="${D}(pwd)/my-upload-key.jks"
          ./gradlew assembleRelease --no-daemon --stacktrace --no-configuration-cache

      - name: Locate Output APK
        id: find_apk
        run: |
          mkdir -p artifacts
          APK_PATH=${D}(find app/build/outputs/apk -type f -name "*.apk" | head -n 1)
          if [ -z "${D}APK_PATH" ]; then
            APK_PATH=${D}(find . -type f -path "*/build/outputs/apk/*" -name "*.apk" | head -n 1)
          fi
          if [ -z "${D}APK_PATH" ]; then
            echo "Error: No APK found in build outputs"
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
        if: startsWith(github.ref, 'refs/tags/v') || github.event_name == 'workflow_dispatch' || github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master'
        uses: softprops/action-gh-release@v2
        continue-on-error: true
        with:
          files: artifacts/DroidIDE-app.apk
          name: "DroidIDE ${D}{{ startsWith(github.ref, 'refs/tags/v') && github.ref_name || format('v1.0.{0}', github.run_number) }}"
          tag_name: "${D}{{ startsWith(github.ref, 'refs/tags/v') && github.ref_name || format('v1.0.{0}', github.run_number) }}"
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

      - name: Setup Android SDK & Accept Licenses
        run: |
          ANDROID_SDK_ROOT="${D}{ANDROID_HOME:-/usr/local/lib/android/sdk}"
          export PATH="${D}ANDROID_SDK_ROOT/cmdline-tools/latest/bin:${D}ANDROID_SDK_ROOT/platform-tools:${D}PATH"
          mkdir -p "${D}ANDROID_SDK_ROOT/licenses" || true
          printf "\n24333f8a63b6825ea9c5514f83c2829b004d1fee\nd56f5187479451eabf01fb78af6dfcb131a6481e\n84831b9409646a3e80e4b2e74ba07519965a7f76" > "${D}ANDROID_SDK_ROOT/licenses/android-sdk-license"
          yes | sdkmanager --licenses || true

      - name: Make Gradle Wrapper Executable
        run: chmod +x gradlew || true

      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon --stacktrace --no-configuration-cache

      - name: Locate Output APK
        run: |
          mkdir -p artifacts
          APK_PATH=${D}(find . -path "*/build/outputs/apk/*" -name "*.apk" | head -n 1)
          if [ -n "${D}APK_PATH" ]; then
            cp "${D}APK_PATH" artifacts/$projectName-debug.apk
          fi

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: $projectName-Debug-APK
          path: artifacts/*.apk
          retention-days: 30

      - name: Create GitHub Release
        if: startsWith(github.ref, 'refs/tags/v') || github.event_name == 'workflow_dispatch' || github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master'
        uses: softprops/action-gh-release@v2
        continue-on-error: true
        with:
          files: artifacts/*.apk
          name: "$projectName Release ${D}{{ startsWith(github.ref, 'refs/tags/v') && github.ref_name || format('v1.0.{0}', github.run_number) }}"
          tag_name: "${D}{{ startsWith(github.ref, 'refs/tags/v') && github.ref_name || format('v1.0.{0}', github.run_number) }}"
          body: "Direct APK installation build from GitHub Actions workflow for $projectName."
        env:
          GITHUB_TOKEN: ${D}{{ secrets.GITHUB_TOKEN }}
""".trimIndent()
    }
}
