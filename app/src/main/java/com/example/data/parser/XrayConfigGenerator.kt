package com.example.data.parser

import com.example.data.model.ProtocolType
import com.example.data.model.RoutingMode
import com.example.data.model.RoutingSettings
import com.example.data.model.ServerConfig
import org.json.JSONArray
import org.json.JSONObject

object XrayConfigGenerator {

    /**
     * Generates a complete standard Xray/V2Ray JSON configuration
     */
    fun generateConfig(server: ServerConfig, settings: RoutingSettings, localSocksPort: Int = 10808, localHttpPort: Int = 10809): String {
        val root = JSONObject()

        // 1. Log
        val log = JSONObject().apply {
            put("access", "")
            put("error", "")
            put("loglevel", "warning")
        }
        root.put("log", log)

        // 2. Inbounds (Local SOCKS5 and HTTP inbound for apps/tun2socks)
        val inbounds = JSONArray()

        val socksInbound = JSONObject().apply {
            put("tag", "socks-in")
            put("port", localSocksPort)
            put("listen", "127.0.0.1")
            put("protocol", "socks")
            put("settings", JSONObject().apply {
                put("auth", "noauth")
                put("udp", true)
            })
            put("sniffing", JSONObject().apply {
                put("enabled", true)
                put("destOverride", JSONArray(listOf("http", "tls")))
            })
        }
        inbounds.put(socksInbound)

        val httpInbound = JSONObject().apply {
            put("tag", "http-in")
            put("port", localHttpPort)
            put("listen", "127.0.0.1")
            put("protocol", "http")
            put("settings", JSONObject().apply {
                put("timeout", 0)
            })
        }
        inbounds.put(httpInbound)

        root.put("inbounds", inbounds)

        // 3. Outbounds
        val outbounds = JSONArray()

        // Main proxy outbound
        val proxyOutbound = JSONObject().apply {
            put("tag", "proxy")
            when (server.protocol) {
                ProtocolType.VMESS -> {
                    put("protocol", "vmess")
                    put("settings", JSONObject().apply {
                        val vnext = JSONArray()
                        val serverObj = JSONObject().apply {
                            put("address", server.address)
                            put("port", server.port)
                            val users = JSONArray()
                            users.put(JSONObject().apply {
                                put("id", server.uuidOrPassword)
                                put("alterId", server.alterId)
                                put("security", if (server.security.isNotBlank()) server.security else "auto")
                            })
                            put("users", users)
                        }
                        vnext.put(serverObj)
                        put("vnext", vnext)
                    })
                }
                ProtocolType.VLESS -> {
                    put("protocol", "vless")
                    put("settings", JSONObject().apply {
                        val vnext = JSONArray()
                        val serverObj = JSONObject().apply {
                            put("address", server.address)
                            put("port", server.port)
                            val users = JSONArray()
                            users.put(JSONObject().apply {
                                put("id", server.uuidOrPassword)
                                put("encryption", "none")
                                if (server.flow.isNotBlank()) {
                                    put("flow", server.flow)
                                }
                            })
                            put("users", users)
                        }
                        vnext.put(serverObj)
                        put("vnext", vnext)
                    })
                }
                ProtocolType.TROJAN -> {
                    put("protocol", "trojan")
                    put("settings", JSONObject().apply {
                        val servers = JSONArray()
                        servers.put(JSONObject().apply {
                            put("address", server.address)
                            put("port", server.port)
                            put("password", server.uuidOrPassword)
                        })
                        put("servers", servers)
                    })
                }
                ProtocolType.SHADOWSOCKS -> {
                    put("protocol", "shadowsocks")
                    put("settings", JSONObject().apply {
                        val servers = JSONArray()
                        servers.put(JSONObject().apply {
                            put("address", server.address)
                            put("port", server.port)
                            put("method", if (server.security.isNotBlank()) server.security else "aes-256-gcm")
                            put("password", server.uuidOrPassword)
                            put("ota", false)
                        })
                        put("servers", servers)
                    })
                }
            }

            // StreamSettings
            val streamSettings = JSONObject().apply {
                val net = if (server.networkType.isNotBlank()) server.networkType else "tcp"
                put("network", net)

                if (server.tls.equals("tls", ignoreCase = true)) {
                    put("security", "tls")
                    put("tlsSettings", JSONObject().apply {
                        put("allowInsecure", false)
                        if (server.sni.isNotBlank()) {
                            put("serverName", server.sni)
                        } else if (server.host.isNotBlank()) {
                            put("serverName", server.host)
                        }
                        if (server.alpn.isNotBlank()) {
                            val alpnList = server.alpn.split(",").map { it.trim() }
                            put("alpn", JSONArray(alpnList))
                        }
                    })
                } else if (server.tls.equals("reality", ignoreCase = true)) {
                    put("security", "reality")
                    put("realitySettings", JSONObject().apply {
                        put("serverName", if (server.sni.isNotBlank()) server.sni else server.host)
                        put("publicKey", server.publicKey)
                        put("shortId", server.shortId)
                        put("spiderX", "")
                    })
                } else {
                    put("security", "none")
                }

                when (net) {
                    "ws" -> {
                        put("wsSettings", JSONObject().apply {
                            put("path", if (server.path.isNotBlank()) server.path else "/")
                            val headers = JSONObject()
                            if (server.host.isNotBlank()) {
                                headers.put("Host", server.host)
                            }
                            put("headers", headers)
                        })
                    }
                    "grpc" -> {
                        put("grpcSettings", JSONObject().apply {
                            put("serviceName", server.path)
                            put("multiMode", false)
                        })
                    }
                    "h2", "http" -> {
                        put("httpSettings", JSONObject().apply {
                            put("path", if (server.path.isNotBlank()) server.path else "/")
                            if (server.host.isNotBlank()) {
                                put("host", JSONArray(server.host.split(",")))
                            }
                        })
                    }
                }
            }
            put("streamSettings", streamSettings)
        }
        outbounds.put(proxyOutbound)

        // Direct outbound
        val directOutbound = JSONObject().apply {
            put("tag", "direct")
            put("protocol", "freedom")
            put("settings", JSONObject())
        }
        outbounds.put(directOutbound)

        // Block outbound
        val blockOutbound = JSONObject().apply {
            put("tag", "block")
            put("protocol", "blackhole")
            put("settings", JSONObject())
        }
        outbounds.put(blockOutbound)

        root.put("outbounds", outbounds)

        // 4. Routing
        val routing = JSONObject().apply {
            put("domainStrategy", "IPIfNonMatch")
            val rules = JSONArray()

            if (settings.mode == RoutingMode.BYPASS_LAN) {
                // Bypass private LAN IPs
                val lanRule = JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "direct")
                    put("ip", JSONArray(listOf("geoip:private", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "127.0.0.0/8")))
                }
                rules.put(lanRule)
            }

            // Default proxy rule for all other traffic
            val defaultRule = JSONObject().apply {
                put("type", "field")
                put("outboundTag", "proxy")
                put("port", "0-65535")
            }
            rules.put(defaultRule)

            put("rules", rules)
        }
        root.put("routing", routing)

        // 5. DNS
        val dns = JSONObject().apply {
            val serversArray = JSONArray()
            serversArray.put(settings.dnsServer.ifBlank { "1.1.1.1" })
            serversArray.put("8.8.8.8")
            put("servers", serversArray)
        }
        root.put("dns", dns)

        return root.toString(2)
    }
}
