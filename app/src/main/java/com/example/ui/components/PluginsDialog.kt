package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plugins.IdePlugin
import com.example.plugins.PluginCategory
import com.example.ui.theme.IdeCyan

@Composable
fun PluginsDialog(
    plugins: List<IdePlugin>,
    onTogglePlugin: (id: String, enabled: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<PluginCategory?>(null) }

    val filteredPlugins = remember(plugins, selectedCategory) {
        if (selectedCategory == null) plugins else plugins.filter { it.category == selectedCategory }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Extension, contentDescription = null, tint = IdeCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("IDE Extensions & Plugins", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enable plugin extensions to enhance editor autocomplete snippets and toolsets.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All", fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                    items(PluginCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.displayName, fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPlugins, key = { it.id }) { plugin ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(plugin.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge { Text("v${plugin.version}", fontSize = 9.sp) }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = plugin.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "By ${plugin.author} • ${plugin.snippets.size} snippets",
                                        fontSize = 10.sp,
                                        color = IdeCyan
                                    )
                                }

                                Switch(
                                    checked = plugin.isEnabled,
                                    onCheckedChange = { onTogglePlugin(plugin.id, it) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
