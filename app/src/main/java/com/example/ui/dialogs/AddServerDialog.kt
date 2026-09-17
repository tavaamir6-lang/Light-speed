package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.example.data.model.ProtocolType
import com.example.data.model.ServerConfig
import com.example.data.parser.V2RayParser
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onAddFromUri: (String) -> Boolean,
    onAddManual: (ServerConfig) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val clipboardManager = LocalClipboardManager.current

    // Tab 0: Link Import
    var uriInput by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }

    // Tab 1: Manual Input
    var name by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf(ProtocolType.VLESS) }
    var address by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("443") }
    var uuidOrPassword by remember { mutableStateOf("") }
    var networkType by remember { mutableStateOf("ws") }
    var path by remember { mutableStateOf("") }
    var tls by remember { mutableStateOf("tls") }
    var sni by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberDarkSurface,
        title = {
            Text(
                text = "Add Proxy Server",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CyberDarkSurface,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Import Link") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Manual") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Link paste tab
                    Text(
                        text = "Paste a vmess://, vless://, trojan://, or ss:// configuration URI:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uriInput,
                        onValueChange = {
                            uriInput = it
                            parseError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("vmess://... or vless://...") },
                        maxLines = 4,
                        trailingIcon = {
                            IconButton(onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    uriInput = clip.trim()
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = NeonCyan)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedContainerColor = CyberCardSurface,
                            focusedContainerColor = CyberCardSurface
                        )
                    )

                    if (parseError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = parseError ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // Manual config tab
                    Text("Protocol", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProtocolType.entries.forEach { p ->
                            val isSelected = protocol == p
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) NeonCyan.copy(alpha = 0.25f) else CyberCardSurface,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { protocol = p }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) NeonCyan else TextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Server Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Host / IP") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = portText,
                            onValueChange = { portText = it },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uuidOrPassword,
                        onValueChange = { uuidOrPassword = it },
                        label = { Text(if (protocol == ProtocolType.VMESS || protocol == ProtocolType.VLESS) "UUID" else "Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = networkType,
                            onValueChange = { networkType = it },
                            label = { Text("Network (ws/tcp/grpc)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tls,
                            onValueChange = { tls = it },
                            label = { Text("TLS (none/tls/reality)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = sni,
                        onValueChange = { sni = it },
                        label = { Text("SNI / Host (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = path,
                        onValueChange = { path = it },
                        label = { Text("Path / Service (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        if (uriInput.isBlank()) {
                            parseError = "Please enter a URI link"
                            return@Button
                        }
                        val success = onAddFromUri(uriInput)
                        if (success) {
                            onDismiss()
                        } else {
                            parseError = "Failed to parse link. Check format."
                        }
                    } else {
                        if (address.isBlank() || uuidOrPassword.isBlank()) {
                            return@Button
                        }
                        val port = portText.toIntOrNull() ?: 443
                        val server = ServerConfig(
                            name = name.ifBlank { "${protocol.displayName} Server" },
                            protocol = protocol,
                            address = address.trim(),
                            port = port,
                            uuidOrPassword = uuidOrPassword.trim(),
                            networkType = networkType.trim().lowercase(),
                            tls = tls.trim().lowercase(),
                            sni = sni.trim(),
                            path = path.trim()
                        )
                        onAddManual(server)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
            ) {
                Text("Save Server", color = CyberDarkSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
