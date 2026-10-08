package com.example

import com.example.build.ApkPackager
import com.example.core.model.EditorThemeType
import com.example.core.model.Project
import com.example.core.model.ProjectTemplateType
import com.example.core.syntax.CodeDiagnostics
import com.example.core.syntax.SyntaxHighlighter
import com.example.github.model.GitHubRepo
import com.example.github.model.GitHubUser
import com.example.project.ProjectTemplates
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class IdeLogicTest {

    @Test
    fun testSyntaxHighlighterKotlin() {
        val code = """
            package com.example
            import androidx.compose.runtime.Composable
            @Composable
            fun MyScreen() {
                val message = "Hello Android"
                val count = 42
            }
        """.trimIndent()

        val highlighted = SyntaxHighlighter.highlight(code, "kt", EditorThemeType.DARCULA)
        assertNotNull(highlighted)
        assertTrue(highlighted.spanStyles.isNotEmpty())
        assertEquals(code, highlighted.text)
    }

    @Test
    fun testSyntaxHighlighterXml() {
        val xml = """<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent"/>"""
        val highlighted = SyntaxHighlighter.highlight(xml, "xml", EditorThemeType.ONE_DARK)
        assertNotNull(highlighted)
        assertTrue(highlighted.spanStyles.isNotEmpty())
    }

    @Test
    fun testCodeDiagnosticsDetectsUnbalancedBraces() {
        val brokenCode = """
            fun broken() {
                if (true) {
                    println("missing closing brace")
            }
        """.trimIndent()

        val diagnostics = CodeDiagnostics.analyze(brokenCode, "kt")
        assertTrue("Diagnostics should detect unclosed brace", diagnostics.isNotEmpty())
    }

    @Test
    fun testCodeDiagnosticsValidCodeHasNoErrors() {
        val validCode = """
            fun valid() {
                if (true) {
                    println("closed")
                }
            }
        """.trimIndent()

        val diagnostics = CodeDiagnostics.analyze(validCode, "kt")
        assertTrue("Valid code should have no errors", diagnostics.isEmpty())
    }

    @Test
    fun testProjectTemplatesGenerateEssentialFiles() {
        val files = ProjectTemplates.getFilesForTemplate(
            projectName = "TestApp",
            packageName = "com.test.app",
            template = ProjectTemplateType.COMPOSE_COUNTER,
            minSdk = 26
        )

        val paths = files.map { it.relativePath }
        assertTrue(paths.contains("settings.gradle.kts"))
        assertTrue(paths.contains("app/build.gradle.kts"))
        assertTrue(paths.contains("app/src/main/AndroidManifest.xml"))
        assertTrue(paths.contains("app/src/main/java/com/test/app/MainActivity.kt"))
    }

    @Test
    fun testApkPackagerBuildsValidApkZip() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_proj_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val manifest = File(tempDir, "app/src/main/AndroidManifest.xml")
        manifest.parentFile?.mkdirs()
        manifest.writeText("""<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.test.app"/>""")

        val project = Project(
            id = "test",
            name = "TestApp",
            packageName = "com.test.app",
            template = ProjectTemplateType.COMPOSE_EMPTY,
            minSdk = 26,
            rootDir = tempDir
        )

        val apk = ApkPackager.packageApk(project)
        assertTrue("APK file must exist", apk.exists())
        assertTrue("APK file size must be > 0", apk.length() > 0)

        ZipFile(apk).use { zip ->
            assertNotNull(zip.getEntry("AndroidManifest.xml"))
            assertNotNull(zip.getEntry("classes.dex"))
            assertNotNull(zip.getEntry("META-INF/MANIFEST.MF"))
        }

        tempDir.deleteRecursively()
    }

    @Test
    fun testGitHubModelsParsing() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val userJson = """
            {
                "login": "octocat",
                "id": 1,
                "avatar_url": "https://github.com/images/error/octocat_happy.gif",
                "name": "The Octocat",
                "public_repos": 8,
                "bio": "Android Developer"
            }
        """.trimIndent()

        val adapter = moshi.adapter(GitHubUser::class.java)
        val user = adapter.fromJson(userJson)
        assertNotNull(user)
        assertEquals("octocat", user?.login)
        assertEquals("The Octocat", user?.name)
        assertEquals(8, user?.publicRepos)

        val repoJson = """
            {
                "id": 1296269,
                "name": "Hello-World",
                "full_name": "octocat/Hello-World",
                "private": false,
                "description": "This is your first repo!",
                "default_branch": "main",
                "html_url": "https://github.com/octocat/Hello-World",
                "updated_at": "2024-01-01T00:00:00Z"
            }
        """.trimIndent()
        val repoAdapter = moshi.adapter(GitHubRepo::class.java)
        val repo = repoAdapter.fromJson(repoJson)
        assertNotNull(repo)
        assertEquals("Hello-World", repo?.name)
        assertEquals("main", repo?.defaultBranch)
        assertFalse(repo?.private ?: true)
    }

    @Test
    fun testZipSlipSecurityCheck() {
        val tempExtractDir = File(System.getProperty("java.io.tmpdir"), "zip_slip_test_${System.currentTimeMillis()}")
        tempExtractDir.mkdirs()

        val maliciousPath = "../malicious.txt"
        val resolved = File(tempExtractDir, maliciousPath)
        val isSlip = !resolved.canonicalPath.startsWith(tempExtractDir.canonicalPath)
        assertTrue("Path traversal should be detected", isSlip)

        tempExtractDir.deleteRecursively()
    }
}
