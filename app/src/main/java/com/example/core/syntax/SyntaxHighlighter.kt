package com.example.core.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.core.model.EditorThemeType
import com.example.ui.theme.*

object SyntaxHighlighter {

    private val KOTLIN_KEYWORDS = setOf(
        "package", "import", "class", "interface", "object", "fun", "val", "var",
        "return", "if", "else", "when", "for", "while", "do", "try", "catch",
        "finally", "throw", "override", "private", "public", "protected", "internal",
        "abstract", "open", "data", "sealed", "enum", "companion", "by", "lazy",
        "in", "is", "as", "null", "true", "false", "this", "super", "suspend",
        "inline", "reified", "crossinline", "tailrec", "operator", "infix", "const",
        "lateinit", "typealias", "yield", "where"
    )

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "final", "finally", "float", "for", "if", "implements", "import",
        "instanceof", "int", "interface", "long", "native", "new", "package",
        "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "void", "volatile", "while", "true", "false", "null"
    )

    private val GRADLE_KEYWORDS = setOf(
        "plugins", "alias", "android", "namespace", "compileSdk", "defaultConfig",
        "applicationId", "minSdk", "targetSdk", "versionCode", "versionName",
        "buildTypes", "release", "debug", "compileOptions", "buildFeatures",
        "compose", "dependencies", "implementation", "testImplementation",
        "androidTestImplementation", "ksp", "platform", "include", "rootProject"
    )

    data class ThemeColors(
        val keyword: Color,
        val string: Color,
        val number: Color,
        val type: Color,
        val annotation: Color,
        val comment: Color,
        val punctuation: Color,
        val defaultText: Color
    )

    fun getThemeColors(theme: EditorThemeType): ThemeColors {
        return when (theme) {
            EditorThemeType.DARCULA -> ThemeColors(
                keyword = DarculaKeyword,
                string = DarculaString,
                number = DarculaNumber,
                type = DarculaType,
                annotation = DarculaAnnotation,
                comment = DarculaComment,
                punctuation = DarculaPunctuation,
                defaultText = Color(0xFFA9B7C6)
            )
            EditorThemeType.ONE_DARK -> ThemeColors(
                keyword = OneDarkKeyword,
                string = OneDarkString,
                number = OneDarkNumber,
                type = OneDarkType,
                annotation = OneDarkAnnotation,
                comment = OneDarkComment,
                punctuation = Color(0xFFABB2BF),
                defaultText = Color(0xFFABB2BF)
            )
            EditorThemeType.MONOKAI -> ThemeColors(
                keyword = MonokaiKeyword,
                string = MonokaiString,
                number = MonokaiNumber,
                type = MonokaiType,
                annotation = MonokaiAnnotation,
                comment = MonokaiComment,
                punctuation = Color(0xFFF8F8F2),
                defaultText = Color(0xFFF8F8F2)
            )
            EditorThemeType.CYBERPUNK -> ThemeColors(
                keyword = CyberpunkKeyword,
                string = CyberpunkString,
                number = CyberpunkNumber,
                type = CyberpunkType,
                annotation = CyberpunkAnnotation,
                comment = CyberpunkComment,
                punctuation = Color(0xFFE2E8F0),
                defaultText = Color(0xFFE2E8F0)
            )
        }
    }

    fun highlight(code: String, extension: String, theme: EditorThemeType): AnnotatedString {
        val colors = getThemeColors(theme)
        return when (extension.lowercase()) {
            "kt", "kts" -> highlightKotlin(code, colors)
            "java" -> highlightJava(code, colors)
            "xml" -> highlightXml(code, colors)
            "gradle" -> highlightGradle(code, colors)
            "json" -> highlightJson(code, colors)
            else -> buildAnnotatedString {
                append(code)
                addStyle(SpanStyle(color = colors.defaultText), 0, code.length)
            }
        }
    }

    private fun highlightKotlin(code: String, colors: ThemeColors): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = colors.defaultText), 0, code.length)

            // 1. Strings: "..." and """..."""
            val stringRegex = Regex("\"\"\"[\\s\\S]*?\"\"\"|\"[^\"\\n\\\\]*(?:\\\\.[^\"\\n\\\\]*)*\"|'[^'\\n\\\\]*(?:\\\\.[^'\\n\\\\]*)*'")
            stringRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.string), match.range.first, match.range.last + 1)
            }

            // 2. Comments: // ... and /* ... */
            val commentRegex = Regex("//.*|/\\*[\\s\\S]*?\\*/")
            commentRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.comment), match.range.first, match.range.last + 1)
            }

            // 3. Annotations: @Composable, @Preview, etc.
            val annotationRegex = Regex("@[A-Za-z0-9_]+")
            annotationRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = colors.annotation, fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 4. Numbers
            val numberRegex = Regex("\\b(?:0x[0-9a-fA-F]+|[0-9]+(?:\\.[0-9]+)?(?:[fFLdD]|\\.dp|\\.sp)?)\\b")
            numberRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.number), match.range.first, match.range.last + 1)
            }

            // 5. Keywords
            val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
            wordRegex.findAll(code).forEach { match ->
                val word = match.value
                if (KOTLIN_KEYWORDS.contains(word)) {
                    addStyle(
                        SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold),
                        match.range.first,
                        match.range.last + 1
                    )
                } else if (word.first().isUpperCase()) {
                    // Type or PascalCase Class / Composable
                    addStyle(SpanStyle(color = colors.type), match.range.first, match.range.last + 1)
                }
            }
        }
    }

    private fun highlightJava(code: String, colors: ThemeColors): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = colors.defaultText), 0, code.length)

            val stringRegex = Regex("\"[^\"\\n\\\\]*(?:\\\\.[^\"\\n\\\\]*)*\"|'[^'\\n\\\\]*(?:\\\\.[^'\\n\\\\]*)*'")
            stringRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.string), match.range.first, match.range.last + 1)
            }

            val commentRegex = Regex("//.*|/\\*[\\s\\S]*?\\*/")
            commentRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.comment), match.range.first, match.range.last + 1)
            }

            val annotationRegex = Regex("@[A-Za-z0-9_]+")
            annotationRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.annotation), match.range.first, match.range.last + 1)
            }

            val numberRegex = Regex("\\b[0-9]+(?:\\.[0-9]+)?[fFLdD]?\\b")
            numberRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.number), match.range.first, match.range.last + 1)
            }

            val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
            wordRegex.findAll(code).forEach { match ->
                val word = match.value
                if (JAVA_KEYWORDS.contains(word)) {
                    addStyle(
                        SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold),
                        match.range.first,
                        match.range.last + 1
                    )
                } else if (word.first().isUpperCase()) {
                    addStyle(SpanStyle(color = colors.type), match.range.first, match.range.last + 1)
                }
            }
        }
    }

    private fun highlightXml(code: String, colors: ThemeColors): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = colors.defaultText), 0, code.length)

            // Tags: <tag or </tag or />
            val tagRegex = Regex("</?[A-Za-z0-9_\\-\\.:]+|/?>")
            tagRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Attribute values
            val attrValRegex = Regex("\"[^\"]*\"")
            attrValRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.string), match.range.first, match.range.last + 1)
            }

            // Attribute names
            val attrNameRegex = Regex("\\b[A-Za-z0-9_\\-:]+(?=\\=)")
            attrNameRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.type), match.range.first, match.range.last + 1)
            }

            // XML comments <!-- ... -->
            val commentRegex = Regex("<!--[\\s\\S]*?-->")
            commentRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.comment), match.range.first, match.range.last + 1)
            }
        }
    }

    private fun highlightGradle(code: String, colors: ThemeColors): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = colors.defaultText), 0, code.length)

            val stringRegex = Regex("\"[^\"]*\"|'[^']*'")
            stringRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.string), match.range.first, match.range.last + 1)
            }

            val commentRegex = Regex("//.*|/\\*[\\s\\S]*?\\*/")
            commentRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.comment), match.range.first, match.range.last + 1)
            }

            val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
            wordRegex.findAll(code).forEach { match ->
                if (GRADLE_KEYWORDS.contains(match.value) || KOTLIN_KEYWORDS.contains(match.value)) {
                    addStyle(
                        SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }
        }
    }

    private fun highlightJson(code: String, colors: ThemeColors): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = colors.defaultText), 0, code.length)

            // Keys
            val keyRegex = Regex("\"[^\"]+\"\\s*:")
            keyRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.type), match.range.first, match.range.last)
            }

            // String values
            val strRegex = Regex(":\\s*\"[^\"]*\"")
            strRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.string), match.range.first + 1, match.range.last + 1)
            }

            // Numbers
            val numRegex = Regex(":\\s*[-0-9\\.]+")
            numRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = colors.number), match.range.first + 1, match.range.last + 1)
            }

            // Booleans / null
            val boolRegex = Regex("\\b(true|false|null)\\b")
            boolRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
    }
}
