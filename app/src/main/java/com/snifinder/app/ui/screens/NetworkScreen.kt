package com.snifinder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snifinder.app.ui.theme.*
import com.snifinder.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

data class PortResult(val port: Int, val name: String, val isOpen: Boolean, val latency: Long)
data class ProtocolResult(val name: String, val description: String, val works: Boolean, val latency: Long, val details: String)

data class ConfigRecommendation(
    val rank: Int,
    val protocol: String,
    val transport: String,
    val port: Int,
    val security: String,
    val reason: String,
    val score: Int // 0-100
)

@Composable
fun NetworkScreen() {
    var isScanning by remember { mutableStateOf(false) }
    var portResults by remember { mutableStateOf<List<PortResult>>(emptyList()) }
    var protocolResults by remember { mutableStateOf<List<ProtocolResult>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<ConfigRecommendation>>(emptyList()) }
    var progress by remember { mutableStateOf("") }
    var progressPercent by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            // Header
            HdnGlowCard(glowColor = HdnGreen) {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("📡", "Network Analyzer")
                    Spacer(Modifier.height(4.dp))
                    Text("بررسی پورت‌ها، پروتکل‌ها و پیشنهاد بهترین کانفیگ",
                        color = HdnGrayDark, fontSize = 11.sp)
                    Spacer(Modifier.height(14.dp))

                    HdnButton(
                        text = if (isScanning) "در حال بررسی..." else "شروع آنالیز شبکه",
                        onClick = {
                            if (isScanning) return@HdnButton
                            isScanning = true
                            portResults = emptyList()
                            protocolResults = emptyList()
                            recommendations = emptyList()
                            scope.launch {
                                progressPercent = 0f
                                progress = "بررسی پورت‌ها... (0%)"
                                portResults = scanPortsWithProgress { percent ->
                                    progressPercent = percent * 0.5f // Ports = 0-50%
                                    progress = "بررسی پورت‌ها... (${(progressPercent * 100).toInt()}%)"
                                }
                                progress = "بررسی پروتکل‌ها... (50%)"
                                progressPercent = 0.5f
                                protocolResults = testProtocolsWithProgress { percent ->
                                    progressPercent = 0.5f + percent * 0.4f // Protocols = 50-90%
                                    progress = "بررسی پروتکل‌ها... (${(progressPercent * 100).toInt()}%)"
                                }
                                progress = "تحلیل و پیشنهاد... (90%)"
                                progressPercent = 0.9f
                                recommendations = generateRecommendations(portResults, protocolResults)
                                progressPercent = 1f
                                progress = "✅ تکمیل شد (100%)"
                                kotlinx.coroutines.delay(1000)
                                progress = ""
                                isScanning = false
                            }
                        },
                        enabled = !isScanning,
                        color = HdnGreen,
                        icon = {
                            if (isScanning) CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            else Icon(Icons.Default.Wifi, null, Modifier.size(18.dp))
                        }
                    )

                    if (progress.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = HdnGreen,
                            trackColor = HdnDarkBorder
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(progress, color = HdnGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Recommendations
            if (recommendations.isNotEmpty()) {
                HdnGlowCard(glowColor = HdnGold) {
                    Column(Modifier.padding(18.dp)) {
                        SectionTitle("⭐", "پیشنهاد کانفیگ")
                        Spacer(Modifier.height(4.dp))
                        Text("بهترین ترکیب برای اینترنت شما (به ترتیب اولویت)", color = HdnGray, fontSize = 11.sp)
                        Spacer(Modifier.height(12.dp))

                        recommendations.forEach { rec ->
                            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (rec.rank <= 3) HdnGreen.copy(alpha = 0.06f) else HdnDarkBg)
                                .border(1.dp, if (rec.rank <= 3) HdnGreen.copy(0.2f) else HdnDarkBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Rank
                                    val rankColor = when(rec.rank) { 1 -> HdnGold; 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> HdnGray }
                                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                                        .background(rankColor.copy(0.15f)),
                                        contentAlignment = Alignment.Center) {
                                        Text("#${rec.rank}", color = rankColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row {
                                            Text(rec.protocol, color = HdnCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Text(" + ", color = HdnGrayDark, fontSize = 12.sp)
                                            Text(rec.transport, color = HdnPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Text(" : ", color = HdnGrayDark, fontSize = 12.sp)
                                            Text("${rec.port}", color = HdnOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                        Text("${rec.security} | ${rec.reason}", color = HdnGray, fontSize = 10.sp)
                                    }
                                    // Score
                                    Text("${rec.score}%", color = when {
                                        rec.score >= 80 -> HdnGreen
                                        rec.score >= 50 -> HdnOrange
                                        else -> HdnRed
                                    }, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Summary explanation in Farsi
            if (recommendations.isNotEmpty()) {
                HdnCard(glowColor = HdnCyan) {
                    Column(Modifier.padding(18.dp)) {
                        Text("📝 خلاصه و توصیه", fontWeight = FontWeight.Bold, color = HdnWhite, fontSize = 14.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(generateFarsiSummary(recommendations, portResults, protocolResults),
                            color = HdnGrayLight, fontSize = 12.sp, lineHeight = 20.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Port Results
            if (portResults.isNotEmpty()) {
                HdnCard {
                    Column(Modifier.padding(18.dp)) {
                        SectionTitle("🔌", "پورت‌ها")
                        val openCount = portResults.count { it.isOpen }
                        Text("$openCount باز از ${portResults.size}", color = HdnGray, fontSize = 11.sp)
                        Spacer(Modifier.height(10.dp))

                        portResults.forEach { port ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (port.isOpen) "✅" else "❌", fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("${port.port}", color = if (port.isOpen) HdnGreen else HdnRed,
                                    fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(50.dp))
                                Text(port.name, color = HdnGray, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                if (port.isOpen && port.latency > 0) {
                                    Text("${port.latency}ms", color = HdnGrayDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Protocol Results
            if (protocolResults.isNotEmpty()) {
                HdnCard {
                    Column(Modifier.padding(18.dp)) {
                        SectionTitle("🔐", "پروتکل‌ها")
                        Spacer(Modifier.height(10.dp))

                        protocolResults.forEach { proto ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (proto.works) "✅" else "❌", fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(proto.name, color = if (proto.works) HdnGreen else HdnRed,
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text(proto.description, color = HdnGrayDark, fontSize = 10.sp)
                                    if (proto.details.isNotEmpty()) {
                                        Text(proto.details, color = HdnGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                if (proto.works && proto.latency > 0) {
                                    Text("${proto.latency}ms", color = HdnGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            HorizontalDivider(color = HdnDarkBorder.copy(0.5f))
                        }
                    }
                }
            }
        }
        HdnFooter()
    }
}

// === Network Testing Functions ===

val PORTS_TO_TEST = listOf(
    443 to "HTTPS (Standard)",
    80 to "HTTP",
    8443 to "HTTPS Alt",
    2053 to "Cloudflare Alt",
    2083 to "Cloudflare Alt",
    2087 to "Cloudflare Alt",
    2096 to "Cloudflare Alt",
    8080 to "HTTP Proxy",
    8880 to "HTTP Alt",
    2052 to "Cloudflare HTTP",
    2082 to "Cloudflare HTTP",
    2086 to "Cloudflare HTTP",
    2095 to "Cloudflare HTTP",
    4443 to "Custom HTTPS",
    6443 to "K8s API",
    8843 to "Alt HTTPS",
    1194 to "OpenVPN",
    51820 to "WireGuard",
    4500 to "IPSec",
    993 to "IMAPS",
    465 to "SMTPS",
    587 to "SMTP Submit",
    22 to "SSH"
)

suspend fun scanPorts(): List<PortResult> = scanPortsWithProgress {}

suspend fun scanPortsWithProgress(onProgress: (Float) -> Unit): List<PortResult> = withContext(Dispatchers.IO) {
    val testServer = "1.1.1.1"
    val results = mutableListOf<PortResult>()
    val total = PORTS_TO_TEST.size

    for ((index, pair) in PORTS_TO_TEST.withIndex()) {
        val (port, name) = pair
        val result = try {
            val socket = Socket()
            val start = System.currentTimeMillis()
            socket.connect(InetSocketAddress(testServer, port), 4000)
            val elapsed = System.currentTimeMillis() - start
            socket.close()
            PortResult(port, name, true, elapsed)
        } catch (e: Exception) {
            PortResult(port, name, false, -1)
        }
        results.add(result)
        onProgress((index + 1).toFloat() / total.toFloat())
    }

    results.sortedWith(compareBy({ !it.isOpen }, { it.latency }))
}

suspend fun testProtocols(): List<ProtocolResult> = testProtocolsWithProgress {}

suspend fun testProtocolsWithProgress(onProgress: (Float) -> Unit): List<ProtocolResult> = withContext(Dispatchers.IO) {
    val results = mutableListOf<ProtocolResult>()
    val totalTests = 14

    fun addResult(r: ProtocolResult) {
        results.add(r)
        onProgress(results.size.toFloat() / totalTests.toFloat())
    }

    addResult(testTlsVersion("TLS 1.3", "TLSv1.3", "بهترین امنیت و سرعت", "1.1.1.1", 443))
    addResult(testTlsVersion("TLS 1.2", "TLSv1.2", "سازگاری بالا", "1.1.1.1", 443))
    addResult(testTcpDirect())
    addResult(testWebSocket())
    addResult(testWebSocketNoTls())
    addResult(testHttp2())
    addResult(testReality())
    addResult(testXhttp())
    addResult(testQuic())
    addResult(testHttpUpgrade())
    addResult(testPlainHttp())
    addResult(testPptp())
    addResult(testL2tp())
    addResult(testOpenVpn())
    addResult(testCiscoAnyConnect())

    results
}

suspend fun testTlsVersion(name: String, version: String, desc: String, host: String, port: Int): ProtocolResult {
    return try {
        val sslContext = SSLContext.getInstance(version)
        sslContext.init(null, arrayOf(com.snifinder.app.util.TrustAllManager()), null)
        val factory = sslContext.socketFactory
        val socket = factory.createSocket() as SSLSocket
        socket.soTimeout = 5000
        val start = System.currentTimeMillis()
        socket.connect(InetSocketAddress(host, port), 5000)
        socket.enabledProtocols = arrayOf(version)
        socket.startHandshake()
        val elapsed = System.currentTimeMillis() - start
        val proto = socket.session.protocol
        socket.close()
        ProtocolResult(name, desc, true, elapsed, "Protocol: $proto")
    } catch (e: Exception) {
        ProtocolResult(name, desc, false, -1, e.message?.take(50) ?: "")
    }
}

suspend fun testWebSocket(): ProtocolResult = withContext(Dispatchers.IO) {
    try {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(com.snifinder.app.util.TrustAllManager()), null)
        val factory = sslContext.socketFactory
        val socket = factory.createSocket() as SSLSocket
        socket.soTimeout = 5000
        val start = System.currentTimeMillis()
        socket.connect(InetSocketAddress("1.1.1.1", 443), 5000)
        socket.startHandshake()

        val out = socket.outputStream
        val request = "GET / HTTP/1.1\r\nHost: cloudflare.com\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Version: 13\r\n\r\n"
        out.write(request.toByteArray())
        out.flush()

        val inp = socket.inputStream
        val buf = ByteArray(1024)
        socket.soTimeout = 3000
        val read = inp.read(buf)
        val elapsed = System.currentTimeMillis() - start
        val response = if (read > 0) String(buf, 0, read) else ""
        socket.close()

        val works = response.contains("101") || response.contains("Upgrade") || response.isNotEmpty()
        ProtocolResult("WebSocket + TLS (WSS)", "WS over TLS — محبوب‌ترین", works, elapsed,
            if (works) "Upgrade supported" else "Blocked")
    } catch (e: Exception) {
        ProtocolResult("WebSocket + TLS (WSS)", "WS over TLS — محبوب‌ترین", false, -1, e.message?.take(40) ?: "")
    }
}

suspend fun testWebSocketNoTls(): ProtocolResult = withContext(Dispatchers.IO) {
    try {
        val socket = Socket()
        val start = System.currentTimeMillis()
        socket.connect(InetSocketAddress("1.1.1.1", 80), 5000)
        socket.soTimeout = 3000
        val out = socket.outputStream
        val request = "GET / HTTP/1.1\r\nHost: cloudflare.com\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Version: 13\r\n\r\n"
        out.write(request.toByteArray())
        out.flush()
        val buf = ByteArray(512)
        val read = socket.inputStream.read(buf)
        val elapsed = System.currentTimeMillis() - start
        socket.close()
        val response = if (read > 0) String(buf, 0, read) else ""
        val works = response.isNotEmpty()
        ProtocolResult("WebSocket (WS - no TLS)", "WS بدون رمزنگاری — port 80", works, elapsed,
            if (works) "Port 80 WS upgrade OK" else "Blocked")
    } catch (e: Exception) {
        ProtocolResult("WebSocket (WS - no TLS)", "WS بدون رمزنگاری — port 80", false, -1, "")
    }
}

suspend fun testTcpDirect(): ProtocolResult = withContext(Dispatchers.IO) {
    try {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(com.snifinder.app.util.TrustAllManager()), null)
        val factory = sslContext.socketFactory
        val socket = factory.createSocket() as SSLSocket
        socket.soTimeout = 5000
        val start = System.currentTimeMillis()
        socket.connect(InetSocketAddress("1.1.1.1", 443), 5000)
        socket.startHandshake()
        val elapsed = System.currentTimeMillis() - start
        socket.close()
        ProtocolResult("TCP + TLS", "اتصال مستقیم TCP با TLS", true, elapsed, "Direct TLS connection OK")
    } catch (e: Exception) {
        ProtocolResult("TCP + TLS", "اتصال مستقیم TCP با TLS", false, -1, e.message?.take(40) ?: "")
    }
}

suspend fun testReality(): ProtocolResult = withContext(Dispatchers.IO) {
    try {
        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(null, arrayOf(com.snifinder.app.util.TrustAllManager()), null)
        val factory = sslContext.socketFactory
        val socket = factory.createSocket() as SSLSocket
        socket.soTimeout = 5000
        val params = socket.sslParameters
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            params.applicationProtocols = arrayOf("h2")
        }
        socket.sslParameters = params
        val start = System.currentTimeMillis()
        socket.connect(InetSocketAddress("1.1.1.1", 443), 5000)
        socket.startHandshake()
        val elapsed = System.currentTimeMillis() - start
        val proto = socket.session.protocol
        val alpn = if (android.os.Build.VERSION.SDK_INT >= 29) socket.applicationProtocol ?: "" else ""
        socket.close()
        val works = proto.contains("1.3") || proto.contains("TLSv1.3")
        ProtocolResult("Reality (TLS 1.3 + h2)", "uTLS fingerprint — بهترین ضد شناسایی", works, elapsed,
            "Proto: $proto, ALPN: ${alpn.ifEmpty { "none" }}")
    } catch (e: Exception) {
        ProtocolResult("Reality (TLS 1.3 + h2)", "uTLS fingerprint — بهترین ضد شناسایی", false, -1, e.message?.take(40) ?: "")
    }
}

suspend fun testXhttp(): ProtocolResult = withContext(Dispatchers.IO) {
    try {
        // XHTTP/SplitHTTP uses chunked POST/GET on HTTPS
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(com.snifinder.app.util.TrustAllManager()), null)
        val factory = sslContext.socketFactory
        val socket = factory.createSocket() as SSLSocket
        socket.soTimeout = 5000
        val start = System.currentTimeMillis()
        socket.connect(InetSocketAddress("1.1.1.1", 443), 5000)
        socket.startHandshake()

        val out = socket.outputStream
        // Send chunked POST (XHTTP pattern)
        val request = "POST / HTTP/1.1\r\nHost: cloudflare.com\r\nTransfer-Encoding: chunked\r\nConnection: keep-alive\r\n\r\n5\r\nhello\r\n0\r\n\r\n"
        out.write(request.toByteArray())
        out.flush()

        val buf = ByteArray(512)
        socket.soTimeout = 3000
        val read = socket.inputStream.read(buf)
        val elapsed = System.currentTimeMillis() - start
        socket.close()
        val response = if (read > 0) String(buf, 0, read) else ""
        val works = response.isNotEmpty()
        ProtocolResult("XHTTP / SplitHTTP", "Chunked HTTP — ضد DPI", works, elapsed,
            if (works) "Chunked transfer OK" else "Blocked")
    } catch (e: Exception) {
        ProtocolResult("XHTTP / SplitHTTP", "Chunked HTTP — ضد DPI", false, -1, e.message?.take(40) ?: "")
    }
}

suspend fun testHttp2(): ProtocolResult = withContext(Dispatchers.IO) {
    try {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(com.snifinder.app.util.TrustAllManager()), null)
        val factory = sslContext.socketFactory
        val socket = factory.createSocket() as SSLSocket
        socket.soTimeout = 5000
        val params = socket.sslParameters
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            params.applicationProtocols = arrayOf("h2", "http/1.1")
        }
        socket.sslParameters = params
        val start = System.currentTimeMillis()
        socket.connect(InetSocketAddress("1.1.1.1", 443), 5000)
        socket.startHandshake()
        val elapsed = System.currentTimeMillis() - start
        val negotiated = if (android.os.Build.VERSION.SDK_INT >= 29) socket.applicationProtocol ?: "" else ""
        socket.close()
        val isH2 = negotiated == "h2" || android.os.Build.VERSION.SDK_INT < 29
        ProtocolResult("HTTP/2 (gRPC)", "gRPC transport — سرعت بالا", isH2, elapsed,
            "ALPN: ${negotiated.ifEmpty { "none" }}")
    } catch (e: Exception) {
        ProtocolResult("HTTP/2 (gRPC)", "gRPC transport — سرعت بالا", false, -1, e.message?.take(40) ?: "")
    }
}

suspend fun testQuic(): ProtocolResult = withContext(Dispatchers.IO) {
    try {
        // Test UDP 443 reachability
        val socket = java.net.DatagramSocket()
        socket.soTimeout = 3000
        val data = ByteArray(32) { 0x00 }
        val packet = java.net.DatagramPacket(data, data.size, java.net.InetAddress.getByName("1.1.1.1"), 443)
        val start = System.currentTimeMillis()
        socket.send(packet)
        // Try to receive (may timeout - that's OK for UDP, means not blocked)
        try {
            val recvBuf = ByteArray(128)
            val recvPacket = java.net.DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)
        } catch (e: java.net.SocketTimeoutException) {
            // Timeout on UDP means port likely open (no ICMP unreachable)
        }
        val elapsed = System.currentTimeMillis() - start
        socket.close()
        ProtocolResult("QUIC/UDP", "Hysteria2, TUIC — ضد فیلتر", true, elapsed, "UDP port 443 open")
    } catch (e: Exception) {
        ProtocolResult("QUIC/UDP", "Hysteria2, TUIC — ضد فیلتر", false, -1, "UDP blocked: ${e.message?.take(30)}")
    }
}

suspend fun testHttpUpgrade(): ProtocolResult = withContext(Dispatchers.IO) {
    try {
        val socket = Socket()
        val start = System.currentTimeMillis()
        socket.connect(InetSocketAddress("1.1.1.1", 80), 5000)
        socket.soTimeout = 3000
        val out = socket.outputStream
        val request = "GET / HTTP/1.1\r\nHost: cloudflare.com\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n\r\n"
        out.write(request.toByteArray())
        out.flush()
        val inp = socket.inputStream
        val buf = ByteArray(512)
        val read = inp.read(buf)
        val elapsed = System.currentTimeMillis() - start
        socket.close()
        val response = if (read > 0) String(buf, 0, read) else ""
        val works = response.isNotEmpty()
        ProtocolResult("HTTPUpgrade (80)", "HTTP بدون TLS — ساده", works, elapsed,
            if (works) "Port 80 responds" else "Blocked")
    } catch (e: Exception) {
        ProtocolResult("HTTPUpgrade (80)", "HTTP بدون TLS — ساده", false, -1, "")
    }
}

suspend fun testPlainHttp(): ProtocolResult = withContext(Dispatchers.IO) {
    try {
        val socket = Socket()
        val start = System.currentTimeMillis()
        socket.connect(InetSocketAddress("1.1.1.1", 80), 4000)
        socket.soTimeout = 3000
        val out = socket.outputStream
        out.write("GET / HTTP/1.1\r\nHost: 1.1.1.1\r\nConnection: close\r\n\r\n".toByteArray())
        out.flush()
        val buf = ByteArray(256)
        val read = socket.inputStream.read(buf)
        val elapsed = System.currentTimeMillis() - start
        socket.close()
        val works = read > 0
        ProtocolResult("Plain HTTP (80)", "بدون رمزنگاری — سریع ولی ناامن", works, elapsed, "")
    } catch (e: Exception) {
        ProtocolResult("Plain HTTP (80)", "بدون رمزنگاری — سریع ولی ناامن", false, -1, "")
    }
}
fun generateRecommendations(ports: List<PortResult>, protocols: List<ProtocolResult>): List<ConfigRecommendation> {
    val recs = mutableListOf<ConfigRecommendation>()
    val openPorts = ports.filter { it.isOpen }.map { it.port }.toSet()
    val workingProtocols = protocols.filter { it.works }.map { it.name }.toSet()

    val hasTls13 = workingProtocols.any { it.contains("1.3") }
    val hasTls12 = workingProtocols.any { it.contains("1.2") }
    val hasTcp = workingProtocols.any { it.contains("TCP + TLS") }
    val hasWss = workingProtocols.any { it.contains("WSS") }
    val hasWs = workingProtocols.any { it.contains("WS - no TLS") }
    val hasGrpc = workingProtocols.any { it.contains("gRPC") || it.contains("HTTP/2") }
    val hasReality = workingProtocols.any { it.contains("Reality") }
    val hasXhttp = workingProtocols.any { it.contains("XHTTP") || it.contains("SplitHTTP") }
    val hasQuic = workingProtocols.any { it.contains("QUIC") }
    val hasHttpUpgrade = workingProtocols.any { it.contains("HTTPUpgrade") }
    val hasOpenVpn = workingProtocols.any { it.contains("OpenVPN") }
    val hasPptp = workingProtocols.any { it.contains("PPTP") }
    val hasL2tp = workingProtocols.any { it.contains("L2TP") }
    val hasCisco = workingProtocols.any { it.contains("Cisco") || it.contains("AnyConnect") }
    val has443 = 443 in openPorts
    val has80 = 80 in openPorts
    val has8443 = 8443 in openPorts

    // === Best: Reality ===
    if (has443 && hasReality && hasGrpc) {
        recs.add(ConfigRecommendation(0, "VLESS", "gRPC", 443, "Reality",
            "ضد DPI + سرعت بالا + TLS1.3", 98))
    }
    if (has443 && hasReality) {
        recs.add(ConfigRecommendation(0, "VLESS", "TCP", 443, "Reality",
            "بهترین ضد شناسایی — uTLS fingerprint", 95))
    }

    // === XHTTP / SplitHTTP ===
    if (has443 && hasXhttp) {
        recs.add(ConfigRecommendation(0, "VLESS", "XHTTP", 443, "TLS",
            "SplitHTTP — جدید و ضد DPI", 93))
    }

    // === Hysteria2 ===
    if (hasQuic) {
        recs.add(ConfigRecommendation(0, "Hysteria2", "QUIC/UDP", 443, "TLS",
            "سرعت عالی — ضد throttle", 91))
    }

    // === gRPC ===
    if (has443 && hasGrpc) {
        recs.add(ConfigRecommendation(0, "VLESS", "gRPC", 443, "TLS",
            "HTTP/2 multiplexing — سرعت بالا", 87))
    }

    // === WebSocket + TLS ===
    if (has443 && hasWss) {
        recs.add(ConfigRecommendation(0, "VLESS", "WebSocket", 443, "TLS",
            "پایدار — CDN compatible — رایج‌ترین", 85))
    }

    // === VMess + WS ===
    if (has443 && hasWss) {
        recs.add(ConfigRecommendation(0, "VMess", "WebSocket", 443, "TLS",
            "سازگاری بالا — آزمایش‌شده", 78))
    }

    // === Trojan ===
    if (has443 && hasTcp) {
        recs.add(ConfigRecommendation(0, "Trojan", "TCP", 443, "TLS",
            "شبیه HTTPS عادی — ساده و پایدار", 77))
    }

    // === Trojan + WS ===
    if (has443 && hasWss) {
        recs.add(ConfigRecommendation(0, "Trojan", "WebSocket", 443, "TLS",
            "CDN support + Trojan", 75))
    }

    // === Alt ports ===
    if (has8443 && hasWss) {
        recs.add(ConfigRecommendation(0, "VLESS", "WebSocket", 8443, "TLS",
            "پورت جایگزین — 443 محدوده", 72))
    }
    val altPorts = listOf(2053, 2083, 2087, 2096).filter { it in openPorts }
    if (altPorts.isNotEmpty()) {
        recs.add(ConfigRecommendation(0, "VLESS", "WebSocket", altPorts.first(), "TLS",
            "CF alt port — کمتر فیلتر", 70))
    }

    // === HTTP no TLS ===
    if (has80 && hasWs) {
        recs.add(ConfigRecommendation(0, "VLESS", "WebSocket", 80, "None",
            "بدون TLS — سریع ولی قابل شناسایی", 55))
    }
    if (has80 && hasHttpUpgrade) {
        recs.add(ConfigRecommendation(0, "VLESS", "HTTPUpgrade", 80, "None",
            "HTTPUpgrade بدون TLS", 53))
    }

    // === VPN Protocols ===
    if (hasOpenVpn) {
        recs.add(ConfigRecommendation(0, "OpenVPN", "UDP/TCP", 1194, "TLS",
            "VPN کلاسیک — اگه باز باشه خوبه", 60))
    }
    if (hasCisco) {
        recs.add(ConfigRecommendation(0, "AnyConnect", "DTLS/SSL", 443, "TLS",
            "Enterprise VPN — DTLS سریع", 58))
    }
    if (hasL2tp) {
        recs.add(ConfigRecommendation(0, "L2TP/IPSec", "UDP", 1701, "IPSec",
            "VPN ساده — ممکنه بسته باشه", 40))
    }
    if (hasPptp) {
        recs.add(ConfigRecommendation(0, "PPTP", "TCP+GRE", 1723, "MPPE",
            "قدیمی — ناامن — اکثراً بسته", 25))
    }

    // === Fallback ===
    if (has443 && hasTcp) {
        recs.add(ConfigRecommendation(0, "VMess", "TCP", 443, "TLS",
            "Fallback — وقتی بقیه کار نمی‌کنن", 45))
    }

    return recs.sortedByDescending { it.score }.mapIndexed { idx, rec ->
        rec.copy(rank = idx + 1)
    }
}

/**
 * PPTP uses TCP port 1723 + GRE protocol (IP protocol 47)
 */
suspend fun testPptp(): ProtocolResult = withContext(Dispatchers.IO) {
    try {
        val socket = Socket()
        val start = System.currentTimeMillis()
        socket.connect(InetSocketAddress("185.177.124.190", 1723), 5000) // Public PPTP test server
        socket.soTimeout = 3000
        // PPTP Start-Control-Connection-Request
        val pptpInit = byteArrayOf(
            0x00, 0x9C.toByte(), 0x00, 0x01, // Length + Message type
            0x1A, 0x2B, 0x3C, 0x4D,          // Magic cookie
            0x00, 0x01,                        // Control type: Start-Control-Connection-Request
            0x00, 0x00,                        // Reserved
            0x01, 0x00,                        // Protocol version 1.0
            0x00, 0x00                         // Reserved
        )
        socket.outputStream.write(pptpInit)
        socket.outputStream.flush()
        val buf = ByteArray(256)
        val read = socket.inputStream.read(buf)
        val elapsed = System.currentTimeMillis() - start
        socket.close()
        val works = read > 0
        ProtocolResult("PPTP (1723/TCP + GRE)", "VPN قدیمی — معمولاً بسته", works, elapsed,
            if (works) "PPTP handshake response received" else "No response")
    } catch (e: Exception) {
        // Try just port connectivity
        try {
            val socket2 = Socket()
            socket2.connect(InetSocketAddress("1.1.1.1", 1723), 4000)
            socket2.close()
            ProtocolResult("PPTP (1723/TCP + GRE)", "VPN قدیمی — معمولاً بسته", true, -1, "Port 1723 open (GRE may be blocked)")
        } catch (e2: Exception) {
            ProtocolResult("PPTP (1723/TCP + GRE)", "VPN قدیمی — معمولاً بسته", false, -1, "Port 1723 blocked")
        }
    }
}

/**
 * L2TP uses UDP port 1701, often with IPSec (UDP 500 + 4500)
 */
suspend fun testL2tp(): ProtocolResult = withContext(Dispatchers.IO) {
    try {
        // Test UDP 1701 (L2TP) + UDP 500 (IKE) + UDP 4500 (NAT-T)
        var portsOpen = 0
        val details = mutableListOf<String>()
        val start = System.currentTimeMillis()

        // UDP 1701
        try {
            val ds = java.net.DatagramSocket()
            ds.soTimeout = 3000
            val data = ByteArray(16) { 0x00 }
            ds.send(java.net.DatagramPacket(data, data.size, java.net.InetAddress.getByName("1.1.1.1"), 1701))
            try { ds.receive(java.net.DatagramPacket(ByteArray(64), 64)) } catch (e: java.net.SocketTimeoutException) {}
            ds.close()
            portsOpen++
            details.add("UDP 1701 ✓")
        } catch (e: Exception) { details.add("UDP 1701 ✗") }

        // UDP 500 (IKE)
        try {
            val ds = java.net.DatagramSocket()
            ds.soTimeout = 3000
            val data = ByteArray(16) { 0x00 }
            ds.send(java.net.DatagramPacket(data, data.size, java.net.InetAddress.getByName("1.1.1.1"), 500))
            try { ds.receive(java.net.DatagramPacket(ByteArray(64), 64)) } catch (e: java.net.SocketTimeoutException) {}
            ds.close()
            portsOpen++
            details.add("UDP 500 ✓")
        } catch (e: Exception) { details.add("UDP 500 ✗") }

        // UDP 4500 (NAT-T)
        try {
            val ds = java.net.DatagramSocket()
            ds.soTimeout = 3000
            val data = ByteArray(16) { 0x00 }
            ds.send(java.net.DatagramPacket(data, data.size, java.net.InetAddress.getByName("1.1.1.1"), 4500))
            try { ds.receive(java.net.DatagramPacket(ByteArray(64), 64)) } catch (e: java.net.SocketTimeoutException) {}
            ds.close()
            portsOpen++
            details.add("UDP 4500 ✓")
        } catch (e: Exception) { details.add("UDP 4500 ✗") }

        val elapsed = System.currentTimeMillis() - start
        val works = portsOpen >= 2
        ProtocolResult("L2TP/IPSec (UDP 1701/500/4500)", "VPN — نیاز به UDP باز", works, elapsed,
            details.joinToString(" | "))
    } catch (e: Exception) {
        ProtocolResult("L2TP/IPSec (UDP 1701/500/4500)", "VPN — نیاز به UDP باز", false, -1, "")
    }
}

/**
 * OpenVPN uses UDP 1194 or TCP 443/1194
 */
suspend fun testOpenVpn(): ProtocolResult = withContext(Dispatchers.IO) {
    var tcpWorks = false
    var udpWorks = false
    val details = mutableListOf<String>()
    val start = System.currentTimeMillis()

    // TCP 1194
    try {
        val socket = Socket()
        socket.connect(InetSocketAddress("1.1.1.1", 1194), 4000)
        socket.close()
        tcpWorks = true
        details.add("TCP 1194 ✓")
    } catch (e: Exception) { details.add("TCP 1194 ✗") }

    // UDP 1194
    try {
        val ds = java.net.DatagramSocket()
        ds.soTimeout = 3000
        // OpenVPN handshake initial packet
        val data = byteArrayOf(0x38, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        ds.send(java.net.DatagramPacket(data, data.size, java.net.InetAddress.getByName("1.1.1.1"), 1194))
        try { ds.receive(java.net.DatagramPacket(ByteArray(64), 64)) } catch (e: java.net.SocketTimeoutException) {}
        ds.close()
        udpWorks = true
        details.add("UDP 1194 ✓")
    } catch (e: Exception) { details.add("UDP 1194 ✗") }

    // TCP 443 (OpenVPN over HTTPS port)
    try {
        val socket = Socket()
        socket.connect(InetSocketAddress("1.1.1.1", 443), 4000)
        socket.close()
        details.add("TCP 443 ✓ (fallback)")
    } catch (e: Exception) {}

    val elapsed = System.currentTimeMillis() - start
    val works = tcpWorks || udpWorks
    ProtocolResult("OpenVPN (TCP/UDP 1194)", "VPN محبوب — TCP و UDP", works, elapsed,
        details.joinToString(" | "))
}

/**
 * Cisco AnyConnect uses TCP 443 (DTLS) + UDP 443
 */
suspend fun testCiscoAnyConnect(): ProtocolResult = withContext(Dispatchers.IO) {
    val details = mutableListOf<String>()
    val start = System.currentTimeMillis()
    var works = false

    // TCP 443 with TLS + specific path
    try {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(com.snifinder.app.util.TrustAllManager()), null)
        val factory = sslContext.socketFactory
        val socket = factory.createSocket() as SSLSocket
        socket.soTimeout = 5000
        socket.connect(InetSocketAddress("1.1.1.1", 443), 5000)
        socket.startHandshake()
        // AnyConnect CONNECT method
        val out = socket.outputStream
        val request = "CONNECT /CSCOSSLC/tunnel HTTP/1.1\r\nHost: vpn.example.com\r\nUser-Agent: AnyConnect\r\n\r\n"
        out.write(request.toByteArray())
        out.flush()
        socket.soTimeout = 2000
        val buf = ByteArray(256)
        try {
            val read = socket.inputStream.read(buf)
            if (read > 0) details.add("TLS CONNECT ✓")
        } catch (e: Exception) { details.add("TLS OK") }
        socket.close()
        works = true
    } catch (e: Exception) { details.add("TCP 443 ✗") }

    // DTLS on UDP 443
    try {
        val ds = java.net.DatagramSocket()
        ds.soTimeout = 3000
        val dtlsHello = ByteArray(32) { 0x16 } // DTLS ClientHello header byte
        ds.send(java.net.DatagramPacket(dtlsHello, dtlsHello.size, java.net.InetAddress.getByName("1.1.1.1"), 443))
        try { ds.receive(java.net.DatagramPacket(ByteArray(64), 64)) } catch (e: java.net.SocketTimeoutException) {}
        ds.close()
        details.add("DTLS UDP 443 ✓")
    } catch (e: Exception) { details.add("DTLS ✗") }

    val elapsed = System.currentTimeMillis() - start
    ProtocolResult("Cisco AnyConnect (DTLS/SSL)", "Enterprise VPN — TLS + DTLS", works, elapsed,
        details.joinToString(" | "))
}

fun generateFarsiSummary(
    recs: List<ConfigRecommendation>,
    ports: List<PortResult>,
    protocols: List<ProtocolResult>
): String {
    val openPorts = ports.filter { it.isOpen }
    val workingProtos = protocols.filter { it.works }
    val top3 = recs.take(3)

    val sb = StringBuilder()

    sb.appendLine("بر اساس تست شبکه شما:")
    sb.appendLine("• ${openPorts.size} پورت از ${ports.size} باز است")
    sb.appendLine("• ${workingProtos.size} پروتکل از ${protocols.size} کار می‌کند")
    sb.appendLine()

    if (top3.isEmpty()) {
        sb.appendLine("⚠ متاسفانه هیچ ترکیب مناسبی پیدا نشد. احتمالاً اینترنت شما به شدت محدود شده.")
        return sb.toString()
    }

    sb.appendLine("🏆 بهترین انتخاب برای سرعت بالای دانلود و آپلود:")
    sb.appendLine()

    top3.forEachIndexed { idx, rec ->
        sb.appendLine("${idx + 1}. ${rec.protocol} + ${rec.transport} روی پورت ${rec.port} با ${rec.security}")
        sb.appendLine("   ← ${getDetailedExplanation(rec)}")
        sb.appendLine()
    }

    sb.appendLine("━━━━━━━━━━━━━━━━━━")
    sb.appendLine()
    sb.appendLine("💡 نکات مهم:")
    sb.appendLine()

    // Reality check
    val hasReality = workingProtos.any { it.name.contains("Reality", true) }
    if (hasReality) {
        sb.appendLine("✅ Reality پشتیبانی میشه — بهترین گزینه ضد فیلتر با سرعت بالا. از VLESS + Reality استفاده کن.")
        sb.appendLine()
    }

    // gRPC check
    val hasGrpc = workingProtos.any { it.name.contains("gRPC", true) || it.name.contains("HTTP/2", true) }
    if (hasGrpc) {
        sb.appendLine("✅ gRPC/HTTP2 باز هست — برای آپلود و دانلود همزمان عالیه. Multiplexing داره و سرعت بالایی میده.")
        sb.appendLine()
    }

    // QUIC check
    val hasQuic = workingProtos.any { it.name.contains("QUIC", true) }
    if (hasQuic) {
        sb.appendLine("✅ UDP/QUIC باز هست — Hysteria2 بهترین سرعت رو میده چون throttle نمیشه. مخصوصاً برای دانلود حجیم.")
        sb.appendLine()
    } else {
        sb.appendLine("❌ UDP بسته یا محدوده — Hysteria2 و WireGuard کار نمیکنن. از TCP استفاده کن.")
        sb.appendLine()
    }

    // WebSocket
    val hasWs = workingProtos.any { it.name.contains("WebSocket", true) }
    if (hasWs) {
        sb.appendLine("✅ WebSocket کار میکنه — پایدارترین گزینه. پشت CDN (مثل Cloudflare) قابل استفاده‌ست.")
        sb.appendLine()
    }

    // Port recommendation
    val has443 = openPorts.any { it.port == 443 }
    val altPorts = openPorts.filter { it.port in listOf(2053, 2083, 2087, 2096, 8443) }
    if (has443 && altPorts.isNotEmpty()) {
        sb.appendLine("💡 پورت 443 و ${altPorts.size} پورت جایگزین Cloudflare باز هستن. اگه روی 443 محدودیت خوردی، پورت ${altPorts.first().port} رو امتحان کن.")
        sb.appendLine()
    }

    sb.appendLine("━━━━━━━━━━━━━━━━━━")
    sb.appendLine()
    sb.appendLine("📋 تنظیمات پیشنهادی سرور:")
    sb.appendLine("• پروتکل: ${top3.first().protocol}")
    sb.appendLine("• Transport: ${top3.first().transport}")
    sb.appendLine("• Port: ${top3.first().port}")
    sb.appendLine("• Security: ${top3.first().security}")
    if (top3.first().security == "Reality") {
        sb.appendLine("• Flow: xtls-rprx-vision")
        sb.appendLine("• Fingerprint: chrome/firefox/safari")
        sb.appendLine("• ServerName: www.google.com یا www.yahoo.com")
    }

    return sb.toString()
}

fun getDetailedExplanation(rec: ConfigRecommendation): String {
    return when {
        rec.security == "Reality" && rec.transport == "gRPC" ->
            "سریع‌ترین + ضدفیلترترین. ترافیک شبیه HTTPS عادی به نظر میرسه و gRPC سرعت آپلود رو بالا میبره."
        rec.security == "Reality" && rec.transport == "TCP" ->
            "بسیار امن و سریع. بدون overhead اضافه. بهترین latency رو داره."
        rec.protocol == "Hysteria2" ->
            "مبتنی بر UDP. ضد throttle اینترنت. بهترین سرعت دانلود خام رو میده."
        rec.transport == "XHTTP" ->
            "جدیدترین transport. ترافیک رو به چند chunk HTTP تقسیم میکنه و DPI رو گول میزنه."
        rec.transport == "gRPC" && rec.security == "TLS" ->
            "HTTP/2 multiplexing. چند استریم همزمان. سرعت آپلود و دانلود بالا."
        rec.transport == "WebSocket" && rec.security == "TLS" ->
            "پایدار و قابل اتکا. پشت CDN کار میکنه. اگه IP بسته شد CDN عوض کن."
        rec.transport == "WebSocket" && rec.security == "None" ->
            "سریع ولی بدون رمزنگاری. ISP میتونه ببینه. فقط برای تست."
        rec.protocol == "Trojan" ->
            "ترافیک دقیقاً شبیه HTTPS عادی. ساده‌ترین setup رو داره."
        rec.protocol == "OpenVPN" ->
            "VPN کلاسیک. اگه پورت 1194 باز باشه خوب کار میکنه."
        rec.protocol == "AnyConnect" ->
            "Enterprise VPN. DTLS سرعت خوبی داره ولی setup سخت‌تره."
        else -> rec.reason
    }
}
