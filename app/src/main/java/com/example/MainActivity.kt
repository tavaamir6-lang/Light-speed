package com.example

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VpnState
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ServerListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.screens.TrafficStatsScreen
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Shield),
    SERVERS("Servers", Icons.Default.Dns),
    SUBS("Subs", Icons.Default.RssFeed),
    TRAFFIC("Traffic", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(
                    viewModel = viewModel,
                    activity = this
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    activity: Activity
) {
    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // State collections
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val dailyStats by viewModel.dailyTrafficStats.collectAsStateWithLifecycle()
    val serverStats by viewModel.serverTrafficStats.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val vpnState by viewModel.vpnState.collectAsStateWithLifecycle()
    val uploadSpeed by viewModel.uploadSpeedBps.collectAsStateWithLifecycle()
    val downloadSpeed by viewModel.downloadSpeedBps.collectAsStateWithLifecycle()
    val sessionDuration by viewModel.sessionDurationSeconds.collectAsStateWithLifecycle()
    val sessionUpload by viewModel.sessionUploadBytes.collectAsStateWithLifecycle()
    val sessionDownload by viewModel.sessionDownloadBytes.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val isTestingPing by viewModel.isTestingPing.collectAsStateWithLifecycle()
    val isRefreshingSubs by viewModel.isRefreshingSubs.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // Standard Android VpnService permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.connectOrDisconnect(activity)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("VPN permission was denied.")
            }
        }
    }

    // Android 13+ Notification permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Handle connection trigger with permission check
    val onConnectToggle: () -> Unit = {
        if (vpnState == VpnState.CONNECTED || vpnState == VpnState.CONNECTING) {
            viewModel.connectOrDisconnect(activity)
        } else {
            val vpnIntent = VpnService.prepare(activity)
            if (vpnIntent != null) {
                vpnPermissionLauncher.launch(vpnIntent)
            } else {
                viewModel.connectOrDisconnect(activity)
            }
        }
    }

    // Show status toasts/snackbars
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(lastError) {
        lastError?.let { err ->
            snackbarHostState.showSnackbar("Error: $err")
            viewModel.clearLastError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = CyberDarkSurface,
                tonalElevation = androidx.compose.ui.unit.Dp(0f),
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberDarkSurface,
                            selectedTextColor = NeonEmerald,
                            indicatorColor = NeonEmerald,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.HOME -> {
                    DashboardScreen(
                        vpnState = vpnState,
                        selectedServer = selectedServer,
                        uploadSpeedBps = uploadSpeed,
                        downloadSpeedBps = downloadSpeed,
                        sessionDurationSeconds = sessionDuration,
                        sessionUploadBytes = sessionUpload,
                        sessionDownloadBytes = sessionDownload,
                        settings = settings,
                        onConnectToggle = onConnectToggle,
                        onNavigateToServers = { currentTab = AppTab.SERVERS }
                    )
                }
                AppTab.SERVERS -> {
                    ServerListScreen(
                        servers = servers,
                        selectedServer = selectedServer,
                        settings = settings,
                        isTestingPing = isTestingPing,
                        onSelectServer = { viewModel.selectServer(it) },
                        onDeleteServer = { viewModel.deleteServer(it) },
                        onTestSinglePing = { viewModel.testSinglePing(it) },
                        onTestAllPings = { viewModel.testAllPings() },
                        onAddFromUri = { viewModel.importConfigFromUri(it) },
                        onAddManual = { viewModel.addManualServer(it) },
                        onUpdateSettings = { viewModel.updateSettings(it) }
                    )
                }
                AppTab.SUBS -> {
                    SubscriptionsScreen(
                        subscriptions = subscriptions,
                        isRefreshing = isRefreshingSubs,
                        onRefreshAll = { viewModel.refreshAllSubscriptions() },
                        onRefreshSingle = { viewModel.refreshSubscription(it) },
                        onDeleteSubscription = { viewModel.deleteSubscription(it) },
                        onAddSubscription = { title, url, hours ->
                            viewModel.addSubscription(title, url, hours)
                        }
                    )
                }
                AppTab.TRAFFIC -> {
                    TrafficStatsScreen(
                        dailyStats = dailyStats,
                        serverStats = serverStats,
                        sessionUploadBytes = sessionUpload,
                        sessionDownloadBytes = sessionDownload,
                        onClearStats = { viewModel.clearTrafficStats() }
                    )
                }
                AppTab.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onUpdateSettings = { viewModel.updateSettings(it) }
                    )
                }
            }
        }
    }
}
