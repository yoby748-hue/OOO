package com.example.vpn.v2ray

import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.net.URLDecoder

data class V2RayConfig(
    val protocol: String, // "vless", "vmess", "trojan", "shadowsocks"
    val server: String,
    val port: Int,
    val uuid: String, // or password or secret key
    val security: String = "none", // "tls", "reality", "none"
    val sni: String = "",
    val network: String = "tcp", // "tcp", "ws", "grpc", "h2"
    val path: String = "/",
    val host: String = "",
    val headerType: String = "none", // "none", "http", "srtp", "utp", "wechat-video", "dtls", "wireguard"
    val flow: String = "",
    val encryption: String = "none",
    val rawLink: String = "",
    val remark: String = "",
    val alpn: String = "http/1.1"
) {
    val isTls: Boolean
        get() = security.equals("tls", ignoreCase = true) || security.equals("reality", ignoreCase = true)

    val isWebSocket: Boolean
        get() = network.equals("ws", ignoreCase = true) || network.equals("websocket", ignoreCase = true)

    val isGrpc: Boolean
        get() = network.equals("grpc", ignoreCase = true)

    val isHttpObfuscated: Boolean
        get() = headerType.equals("http", ignoreCase = true)

    companion object {
        fun parse(rawLink: String): V2RayConfig? {
            val link = rawLink.trim()
            return try {
                when {
                    link.startsWith("vless://", ignoreCase = true) -> parseVless(link)
                    link.startsWith("vmess://", ignoreCase = true) -> parseVmess(link)
                    link.startsWith("trojan://", ignoreCase = true) -> parseTrojan(link)
                    link.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(link)
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }

        private fun parseVless(link: String): V2RayConfig? {
            val uri = Uri.parse(link)
            val userInfo = uri.userInfo ?: return null
            val uuid = userInfo
            val host = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 443
            val security = uri.getQueryParameter("security")?.ifBlank { "none" } ?: "none"
            val sni = uri.getQueryParameter("sni") ?: uri.getQueryParameter("peer") ?: host
            val network = uri.getQueryParameter("type") ?: uri.getQueryParameter("net") ?: "tcp"
            val rawPath = uri.getQueryParameter("path") ?: "/"
            val path = try { URLDecoder.decode(rawPath, "UTF-8") } catch (_: Exception) { rawPath }
            val headerHost = uri.getQueryParameter("host") ?: sni
            val headerType = uri.getQueryParameter("headerType") ?: uri.getQueryParameter("header_type") ?: "none"
            val flow = uri.getQueryParameter("flow") ?: ""
            val encryption = uri.getQueryParameter("encryption") ?: "none"
            val alpn = uri.getQueryParameter("alpn")?.let { try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it } } ?: "http/1.1"
            val remark = uri.fragment?.let { try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it } } ?: ""

            return V2RayConfig(
                protocol = "vless",
                server = host,
                port = port,
                uuid = uuid,
                security = security,
                sni = sni,
                network = network,
                path = if (path.startsWith("/")) path else "/$path",
                host = headerHost,
                headerType = headerType,
                flow = flow,
                encryption = encryption,
                rawLink = link,
                remark = remark,
                alpn = alpn
            )
        }

        private fun parseVmess(link: String): V2RayConfig? {
            val base64Part = link.substringAfter("vmess://").trim()
            val jsonStr = try {
                val decodedBytes = Base64.decode(base64Part, Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP)
                String(decodedBytes, Charsets.UTF_8)
            } catch (_: Exception) {
                return null
            }

            val json = JSONObject(jsonStr)
            val server = json.optString("add", "").trim()
            val portStr = json.optString("port", "").trim()
            val port = portStr.toIntOrNull() ?: json.optInt("port", 443)
            val uuid = json.optString("id", "").trim()
            if (server.isBlank() || uuid.isBlank()) return null

            val tlsVal = json.optString("tls", "").trim()
            val security = if (tlsVal.equals("tls", ignoreCase = true) || tlsVal.equals("reality", ignoreCase = true)) "tls" else "none"
            val sni = json.optString("sni", "").trim().ifBlank { server }
            val network = json.optString("net", "tcp").trim().ifBlank { "tcp" }
            val rawPath = json.optString("path", "/").trim()
            val path = if (rawPath.isBlank()) "/" else if (rawPath.startsWith("/")) rawPath else "/$rawPath"
            val host = json.optString("host", "").trim().ifBlank { sni }
            val headerType = json.optString("type", "none").trim().ifBlank { json.optString("headerType", "none").trim() }
            val remark = json.optString("ps", "").trim()
            val alpn = json.optString("alpn", "http/1.1").trim().ifBlank { "http/1.1" }
            val encryption = json.optString("scy", "none").trim().ifBlank { "none" }

            return V2RayConfig(
                protocol = "vmess",
                server = server,
                port = port,
                uuid = uuid,
                security = security,
                sni = sni,
                network = network,
                path = path,
                host = host,
                headerType = headerType,
                rawLink = link,
                remark = remark,
                alpn = alpn,
                encryption = encryption
            )
        }

        private fun parseTrojan(link: String): V2RayConfig? {
            val uri = Uri.parse(link)
            val password = uri.userInfo ?: return null
            val host = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 443
            val security = uri.getQueryParameter("security")?.ifBlank { "tls" } ?: "tls"
            val sni = uri.getQueryParameter("sni") ?: uri.getQueryParameter("peer") ?: host
            val network = uri.getQueryParameter("type") ?: "tcp"
            val rawPath = uri.getQueryParameter("path") ?: "/"
            val path = try { URLDecoder.decode(rawPath, "UTF-8") } catch (_: Exception) { rawPath }
            val headerHost = uri.getQueryParameter("host") ?: sni
            val headerType = uri.getQueryParameter("headerType") ?: uri.getQueryParameter("header_type") ?: "none"
            val remark = uri.fragment?.let { try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it } } ?: ""

            return V2RayConfig(
                protocol = "trojan",
                server = host,
                port = port,
                uuid = password,
                security = security,
                sni = sni,
                network = network,
                path = if (path.startsWith("/")) path else "/$path",
                host = headerHost,
                headerType = headerType,
                rawLink = link,
                remark = remark
            )
        }

        private fun parseShadowsocks(link: String): V2RayConfig? {
            val withoutScheme = link.substringAfter("ss://").trim()
            val fragment = if (withoutScheme.contains("#")) withoutScheme.substringAfter("#") else ""
            val mainPart = withoutScheme.substringBefore("#")

            val remark = try { URLDecoder.decode(fragment, "UTF-8") } catch (_: Exception) { fragment }

            // ss://base64(method:password@host:port) or ss://base64(method:password)@host:port
            val (userInfo, hostPort) = if (mainPart.contains("@")) {
                val user = mainPart.substringBeforeLast("@")
                val hp = mainPart.substringAfterLast("@")
                val decodedUser = try {
                    String(Base64.decode(user, Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
                } catch (_: Exception) {
                    user
                }
                Pair(decodedUser, hp)
            } else {
                val decoded = try {
                    String(Base64.decode(mainPart, Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
                } catch (_: Exception) {
                    return null
                }
                if (!decoded.contains("@")) return null
                Pair(decoded.substringBeforeLast("@"), decoded.substringAfterLast("@"))
            }

            val colonIdx = hostPort.lastIndexOf(":")
            if (colonIdx == -1) return null
            val server = hostPort.substring(0, colonIdx)
            val port = hostPort.substring(colonIdx + 1).toIntOrNull() ?: 8388

            val userColon = userInfo.indexOf(":")
            val encryption = if (userColon != -1) userInfo.substring(0, userColon) else "aes-256-gcm"
            val password = if (userColon != -1) userInfo.substring(userColon + 1) else userInfo

            return V2RayConfig(
                protocol = "shadowsocks",
                server = server,
                port = port,
                uuid = password,
                encryption = encryption,
                rawLink = link,
                remark = remark
            )
        }
    }
}
