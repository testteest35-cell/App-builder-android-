package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily

data class DependencyItem(
    val name: String,
    val coordinate: String,
    val description: String,
    val category: String
)

val POPULAR_DEPENDENCIES = listOf(
    DependencyItem("Navigation Compose", "androidx.navigation:navigation-compose:2.8.9", "Type-safe Compose routing & backstack", "Architecture"),
    DependencyItem("Room Database", "androidx.room:room-runtime:2.7.0", "Local SQLite database with Coroutines", "Persistence"),
    DependencyItem("Retrofit HTTP", "com.squareup.retrofit2:retrofit:2.12.0", "REST API client with coroutines support", "Networking"),
    DependencyItem("Coil Image Loader", "io.coil-kt:coil-compose:2.7.0", "Fast asynchronous image loading for Compose", "UI / Media"),
    DependencyItem("Moshi JSON", "com.squareup.moshi:moshi-kotlin:1.15.2", "Modern JSON library for Kotlin", "Serialization"),
    DependencyItem("DataStore Preferences", "androidx.datastore:datastore-preferences:1.1.7", "Async key-value storage solution", "Persistence"),
    DependencyItem("CameraX Core", "androidx.camera:camera-camera2:1.5.0", "Android camera hardware API", "Hardware"),
    DependencyItem("Kotlinx Coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2", "Asynchronous programming primitives", "Core")
)

@Composable
fun DependencyManagerDialog(
    onDismiss: () -> Unit,
    onAddDependency: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Android Dependency Manager",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Add libraries directly to app/build.gradle.kts:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(POPULAR_DEPENDENCIES) { dep ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = dep.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = dep.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = dep.coordinate,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { onAddDependency(dep.coordinate) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add dependency")
                                }
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
