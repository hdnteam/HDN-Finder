package com.snifinder.app.util

import android.util.Base64
import com.snifinder.app.model.ConfigData
import com.snifinder.app.model.Protocol
import com.snifinder.app.model.Transport
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

/**
 * Parses proxy configs and supports all protocols/transports:
 * - VLESS (tcp, ws, grpc, http, reality, etc.)
 * - VMess (all transports)
 * - Trojan (all transports)
 * - Shadowsocks
 * - Hysteria2
 *
 * Supports security types: tls, reality, none
 * Supports transports: tcp, ws, grpc, http, quic, httpupgrade, splithttp
 */
object ConfigParser {

    fun parse(rawConfig: String): ConfigData? {
        val trimmed = rawConfig.trim()
        return when {
            trimmed.startsWith("vless://") -> parseVless(trimmed)
            trimmed.startsWith("vmess://") -> parseVmess(trimmed)
            trimmed.startsWith("trojan://") -> parseTrojan(trimmed)
            trimmed.startsWith("ss://") -> parseShadowsocks(trimmed)
            trimmed.startsWith("hysteria2://") || trimmed.startsWith("hy2://") -> parseHysteria2(trimmed)
            else -> null
        }
    }

    private fun parseVless(config: String): ConfigData? {
        return try {
            val uri = URI(config)
            val uuid = uri.userInfo ?: return null
            val host = uri.host ?: return null
            val port = if (uri.port > 0) uri.port else 443

            val params = parseQueryParams(uri.rawQuery ?: "")

            val security = params["security"] ?: "tls"
            val sni = params["sni"] ?: params["peer"] ?: params["serverName"] ?: host
            val transport = parseTransport(params["type"] ?: "tcp")
            val flow = params["flow"] ?: ""
            val pbk = params["pbk"] ?: ""
            val sid = params["sid"] ?: ""
            val fp = params["fp"] ?: ""
            val alpn = params["alpn"] ?: ""
            val path = params["path"] ?: params["serviceName"] ?: ""
            val hostHeader = params["host"] ?: ""

            ConfigData(
                rawConfig = config,
                protocol = Protocol.VLESS,
                server = host,
                port = port,
                uuid = uuid,
                security = security,
                transport = transport,
                originalSni = sni,
                serverName = params["serverName"] ?: sni,
                flow = flow,
                pbk = pbk,
                sid = sid,
                fp = fp,
                alpn = alpn,
                path = path,
                host = hostHeader
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseVmess(config: String): ConfigData? {
        return try {
            val encoded = config.removePrefix("vmess://")
            val decoded = String(Base64.decode(encoded, Base64.DEFAULT))
            val json = JSONObject(decoded)

            val host = json.optString("add", "")
            val port = json.optInt("port", 443)
            val uuid = json.optString("id", "")
            val sni = json.optString("sni", json.optString("host", host))
            val security = json.optString("tls", "")
            val net = json.optString("net", "tcp")
            val path = json.optString("path", "")
            val wsHost = json.optString("host", "")
            val alpn = json.optString("alpn", "")
            val fp = json.optString("fp", "")

            if (host.isEmpty()) return null

            ConfigData(
                rawConfig = config,
                protocol = Protocol.VMESS,
                server = host,
                port = port,
                uuid = uuid,
                security = if (security.isNotEmpty()) security else "tls",
                transport = parseTransport(net),
                originalSni = sni,
                serverName = sni,
                flow = "",
                pbk = "",
                sid = "",
                fp = fp,
                alpn = alpn,
                path = path,
                host = wsHost
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTrojan(config: String): ConfigData? {
        return try {
            val uri = URI(config)
            val password = uri.userInfo ?: return null
            val host = uri.host ?: return null
            val port = if (uri.port > 0) uri.port else 443

            val params = parseQueryParams(uri.rawQuery ?: "")
            val sni = params["sni"] ?: params["peer"] ?: host
            val security = params["security"] ?: "tls"
            val transport = parseTransport(params["type"] ?: "tcp")
            val path = params["path"] ?: params["serviceName"] ?: ""
            val hostHeader = params["host"] ?: ""
            val alpn = params["alpn"] ?: ""
            val fp = params["fp"] ?: ""

            ConfigData(
                rawConfig = config,
                protocol = Protocol.TROJAN,
                server = host,
                port = port,
                uuid = password,
                security = security,
                transport = transport,
                originalSni = sni,
                serverName = sni,
                flow = "",
                pbk = "",
                sid = "",
                fp = fp,
                alpn = alpn,
                path = path,
                host = hostHeader
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseShadowsocks(config: String): ConfigData? {
        return try {
            // ss://base64(method:password)@host:port#name
            // or ss://base64(method:password@host:port)#name
            val withoutScheme = config.removePrefix("ss://")
            val fragmentIdx = withoutScheme.indexOf('#')
            val mainPart = if (fragmentIdx > 0) withoutScheme.substring(0, fragmentIdx) else withoutScheme

            val atIdx = mainPart.lastIndexOf('@')
            val host: String
            val port: Int
            val password: String

            if (atIdx > 0) {
                val userInfo = String(Base64.decode(mainPart.substring(0, atIdx), Base64.DEFAULT))
                val serverPart = mainPart.substring(atIdx + 1)
                val colonIdx = serverPart.lastIndexOf(':')
                host = serverPart.substring(0, colonIdx)
                port = serverPart.substring(colonIdx + 1).toInt()
                password = userInfo
            } else {
                val decoded = String(Base64.decode(mainPart, Base64.DEFAULT))
                val atIdx2 = decoded.lastIndexOf('@')
                password = decoded.substring(0, atIdx2)
                val serverPart = decoded.substring(atIdx2 + 1)
                val colonIdx = serverPart.lastIndexOf(':')
                host = serverPart.substring(0, colonIdx)
                port = serverPart.substring(colonIdx + 1).toInt()
            }

            ConfigData(
                rawConfig = config,
                protocol = Protocol.SHADOWSOCKS,
                server = host,
                port = port,
                uuid = password,
                security = "none",
                transport = Transport.TCP,
                originalSni = host,
                serverName = host,
                flow = "", pbk = "", sid = "", fp = "", alpn = "", path = "", host = ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseHysteria2(config: String): ConfigData? {
        return try {
            val cleaned = config.replace("hy2://", "hysteria2://")
            val uri = URI(cleaned)
            val password = uri.userInfo ?: return null
            val host = uri.host ?: return null
            val port = if (uri.port > 0) uri.port else 443
            val params = parseQueryParams(uri.rawQuery ?: "")
            val sni = params["sni"] ?: host

            ConfigData(
                rawConfig = config,
                protocol = Protocol.HYSTERIA2,
                server = host,
                port = port,
                uuid = password,
                security = "tls",
                transport = Transport.QUIC,
                originalSni = sni,
                serverName = sni,
                flow = "", pbk = "", sid = "",
                fp = params["fp"] ?: "",
                alpn = params["alpn"] ?: "",
                path = "", host = ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTransport(type: String): Transport {
        return when (type.lowercase()) {
            "tcp" -> Transport.TCP
            "ws", "websocket" -> Transport.WS
            "grpc", "gun" -> Transport.GRPC
            "http", "h2" -> Transport.HTTP
            "quic" -> Transport.QUIC
            "httpupgrade" -> Transport.HTTPUPGRADE
            "splithttp", "xhttp" -> Transport.SPLITHTTP
            else -> Transport.UNKNOWN
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&").mapNotNull { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                try {
                    URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
                } catch (e: Exception) {
                    parts[0] to parts[1]
                }
            } else null
        }.toMap()
    }

    /**
     * Replace SNI in config string - supports all protocols
     */
    fun replaceSni(config: ConfigData, newSni: String): String {
        return when (config.protocol) {
            Protocol.VLESS, Protocol.TROJAN, Protocol.HYSTERIA2 -> {
                var result = config.rawConfig
                // Replace sni parameter
                result = replaceOrAddQueryParam(result, "sni", newSni)
                // Also replace serverName if present (for reality)
                if (config.security == "reality") {
                    result = replaceOrAddQueryParam(result, "serverName", newSni)
                }
                // Replace host header for ws/http transports
                if (config.transport == Transport.WS || config.transport == Transport.HTTP) {
                    result = replaceOrAddQueryParam(result, "host", newSni)
                }
                result
            }
            Protocol.VMESS -> replaceVmessSni(config.rawConfig, newSni)
            Protocol.SHADOWSOCKS -> config.rawConfig // SS doesn't use SNI
            Protocol.UNKNOWN -> config.rawConfig
        }
    }

    private fun replaceOrAddQueryParam(url: String, key: String, value: String): String {
        return try {
            val questionIdx = url.indexOf('?')
            if (questionIdx < 0) return url

            val fragmentIdx = url.indexOf('#', questionIdx)
            val queryEnd = if (fragmentIdx > 0) fragmentIdx else url.length
            val query = url.substring(questionIdx + 1, queryEnd)

            val params = query.split("&").toMutableList()
            var found = false
            val newParams = params.map { param ->
                val parts = param.split("=", limit = 2)
                if (parts[0] == key) {
                    found = true
                    "$key=$value"
                } else param
            }.toMutableList()

            if (!found) newParams.add("$key=$value")

            val before = url.substring(0, questionIdx + 1)
            val after = if (fragmentIdx > 0) url.substring(fragmentIdx) else ""
            before + newParams.joinToString("&") + after
        } catch (e: Exception) {
            url
        }
    }

    private fun replaceVmessSni(config: String, newSni: String): String {
        return try {
            val encoded = config.removePrefix("vmess://")
            val decoded = String(Base64.decode(encoded, Base64.DEFAULT))
            val json = JSONObject(decoded)
            json.put("sni", newSni)
            json.put("host", newSni)
            "vmess://" + Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            config
        }
    }
}
