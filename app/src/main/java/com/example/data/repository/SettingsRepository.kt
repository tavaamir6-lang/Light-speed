package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.PerAppMode
import com.example.data.model.RoutingMode
import com.example.data.model.RoutingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("xray_vpn_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<RoutingSettings> = _settings.asStateFlow()

    private fun loadSettings(): RoutingSettings {
        val modeStr = prefs.getString("routing_mode", RoutingMode.BYPASS_LAN.name) ?: RoutingMode.BYPASS_LAN.name
        val mode = try { RoutingMode.valueOf(modeStr) } catch (_: Exception) { RoutingMode.BYPASS_LAN }

        val perAppModeStr = prefs.getString("per_app_mode", PerAppMode.ALLOW_SELECTED.name) ?: PerAppMode.ALLOW_SELECTED.name
        val perAppMode = try { PerAppMode.valueOf(perAppModeStr) } catch (_: Exception) { PerAppMode.ALLOW_SELECTED }

        val packages = prefs.getStringSet("selected_packages", emptySet()) ?: emptySet()
        val dns = prefs.getString("dns_server", "1.1.1.1") ?: "1.1.1.1"
        val autoBoot = prefs.getBoolean("auto_boot", false)
        val pingTimeout = prefs.getInt("ping_timeout", 3000)
        val testUrl = prefs.getString("test_url", "https://www.google.com/generate_204") ?: "https://www.google.com/generate_204"
        val autoLowest = prefs.getBoolean("auto_lowest_ping", false)

        return RoutingSettings(
            mode = mode,
            perAppMode = perAppMode,
            selectedPackages = packages,
            dnsServer = dns,
            autoConnectOnBoot = autoBoot,
            pingTimeoutMs = pingTimeout,
            testUrl = testUrl,
            autoSelectLowestPing = autoLowest
        )
    }

    fun updateSettings(newSettings: RoutingSettings) {
        prefs.edit()
            .putString("routing_mode", newSettings.mode.name)
            .putString("per_app_mode", newSettings.perAppMode.name)
            .putStringSet("selected_packages", newSettings.selectedPackages)
            .putString("dns_server", newSettings.dnsServer)
            .putBoolean("auto_boot", newSettings.autoConnectOnBoot)
            .putInt("ping_timeout", newSettings.pingTimeoutMs)
            .putString("test_url", newSettings.testUrl)
            .putBoolean("auto_lowest_ping", newSettings.autoSelectLowestPing)
            .apply()

        _settings.value = newSettings
    }
}
