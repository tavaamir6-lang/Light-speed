package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProtocolType
import com.example.data.model.RoutingSettings
import com.example.data.model.ServerConfig
import com.example.ui.components.PingBadge
import com.example.ui.components.ProtocolBadge
import com.example.ui.dialogs.AddServerDialog
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ServerListScreen(
    servers: List<ServerConfig>,
    selectedServer: ServerConfig?,
    settings: RoutingSettings,
    isTestingPing: Boolean,
    onSelectServer: (Long) -> Unit,
    onDeleteServer: (Long) -> Unit,
    onTestSinglePing: (ServerConfig) -> Unit,
    onTestAllPings: () -> Unit,
    onAddFromUri: (String) -> Boolean,
    onAddManual: (ServerConfig) -> Unit,
    onUpdateSettings: (RoutingSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProtocolFilter by remember { mutableStateOf<ProtocolType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val filteredServers = remember(servers, searchQuery, selectedProtocolFilter) {
        servers.filter { server ->
            val matchesSearch = searchQuery.isBlank() ||
                    server.name.contains(searchQuery, ignoreCase = true) ||
                    server.address.contains(searchQuery, ignoreCase = true)
            val matchesProtocol = selectedProtocolFilter == null || server.protocol == selectedProtocolFilter
            matchesSearch && matchesProtocol
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Screen Title & Ping All Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Proxy Servers",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "${servers.size} total configs",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Button(
                    onClick = onTestAllPings,
                    enabled = !isTestingPing && servers.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan.copy(alpha = 0.2f),
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isTestingPing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testing...", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Icon(Icons.Default.Speed, contentDescription = "Ping", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test All", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Auto select lowest ping toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-connect to lowest ping",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Automatically switches to the fastest server after testing",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = settings.autoSelectLowestPing,
                        onCheckedChange = { onUpdateSettings(settings.copy(autoSelectLowestPing = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberDarkSurface,
                            checkedTrackColor = NeonEmerald
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search servers by name or IP...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    focusedContainerColor = CyberCardSurface,
                    unfocusedContainerColor = CyberCardSurface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Protocol filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isAllSelected = selectedProtocolFilter == null
                Box(
                    modifier = Modifier
                        .background(
                            if (isAllSelected) NeonCyan.copy(alpha = 0.2f) else CyberCardSurface,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedProtocolFilter = null }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("All", style = MaterialTheme.typography.labelSmall, color = if (isAllSelected) NeonCyan else TextMuted)
                }

                ProtocolType.entries.forEach { p ->
                    val isSelected = selectedProtocolFilter == p
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) NeonCyan.copy(alpha = 0.2f) else CyberCardSurface,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedProtocolFilter = p }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(p.displayName, style = MaterialTheme.typography.labelSmall, color = if (isSelected) NeonCyan else TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Servers List
            if (filteredServers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isBlank()) "No servers available" else "No matching servers found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap '+' below to import or add server configs",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredServers, key = { it.id }) { server ->
                        val isSelected = selectedServer?.id == server.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectServer(server.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CyberCardSurface else CyberDarkSurface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) NeonEmerald else CyberCardBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = NeonEmerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = server.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${server.address}:${server.port} • ${server.networkType}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }

                                    ProtocolBadge(server.protocol)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { onTestSinglePing(server) }
                                    ) {
                                        PingBadge(server.pingMs)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.NetworkPing,
                                            contentDescription = "Ping",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Row {
                                        if (server.rawUri.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(server.rawUri))
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Copy URI",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { onDeleteServer(server.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = DangerRed.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
                    }
                }
            }
        }

        // Floating Action Button to add server
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = NeonEmerald,
            contentColor = CyberDarkSurface
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Server")
        }

        if (showAddDialog) {
            AddServerDialog(
                onDismiss = { showAddDialog = false },
                onAddFromUri = onAddFromUri,
                onAddManual = onAddManual
            )
        }
    }
}
