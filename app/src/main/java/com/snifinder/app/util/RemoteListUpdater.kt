package com.snifinder.app.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles auto-updating SNI list and Cloudflare IP ranges from remote sources.
 * - Checks for updates every 24 hours
 * - Falls back to built-in lists if network unavailable
 * - Stores updated lists in SharedPreferences
 */
object RemoteListUpdater {

    private const val PREFS_NAME = "hdn_finder_lists"
    private const val KEY_SNI_LIST = "sni_list"
    private const val KEY_CF_RANGES = "cf_ranges"
    private const val KEY_LAST_UPDATE_SNI = "last_update_sni"
    private const val KEY_LAST_UPDATE_CF = "last_update_cf"
    private const val UPDATE_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours

    // Remote URLs for lists - hosted on GitHub (easy to update)
    // You can change these to your own hosted files
    private const val SNI_LIST_URL = "https://raw.githubusercontent.com/nicercode/nice-lists/main/sni-list.txt"
    private const val SNI_LIST_URL_FALLBACK = "https://raw.githubusercontent.com/nicercode/nice-lists/main/sni.txt"
    private const val CF_RANGES_URL = "https://www.cloudflare.com/ips-v4"

    // Alternative community SNI lists
    private val SNI_REMOTE_URLS = listOf(
        "https://raw.githubusercontent.com/nicercode/nice-lists/main/sni-list.txt",
        "https://raw.githubusercontent.com/mahdibland/V2RayAggregator/master/sub/sni.txt"
    )

    // V2RayNG GitHub for protocol updates
    private const val V2RAYNG_RELEASES_URL = "https://api.github.com/repos/2dust/v2rayNG/releases/latest"
    private const val XRAY_RELEASES_URL = "https://api.github.com/repos/XTLS/Xray-core/releases/latest"

    private const val KEY_PROTOCOLS = "protocols_list"
    private const val KEY_LAST_UPDATE_PROTO = "last_update_proto"
    private const val KEY_XRAY_VERSION = "xray_latest_version"
    private const val KEY_V2RAYNG_VERSION = "v2rayng_latest_version"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get SNI list - tries remote first (if stale), falls back to cached, then built-in
     */
    suspend fun getSniList(context: Context): List<String> {
        val prefs = getPrefs(context)
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE_SNI, 0)
        val now = System.currentTimeMillis()

        // If recently updated, use cached
        if (now - lastUpdate < UPDATE_INTERVAL_MS) {
            val cached = prefs.getString(KEY_SNI_LIST, null)
            if (!cached.isNullOrEmpty()) {
                val list = cached.split("\n").filter { it.isNotBlank() && it.contains(".") }
                if (list.isNotEmpty()) return list
            }
        }

        // Try to update from remote
        val remoteList = fetchRemoteSniList()
        if (remoteList.isNotEmpty()) {
            // Save to cache
            prefs.edit()
                .putString(KEY_SNI_LIST, remoteList.joinToString("\n"))
                .putLong(KEY_LAST_UPDATE_SNI, now)
                .apply()
            return remoteList
        }

        // Fallback to cached
        val cached = prefs.getString(KEY_SNI_LIST, null)
        if (!cached.isNullOrEmpty()) {
            val list = cached.split("\n").filter { it.isNotBlank() }
            if (list.isNotEmpty()) return list
        }

