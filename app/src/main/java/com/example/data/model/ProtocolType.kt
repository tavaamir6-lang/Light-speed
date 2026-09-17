package com.example.data.model

enum class ProtocolType(val displayName: String) {
    VMESS("VMess"),
    VLESS("VLESS"),
    TROJAN("Trojan"),
    SHADOWSOCKS("Shadowsocks");

    companion object {
        fun fromString(value: String): ProtocolType {
            return when (value.trim().lowercase()) {
                "vmess" -> VMESS
                "vless" -> VLESS
                "trojan" -> TROJAN
                "shadowsocks", "ss" -> SHADOWSOCKS
                else -> VMESS
            }
        }
    }
}
