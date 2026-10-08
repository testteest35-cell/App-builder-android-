package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.EditorThemeType
import com.example.core.model.IdeSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: IdeSettings,
    onUpdateSettings: (IdeSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var fontSize by remember { mutableFloatStateOf(settings.fontSizeSp.toFloat()) }
    var selectedTheme by remember { mutableStateOf(settings.theme) }
    var showLineNumbers by remember { mutableStateOf(settings.showLineNumbers) }
    var wordWrap by remember { mutableStateOf(settings.wordWrap) }
    var autoSave by remember { mutableStateOf(settings.autoSaveEnabled) }
    var useRemoteBuild by remember { mutableStateOf(settings.useRemoteBuild) }
    var remoteUrl by remember { mutableStateOf(settings.remoteBuildUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("IDE & Editor Preferences", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Editor Theme
                Column {
                    Text("Editor Theme", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    var themeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTheme.displayName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            EditorThemeType.values().forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.displayName) },
                                    onClick = {
                                        selectedTheme = t
                                        themeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Font Size Slider
                Column {
                    Text("Font Size: ${fontSize.toInt()}sp", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 10f..22f,
                        steps = 11
                    )
                }

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Line Numbers", fontSize = 13.sp)
                    Switch(checked = showLineNumbers, onCheckedChange = { showLineNumbers = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Word Wrap", fontSize = 13.sp)
                    Switch(checked = wordWrap, onCheckedChange = { wordWrap = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Save on Edit", fontSize = 13.sp)
                    Switch(checked = autoSave, onCheckedChange = { autoSave = it })
                }

                HorizontalDivider()

                // Remote Build Server
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cloud Remote Builder", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Offload heavy compilation to server", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = useRemoteBuild, onCheckedChange = { useRemoteBuild = it })
                }

                if (useRemoteBuild) {
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = { Text("Remote Build Server URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateSettings(
                        settings.copy(
                            fontSizeSp = fontSize.toInt(),
                            theme = selectedTheme,
                            showLineNumbers = showLineNumbers,
                            wordWrap = wordWrap,
                            autoSaveEnabled = autoSave,
                            useRemoteBuild = useRemoteBuild,
                            remoteBuildUrl = remoteUrl
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
