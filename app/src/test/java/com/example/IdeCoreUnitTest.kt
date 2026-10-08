package com.example

import com.example.core.model.EditorThemeType
import com.example.core.model.ProjectTemplateType
import com.example.core.syntax.CodeDiagnostics
import com.example.core.syntax.SyntaxHighlighter
import com.example.project.ProjectTemplates
import org.junit.Assert.*
import org.junit.Test

class IdeCoreUnitTest {

    @Test
    fun testSyntaxHighlighterKotlin() {
        val code = """
            package com.example
            import androidx.compose.runtime.Composable
            
            // This is a comment
            val appTitle: String = "Hello DroidIDE"
            val count: Int = 42
            
            @Composable
            fun MainScreen() {
                Text(text = appTitle)
            }
        """.trimIndent()

        val annotated = SyntaxHighlighter.highlight(code, "kt", EditorThemeType.DARCULA)
        assertNotNull(annotated)
        assertEquals(code, annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }

    @Test
    fun testSyntaxHighlighterXml() {
        val xmlCode = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application android:label="App">
                </application>
            </manifest>
        """.trimIndent()

        val annotated = SyntaxHighlighter.highlight(xmlCode, "xml", EditorThemeType.DARCULA)
        assertNotNull(annotated)
        assertEquals(xmlCode, annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }

    @Test
    fun testCodeDiagnosticsBraceMatching() {
        val brokenCode = """
            fun test() {
                val x = 10
            // missing closing brace
        """.trimIndent()

        val diagnostics = CodeDiagnostics.analyze(brokenCode, "kt")
        val hasError = diagnostics.any { it.message.contains("brace") || it.message.contains("bracket") }
        assertTrue("Diagnostics should detect unmatched open brace", hasError)
    }

    @Test
    fun testCodeDiagnosticsUnclosedString() {
        val brokenStringCode = """
            val name = "Unclosed String
            val y = 20
        """.trimIndent()

        val diagnostics = CodeDiagnostics.analyze(brokenStringCode, "kt")
        val hasStringError = diagnostics.any { it.message.contains("Unclosed string") }
        assertTrue("Diagnostics should detect unclosed string literal", hasStringError)
    }

    @Test
    fun testAutocompleteSuggestions() {
        val suggestions = CodeDiagnostics.getSuggestions("Col", "kt")
        assertTrue("Suggestions should contain Column", suggestions.any { it.label == "Column" })
    }

    @Test
    fun testProjectTemplates() {
        val files = ProjectTemplates.getFilesForTemplate(
            projectName = "TestApp",
            packageName = "com.test.app",
            template = ProjectTemplateType.COMPOSE_COUNTER,
            minSdk = 26
        )
        assertTrue("Template should generate files", files.isNotEmpty())
        assertTrue("Template should contain build.gradle.kts", files.any { it.relativePath.contains("build.gradle.kts") })
        assertTrue("Template should contain AndroidManifest.xml", files.any { it.relativePath.contains("AndroidManifest.xml") })
        assertTrue("Template should contain MainActivity.kt", files.any { it.relativePath.contains("MainActivity.kt") })
    }

    @Test
    fun testWorkflowGenerator() {
        val appWorkflow = com.example.github.WorkflowGenerator.DROIDIDE_APP_WORKFLOW
        assertTrue("Workflow should contain assembleDebug", appWorkflow.contains("assembleDebug"))
        assertTrue("Workflow should contain upload-artifact", appWorkflow.contains("upload-artifact"))
        assertTrue("Workflow should contain setup-android", appWorkflow.contains("setup-android"))

        val projWorkflow = com.example.github.WorkflowGenerator.getProjectWorkflow("SuperApp")
        assertTrue("Project workflow should mention SuperApp", projWorkflow.contains("SuperApp"))
        assertTrue("Project workflow should contain assembleDebug", projWorkflow.contains("assembleDebug"))
    }
}
