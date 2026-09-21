package com.example.data.parser

import com.example.data.model.ProtocolType
import com.example.data.model.ServerConfig
import org.json.JSONArray
import org.json.JSONObject

object SingBoxConfigGenerator {
    fun generate(server: ServerConfig): String {
        val outbound = buildOutbound(server)
        val tun = JSONObject()
            .put("type", "tun")
            .put("tag", "tun-in")
            .put("address", JSONArray().put("172.19.0.1/30").put("fdfe:dcba:9876::1/126"))
            .put("mtu", 1500)
            .put("auto_route", true)
                        .put("dns_mode", "hijack")
            .put("dns_address", JSONArray().put("172.19.0.2").put("fdfe:dcba:9876::2"))

        val route = JSONObject()
            .put("auto_detect_interface", true)
            .put("override_android_vpn", true)
            .put("final", "proxy")

        val dns = JSONObject()
            .put("servers", JSONArray()
                .put(JSONObject().put("type", "https").put("tag", "cloudflare").put("server", "1.1.1.1").put("path", "/dns-query")))
            .put("final", "cloudflare")

        return JSONObject()
            .put("log", JSONObject().put("level", "warn"))
            .put("dns", dns)
            .put("inbounds", JSONArray().put(tun))
            .put("outbounds", JSONArray().put(outbound).put(JSONObject().put("type", "direct").put("tag", "direct")))
            .put("route", route)
            .toString()
    }

    private fun buildOutbound(server: ServerConfig): JSONObject {
        val out = JSONObject()
            .put("server", server.address)
            .put("server_port", server.port)
            .put("tag", "proxy")

        when (server.protocol) {
            ProtocolType.VLESS -> {
                out.put("type", "vless")
                    .put("uuid", server.uuidOrPassword)
                if (server.flow.isNotBlank()) out.put("flow", server.flow)
            }
            ProtocolType.VMESS -> {
                out.put("type", "vmess")
                    .put("uuid", server.uuidOrPassword)
                    .put("security", if (server.security.isBlank()) "auto" else server.security)
                    .put("alter_id", server.alterId)
            }
            ProtocolType.TROJAN -> {
                out.put("type", "trojan")
                    .put("password", server.uuidOrPassword)
            }
            ProtocolType.SHADOWSOCKS -> {
                out.put("type", "shadowsocks")
                    .put("method", if (server.security.isBlank()) "aes-128-gcm" else server.security)
                    .put("password", server.uuidOrPassword)
            }
        }

        if (server.tls.equals("tls", true) || server.tls.equals("reality", true)) {
            val tls = JSONObject().put("enabled", true)
            if (server.sni.isNotBlank()) tls.put("server_name", server.sni)
            if (server.alpn.isNotBlank()) {
                tls.put("alpn", JSONArray(server.alpn.split(',').map { it.trim() }))
            }
            if (server.tls.equals("reality", true)) {
                tls.put("reality", JSONObject()
                    .put("enabled", true)
                    .put("public_key", server.publicKey)
                    .put("short_id", server.shortId))
            }
            out.put("tls", tls)
        }

        when (server.networkType.lowercase()) {
            "ws" -> out.put("transport", JSONObject()
                .put("type", "ws")
                .put("path", server.path.ifBlank { "/" })
                .apply { if (server.host.isNotBlank()) put("headers", JSONObject().put("Host", server.host)) })
            "grpc" -> out.put("transport", JSONObject()
                .put("type", "grpc")
                .put("service_name", server.path))
            "h2", "http" -> out.put("transport", JSONObject()
                .put("type", "http")
                .put("path", server.path.ifBlank { "/" })
                .apply { if (server.host.isNotBlank()) put("host", JSONArray().put(server.host)) })
        }

        return out
    }
}
