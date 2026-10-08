package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.EditorThemeType
import com.example.core.model.IdeSettings
import com.example.ui.theme.IdeCyan
import com.example.ui.theme.IdePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: IdeSettings,
    isGitHubAuthenticated: Boolean,
    onUpdateSettings: (IdeSettings) -> Unit,
    onOpenGitHubTab: () -> Unit,
    onResetDefaultProjects: () -> Unit,
    onOpenSshKeys: () -> Unit = {},
    onOpenPlugins: () -> Unit = {},
    onOpenWorkflows: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var fontSize by remember(settings.fontSizeSp) { mutableFloatStateOf(settings.fontSizeSp.toFloat()) }
    var selectedTheme by remember(settings.theme) { mutableStateOf(settings.theme) }
    var showLineNumbers by remember(settings.showLineNumbers) { mutableStateOf(settings.showLineNumbers) }
    var wordWrap by remember(settings.wordWrap) { mutableStateOf(settings.wordWrap) }
    var autoSave by remember(settings.autoSaveEnabled) { mutableStateOf(settings.autoSaveEnabled) }
    var useRemoteBuild by remember(settings.useRemoteBuild) { mutableStateOf(settings.useRemoteBuild) }
    var remoteUrl by remember(settings.remoteBuildUrl) { mutableStateOf(settings.remoteBuildUrl) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "IDE Preferences & Tools",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 1. GitHub Integration Card
        ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("GitHub Integration", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            if (isGitHubAuthenticated) "Connected with Personal Access Token" else "Not authenticated",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onOpenGitHubTab,
                        colors = ButtonDefaults.buttonColors(containerColor = IdePurple),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (isGitHubAuthenticated) "Manage" else "Connect", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenSshKeys,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SSH Keys", fontSize = 12.sp)
                    }
                    FilledTonalButton(
                        onClick = onOpenPlugins,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Plugins", fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Build Server Settings Card
        ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Build Engine", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Remote Cloud Builder", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Delegate compilation to Docker/Node.js Gradle worker", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = useRemoteBuild,
                        onCheckedChange = {
                            useRemoteBuild = it
                            onUpdateSettings(settings.copy(useRemoteBuild = it))
                        }
                    )
                }

                if (useRemoteBuild) {
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = {
                            remoteUrl = it
                            onUpdateSettings(settings.copy(remoteBuildUrl = it))
                        },
                        label = { Text("Server URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. Editor Preferences Card
        ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Code Editor Styling", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                // Theme Dropdown
                var themeExpanded by remember { mutableStateOf(false) }
                Column {
                    Text("Theme", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        onUpdateSettings(settings.copy(theme = t))
                                    }
                                )
                            }
                        }
                    }
                }

                // Font Size Slider
                Column {
                    Text("Editor Font Size: ${fontSize.toInt()}sp", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = fontSize,
                        onValueChange = {
                            fontSize = it
                            onUpdateSettings(settings.copy(fontSizeSp = it.toInt()))
                        },
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
                    Switch(
                        checked = showLineNumbers,
                        onCheckedChange = {
                            showLineNumbers = it
                            onUpdateSettings(settings.copy(showLineNumbers = it))
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Word Wrap", fontSize = 13.sp)
                    Switch(
                        checked = wordWrap,
                        onCheckedChange = {
                            wordWrap = it
                            onUpdateSettings(settings.copy(wordWrap = it))
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Save Changes", fontSize = 13.sp)
                    Switch(
                        checked = autoSave,
                        onCheckedChange = {
                            autoSave = it
                            onUpdateSettings(settings.copy(autoSaveEnabled = it))
                        }
                    )
                }
            }
        }

        // 4. GitHub Actions CI/CD Card
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = IdeCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GitHub Actions CI/CD Workflow", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure automated GitHub Actions to build, release, and install this app directly on Android devices via cloud workflow runs.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onOpenWorkflows,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View & Copy Workflow Actions")
                }
            }
        }

        // 5. Reset & Diagnostics
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Sample Projects", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Restore pre-bundled Compose Counter and TaskCraft projects.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onResetDefaultProjects) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Sample Projects")
                }
            }
        }
    }
}