        // Final fallback: built-in list
        return SniListProvider.defaultSniList
    }

    /**
     * Get Cloudflare IP ranges - fetches from official CF page
     */
    suspend fun getCloudflareRanges(context: Context): List<String> {
        val prefs = getPrefs(context)
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE_CF, 0)
        val now = System.currentTimeMillis()

        // If recently updated, use cached
        if (now - lastUpdate < UPDATE_INTERVAL_MS) {
            val cached = prefs.getString(KEY_CF_RANGES, null)
            if (!cached.isNullOrEmpty()) {
                val list = cached.split("\n").filter { it.isNotBlank() }
                if (list.isNotEmpty()) return list
            }
        }

        // Try to fetch from Cloudflare official
        val remoteRanges = fetchCloudflareCidrs()
        if (remoteRanges.isNotEmpty()) {
            prefs.edit()
                .putString(KEY_CF_RANGES, remoteRanges.joinToString("\n"))
                .putLong(KEY_LAST_UPDATE_CF, now)
                .apply()
            return remoteRanges
        }

        // Fallback to cached
        val cached = prefs.getString(KEY_CF_RANGES, null)
        if (!cached.isNullOrEmpty()) {
            return cached.split("\n").filter { it.isNotBlank() }
        }

        // Final fallback: built-in
        return CloudflareIpProvider.allCidrRanges.map { cidr ->
            "${cidr.base1}.${cidr.base2}.${cidr.base3}.${cidr.base4}/${cidr.mask}"
        }
    }

    /**
     * Force update all lists from remote
     */
    suspend fun forceUpdate(context: Context): UpdateResult {
        val sniResult = fetchRemoteSniList()
        val cfResult = fetchCloudflareCidrs()
        val protoResult = fetchProtocolUpdates()
        val prefs = getPrefs(context)
        val now = System.currentTimeMillis()

        var sniUpdated = false
        var cfUpdated = false
        var protoUpdated = false

        if (sniResult.isNotEmpty()) {
            prefs.edit()
                .putString(KEY_SNI_LIST, sniResult.joinToString("\n"))
                .putLong(KEY_LAST_UPDATE_SNI, now)
                .apply()
            sniUpdated = true
        }

        if (cfResult.isNotEmpty()) {
            prefs.edit()
                .putString(KEY_CF_RANGES, cfResult.joinToString("\n"))
                .putLong(KEY_LAST_UPDATE_CF, now)
                .apply()
            cfUpdated = true
        }

        if (protoResult.protocols.isNotEmpty()) {
            prefs.edit()
                .putString(KEY_PROTOCOLS, protoResult.protocols.joinToString("\n"))
                .putString(KEY_XRAY_VERSION, protoResult.xrayVersion)
                .putString(KEY_V2RAYNG_VERSION, protoResult.v2rayNgVersion)
                .putString("net_test_protocols", protoResult.netProtocols.joinToString("\n"))
                .putLong(KEY_LAST_UPDATE_PROTO, now)
                .apply()
            protoUpdated = true
        }

        return UpdateResult(
            sniUpdated = sniUpdated,
            cfUpdated = cfUpdated,
            protoUpdated = protoUpdated,
            sniCount = sniResult.size,
            cfCount = cfResult.size,
            xrayVersion = protoResult.xrayVersion,
            v2rayNgVersion = protoResult.v2rayNgVersion
        )
    }

    /**
     * Get protocol info
     */
    fun getProtocolInfo(context: Context): ProtocolInfo {
        val prefs = getPrefs(context)
        return ProtocolInfo(
            protocols = prefs.getString(KEY_PROTOCOLS, null)?.split("\n")?.filter { it.isNotBlank() } ?: defaultProtocols,
            xrayVersion = prefs.getString(KEY_XRAY_VERSION, "unknown") ?: "unknown",
            v2rayNgVersion = prefs.getString(KEY_V2RAYNG_VERSION, "unknown") ?: "unknown",
            lastUpdate = prefs.getLong(KEY_LAST_UPDATE_PROTO, 0)
        )
    }

    data class ProtocolInfo(
        val protocols: List<String>,
        val xrayVersion: String,
        val v2rayNgVersion: String,
        val lastUpdate: Long
    )

    // Default supported protocols (built-in)
    val defaultProtocols = listOf(
        "VLESS + TCP + Reality",
        "VLESS + gRPC + Reality",
        "VLESS + WebSocket + TLS",
        "VLESS + gRPC + TLS",
        "VLESS + XHTTP + TLS",
        "VLESS + HTTPUpgrade + TLS",
        "VLESS + SplitHTTP + TLS",
        "VLESS + TCP + TLS",
        "VMess + WebSocket + TLS",
        "VMess + gRPC + TLS",
        "VMess + TCP + TLS",
        "VMess + HTTPUpgrade + TLS",
        "Trojan + TCP + TLS",
        "Trojan + WebSocket + TLS",
        "Trojan + gRPC + TLS",
        "Shadowsocks + TCP",
        "Shadowsocks + WebSocket + TLS",
        "Hysteria2 + QUIC + TLS",
        "TUIC + QUIC + TLS",
        "WireGuard + UDP",
        "OpenVPN + TCP",
        "OpenVPN + UDP"
    )

    // Network test configurations - ports and protocols to test
    val defaultNetTestPorts = listOf(
        443, 80, 8443, 2053, 2083, 2087, 2096, 8080, 8880,
        2052, 2082, 2086, 2095, 4443, 6443, 8843,
        1194, 51820, 4500, 993, 465, 587, 22, 1723
    )

    val defaultNetTestProtocols = listOf(
        "TLS 1.3",
        "TLS 1.2",
        "TCP + TLS",
        "WebSocket + TLS (WSS)",
        "WebSocket (WS - no TLS)",
        "HTTP/2 (gRPC)",
        "Reality (TLS 1.3 + h2)",
        "XHTTP / SplitHTTP",
        "QUIC/UDP",
        "HTTPUpgrade",
        "Plain HTTP",
        "PPTP",
        "L2TP/IPSec",
        "OpenVPN",
        "Cisco AnyConnect",
        "WireGuard"
    )

    /**
     * Get network test config (protocols + ports to test)
     * Updated when Xray adds new transports
     */
    fun getNetTestConfig(context: Context): NetTestConfig {
        val prefs = getPrefs(context)
        val protocols = prefs.getString("net_test_protocols", null)
            ?.split("\n")?.filter { it.isNotBlank() }
            ?: defaultNetTestProtocols
        val ports = prefs.getString("net_test_ports", null)
            ?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
            ?: defaultNetTestPorts
        return NetTestConfig(protocols, ports, prefs.getLong(KEY_LAST_UPDATE_PROTO, 0))
    }

    data class NetTestConfig(
        val protocols: List<String>,
        val ports: List<Int>,
        val lastUpdate: Long
    )

    /**
     * Get last update timestamps
     */
    fun getLastUpdateInfo(context: Context): Pair<Long, Long> {
        val prefs = getPrefs(context)
        return prefs.getLong(KEY_LAST_UPDATE_SNI, 0) to prefs.getLong(KEY_LAST_UPDATE_CF, 0)
    }

    // === Private fetch methods ===

    private suspend fun fetchRemoteSniList(): List<String> = withContext(Dispatchers.IO) {
        for (remoteUrl in SNI_REMOTE_URLS) {
            try {
                val url = URL(remoteUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.useCaches = false

                if (conn.responseCode == 200) {
                    val content = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val list = content.split("\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it.contains(".") && !it.startsWith("#") }
                        .distinct()
                    if (list.size > 50) return@withContext list
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Try next URL
            }
        }
        emptyList()
    }

    private suspend fun fetchCloudflareCidrs(): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(CF_RANGES_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.useCaches = false

            if (conn.responseCode == 200) {
                val content = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val ranges = content.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it.contains("/") }
                if (ranges.isNotEmpty()) return@withContext ranges
            }
            conn.disconnect()
        } catch (e: Exception) {
            // Fallback
        }
        emptyList()
    }

    data class ProtoFetchResult(
        val protocols: List<String>,
        val xrayVersion: String,
        val v2rayNgVersion: String,
        val netProtocols: List<String>
    )

    /**
     * Fetches latest protocol support info from V2RayNG and Xray-core GitHub releases
     */
    private suspend fun fetchProtocolUpdates(): ProtoFetchResult = withContext(Dispatchers.IO) {
        var xrayVersion = ""
        var v2rayNgVersion = ""
        val protocols = mutableListOf<String>()
        val netProtocols = mutableListOf<String>()

        // Get Xray-core latest release info
        try {
            val url = URL(XRAY_RELEASES_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Accept", "application/json")
            conn.useCaches = false

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = org.json.JSONObject(response)
                xrayVersion = json.optString("tag_name", "")
                val body = json.optString("body", "")

                // Parse supported features from release notes
                val features = parseXrayFeatures(body)
                protocols.addAll(features)

                // Extract network test protocols from features
                netProtocols.addAll(extractNetTestProtocols(body))
            } else {
                conn.disconnect()
            }
        } catch (e: Exception) { }

        // Get V2RayNG latest version
        try {
            val url = URL(V2RAYNG_RELEASES_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Accept", "application/json")
            conn.useCaches = false

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = org.json.JSONObject(response)
                v2rayNgVersion = json.optString("tag_name", "")
            } else {
                conn.disconnect()
            }
        } catch (e: Exception) { }

        if (protocols.isEmpty()) protocols.addAll(defaultProtocols)
        if (netProtocols.isEmpty()) netProtocols.addAll(defaultNetTestProtocols)

        ProtoFetchResult(protocols, xrayVersion, v2rayNgVersion, netProtocols)
    }

    /**
     * Extract testable network protocols from Xray release notes
     */
    private fun extractNetTestProtocols(body: String): List<String> {
        val result = mutableListOf<String>()
        val lowerBody = body.lowercase()

        // Always include base tests
        result.addAll(defaultNetTestProtocols)

        // Check for new transports mentioned in release
        val newTransports = listOf(
            "meek" to "Meek (Domain Fronting)",
            "browserdialer" to "BrowserDialer",
            "mux" to "Mux/Multiplexing",
            "fragment" to "TLS Fragment",
            "muxcool" to "MuxCool",
            "xtls" to "XTLS Vision",
            "splice" to "Splice",
            "seedtls" to "SeedTLS"
        )

        for ((keyword, name) in newTransports) {
            if (lowerBody.contains(keyword) && name !in result) {
                result.add(name)
            }
        }

        return result.distinct()
    }

    /**
     * Parse Xray release notes to extract supported protocols/transports
     */
    private fun parseXrayFeatures(body: String): List<String> {
        val protocols = mutableListOf<String>()
        val lowerBody = body.lowercase()

        // Check for mentioned protocols and transports
        val knownProtocols = listOf("vless", "vmess", "trojan", "shadowsocks", "hysteria2", "tuic", "wireguard")
        val knownTransports = listOf("tcp", "websocket", "ws", "grpc", "http", "quic", "httpupgrade", "splithttp", "xhttp", "reality")

        val foundProtocols = knownProtocols.filter { lowerBody.contains(it) }
        val foundTransports = knownTransports.filter { lowerBody.contains(it) }

        // Generate combinations
        for (proto in foundProtocols) {
            for (transport in foundTransports) {
                val protoName = proto.replaceFirstChar { it.uppercase() }
                val transportName = when (transport) {
                    "ws", "websocket" -> "WebSocket"
                    "grpc" -> "gRPC"
                    "tcp" -> "TCP"
                    "http" -> "HTTP"
                    "quic" -> "QUIC"
                    "httpupgrade" -> "HTTPUpgrade"
                    "splithttp", "xhttp" -> "XHTTP"
                    "reality" -> "Reality"
                    else -> transport
                }
                val security = if (transport == "reality") "Reality" else "TLS"
                protocols.add("$protoName + $transportName + $security")
            }
        }

        return protocols.distinct().ifEmpty { defaultProtocols }
    }

    data class UpdateResult(
        val sniUpdated: Boolean,
        val cfUpdated: Boolean,
        val protoUpdated: Boolean,
        val sniCount: Int,
        val cfCount: Int,
        val xrayVersion: String,
        val v2rayNgVersion: String
    )
}
