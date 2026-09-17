package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SampleData
import com.example.data.db.AppDatabase
import com.example.data.model.RoutingSettings
import com.example.data.model.ServerConfig
import com.example.data.model.Subscription
import com.example.data.model.TrafficStat
import com.example.data.model.VpnState
import com.example.data.parser.V2RayParser
import com.example.data.repository.ServerRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SubscriptionRepository
import com.example.data.repository.TrafficRepository
import com.example.network.PingTester
import com.example.vpn.VpnManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val serverRepo = ServerRepository(db.serverDao())
    private val subscriptionRepo = SubscriptionRepository(db.subscriptionDao(), db.serverDao())
    private val trafficRepo = TrafficRepository(db.trafficStatsDao())
    val settingsRepo = SettingsRepository(application)

    val servers: StateFlow<List<ServerConfig>> = serverRepo.allServers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedServer: StateFlow<ServerConfig?> = serverRepo.selectedServer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subscriptions: StateFlow<List<Subscription>> = subscriptionRepo.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTrafficStats: StateFlow<List<TrafficStat>> = trafficRepo.dailyStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serverTrafficStats: StateFlow<List<TrafficStat>> = trafficRepo.serverStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<RoutingSettings> = settingsRepo.settings

    val vpnState: StateFlow<VpnState> = VpnManager.vpnState
    val uploadSpeedBps: StateFlow<Long> = VpnManager.uploadSpeedBps
    val downloadSpeedBps: StateFlow<Long> = VpnManager.downloadSpeedBps
    val sessionDurationSeconds: StateFlow<Long> = VpnManager.sessionDurationSeconds
    val sessionUploadBytes: StateFlow<Long> = VpnManager.sessionUploadBytes
    val sessionDownloadBytes: StateFlow<Long> = VpnManager.sessionDownloadBytes
    val lastError: StateFlow<String?> = VpnManager.lastError

    private val _isTestingPing = MutableStateFlow(false)
    val isTestingPing: StateFlow<Boolean> = _isTestingPing.asStateFlow()

    private val _isRefreshingSubs = MutableStateFlow(false)
    val isRefreshingSubs: StateFlow<Boolean> = _isRefreshingSubs.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // Seed default sample data if database is empty and ensure user's subscription is present
        viewModelScope.launch(Dispatchers.IO) {
            val userSubUrl = "https://orginal.iranlightspeed.xyz:2096/sub/w63xmya59lum3y8n"
            val existingServers = db.serverDao().getAllServers().first()
            val existingSubs = db.subscriptionDao().getAllSubscriptions().first()

            if (existingServers.isEmpty()) {
                for (sub in SampleData.defaultSubscriptions) {
                    db.subscriptionDao().insertSubscription(sub)
                }
                serverRepo.insertServers(SampleData.defaultServers)
            } else if (existingSubs.none { it.url == userSubUrl }) {
                // Subscription is not yet added, add and fetch immediately
                subscriptionRepo.addSubscription("Iran LightSpeed 🇹🇷", userSubUrl, 6)
            }
        }
    }

    fun selectServer(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            serverRepo.selectServer(id)
        }
    }

    fun deleteServer(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            serverRepo.deleteServerById(id)
            _statusMessage.value = "Server removed"
        }
    }

    fun importConfigFromUri(uri: String): Boolean {
        val server = V2RayParser.parseUri(uri.trim())
        return if (server != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val id = serverRepo.insertServer(server)
                _statusMessage.value = "Imported: ${server.name}"
            }
            true
        } else {
            _statusMessage.value = "Invalid configuration link (Must be vmess://, vless://, trojan://, or ss://)"
            false
        }
    }

    fun addManualServer(server: ServerConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            serverRepo.insertServer(server)
            _statusMessage.value = "Server created: ${server.name}"
        }
    }

    fun updateServer(server: ServerConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            serverRepo.updateServer(server)
            _statusMessage.value = "Server updated"
        }
    }

    fun testSinglePing(server: ServerConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            val timeout = settings.value.pingTimeoutMs
            val ping = PingTester.testTcpPing(server, timeout)
            serverRepo.updatePing(server.id, ping)
        }
    }

    fun testAllPings() {
        if (_isTestingPing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isTestingPing.value = true
            val currentList = servers.value
            val timeout = settings.value.pingTimeoutMs

            val jobs = currentList.map { server ->
                async {
                    val ping = PingTester.testTcpPing(server, timeout)
                    serverRepo.updatePing(server.id, ping)
                    server to ping
                }
            }

            val results = jobs.awaitAll()

            // If auto select lowest ping is enabled, pick the best ping server
            if (settings.value.autoSelectLowestPing) {
                val validPings = results.filter { it.second > 0 }.sortedBy { it.second }
                val best = validPings.firstOrNull()?.first
                if (best != null) {
                    serverRepo.selectServer(best.id)
                    _statusMessage.value = "Auto-selected best server: ${best.name} (${best.pingMs}ms)"
                }
            }

            _isTestingPing.value = false
        }
    }

    fun addSubscription(title: String, url: String, autoUpdateHours: Int = 12) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingSubs.value = true
            val res = subscriptionRepo.addSubscription(title, url, autoUpdateHours)
            _isRefreshingSubs.value = false
            if (res.isSuccess) {
                _statusMessage.value = "Subscription added and synced"
            } else {
                _statusMessage.value = "Error: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun refreshSubscription(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingSubs.value = true
            val res = subscriptionRepo.refreshSubscription(id)
            _isRefreshingSubs.value = false
            if (res.isSuccess) {
                _statusMessage.value = "Updated ${res.getOrNull()} servers"
            } else {
                _statusMessage.value = "Update failed: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun refreshAllSubscriptions() {
        if (_isRefreshingSubs.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingSubs.value = true
            val results = subscriptionRepo.refreshAllSubscriptions()
            _isRefreshingSubs.value = false
            val successCount = results.count { it.value.isSuccess }
            _statusMessage.value = "Updated $successCount subscriptions"
        }
    }

    fun deleteSubscription(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            subscriptionRepo.deleteSubscription(id)
            _statusMessage.value = "Subscription and related servers deleted"
        }
    }

    fun updateSettings(newSettings: RoutingSettings) {
        settingsRepo.updateSettings(newSettings)
    }

    fun connectOrDisconnect(context: Context) {
        when (vpnState.value) {
            VpnState.CONNECTED, VpnState.CONNECTING -> {
                VpnManager.stopVpn(context)
            }
            VpnState.DISCONNECTED, VpnState.STOPPING -> {
                val server = selectedServer.value ?: servers.value.firstOrNull()
                if (server != null) {
                    VpnManager.startVpn(context, server)
                } else {
                    _statusMessage.value = "Please select or add a server first"
                }
            }
        }
    }

    fun clearTrafficStats() {
        viewModelScope.launch(Dispatchers.IO) {
            trafficRepo.clearAll()
            _statusMessage.value = "Traffic statistics cleared"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearLastError() {
        VpnManager.clearError()
    }
}
