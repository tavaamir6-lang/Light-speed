package com.example.vpn

import android.content.Context
import com.example.data.model.ServerConfig
import com.example.data.model.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnManager {

    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _currentServer = MutableStateFlow<ServerConfig?>(null)
    val currentServer: StateFlow<ServerConfig?> = _currentServer.asStateFlow()

    private val _sessionDurationSeconds = MutableStateFlow(0L)
    val sessionDurationSeconds: StateFlow<Long> = _sessionDurationSeconds.asStateFlow()

    private val _uploadSpeedBps = MutableStateFlow(0L)
    val uploadSpeedBps: StateFlow<Long> = _uploadSpeedBps.asStateFlow()

    private val _downloadSpeedBps = MutableStateFlow(0L)
    val downloadSpeedBps: StateFlow<Long> = _downloadSpeedBps.asStateFlow()

    private val _sessionUploadBytes = MutableStateFlow(0L)
    val sessionUploadBytes: StateFlow<Long> = _sessionUploadBytes.asStateFlow()

    private val _sessionDownloadBytes = MutableStateFlow(0L)
    val sessionDownloadBytes: StateFlow<Long> = _sessionDownloadBytes.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun setConnecting(server: ServerConfig) {
        _currentServer.value = server
        _vpnState.value = VpnState.CONNECTING
        _lastError.value = null
        _sessionDurationSeconds.value = 0L
        _uploadSpeedBps.value = 0L
        _downloadSpeedBps.value = 0L
        _sessionUploadBytes.value = 0L
        _sessionDownloadBytes.value = 0L
    }

    fun setConnected(server: ServerConfig, startTime: Long) {
        _currentServer.value = server
        _vpnState.value = VpnState.CONNECTED
        _lastError.value = null
    }

    fun updateTraffic(
        durationSeconds: Long,
        uploadSpeedBps: Long,
        downloadSpeedBps: Long,
        totalUploadBytes: Long,
        totalDownloadBytes: Long
    ) {
        _sessionDurationSeconds.value = durationSeconds
        _uploadSpeedBps.value = uploadSpeedBps
        _downloadSpeedBps.value = downloadSpeedBps
        _sessionUploadBytes.value = totalUploadBytes
        _sessionDownloadBytes.value = totalDownloadBytes
    }

    fun setStopping() {
        _vpnState.value = VpnState.STOPPING
        _uploadSpeedBps.value = 0L
        _downloadSpeedBps.value = 0L
    }

    fun setDisconnected() {
        _vpnState.value = VpnState.DISCONNECTED
        _uploadSpeedBps.value = 0L
        _downloadSpeedBps.value = 0L
    }

    fun setError(error: String) {
        _lastError.value = error
        _vpnState.value = VpnState.DISCONNECTED
    }

    fun clearError() {
        _lastError.value = null
    }

    fun startVpn(context: Context, server: ServerConfig) {
        V2RayVpnService.start(context, server)
    }

    fun stopVpn(context: Context) {
        V2RayVpnService.stop(context)
    }
}
