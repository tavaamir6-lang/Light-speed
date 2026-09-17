package com.example.data.parser

import android.net.Uri
import android.util.Base64
import com.example.data.model.ProtocolType
import com.example.data.model.ServerConfig
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object V2RayParser {

    /**
     * Decodes a subscription content which may be base64 encoded or plain text,
     * and extracts all valid server configs.
     */
    fun parseSubscriptionContent(content: String, subscriptionId: Long? = null): List<ServerConfig> {
        val trimmed = content.trim()
        val plainText = decodeBase64Safely(trimmed) ?: trimmed

        // Ensure consecutive config URIs without newlines are properly split
        val normalized = plainText.replace(
            Regex("(?<=[^\\r\\n])(?=(?:vmess|vless|trojan|ss)://)", RegexOption.IGNORE_CASE),
            "\n"
        )

        val servers = mutableListOf<ServerConfig>()
        val lines = normalized.split("\r\n", "\n", "\r")

        for (line in lines) {
            val configLine = line.trim()
            if (configLine.isBlank() || configLine.startsWith("#")) continue
            val parsed = parseUri(configLine, subscriptionId)
            if (parsed != null) {
                servers.add(parsed)
            }
        }
        return servers
    }

    /**
     * Parses a single VPN configuration URI (vmess://, vless://, trojan://, ss://)
     */
    fun parseUri(uriString: String, subscriptionId: Long? = null): ServerConfig? {
        val trimmed = uriString.trim()
        return try {
            when {
                trimmed.startsWith("vmess://", ignoreCase = true) -> parseVMess(trimmed, subscriptionId)
                trimmed.startsWith("vless://", ignoreCase = true) -> parseVLess(trimmed, subscriptionId)
                trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed, subscriptionId)
                trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed, subscriptionId)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses vmess://<base64>
     */
    private fun parseVMess(uriString: String, subscriptionId: Long?): ServerConfig? {
        val rawBase64 = uriString.substring(8).trim()
        val jsonString = decodeBase64Safely(rawBase64) ?: return null
        val json = JSONObject(jsonString)

        val name = json.optString("ps", "VMess Server").trim().ifBlank { "VMess Server" }
        val address = json.optString("add", "").trim()
        val port = json.optInt("port", 443)
        val uuid = json.optString("id", "").trim()
        val aid = json.optInt("aid", 0)
        val scy = json.optString("scy", "auto").trim().ifBlank { "auto" }
        val net = json.optString("net", "tcp").trim().lowercase().ifBlank { "tcp" }
        val type = json.optString("type", "none").trim().ifBlank { "none" }
        val host = json.optString("host", "").trim()
        val path = json.optString("path", "").trim()
        val tls = json.optString("tls", "none").trim().lowercase().ifBlank { "none" }
        val sni = json.optString("sni", "").trim().ifBlank { host }
        val alpn = json.optString("alpn", "").trim()

        if (address.isBlank() || uuid.isBlank()) return null

        return ServerConfig(
            subscriptionId = subscriptionId,
            name = name,
            protocol = ProtocolType.VMESS,
            address = address,
            port = port,
            uuidOrPassword = uuid,
            alterId = aid,
            security = scy,
            networkType = net,
            headerType = type,
            host = host,
            path = path,
            tls = tls,
            sni = sni,
            alpn = alpn,
            rawUri = uriString
        )
    }

    /**
     * Parses vless://uuid@host:port?query#name
     */
    private fun parseVLess(uriString: String, subscriptionId: Long?): ServerConfig? {
        val uri = Uri.parse(uriString)
        val userInfo = uri.userInfo ?: ""
        val address = uri.host ?: ""
        val port = if (uri.port > 0) uri.port else 443
        val rawFragment = uri.fragment ?: "VLESS Server"
        val name = decodeUrlComponent(rawFragment).ifBlank { "VLESS Server" }

        val type = uri.getQueryParameter("type") ?: "tcp"
        val security = uri.getQueryParameter("security") ?: "none"
        val path = uri.getQueryParameter("path") ?: ""
        val host = uri.getQueryParameter("host") ?: ""
        val sni = uri.getQueryParameter("sni") ?: host
        val alpn = uri.getQueryParameter("alpn") ?: ""
        val flow = uri.getQueryParameter("flow") ?: ""
        val pbk = uri.getQueryParameter("pbk") ?: ""
        val sid = uri.getQueryParameter("sid") ?: ""
        val headerType = uri.getQueryParameter("headerType") ?: "none"

        if (userInfo.isBlank() || address.isBlank()) return null

        return ServerConfig(
            subscriptionId = subscriptionId,
            name = name,
            protocol = ProtocolType.VLESS,
            address = address,
            port = port,
            uuidOrPassword = userInfo,
            networkType = type.lowercase(),
            headerType = headerType,
            host = host,
            path = path,
            tls = security.lowercase(),
            sni = sni,
            alpn = alpn,
            flow = flow,
            publicKey = pbk,
            shortId = sid,
            rawUri = uriString
        )
    }

    /**
     * Parses trojan://password@host:port?query#name
     */
    private fun parseTrojan(uriString: String, subscriptionId: Long?): ServerConfig? {
        val uri = Uri.parse(uriString)
        val password = uri.userInfo ?: ""
        val address = uri.host ?: ""
        val port = if (uri.port > 0) uri.port else 443
        val rawFragment = uri.fragment ?: "Trojan Server"
        val name = decodeUrlComponent(rawFragment).ifBlank { "Trojan Server" }

        val type = uri.getQueryParameter("type") ?: "tcp"
        val security = uri.getQueryParameter("security") ?: "tls"
        val path = uri.getQueryParameter("path") ?: ""
        val host = uri.getQueryParameter("host") ?: ""
        val sni = uri.getQueryParameter("sni") ?: host
        val alpn = uri.getQueryParameter("alpn") ?: ""

        if (password.isBlank() || address.isBlank()) return null

        return ServerConfig(
            subscriptionId = subscriptionId,
            name = name,
            protocol = ProtocolType.TROJAN,
            address = address,
            port = port,
            uuidOrPassword = password,
            networkType = type.lowercase(),
            host = host,
            path = path,
            tls = security.lowercase(),
            sni = sni,
            alpn = alpn,
            rawUri = uriString
        )
    }

    /**
     * Parses ss://<encoded>#name or ss://method:pass@host:port#name (SIP002)
     */
    private fun parseShadowsocks(uriString: String, subscriptionId: Long?): ServerConfig? {
        val raw = uriString.substring(5)
        val hashIdx = raw.indexOf('#')
        val (body, namePart) = if (hashIdx >= 0) {
            raw.substring(0, hashIdx) to decodeUrlComponent(raw.substring(hashIdx + 1))
        } else {
            raw to "Shadowsocks Server"
        }

        val name = namePart.ifBlank { "Shadowsocks Server" }

        return if (body.contains("@")) {
            // SIP002 format: ss://base64(method:password)@hostname:port
            val atIdx = body.indexOf('@')
            val userPart = body.substring(0, atIdx)
            val hostPart = body.substring(atIdx + 1)

            val decodedUser = decodeBase64Safely(userPart) ?: userPart
            val methodAndPass = decodedUser.split(":", limit = 2)
            val method = methodAndPass.getOrElse(0) { "aes-256-gcm" }
            val password = methodAndPass.getOrElse(1) { "" }

            val hostAndPort = hostPart.split(":", limit = 2)
            val address = hostAndPort.getOrElse(0) { "" }
            val port = hostAndPort.getOrNull(1)?.toIntOrNull() ?: 8388

            if (address.isBlank()) return null

            ServerConfig(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.SHADOWSOCKS,
                address = address,
                port = port,
                uuidOrPassword = password,
                security = method,
                rawUri = uriString
            )
        } else {
            // Old format: ss://base64(method:password@hostname:port)
            val decoded = decodeBase64Safely(body) ?: return null
            val atIdx = decoded.indexOf('@')
            if (atIdx < 0) return null

            val userPart = decoded.substring(0, atIdx)
            val hostPart = decoded.substring(atIdx + 1)

            val methodAndPass = userPart.split(":", limit = 2)
            val method = methodAndPass.getOrElse(0) { "aes-256-gcm" }
            val password = methodAndPass.getOrElse(1) { "" }

            val hostAndPort = hostPart.split(":", limit = 2)
            val address = hostAndPort.getOrElse(0) { "" }
            val port = hostAndPort.getOrNull(1)?.toIntOrNull() ?: 8388

            if (address.isBlank()) return null

            ServerConfig(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.SHADOWSOCKS,
                address = address,
                port = port,
                uuidOrPassword = password,
                security = method,
                rawUri = uriString
            )
        }
    }

    private fun decodeBase64Safely(input: String): String? {
        val clean = input.trim()
            .replace("\r", "")
            .replace("\n", "")
            .replace(" ", "")

        // Pad with = if needed
        val padded = when (clean.length % 4) {
            2 -> "$clean=="
            3 -> "$clean="
            else -> clean
        }

        val flagsToTry = intArrayOf(
            Base64.DEFAULT,
            Base64.NO_WRAP,
            Base64.URL_SAFE,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        for (flag in flagsToTry) {
            try {
                val bytes = Base64.decode(padded, flag)
                if (bytes != null && bytes.isNotEmpty()) {
                    val decoded = String(bytes, StandardCharsets.UTF_8)
                    if (decoded.isNotBlank()) return decoded
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun decodeUrlComponent(str: String): String {
        return try {
            URLDecoder.decode(str, "UTF-8")
        } catch (_: Exception) {
            str
        }
    }
}
