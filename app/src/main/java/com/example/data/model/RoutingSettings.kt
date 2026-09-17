package com.example.data.model

enum class RoutingMode(val displayName: String, val description: String) {
    GLOBAL("Global (All Traffic)", "Route all system traffic through proxy"),
    BYPASS_LAN("Bypass LAN & Direct", "Bypass local private networks (LAN) and route other traffic"),
    PER_APP("Per-App Proxy (Split Tunneling)", "Only route selected apps through the VPN tunnel")
}

enum class PerAppMode(val displayName: String) {
    ALLOW_SELECTED("Only Route Selected Apps"),
    BYPASS_SELECTED("Bypass Selected Apps")
}

data class RoutingSettings(
    val mode: RoutingMode = RoutingMode.BYPASS_LAN,
    val perAppMode: PerAppMode = PerAppMode.ALLOW_SELECTED,
    val selectedPackages: Set<String> = emptySet(),
    val dnsServer: String = "1.1.1.1",
    val autoConnectOnBoot: Boolean = false,
    val pingTimeoutMs: Int = 3000,
    val testUrl: String = "https://www.google.com/generate_204",
    val autoSelectLowestPing: Boolean = false
)

enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STOPPING
}
