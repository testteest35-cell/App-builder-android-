package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.github.SshKeyInfo
import com.example.ui.theme.IdeCyan
import com.example.ui.theme.IdeGreen
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun SshKeyDialog(
    keyInfo: SshKeyInfo?,
    isGitHubAuthenticated: Boolean,
    onGenerateNewKey: () -> Unit,
    onRegisterWithGitHub: (title: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var keyTitle by remember { mutableStateOf("DroidIDE Mobile Key") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VpnKey, contentDescription = null, tint = IdeCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SSH Key Manager", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Use this SSH key for passwordless Git transport, push, and clone operations.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (keyInfo != null) {
                    // Public Key Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Public Key (OpenSSH)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("SSH Public Key", keyInfo.publicKeyString)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Public key copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = keyInfo.publicKeyString,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                maxLines = 4,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Fingerprint: ${keyInfo.fingerprint}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = IdeGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Register directly to GitHub if logged in
                    if (isGitHubAuthenticated) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Register with GitHub Account", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = keyTitle,
                                    onValueChange = { keyTitle = it },
                                    label = { Text("Key Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onRegisterWithGitHub(keyTitle.trim()) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add to GitHub Account", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No SSH key generated yet. Tap below to create an RSA 2048-bit key pair.",
                        fontSize = 12.sp
                    )
                }

                FilledTonalButton(
                    onClick = onGenerateNewKey,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (keyInfo != null) "Regenerate Key Pair" else "Generate SSH Key Pair")
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
