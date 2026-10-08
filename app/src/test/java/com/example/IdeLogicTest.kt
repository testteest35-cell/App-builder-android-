package com.example

import com.example.build.ApkPackager
import com.example.core.model.EditorThemeType
import com.example.core.model.Project
import com.example.core.model.ProjectTemplateType
import com.example.core.syntax.CodeDiagnostics
import com.example.core.syntax.SyntaxHighlighter
import com.example.project.ProjectTemplates
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

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

        // Verify it is a valid Zip file with DEX and Manifest entries
        ZipFile(apk).use { zip ->
            val manifestEntry = zip.getEntry("AndroidManifest.xml")
            val dexEntry = zip.getEntry("classes.dex")
            val certEntry = zip.getEntry("META-INF/MANIFEST.MF")

            assertNotNull("Manifest entry must exist in APK", manifestEntry)
            assertNotNull("Dex entry must exist in APK", dexEntry)
            assertNotNull("META-INF signature must exist in APK", certEntry)
        }

        tempDir.deleteRecursively()
    }
}
