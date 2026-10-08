package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.*
import com.example.core.syntax.CodeDiagnostics
import com.example.core.syntax.SyntaxHighlighter
import com.example.ui.theme.*

@Composable
fun CodeEditorPane(
    openTabs: List<EditorTab>,
    activeTabIndex: Int,
    settings: IdeSettings,
    isSearchOpen: Boolean,
    searchQuery: String,
    replaceQuery: String,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onContentChange: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onReplace: (all: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTab = openTabs.getOrNull(activeTabIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                when (settings.theme) {
                    EditorThemeType.DARCULA -> DarculaBg
                    EditorThemeType.ONE_DARK -> OneDarkBg
                    EditorThemeType.MONOKAI -> MonokaiBg
                    EditorThemeType.CYBERPUNK -> CyberpunkBg
                }
            )
    ) {
        // 1. Tab Bar
        EditorTabBar(
            tabs = openTabs,
            activeTabIndex = activeTabIndex,
            onSelectTab = onSelectTab,
            onCloseTab = onCloseTab
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        if (activeTab == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No file open. Select a file from the explorer.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        // 2. Search & Replace Banner
        if (isSearchOpen) {
            SearchReplaceBanner(
                searchQuery = searchQuery,
                replaceQuery = replaceQuery,
                onSearchChange = onSearchQueryChange,
                onReplaceChange = onReplaceQueryChange,
                onReplace = { onReplace(false) },
                onReplaceAll = { onReplace(true) },
                onClose = onToggleSearch
            )
        }

        // 3. Quick Symbol and Mobile Action Bar
        QuickSymbolBar(
            onInsertSymbol = { symbol ->
                onContentChange(activeTab.content + symbol)
            },
            onUndo = onUndo,
            onRedo = onRedo,
            onSave = onSave,
            onToggleSearch = onToggleSearch,
            canUndo = activeTab.undoStack.isNotEmpty(),
            canRedo = activeTab.redoStack.isNotEmpty()
        )

        // 4. Autocomplete Suggestions Bar
        AutocompleteBar(
            extension = activeTab.file.extension,
            onSelectSuggestion = { suggestion ->
                onContentChange(activeTab.content + suggestion.insertText)
            }
        )

        // 5. Diagnostics Banner (Errors / Warnings)
        if (activeTab.diagnostics.isNotEmpty()) {
            DiagnosticsBanner(diagnostics = activeTab.diagnostics)
        }

        // 6. Text Editor Area with Line Numbers
        EditorTextCanvas(
            content = activeTab.content,
            extension = activeTab.file.extension,
            settings = settings,
            onContentChange = onContentChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EditorTabBar(
    tabs: List<EditorTab>,
    activeTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(tabs) { index, tab ->
            val isSelected = index == activeTabIndex
            Surface(
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .clickable { onSelectTab(index) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (tab.isDirty) "${tab.fileName} *" else tab.fileName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) IdeCyan else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { onCloseTab(index) },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close Tab",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSymbolBar(
    onInsertSymbol: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onToggleSearch: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean
) {
    val symbols = listOf("{", "}", "(", ")", "[", "]", "\"", "'", "=", ";", ":", "->", ".", ",", "<", ">", "@", "$", "val ", "fun ", "Modifier.")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Editor Actions (Undo, Redo, Save, Search)
        IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Redo, contentDescription = "Redo", modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onSave, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Save, contentDescription = "Save", tint = IdeGreen, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onToggleSearch, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = IdeCyan, modifier = Modifier.size(16.dp))
        }

        VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 4.dp))

        // Horizontal scrolling symbol buttons
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(symbols) { sym ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onInsertSymbol(sym) }
                ) {
                    Text(
                        text = sym,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AutocompleteBar(
    extension: String,
    onSelectSuggestion: (AutocompleteSuggestion) -> Unit
) {
    val suggestions = remember(extension) { CodeDiagnostics.getSuggestions("", extension) }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(suggestions) { item ->
            SuggestionChip(
                onClick = { onSelectSuggestion(item) },
                label = {
                    Text(
                        text = item.label,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = when (item.category) {
                            SuggestionCategory.COMPOSABLE -> IdePurple
                            SuggestionCategory.MODIFIER -> IdeCyan
                            SuggestionCategory.KEYWORD -> IdeOrange
                            else -> IdeGreen
                        }
                    )
                },
                modifier = Modifier.height(26.dp)
            )
        }
    }
}

@Composable
private fun DiagnosticsBanner(diagnostics: List<DiagnosticItem>) {
    val first = diagnostics.firstOrNull() ?: return
    Surface(
        color = if (first.severity == DiagnosticSeverity.ERROR) IdeRed.copy(alpha = 0.15f) else IdeYellow.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (first.severity == DiagnosticSeverity.ERROR) Icons.Default.Error else Icons.Default.Warning,
                contentDescription = null,
                tint = if (first.severity == DiagnosticSeverity.ERROR) IdeRed else IdeYellow,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Line ${first.line}: ${first.message}",
                fontSize = 11.sp,
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchReplaceBanner(
    searchQuery: String,
    replaceQuery: String,
    onSearchChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Find...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = replaceQuery,
                onValueChange = onReplaceChange,
                placeholder = { Text("Replace...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(onClick = onReplace, modifier = Modifier.height(36.dp)) {
                Text("Replace", fontSize = 11.sp)
            }
            FilledTonalButton(onClick = onReplaceAll, modifier = Modifier.height(36.dp)) {
                Text("All", fontSize = 11.sp)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close search")
            }
        }
    }
}

@Composable
private fun EditorTextCanvas(
    content: String,
    extension: String,
    settings: IdeSettings,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val lines = remember(content) { content.lines() }
    val lineCount = lines.size.coerceAtLeast(1)

    val gutterBg = when (settings.theme) {
        EditorThemeType.DARCULA -> DarculaGutterBg
        EditorThemeType.ONE_DARK -> OneDarkGutterBg
        EditorThemeType.MONOKAI -> MonokaiGutterBg
        EditorThemeType.CYBERPUNK -> CyberpunkGutterBg
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState)
    ) {
        // Line Numbers Gutter
        if (settings.showLineNumbers) {
            Column(
                modifier = Modifier
                    .background(gutterBg)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i",
                        fontSize = settings.fontSizeSp.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = Color(0xFF6C707E),
                        lineHeight = (settings.fontSizeSp * 1.5).sp
                    )
                }
            }
        }

        // Code Editor Text Field with Syntax Highlighting Visual Transformation
        val syntaxTransformation = remember(extension, settings.theme) {
            VisualTransformation { originalText ->
                val annotated = SyntaxHighlighter.highlight(originalText.text, extension, settings.theme)
                TransformedText(annotated, OffsetMapping.Identity)
            }
        }

        val textModifier = if (settings.wordWrap) {
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        } else {
            Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        }

        BasicTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = textModifier,
            visualTransformation = syntaxTransformation,
            textStyle = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = settings.fontSizeSp.sp,
                lineHeight = (settings.fontSizeSp * 1.5).sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(IdeCyan)
        )
    }
}
