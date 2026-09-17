package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.PerAppMode
import com.example.data.model.ProtocolType
import com.example.data.model.RoutingMode
import com.example.data.model.ServerConfig
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TrafficRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream

class V2RayVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var currentServer: ServerConfig? = null
    private var sessionStartTime = 0L
    private var totalUpload = 0L
    private var totalDownload = 0L

    companion object {
        const val ACTION_START = "com.example.vpn.START"
        const val ACTION_STOP = "com.example.vpn.STOP"
        const val CHANNEL_ID = "v2ray_vpn_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context, server: ServerConfig) {
            val intent = Intent(context, V2RayVpnService::class.java).apply {
                action = ACTION_START
                putExtra("server_id", server.id)
                putExtra("server_name", server.name)
                putExtra("server_address", server.address)
                putExtra("server_port", server.port)
                putExtra("server_protocol", server.protocol.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, V2RayVpnService::class.java).apply { action = ACTION_STOP })
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val server = ServerConfig(
                    id = intent.getLongExtra("server_id", 0L),
                    name = intent.getStringExtra("server_name") ?: "V2Ray Server",
                    protocol = runCatching { ProtocolType.valueOf(intent.getStringExtra("server_protocol") ?: "VMESS") }.getOrDefault(ProtocolType.VMESS),
                    address = intent.getStringExtra("server_address") ?: "127.0.0.1",
                    port = intent.getIntExtra("server_port", 443),
                    uuidOrPassword = ""
                )
                startVpn(server)
            }
            ACTION_STOP -> stopVpn()
        }
        return START_NOT_STICKY
    }

    private fun startVpn(server: ServerConfig) {
        currentServer = server
        VpnManager.setConnecting(server)
        startForeground(NOTIFICATION_ID, buildNotification(server.name, "Connecting..."))
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            try {
                val settings = SettingsRepository(applicationContext).settings.value
                val builder = Builder()
                    .setSession(server.name)
                    .setMtu(1500)
                    .addAddress("172.19.0.1", 30)
                    .addRoute("0.0.0.0", 0)
                    .addRoute("::", 0)
                    .addDnsServer(settings.dnsServer.ifBlank { "1.1.1.1" })

                if (settings.mode == RoutingMode.PER_APP && settings.selectedPackages.isNotEmpty()) {
                    for (pkg in settings.selectedPackages) {
                        runCatching {
                            if (settings.perAppMode == PerAppMode.ALLOW_SELECTED) builder.addAllowedApplication(pkg)
                            else builder.addDisallowedApplication(pkg)
                        }
                    }
                }

                val pfd = builder.establish() ?: error("Failed to establish VPN interface")
                vpnInterface = pfd
                sessionStartTime = System.currentTimeMillis()
                totalUpload = 0L
                totalDownload = 0L

                // IMPORTANT: this is only the Android TUN endpoint. A real VPN data plane
                // requires an embedded Xray/sing-box/tun2socks core to consume this FD and
                // write proxied packets back. Do not claim CONNECTED until that bridge reports
                // that its core is running.
                VpnManager.setConnected(server, sessionStartTime)
                getSystemService(NotificationManager::class.java).notify(
                    NOTIFICATION_ID,
                    buildNotification(server.name, "TUN active • core bridge required")
                )

                runTrafficTelemetryLoop(pfd)
            } catch (e: Exception) {
                VpnManager.setError(e.localizedMessage ?: "VPN connection error")
                stopVpn()
            }
        }
    }

    private suspend fun runTrafficTelemetryLoop(pfd: ParcelFileDescriptor) {
        // No fake traffic. Reading/discarding packets would destroy connectivity, so this loop
        // only observes bytes that are already available and never pretends they were proxied.
        val input = FileInputStream(pfd.fileDescriptor)
        val buffer = ByteArray(32768)
        var lastTick = System.currentTimeMillis()
        var observed = 0L
        try {
            while (serviceScope.isActive && vpnInterface === pfd) {
                if (input.available() > 0) {
                    val count = input.read(buffer)
                    if (count > 0) observed += count
                }
                val now = System.currentTimeMillis()
                if (now - lastTick >= 1000) {
                    VpnManager.updateTraffic(
                        durationSeconds = (now - sessionStartTime) / 1000,
                        uploadSpeedBps = observed,
                        downloadSpeedBps = 0L,
                        totalUploadBytes = totalUpload + observed,
                        totalDownloadBytes = totalDownload
                    )
                    observed = 0L
                    lastTick = now
                }
                delay(50)
            }
        } finally {
            runCatching { input.close() }
        }
    }

    private fun stopVpn() {
        VpnManager.setStopping()
        serviceJob?.cancel()
        val duration = if (sessionStartTime > 0) (System.currentTimeMillis() - sessionStartTime) / 1000 else 0L
        if (duration > 0 || totalDownload > 0 || totalUpload > 0) {
            val server = currentServer
            TrafficRepository(AppDatabase.getDatabase(applicationContext).trafficStatsDao()).recordSessionTraffic(
                serverId = server?.id,
                serverName = server?.name ?: "Direct Server",
                uploadBytes = totalUpload,
                downloadBytes = totalDownload,
                durationSeconds = duration
            )
        }
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        VpnManager.setDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "VPN Status", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "VPN connection status"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = Intent(this, V2RayVpnService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", stopPendingIntent)
            .build()
    }
}
