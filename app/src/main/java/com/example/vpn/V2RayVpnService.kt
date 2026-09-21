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
import java.io.File
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.ServerConfig
import com.example.data.repository.SettingsRepository
import com.example.data.parser.SingBoxConfigGenerator
import com.example.data.repository.TrafficRepository
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.PlatformInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class V2RayVpnService : VpnService(), CommandServerHandler {
    private var commandServer: CommandServer? = null
    private var platform: SingBoxPlatform? = null
    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentServer: ServerConfig? = null
    private var sessionStartTime = 0L
    private var totalUpload = 0L
    private var totalDownload = 0L

    companion object {
        const val ACTION_START = "com.example.vpn.START"
        const val ACTION_STOP = "com.example.vpn.STOP"
        const val CHANNEL_ID = "v2ray_vpn_channel"
        const val NOTIFICATION_ID = 1001
        @Volatile private var pendingServer: ServerConfig? = null

        fun start(context: Context, server: ServerConfig) {
            pendingServer = server
            val intent = Intent(context, V2RayVpnService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, V2RayVpnService::class.java).setAction(ACTION_STOP))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val basePath = File(filesDir, "libbox").apply { mkdirs() }
        val workingPath = File(filesDir, "libbox-working").apply { mkdirs() }
        val tempPath = File(cacheDir, "libbox-temp").apply { mkdirs() }
        Libbox.setup(basePath.absolutePath, workingPath.absolutePath, tempPath.absolutePath, false)
        DefaultNetworkHolder.start(applicationContext)
        Libbox.promoteOOMDraft()
        Libbox.discardPowerReportDraft()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> pendingServer?.let { startVpn(it) }
                ?: VpnManager.setError("No server configuration")
            ACTION_STOP -> stopVpn()
        }
        return START_NOT_STICKY
    }

    private fun startVpn(server: ServerConfig) {
        serviceJob?.cancel()
        currentServer = server
        VpnManager.setConnecting(server)
        startForeground(NOTIFICATION_ID, buildNotification(server.name, "Connecting..."))

        serviceJob = scope.launch {
            try {
                closeCore()
                val config = SingBoxConfigGenerator.generate(server, SettingsRepository(applicationContext).settings.value)
                val platformInterface = SingBoxPlatform(this@V2RayVpnService, applicationContext) { pfd ->
                    vpnInterface = pfd
                }
                platform = platformInterface
                val core = CommandServer(this@V2RayVpnService, platformInterface as PlatformInterface)
                commandServer = core
                core.start()
                core.startOrReloadService(config, OverrideOptions())

                sessionStartTime = System.currentTimeMillis()
                totalUpload = 0L
                totalDownload = 0L
                VpnManager.setConnected(server, sessionStartTime)
                updateNotification(server.name, "Connected • sing-box TUN")
            } catch (e: Exception) {
                VpnManager.setError(e.message ?: "sing-box start failed")
                closeCore()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun closeCore() {
        runCatching { commandServer?.closeService() }
        runCatching { commandServer?.close() }
        commandServer = null
        runCatching { platform?.close() }
        platform = null
        runCatching { vpnInterface?.close() }
        vpnInterface = null
    }

    private fun stopVpn() {
        VpnManager.setStopping()
        serviceJob?.cancel()
        val duration = if (sessionStartTime > 0) (System.currentTimeMillis() - sessionStartTime) / 1000 else 0L
        val server = currentServer
        if (duration > 0 && server != null) {
            runCatching {
                TrafficRepository(AppDatabase.getDatabase(applicationContext).trafficStatsDao()).recordSessionTraffic(
                    serverId = server.id,
                    serverName = server.name,
                    uploadBytes = totalUpload,
                    downloadBytes = totalDownload,
                    durationSeconds = duration,
                )
            }
        }
        closeCore()
        currentServer = null
        sessionStartTime = 0L
        VpnManager.setDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        closeCore()
        DefaultNetworkHolder.stop(applicationContext)
        scope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun serviceStop() = stopVpn()

    override fun serviceReload() {
        val server = currentServer ?: return
        val config = SingBoxConfigGenerator.generate(server, SettingsRepository(applicationContext).settings.value)
        commandServer?.startOrReloadService(config, OverrideOptions())
    }

    override fun getSystemProxyStatus() = null
    override fun setSystemProxyEnabled(isEnabled: Boolean) = Unit
    override fun triggerNativeCrash() = Unit
    override fun writeDebugMessage(message: String?) = android.util.Log.d("LightSpeed", message ?: "")
    override fun connectSSHAgent(): Int = -1

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "VPN Status", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "VPN connection status"
                    setShowBadge(false)
                },
            )
        }
    }

    private fun updateNotification(title: String, content: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = Intent(this, V2RayVpnService::class.java).setAction(ACTION_STOP)
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
