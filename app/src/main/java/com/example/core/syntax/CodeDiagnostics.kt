package com.example.core.syntax

import com.example.core.model.AutocompleteSuggestion
import com.example.core.model.DiagnosticItem
import com.example.core.model.DiagnosticSeverity
import com.example.core.model.SuggestionCategory
import java.util.Stack

object CodeDiagnostics {

    private val COMPOSE_SUGGESTIONS = listOf(
        AutocompleteSuggestion("@Composable", "@Composable\nfun MyComponent() {\n    \n}", "Composable function annotation", SuggestionCategory.COMPOSABLE),
        AutocompleteSuggestion("Modifier", "Modifier.fillMaxWidth().padding(16.dp)", "Layout modifier chain", SuggestionCategory.MODIFIER),
        AutocompleteSuggestion("Column", "Column(\n    modifier = Modifier.fillMaxWidth(),\n    verticalArrangement = Arrangement.spacedBy(8.dp)\n) {\n    \n}", "Vertical column layout", SuggestionCategory.COMPOSABLE),
        AutocompleteSuggestion("Row", "Row(\n    modifier = Modifier.fillMaxWidth(),\n    horizontalArrangement = Arrangement.SpaceBetween\n) {\n    \n}", "Horizontal row layout", SuggestionCategory.COMPOSABLE),
        AutocompleteSuggestion("Box", "Box(\n    modifier = Modifier.fillMaxSize(),\n    contentAlignment = Alignment.Center\n) {\n    \n}", "Box container layout", SuggestionCategory.COMPOSABLE),
        AutocompleteSuggestion("Text", "Text(\n    text = \"Hello World\",\n    style = MaterialTheme.typography.titleMedium\n)", "Display text element", SuggestionCategory.COMPOSABLE),
        AutocompleteSuggestion("Button", "Button(\n    onClick = { /* TODO */ }\n) {\n    Text(\"Submit\")\n}", "Action button component", SuggestionCategory.COMPOSABLE),
        AutocompleteSuggestion("Card", "Card(\n    modifier = Modifier.fillMaxWidth(),\n    shape = RoundedCornerShape(12.dp)\n) {\n    \n}", "Material Card container", SuggestionCategory.COMPOSABLE),
        AutocompleteSuggestion("Scaffold", "Scaffold(\n    topBar = { TopAppBar(title = { Text(\"Title\") }) }\n) { innerPadding ->\n    \n}", "Scaffold layout structure", SuggestionCategory.COMPOSABLE),
        AutocompleteSuggestion("remember", "var count by remember { mutableIntStateOf(0) }", "State remember delegate", SuggestionCategory.KEYWORD),
        AutocompleteSuggestion("mutableStateOf", "remember { mutableStateOf(\"\") }", "Mutable state wrapper", SuggestionCategory.KEYWORD),
        AutocompleteSuggestion("LazyColumn", "LazyColumn {\n    items(itemsList) { item ->\n        Text(item)\n    }\n}", "Scrollable lazy list", SuggestionCategory.COMPOSABLE),
        AutocompleteSuggestion("LaunchedEffect", "LaunchedEffect(Unit) {\n    \n}", "Side-effect coroutine launcher", SuggestionCategory.COMPOSABLE),
        AutocompleteSuggestion("fillMaxSize()", "fillMaxSize()", "Fill parent size", SuggestionCategory.MODIFIER),
        AutocompleteSuggestion("padding()", "padding(16.dp)", "Apply inner padding", SuggestionCategory.MODIFIER)
    )

    private val KOTLIN_GENERAL_SUGGESTIONS = listOf(
        AutocompleteSuggestion("fun", "fun functionName(): Unit {\n    \n}", "Function declaration", SuggestionCategory.KEYWORD),
        AutocompleteSuggestion("val", "val name = ", "Read-only property", SuggestionCategory.KEYWORD),
        AutocompleteSuggestion("var", "var name = ", "Mutable property", SuggestionCategory.KEYWORD),
        AutocompleteSuggestion("class", "class MyClass {\n    \n}", "Class declaration", SuggestionCategory.KEYWORD),
        AutocompleteSuggestion("data class", "data class MyModel(\n    val id: String,\n    val name: String\n)", "Data class model", SuggestionCategory.KEYWORD),
        AutocompleteSuggestion("override fun", "override fun onCreate(savedInstanceState: Bundle?) {\n    super.onCreate(savedInstanceState)\n}", "Override method", SuggestionCategory.KEYWORD),
        AutocompleteSuggestion("import", "import androidx.compose.runtime.*", "Import statement", SuggestionCategory.KEYWORD)
    )

