package com.example.project

import com.example.core.model.ProjectTemplateType

object ProjectTemplates {

    data class TemplateFile(
        val relativePath: String,
        val content: String
    )

    fun getFilesForTemplate(
        projectName: String,
        packageName: String,
        template: ProjectTemplateType,
        minSdk: Int = 26
    ): List<TemplateFile> {
        val packageDir = packageName.replace('.', '/')

        val gitignore = """
            *.iml
            .gradle
            /local.properties
            /.idea/caches
            /.idea/libraries
            /.idea/modules.xml
            /.idea/workspace.xml
            .DS_Store
            /build
            /captures
            .externalNativeBuild
            .cxx
            local.properties
        """.trimIndent()

        val settingsGradle = """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "$projectName"
            include(":app")
        """.trimIndent()

        val rootBuildGradle = """
            // Top-level build file
            plugins {
                alias(libs.plugins.android.application) apply false
                alias(libs.plugins.kotlin.compose) apply false
            }
        """.trimIndent()

        val manifest = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">

                <application
                    android:allowBackup="true"
                    android:icon="@mipmap/ic_launcher"
                    android:label="@string/app_name"
                    android:roundIcon="@mipmap/ic_launcher_round"
                    android:supportsRtl="true"
                    android:theme="@style/Theme.App">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true"
                        android:label="@string/app_name"
                        android:theme="@style/Theme.App">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>

            </manifest>
        """.trimIndent()

        val stringsXml = """
            <resources>
                <string name="app_name">$projectName</string>
            </resources>
        """.trimIndent()

        val buildGradleKts = """
            plugins {
                alias(libs.plugins.android.application)
                alias(libs.plugins.kotlin.compose)
            }

            android {
                namespace = "$packageName"
                compileSdk = 35

                defaultConfig {
                    applicationId = "$packageName"
                    minSdk = $minSdk
                    targetSdk = 35
                    versionCode = 1
                    versionName = "1.0"
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                    }
                    debug {
                        isDebuggable = true
                    }
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                implementation(platform("androidx.compose:compose-bom:2024.09.00"))
                implementation("androidx.activity:activity-compose:1.10.1")
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.ui:ui-graphics")
                implementation("androidx.compose.ui:ui-tooling-preview")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
            }
        """.trimIndent()

        return when (template) {
            ProjectTemplateType.COMPOSE_EMPTY -> {
                val mainActivity = """
                    package $packageName

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent
                    import androidx.compose.foundation.layout.*
                    import androidx.compose.material3.*
                    import androidx.compose.runtime.*
                    import androidx.compose.ui.Alignment
                    import androidx.compose.ui.Modifier
                    import androidx.compose.ui.unit.dp

                    class MainActivity : ComponentActivity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            setContent {
                                MaterialTheme {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.background
                                    ) {
                                        MainScreen()
                                    }
                                }
                            }
                        }
                    }

                    @Composable
                    fun MainScreen() {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hello from $projectName!",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                """.trimIndent()

                listOf(
                    TemplateFile(".gitignore", gitignore),
                    TemplateFile("settings.gradle.kts", settingsGradle),
                    TemplateFile("build.gradle.kts", rootBuildGradle),
                    TemplateFile("app/build.gradle.kts", buildGradleKts),
                    TemplateFile("app/src/main/AndroidManifest.xml", manifest),
                    TemplateFile("app/src/main/res/values/strings.xml", stringsXml),
                    TemplateFile("app/src/main/java/$packageDir/MainActivity.kt", mainActivity)
                )
            }

            ProjectTemplateType.COMPOSE_COUNTER -> {
                val counterActivity = """
                    package $packageName

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent
                    import androidx.compose.animation.*
                    import androidx.compose.foundation.background
                    import androidx.compose.foundation.layout.*
                    import androidx.compose.foundation.shape.CircleShape
                    import androidx.compose.foundation.shape.RoundedCornerShape
                    import androidx.compose.material3.*
                    import androidx.compose.runtime.*
                    import androidx.compose.ui.Alignment
                    import androidx.compose.ui.Modifier
                    import androidx.compose.ui.draw.clip
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.unit.dp
                    import androidx.compose.ui.unit.sp

                    class MainActivity : ComponentActivity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            setContent {
                                MaterialTheme {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.background
                                    ) {
                                        CounterScreen()
                                    }
                                }
                            }
                        }
                    }

                    @Composable
                    fun CounterScreen() {
                        var count by remember { mutableIntStateOf(0) }
                        var taps by remember { mutableIntStateOf(0) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Reactive Compose Counter",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(targetState = count, label = "counter") { value ->
                                    Text(
                                        text = "${'$'}value",
                                        fontSize = 54.sp,
                                        style = MaterialTheme.typography.displayLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Total Taps: ${'$'}taps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(40.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        count--
                                        taps++
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("- Decrement")
                                }
                                Button(
                                    onClick = {
                                        count++
                                        taps++
                                    }
                                ) {
                                    Text("+ Increment")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    count = 0
                                    taps = 0
                                }
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                """.trimIndent()

                listOf(
                    TemplateFile(".gitignore", gitignore),
                    TemplateFile("settings.gradle.kts", settingsGradle),
                    TemplateFile("build.gradle.kts", rootBuildGradle),
                    TemplateFile("app/build.gradle.kts", buildGradleKts),
                    TemplateFile("app/src/main/AndroidManifest.xml", manifest),
                    TemplateFile("app/src/main/res/values/strings.xml", stringsXml),
                    TemplateFile("app/src/main/java/$packageDir/MainActivity.kt", counterActivity)
                )
            }

            ProjectTemplateType.COMPOSE_NOTES -> {
                val notesActivity = """
                    package $packageName

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent
                    import androidx.compose.foundation.layout.*
                    import androidx.compose.foundation.lazy.LazyColumn
                    import androidx.compose.foundation.lazy.items
                    import androidx.compose.material3.*
                    import androidx.compose.runtime.*
                    import androidx.compose.ui.Modifier
                    import androidx.compose.ui.unit.dp

                    data class TaskItem(val id: String, val title: String, val isDone: Boolean)

                    class MainActivity : ComponentActivity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            setContent {
                                MaterialTheme {
                                    Surface(modifier = Modifier.fillMaxSize()) {
                                        TaskCraftScreen()
                                    }
                                }
                            }
                        }
                    }

                    @OptIn(ExperimentalMaterial3Api::class)
                    @Composable
                    fun TaskCraftScreen() {
                        var tasks by remember {
                            mutableStateOf(
                                listOf(
                                    TaskItem("1", "Design new Android App", true),
                                    TaskItem("2", "Test Compose on device", false),
                                    TaskItem("3", "Publish release APK", false)
                                )
                            )
                        }
                        var inputTitle by remember { mutableStateOf("") }

                        Scaffold(
                            topBar = {
                                TopAppBar(title = { Text("TaskCraft Pro") })
                            }
                        ) { padding ->
                            Column(
                                modifier = Modifier
                                    .padding(padding)
                                    .padding(16.dp)
                                    .fillMaxSize()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = inputTitle,
                                        onValueChange = { inputTitle = it },
                                        label = { Text("New Task") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (inputTitle.isNotBlank()) {
                                                tasks = tasks + TaskItem(System.currentTimeMillis().toString(), inputTitle, false)
                                                inputTitle = ""
                                            }
                                        }
                                    ) {
                                        Text("Add")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(tasks, key = { it.id }) { task ->
                                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = task.title)
                                                Checkbox(
                                                    checked = task.isDone,
                                                    onCheckedChange = { done ->
                                                        tasks = tasks.map { if (it.id == task.id) it.copy(isDone = done) else it }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                """.trimIndent()

                listOf(
                    TemplateFile(".gitignore", gitignore),
                    TemplateFile("settings.gradle.kts", settingsGradle),
                    TemplateFile("build.gradle.kts", rootBuildGradle),
                    TemplateFile("app/build.gradle.kts", buildGradleKts),
                    TemplateFile("app/src/main/AndroidManifest.xml", manifest),
                    TemplateFile("app/src/main/res/values/strings.xml", stringsXml),
                    TemplateFile("app/src/main/java/$packageDir/MainActivity.kt", notesActivity)
                )
            }

            ProjectTemplateType.BASIC_VIEWS -> {
                val viewsActivity = """
                    package $packageName

                    import android.os.Bundle
                    import androidx.appcompat.app.AppCompatActivity

                    class MainActivity : AppCompatActivity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            // setContentView(R.layout.activity_main)
                        }
                    }
                """.trimIndent()

                val activityLayout = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:gravity="center"
                        android:orientation="vertical"
                        android:padding="24dp">

                        <TextView
                            android:id="@+id/textViewTitle"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Welcome to $projectName"
                            android:textSize="22sp" />

                        <Button
                            android:id="@+id/buttonAction"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="16dp"
                            android:text="Click Me" />
                    </LinearLayout>
                """.trimIndent()

                listOf(
                    TemplateFile(".gitignore", gitignore),
                    TemplateFile("settings.gradle.kts", settingsGradle),
                    TemplateFile("build.gradle.kts", rootBuildGradle),
                    TemplateFile("app/build.gradle.kts", buildGradleKts),
                    TemplateFile("app/src/main/AndroidManifest.xml", manifest),
                    TemplateFile("app/src/main/res/values/strings.xml", stringsXml),
                    TemplateFile("app/src/main/res/layout/activity_main.xml", activityLayout),
                    TemplateFile("app/src/main/java/$packageDir/MainActivity.kt", viewsActivity)
                )
            }

            ProjectTemplateType.EMPTY_LIBRARY -> {
                val libBuildGradle = """
                    plugins {
                        id("com.android.library")
                        id("org.jetbrains.kotlin.android")
                    }

                    android {
                        namespace = "$packageName"
                        compileSdk = 35

                        defaultConfig {
                            minSdk = $minSdk
                            consumerProguardFiles("consumer-rules.pro")
                        }
                    }
                """.trimIndent()

                val libCode = """
                    package $packageName

                    /**
                     * Reusable Android Library Module
                     */
                    object MathUtils {
                        fun add(a: Int, b: Int): Int = a + b
                        fun multiply(a: Int, b: Int): Int = a * b
                    }
                """.trimIndent()

                listOf(
                    TemplateFile(".gitignore", gitignore),
                    TemplateFile("settings.gradle.kts", settingsGradle),
                    TemplateFile("build.gradle.kts", rootBuildGradle),
                    TemplateFile("library/build.gradle.kts", libBuildGradle),
                    TemplateFile("library/src/main/java/$packageDir/MathUtils.kt", libCode)
                )
            }
        }
    }
}
