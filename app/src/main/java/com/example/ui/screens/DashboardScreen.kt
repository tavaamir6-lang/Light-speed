package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RoutingSettings
import com.example.data.model.ServerConfig
import com.example.data.model.VpnState
import com.example.ui.components.FormatUtils
import com.example.ui.components.PingBadge
import com.example.ui.components.ProtocolBadge
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
    vpnState: VpnState,
    selectedServer: ServerConfig?,
    uploadSpeedBps: Long,
    downloadSpeedBps: Long,
    sessionDurationSeconds: Long,
    sessionUploadBytes: Long,
    sessionDownloadBytes: Long,
    settings: RoutingSettings,
    onConnectToggle: () -> Unit,
    onNavigateToServers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (vpnState == VpnState.CONNECTED || vpnState == VpnState.CONNECTING) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, NeonEmerald)),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Shield",
                        tint = CyberDarkSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "XRAY CLIENT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "V2Ray & Xray Proxy Engine",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Status chip
            val (statusBg, statusText, statusColor) = when (vpnState) {
                VpnState.CONNECTED -> Triple(NeonEmerald.copy(alpha = 0.15f), "CONNECTED", NeonEmerald)
                VpnState.CONNECTING -> Triple(WarningAmber.copy(alpha = 0.15f), "CONNECTING", WarningAmber)
                VpnState.STOPPING -> Triple(WarningAmber.copy(alpha = 0.15f), "DISCONNECTING", WarningAmber)
                VpnState.DISCONNECTED -> Triple(CyberCardSurface, "DISCONNECTED", TextMuted)
            }

            Box(
                modifier = Modifier
                    .background(statusBg, RoundedCornerShape(12.dp))
                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Big Circular Connection Button
        Box(
            modifier = Modifier
                .size(210.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow ring
            if (vpnState == VpnState.CONNECTED || vpnState == VpnState.CONNECTING) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(pulseScale)
                        .background(
                            if (vpnState == VpnState.CONNECTED) NeonEmerald.copy(alpha = 0.12f)
                            else NeonCyan.copy(alpha = 0.15f),
                            CircleShape
                        )
                )
            }

            // Interactive Button
            val buttonGradient = when (vpnState) {
                VpnState.CONNECTED -> Brush.radialGradient(listOf(NeonEmerald, NeonEmerald.copy(alpha = 0.85f)))
                VpnState.CONNECTING -> Brush.radialGradient(listOf(NeonCyan, ElectricViolet))
                VpnState.STOPPING -> Brush.radialGradient(listOf(WarningAmber, DangerRed))
                VpnState.DISCONNECTED -> Brush.radialGradient(listOf(CyberCardSurface, CyberDarkSurface))
            }

            val borderColor = when (vpnState) {
                VpnState.CONNECTED -> NeonEmerald
                VpnState.CONNECTING -> NeonCyan
                VpnState.STOPPING -> WarningAmber
                VpnState.DISCONNECTED -> CyberCardBorder
            }

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(buttonGradient)
                    .border(3.dp, borderColor, CircleShape)
                    .clickable { onConnectToggle() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = "Power",
                        modifier = Modifier.size(54.dp),
                        tint = if (vpnState == VpnState.DISCONNECTED) TextMuted else CyberDarkSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (vpnState) {
                            VpnState.CONNECTED -> "STOP"
                            VpnState.CONNECTING -> "WAIT..."
                            VpnState.STOPPING -> "STOPPING"
                            VpnState.DISCONNECTED -> "CONNECT"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (vpnState == VpnState.DISCONNECTED) TextSecondary else CyberDarkSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Live Upload/Download Speed Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Download speed
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "Download",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("DOWNLOAD", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = FormatUtils.formatSpeed(downloadSpeedBps),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonCyan
                        )
                    }
                }
            }

            // Upload speed
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonEmerald.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Upload",
                            tint = NeonEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("UPLOAD", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = FormatUtils.formatSpeed(uploadSpeedBps),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonEmerald
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Server Card (clickable to switch server)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToServers() },
            colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE SERVER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = NeonCyan
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Change", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Change Server",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedServer != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedServer.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${selectedServer.address}:${selectedServer.port}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProtocolBadge(selectedServer.protocol)
                            Spacer(modifier = Modifier.width(8.dp))
                            PingBadge(selectedServer.pingMs)
                        }
                    }
                } else {
                    Text(
                        text = "No server selected. Tap to choose a server.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = "Duration", tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DURATION", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatDuration(sessionDurationSeconds),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(32.dp).background(CyberCardBorder))

                // Session Download
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DATA IN", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatBytes(sessionDownloadBytes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(32.dp).background(CyberCardBorder))

                // Session Upload
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DATA OUT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatBytes(sessionUploadBytes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeonEmerald
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Routing Mode Badge
        Row(
            modifier = Modifier
                .background(CyberCardSurface, RoundedCornerShape(10.dp))
                .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Routing Mode: ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
                text = settings.mode.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ElectricViolet
            )
        }
    }
}