    /**
     * Inspects code for basic syntax anomalies (unbalanced braces/parentheses, XML mismatches)
     */
    fun analyze(code: String, extension: String): List<DiagnosticItem> {
        val diagnostics = mutableListOf<DiagnosticItem>()
        if (code.isBlank()) return emptyList()

        val lines = code.lines()

        when (extension.lowercase()) {
            "kt", "kts", "java" -> {
                val braceStack = Stack<Pair<Char, Int>>()
                for ((lineIdx, line) in lines.withIndex()) {
                    var inString = false
                    var inChar = false
                    var i = 0
                    while (i < line.length) {
                        val char = line[i]
                        // Check for comment
                        if (!inString && !inChar && char == '/' && i + 1 < line.length && line[i + 1] == '/') {
                            break // Skip rest of line
                        }

                        if (char == '\\' && (inString || inChar)) {
                            // Skip escaped char
                            i += 2
                            continue
                        }

                        if (char == '"' && !inChar) inString = !inString
                        if (char == '\'' && !inString) inChar = !inChar

                        if (!inString && !inChar) {
                            when (char) {
                                '{', '(', '[' -> braceStack.push(char to (lineIdx + 1))
                                '}' -> {
                                    if (braceStack.isEmpty() || braceStack.peek().first != '{') {
                                        diagnostics.add(
                                            DiagnosticItem(lineIdx + 1, "Unmatched closing brace '}'", DiagnosticSeverity.ERROR)
                                        )
                                    } else {
                                        braceStack.pop()
                                    }
                                }
                                ')' -> {
                                    if (braceStack.isEmpty() || braceStack.peek().first != '(') {
                                        diagnostics.add(
                                            DiagnosticItem(lineIdx + 1, "Unmatched closing parenthesis ')'", DiagnosticSeverity.ERROR)
                                        )
                                    } else {
                                        braceStack.pop()
                                    }
                                }
                                ']' -> {
                                    if (braceStack.isEmpty() || braceStack.peek().first != '[') {
                                        diagnostics.add(
                                            DiagnosticItem(lineIdx + 1, "Unmatched closing bracket ']'", DiagnosticSeverity.ERROR)
                                        )
                                    } else {
                                        braceStack.pop()
                                    }
                                }
                            }
                        }
                        i++
                    }

                    if (inString && !line.trim().startsWith("\"\"\"")) {
                        diagnostics.add(
                            DiagnosticItem(lineIdx + 1, "Unclosed string literal", DiagnosticSeverity.ERROR)
                        )
                    }
                }
                while (braceStack.isNotEmpty()) {
                    val (char, line) = braceStack.pop()
                    val name = when (char) {
                        '{' -> "brace '{'"
                        '(' -> "parenthesis '('"
                        '[' -> "bracket '['"
                        else -> "'$char'"
                    }
                    diagnostics.add(
                        DiagnosticItem(line, "Unclosed opening $name", DiagnosticSeverity.ERROR)
                    )
                }
            }
            "xml" -> {
                val openTags = Stack<Pair<String, Int>>()
                val tagRegex = Regex("<(/)?([A-Za-z0-9_\\-\\.:]+)([^>]*)>")
                for ((lineIdx, line) in lines.withIndex()) {
                    tagRegex.findAll(line).forEach { match ->
                        val isClosing = match.groupValues[1] == "/"
                        val tagName = match.groupValues[2]
                        val rawContent = match.groupValues[3]
                        val isSelfClosing = rawContent.trim().endsWith("/")

                        if (!isSelfClosing && !tagName.startsWith("?")) {
                            if (isClosing) {
                                if (openTags.isEmpty() || openTags.peek().first != tagName) {
                                    diagnostics.add(
                                        DiagnosticItem(lineIdx + 1, "Mismatched closing XML tag </$tagName>", DiagnosticSeverity.ERROR)
                                    )
                                } else {
                                    openTags.pop()
                                }
                            } else {
                                openTags.push(tagName to (lineIdx + 1))
                            }
                        }
                    }
                }
                while (openTags.isNotEmpty()) {
                    val (tag, line) = openTags.pop()
                    diagnostics.add(
                        DiagnosticItem(line, "Unclosed XML tag <$tag>", DiagnosticSeverity.WARNING)
                    )
                }
            }
        }
        return diagnostics
    }

    /**
     * Provide autocomplete suggestions based on current word prefix
     */
    fun getSuggestions(prefix: String, extension: String): List<AutocompleteSuggestion> {
        val pool = when (extension.lowercase()) {
            "kt", "kts" -> COMPOSE_SUGGESTIONS + KOTLIN_GENERAL_SUGGESTIONS
            else -> KOTLIN_GENERAL_SUGGESTIONS
        }

        if (prefix.isBlank()) {
            return pool.take(8)
        }

        val clean = prefix.trim().lowercase()
        return pool.filter {
            it.label.lowercase().contains(clean) || it.detail.lowercase().contains(clean)
        }.take(8)
    }
}
