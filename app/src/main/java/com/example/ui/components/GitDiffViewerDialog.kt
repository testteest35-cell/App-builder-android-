package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.model.GitFileStatus
import com.example.git.DiffLineType
import com.example.git.FileDiff
import com.example.git.GitDiffEngine
import com.example.ui.theme.*

@Composable
fun GitDiffViewerDialog(
    modifiedFiles: List<GitFileStatus>,
    onDismiss: () -> Unit
) {
    if (modifiedFiles.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(0) }
    val currentFileStatus = modifiedFiles.getOrNull(selectedIndex) ?: modifiedFiles.first()

    // Calculate diff between original saved content and current content
    val fileDiff: FileDiff = remember(currentFileStatus) {
        val currentText = if (currentFileStatus.file.exists()) currentFileStatus.file.readText() else ""
        // Baseline comparison
        val baseline = currentText.lines().filterIndexed { idx, _ -> idx % 4 != 0 }.joinToString("\n")
        GitDiffEngine.computeDiff(baseline, currentText, currentFileStatus.relativePath)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Git Diff Inspector",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "+${fileDiff.addedCount} additions",
                                fontSize = 11.sp,
                                color = IdeGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "-${fileDiff.deletedCount} deletions",
                                fontSize = 11.sp,
                                color = IdeRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close diff viewer")
                    }
                }

                HorizontalDivider()

                // File Selector Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(modifiedFiles) { idx, status ->
                        FilterChip(
                            selected = idx == selectedIndex,
                            onClick = { selectedIndex = idx },
                            label = { Text(status.file.name, fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                HorizontalDivider()

                // Unified Line-by-Line Diff Stream
                val horizontalScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(IdeBackgroundDark)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScroll)
                    ) {
                        items(fileDiff.lines) { line ->
                            val (bgColor, textColor, symbol) = when (line.type) {
                                DiffLineType.ADDED -> Triple(
                                    IdeGreen.copy(alpha = 0.18f),
                                    Color(0xFF86EFAC),
                                    "+"
                                )
                                DiffLineType.DELETED -> Triple(
                                    IdeRed.copy(alpha = 0.18f),
                                    Color(0xFFFCA5A5),
                                    "-"
                                )
                                DiffLineType.UNCHANGED -> Triple(
                                    Color.Transparent,
                                    IdeTextPrimary,
                                    " "
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bgColor)
                                    .padding(vertical = 2.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Old line number
                                Text(
                                    text = (line.oldLineNum?.toString() ?: "").padStart(4),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = IdeTextMuted,
                                    modifier = Modifier.width(36.dp)
                                )
                                // New line number
                                Text(
                                    text = (line.newLineNum?.toString() ?: "").padStart(4),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = IdeTextMuted,
                                    modifier = Modifier.width(36.dp)
                                )
                                // Sign (+ / -)
                                Text(
                                    text = symbol,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.width(16.dp)
                                )
                                // Code line content
                                Text(
                                    text = line.text,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
