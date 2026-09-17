package com.example.data.parser

import android.net.Uri
import android.util.Base64
import com.example.data.model.ProtocolType
import com.example.data.model.ServerConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object V2RayParser {

    /**
     * Decodes subscription content which may be plain text, base64 encoded,
     * JSON wrapped, URL encoded, or contain multiple config URIs in one line.
     */
    fun parseSubscriptionContent(content: String, subscriptionId: Long? = null): List<ServerConfig> {
        val candidates = mutableListOf<String>()
        val original = content.trim().removePrefix("\uFEFF")

        // Try the original body first, then decoded/nested forms.
        addCandidate(candidates, original)
        decodeBase64Safely(original)?.let { addCandidate(candidates, it) }
        decodeUrlComponent(original).takeIf { it != original }?.let { addCandidate(candidates, it) }

        val jsonUnwrapped = unwrapJsonSubscription(original)
        if (jsonUnwrapped != null) {
            addCandidate(candidates, jsonUnwrapped)
            decodeBase64Safely(jsonUnwrapped)?.let { addCandidate(candidates, it) }
        }

        val servers = mutableListOf<ServerConfig>()
        val seen = mutableSetOf<String>()

        for (candidate in candidates) {
            // Some providers return several URIs without line breaks, or wrap
            // them in text/HTML. Extract supported URI schemes anywhere in body.
            val extracted = extractConfigUris(candidate)
            for (uri in extracted) {
                if (!seen.add(uri)) continue
                parseUri(uri, subscriptionId)?.let { servers.add(it) }
            }

            // Also support ordinary line-separated content.
            val normalized = candidate
                .replace("\r", "\n")
                .replace("\\n", "\n")
                .replace("(?i)<br\\s*/?>".toRegex(), "\n")

            normalized.split('\n').forEach { line ->
                val configLine = line.trim().trim('"', '\'', ',', '[', ']')
                if (configLine.isBlank() || configLine.startsWith("#")) return@forEach
                parseUri(configLine, subscriptionId)?.let {
                    if (seen.add(it.rawUri)) servers.add(it)
                }
            }
        }

        return servers
    }

    /**
     * Parses a single VPN configuration URI (vmess://, vless://, trojan://, ss://)
     */
    fun parseUri(uriString: String, subscriptionId: Long? = null): ServerConfig? {
        val trimmed = uriString.trim().trim('"', '\'', ',', '[', ']')
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

    /** Extract supported config URIs even when the provider puts them in JSON/HTML/text. */
    private fun extractConfigUris(input: String): List<String> {
        val schemes = "vmess|vless|trojan|ss"
        val regex = Regex("(?i)(?:$schemes)://[^\\s\\\"'<>\\]++")
        return regex.findAll(input)
            .map { it.value.trimEnd(',', ';', ')', ']', '}') }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun unwrapJsonSubscription(input: String): String? {
        val trimmed = input.trim()
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return null
        return try {
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                buildString {
                    for (i in 0 until array.length()) {
                        val item = array.opt(i)
                        if (item is String) append(item).append('\n')
                        else if (item is JSONObject) {
                            append(item.optString("url", ""))
                                .append('\n')
                        }
                    }
                }
            } else {
                val obj = JSONObject(trimmed)
                listOf("content", "data", "subscription", "links", "url")
                    .firstNotNullOfOrNull { key ->
                        obj.optString(key, "").takeIf { it.isNotBlank() }
                    }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun addCandidate(list: MutableList<String>, value: String) {
        val clean = value.trim().removePrefix("\uFEFF")
        if (clean.isNotBlank() && clean !in list) list.add(clean)
    }

    private fun decodeBase64Safely(input: String): String? {
        val clean = input.trim()
            .removePrefix("\uFEFF")
            .replace("\r", "")
            .replace("\n", "")
            .replace(" ", "")

        if (clean.length < 8) return null

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
                if (bytes.isNotEmpty()) {
                    val decoded = String(bytes, StandardCharsets.UTF_8)
                    // Avoid treating arbitrary binary/base64 as a valid subscription.
                    if (decoded.isNotBlank() && decoded.any { it == ':' || it == '/' || it == '{' }) {
                        return decoded
                    }
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
