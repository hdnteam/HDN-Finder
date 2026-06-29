package com.snifinder.app.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snifinder.app.ui.theme.*
import com.snifinder.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

enum class SpeedTestState { IDLE, TESTING, DONE }
enum class SpeedTestMode { UPLOAD, DOWNLOAD, COMPARE }

@Composable
fun SpeedTestScreen() {
    var mode by remember { mutableStateOf(SpeedTestMode.DOWNLOAD) }
    var state by remember { mutableStateOf(SpeedTestState.IDLE) }
    var speedMbps by remember { mutableStateOf(0.0) }
    var currentSpeed by remember { mutableStateOf(0.0) }
    var peakSpeed by remember { mutableStateOf(0.0) }
    var progressPercent by remember { mutableStateOf(0f) }
    var testServer by remember { mutableStateOf("") }
    var serverName by remember { mutableStateOf("") }
    var isDetecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Auto-detect best server on first load
    LaunchedEffect(Unit) {
        isDetecting = true
        val best = findBestServer(context)
        testServer = best.first
        serverName = best.second
        isDetecting = false
    }
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed.toFloat(),
        animationSpec = tween(250, easing = FastOutSlowInEasing), label = "speed"
    )
    val modeColor = if (mode == SpeedTestMode.DOWNLOAD) HdnGreen else HdnCyan

    Column(modifier = Modifier.fillMaxSize().background(HdnDarkBg)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            Spacer(Modifier.height(20.dp))

            // === Gauge Section ===
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 32.dp).aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // Gauge arc
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 24f
                    val radius = size.minDimension / 2 - stroke - 10f
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2, radius * 2)

                    // Background arc
                    drawArc(HdnDarkBorder, 150f, 240f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))

                    // Speed arc
                    val maxSpeed = 100f
                    val sweep = (animatedSpeed / maxSpeed).coerceIn(0f, 1f) * 240f
                    if (sweep > 0) {
                        drawArc(
                            brush = Brush.sweepGradient(listOf(modeColor.copy(0.4f), modeColor, modeColor)),
                            startAngle = 150f, sweepAngle = sweep, useCenter = false,
                            topLeft = topLeft, size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }

                    // Tick marks
                    for (i in 0..10) {
                        val angle = Math.toRadians((150.0 + i * 24.0))
                        val innerR = radius - stroke / 2 - 12
                        val outerR = radius - stroke / 2 - 6
                        val startP = Offset(
                            (center.x + innerR * Math.cos(angle)).toFloat(),
                            (center.y + innerR * Math.sin(angle)).toFloat()
                        )
                        val endP = Offset(
                            (center.x + outerR * Math.cos(angle)).toFloat(),
                            (center.y + outerR * Math.sin(angle)).toFloat()
                        )
                        drawLine(HdnGrayDark, startP, endP, strokeWidth = if (i % 5 == 0) 2f else 1f)
                    }
                }

                // Center content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (state) {
                        SpeedTestState.IDLE -> {
                            if (isDetecting) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = modeColor, strokeWidth = 2.dp)
                                Spacer(Modifier.height(8.dp))
                                Text("یافتن سرور...", color = HdnGray, fontSize = 11.sp)
                            } else {
                                Text("READY", color = HdnGrayDark, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                            }
                        }
                        SpeedTestState.TESTING -> {
                            Text("${(progressPercent * 100).toInt()}%", color = modeColor.copy(0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        SpeedTestState.DONE -> {
                            val q = when { speedMbps >= 20 -> "عالی" to HdnGreen; speedMbps >= 10 -> "خوب" to HdnCyan; speedMbps >= 3 -> "متوسط" to HdnOrange; else -> "ضعیف" to HdnRed }
                            Text(q.first, color = q.second, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when (state) { SpeedTestState.IDLE -> "—"; else -> String.format("%.1f", if (state == SpeedTestState.DONE) speedMbps else currentSpeed) },
                        color = HdnWhite, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace
                    )
                    Text("Mbps", color = HdnGrayDark, fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    if (state == SpeedTestState.DONE) {
                        Spacer(Modifier.height(4.dp))
                        Text("= ${String.format("%.2f", speedMbps / 8.0)} MB/s", color = HdnGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (state == SpeedTestState.TESTING) {
                        Spacer(Modifier.height(6.dp))
                        Text("${((1f - progressPercent) * 10).toInt()}s", color = HdnGrayDark, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // === Mode + Start ===
            Spacer(Modifier.height(20.dp))

            // Mode selector
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { if (state != SpeedTestState.TESTING) { mode = SpeedTestMode.DOWNLOAD; state = SpeedTestState.IDLE } },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mode == SpeedTestMode.DOWNLOAD) HdnGreen.copy(0.15f) else HdnDarkCard,
                        contentColor = if (mode == SpeedTestMode.DOWNLOAD) HdnGreen else HdnGrayDark),
                    shape = RoundedCornerShape(10.dp)) {
                    Text("⬇ Download", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Button(onClick = { if (state != SpeedTestState.TESTING) { mode = SpeedTestMode.UPLOAD; state = SpeedTestState.IDLE } },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mode == SpeedTestMode.UPLOAD) HdnCyan.copy(0.15f) else HdnDarkCard,
                        contentColor = if (mode == SpeedTestMode.UPLOAD) HdnCyan else HdnGrayDark),
                    shape = RoundedCornerShape(10.dp)) {
                    Text("⬆ Upload", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Button(onClick = { if (state != SpeedTestState.TESTING) { mode = SpeedTestMode.COMPARE; state = SpeedTestState.IDLE } },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mode == SpeedTestMode.COMPARE) HdnPurple.copy(0.15f) else HdnDarkCard,
                        contentColor = if (mode == SpeedTestMode.COMPARE) HdnPurple else HdnGrayDark),
                    shape = RoundedCornerShape(10.dp)) {
                    Text("⚔ مقایسه", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // === Compare Mode UI ===
            if (mode == SpeedTestMode.COMPARE) {
                CompareSection(testServer, scope, state) { state = it }
            } else {
                // Start/Stop button for Download/Upload
                Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                if (state == SpeedTestState.TESTING) {
                    Button(onClick = { state = SpeedTestState.DONE },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HdnRed, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)) {
                        Text("STOP", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                    }
                } else {
                    Button(onClick = {
                        state = SpeedTestState.TESTING
                        speedMbps = 0.0; currentSpeed = 0.0; peakSpeed = 0.0; progressPercent = 0f
                        scope.launch {
                            val result = if (mode == SpeedTestMode.UPLOAD) {
                                runUploadTest(testServer, 10000, { s, _, _, p -> currentSpeed = s; if (s > peakSpeed) peakSpeed = s; progressPercent = p }) { state == SpeedTestState.TESTING }
                            } else {
                                runDownloadTest(testServer, 10000, { s, _, _, p -> currentSpeed = s; if (s > peakSpeed) peakSpeed = s; progressPercent = p }) { state == SpeedTestState.TESTING }
                            }
                            speedMbps = result; currentSpeed = result; state = SpeedTestState.DONE
                        }
                    }, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = testServer.isNotEmpty() && !isDetecting,
                        colors = ButtonDefaults.buttonColors(containerColor = modeColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(6.dp)) {
                        Text("START", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
                    }
                }
            }
            } // end else (download/upload mode)

            Spacer(Modifier.height(20.dp))

            // === Server + Peak info ===
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Server card
                Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(HdnDarkCard).padding(14.dp)) {
                    Column {
                        Text("SERVER", color = HdnGrayDark, fontSize = 8.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(serverName.ifEmpty { "..." }, color = HdnWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
                // Peak card
                if (state != SpeedTestState.IDLE) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(HdnDarkCard).padding(14.dp)) {
                        Column {
                            Text("PEAK", color = HdnGrayDark, fontSize = 8.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("${String.format("%.1f", peakSpeed)} Mbps", color = modeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Re-detect server button + manual input
            Spacer(Modifier.height(12.dp))
            var showServerEdit by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    isDetecting = true
                    scope.launch {
                        val best = findBestServer(context)
                        testServer = best.first; serverName = best.second; isDetecting = false
                    }
                }, enabled = state != SpeedTestState.TESTING && !isDetecting,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HdnDarkCard, contentColor = modeColor),
                    shape = RoundedCornerShape(10.dp)) {
                    Text("🔄 سرور اتوماتیک", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { showServerEdit = !showServerEdit },
                    modifier = Modifier.weight(1f).height(38.dp),
                    enabled = state != SpeedTestState.TESTING,
                    colors = ButtonDefaults.buttonColors(containerColor = HdnDarkCard, contentColor = HdnGray),
                    shape = RoundedCornerShape(10.dp)) {
                    Text("✏ سرور دستی", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Manual server input
            if (showServerEdit) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = testServer, onValueChange = { testServer = it; serverName = "Custom" },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    placeholder = { Text("آدرس سرور...", color = HdnGrayDark) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = HdnCyan),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = modeColor.copy(0.5f), unfocusedBorderColor = HdnDarkBorder,
                        cursorColor = modeColor, focusedContainerColor = HdnDarkCard, unfocusedContainerColor = HdnDarkCard),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("speed.hetzner.de" to "Hetzner", "speed.cloudflare.com" to "CF", "proof.ovh.net" to "OVH", "speedtest.mci.ir" to "MCI", "speedtest.irancell.ir" to "MTN").forEach { (host, label) ->
                        FilterChip(
                            selected = testServer == host,
                            onClick = { testServer = host; serverName = label },
                            label = { Text(label, fontSize = 8.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = modeColor.copy(0.15f), selectedLabelColor = modeColor, labelColor = HdnGrayDark),
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
        HdnFooter()
    }
}

// === Speed Test Functions ===

/**
 * Upload test: sends data to server and measures throughput.
 * Uses POST to multiple endpoints for reliability.
 */
suspend fun runUploadTest(
    server: String, durationMs: Long = 10000,
    onProgress: (speedMbps: Double, totalBytes: Long, elapsedMs: Long, progress: Float) -> Unit,
    isRunning: () -> Boolean
): Double = withContext(Dispatchers.IO) {
    val chunkSize = 131072 // 128KB
    val chunk = ByteArray(chunkSize) { (it % 256).toByte() }
    var totalUploaded = 0L
    val startTime = System.currentTimeMillis()
    var lastReportTime = startTime
    var lastReportBytes = 0L

    // Upload URLs for different servers
    val uploadUrl = resolveUploadUrl(server)

    try {
        while (isRunning()) {
            if (System.currentTimeMillis() - startTime >= durationMs) break
            try {
                val conn = URL(uploadUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 10000
                conn.setRequestProperty("Content-Type", "application/octet-stream")
                conn.setRequestProperty("Connection", "keep-alive")
                conn.setChunkedStreamingMode(chunkSize)

                val output = conn.outputStream
                // Upload 2MB per request (16 x 128KB)
                repeat(16) {
                    if (!isRunning() || System.currentTimeMillis() - startTime >= durationMs)
                        return@withContext calcSpeed(totalUploaded, System.currentTimeMillis() - startTime)
                    output.write(chunk)
                    totalUploaded += chunkSize
                    reportProgress(totalUploaded, lastReportBytes, lastReportTime, startTime, durationMs, onProgress).let {
                        if (it.first > 0) { lastReportTime = it.first; lastReportBytes = it.second }
                    }
                }
                output.flush()
                try { conn.responseCode } catch (e: Exception) {}
                conn.disconnect()
            } catch (e: Exception) {
                delay(300)
            }
        }
    } catch (e: Exception) {}

    val avg = calcSpeed(totalUploaded, System.currentTimeMillis() - startTime)
    onProgress(avg, totalUploaded, System.currentTimeMillis() - startTime, 1f)
    avg
}

/**
 * Download test: downloads data from server and measures throughput.
 * Uses real files from speed test servers.
 */
suspend fun runDownloadTest(
    server: String, durationMs: Long = 10000,
    onProgress: (speedMbps: Double, totalBytes: Long, elapsedMs: Long, progress: Float) -> Unit,
    isRunning: () -> Boolean
): Double = withContext(Dispatchers.IO) {
    var totalDl = 0L
    val startTime = System.currentTimeMillis()
    var lastReportTime = startTime
    var lastReportBytes = 0L
    val buffer = ByteArray(131072) // 128KB buffer

    // Download URLs for different servers
    val dlUrl = resolveDownloadUrl(server)

    try {
        while (isRunning()) {
            if (System.currentTimeMillis() - startTime >= durationMs) break
            try {
                var dlUrlFinal = dlUrl
                val conn = URL(dlUrlFinal).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 10000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("Connection", "keep-alive")
                conn.setRequestProperty("Cache-Control", "no-cache")
                conn.setRequestProperty("User-Agent", "HDNFinder/1.0")

                // Handle redirects manually if needed
                val responseCode = conn.responseCode
                if (responseCode == 301 || responseCode == 302) {
                    val newUrl = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (newUrl != null) dlUrlFinal = newUrl
                    val conn2 = URL(dlUrlFinal).openConnection() as HttpURLConnection
                    conn2.connectTimeout = 5000
                    conn2.readTimeout = 10000
                    conn2.setRequestProperty("User-Agent", "HDNFinder/1.0")
                    val input2 = conn2.inputStream
                    while (isRunning() && System.currentTimeMillis() - startTime < durationMs) {
                        val read = input2.read(buffer)
                        if (read == -1) break
                        totalDl += read
                        reportProgress(totalDl, lastReportBytes, lastReportTime, startTime, durationMs, onProgress).let {
                            if (it.first > 0) { lastReportTime = it.first; lastReportBytes = it.second }
                        }
                    }
                    input2.close(); conn2.disconnect()
                } else if (responseCode == 200) {
                    val input = conn.inputStream
                    while (isRunning() && System.currentTimeMillis() - startTime < durationMs) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        totalDl += read
                        reportProgress(totalDl, lastReportBytes, lastReportTime, startTime, durationMs, onProgress).let {
                            if (it.first > 0) { lastReportTime = it.first; lastReportBytes = it.second }
                        }
                    }
                    input.close(); conn.disconnect()
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                delay(300)
            }
        }
    } catch (e: Exception) {}

    val avg = calcSpeed(totalDl, System.currentTimeMillis() - startTime)
    onProgress(avg, totalDl, System.currentTimeMillis() - startTime, 1f)
    avg
}

private fun reportProgress(
    totalBytes: Long, lastBytes: Long, lastTime: Long,
    startTime: Long, durationMs: Long,
    onProgress: (Double, Long, Long, Float) -> Unit
): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    if (now - lastTime >= 250) {
        val intervalBytes = totalBytes - lastBytes
        val intervalMs = now - lastTime
        val speed = (intervalBytes.toDouble() * 8.0) / (intervalMs.toDouble() * 1000.0)
        val progress = ((now - startTime).toFloat() / durationMs).coerceIn(0f, 1f)
        onProgress(speed, totalBytes, now - startTime, progress)
        return now to totalBytes
    }
    return 0L to lastBytes
}

private fun calcSpeed(bytes: Long, ms: Long): Double {
    if (ms <= 0 || bytes <= 0) return 0.0
    return (bytes.toDouble() * 8.0) / (ms.toDouble() * 1000.0)
}

/**
 * Resolve download URL based on server hostname
 */
private fun resolveDownloadUrl(server: String): String = when {
    server.contains("hetzner") -> "https://speed.hetzner.de/1GB.bin"
    server.contains("cloudflare") -> "https://speed.cloudflare.com/__down?bytes=25000000"
    server.contains("ovh") -> "http://proof.ovh.net/files/10Mb.dat"
    server.contains("tele2") -> "https://speedtest.tele2.net/10MB.zip"
    server.contains("belwue") -> "http://speedtest.belwue.net/100M"
    server.contains("mci") -> "http://dl.mci.ir/speedtest/10M.bin"
    server.contains("irancell") -> "http://dl.irancell.ir/speedtest/10M.bin"
    server.contains("rightel") -> "http://dl.rightel.ir/speedtest/10M.bin"
    server.startsWith("http") -> server
    else -> "https://$server"
}

/**
 * Resolve upload URL based on server hostname
 */
private fun resolveUploadUrl(server: String): String = when {
    server.contains("hetzner") -> "https://speed.hetzner.de"
    server.contains("cloudflare") -> "https://speed.cloudflare.com/__up"
    server.contains("ovh") -> "http://proof.ovh.net"
    server.contains("tele2") -> "https://speedtest.tele2.net/upload.php"
    server.contains("belwue") -> "http://speedtest.belwue.net"
    server.contains("mci") -> "http://speedtest.mci.ir/ul"
    server.contains("irancell") -> "http://speedtest.irancell.ir/ul"
    server.contains("rightel") -> "http://speedtest.rightel.ir/ul"
    server.startsWith("http") -> server
    else -> "https://$server"
}

/**
 * Finds best speed test server.
 * When VPN is active: tests international servers (Iranian servers won't work through VPN)
 * When VPN is off: detects carrier and tries local servers first
 */
suspend fun findBestServer(context: Context): Pair<String, String> = withContext(Dispatchers.IO) {
    data class Server(val host: String, val name: String, val testUrl: String)

    // Detect if VPN is active
    val isVpnActive = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    } catch (e: Exception) { false }

    // Detect carrier
    val carrierName = try {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        tm?.networkOperatorName?.lowercase() ?: ""
    } catch (e: Exception) { "" }

    // Build server list based on VPN status
    val candidates = mutableListOf<Server>()

    if (!isVpnActive) {
        // No VPN - try Iranian servers first
        when {
            carrierName.contains("mci") || carrierName.contains("همراه") || carrierName.contains("hamrah") ->
                candidates.add(Server("speedtest.mci.ir", "همراه اول", "http://speedtest.mci.ir"))
            carrierName.contains("mtn") || carrierName.contains("ایرانسل") || carrierName.contains("irancell") ->
                candidates.add(Server("speedtest.irancell.ir", "ایرانسل", "http://speedtest.irancell.ir"))
            carrierName.contains("rightel") || carrierName.contains("رایتل") ->
                candidates.add(Server("speedtest.rightel.ir", "رایتل", "http://speedtest.rightel.ir"))
        }
    }

    // International servers (always available, work with VPN too)
    candidates.addAll(listOf(
        Server("speed.hetzner.de", "Hetzner DE", "https://speed.hetzner.de"),
        Server("speed.cloudflare.com", "Cloudflare", "https://speed.cloudflare.com"),
        Server("proof.ovh.net", "OVH FR", "http://proof.ovh.net"),
        Server("speedtest.tele2.net", "Tele2 EU", "https://speedtest.tele2.net")
    ))

    data class Result(val server: Server, val latency: Long)
    val results = mutableListOf<Result>()

    for (s in candidates) {
        try {
            val conn = URL(s.testUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "GET"
            conn.instanceFollowRedirects = true
            conn.useCaches = false
            conn.setRequestProperty("User-Agent", "HDNFinder/1.0")
            val start = System.currentTimeMillis()
            val code = conn.responseCode
            val latency = System.currentTimeMillis() - start
            conn.disconnect()
            if (code in 200..399) {
                results.add(Result(s, latency))
            }
        } catch (e: Exception) { /* skip */ }
    }

    if (results.isEmpty()) return@withContext "speed.hetzner.de" to "Hetzner (پیش‌فرض)"

    val best = results.sortedBy { it.latency }.first()
    val vpnTag = if (isVpnActive) " [VPN]" else ""
    best.server.host to "${best.server.name} • ${best.latency}ms$vpnTag"
}

/**
 * Compare section: paste two configs, one TEST button tests both sequentially
 */
@Composable
fun CompareSection(server: String, scope: kotlinx.coroutines.CoroutineScope, state: SpeedTestState, onStateChange: (SpeedTestState) -> Unit) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    var config1 by remember { mutableStateOf("") }
    var config2 by remember { mutableStateOf("") }
    var dl1 by remember { mutableStateOf(-1.0) }
    var ul1 by remember { mutableStateOf(-1.0) }
    var dl2 by remember { mutableStateOf(-1.0) }
    var ul2 by remember { mutableStateOf(-1.0) }
    var testingSlot by remember { mutableStateOf(0) } // 0=idle, 1=testing #1, 2=testing #2
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {

        // Config 1 box
        Card(colors = CardDefaults.cardColors(containerColor = HdnDarkCard), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)).background(HdnCyan.copy(0.15f)),
                        contentAlignment = Alignment.Center) {
                        Text("1", color = HdnCyan, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (testingSlot == 1) {
                        Text("${(progress * 100).toInt()}%", color = HdnCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.width(6.dp))
                    }
                    Button(onClick = {
                        val clip = clipboardManager.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            config1 = clip.getItemAt(0).text?.toString() ?: ""
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = HdnCyan.copy(0.12f), contentColor = HdnCyan),
                        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("PASTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
                if (config1.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(config1.take(60) + if (config1.length > 60) "..." else "",
                        color = HdnGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 2)
                }
                if (dl1 >= 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${String.format("%.1f", dl1)}", color = HdnGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("DL Mbps", color = HdnGrayDark, fontSize = 8.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${String.format("%.1f", ul1)}", color = HdnCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("UL Mbps", color = HdnGrayDark, fontSize = 8.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Config 2 box
        Card(colors = CardDefaults.cardColors(containerColor = HdnDarkCard), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)).background(HdnPurple.copy(0.15f)),
                        contentAlignment = Alignment.Center) {
                        Text("2", color = HdnPurple, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (testingSlot == 2) {
                        Text("${(progress * 100).toInt()}%", color = HdnPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.width(6.dp))
                    }
                    Button(onClick = {
                        val clip = clipboardManager.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            config2 = clip.getItemAt(0).text?.toString() ?: ""
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = HdnPurple.copy(0.12f), contentColor = HdnPurple),
                        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("PASTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
                if (config2.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(config2.take(60) + if (config2.length > 60) "..." else "",
                        color = HdnGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 2)
                }
                if (dl2 >= 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${String.format("%.1f", dl2)}", color = HdnGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("DL Mbps", color = HdnGrayDark, fontSize = 8.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${String.format("%.1f", ul2)}", color = HdnCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("UL Mbps", color = HdnGrayDark, fontSize = 8.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Status
        if (statusText.isNotEmpty()) {
            Text(statusText, color = HdnGray, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }

        // Single TEST button
        if (testingSlot > 0) {
            Button(onClick = { testingSlot = 0; onStateChange(SpeedTestState.DONE); statusText = "متوقف شد" },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HdnRed, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)) {
                Text("STOP", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 2.sp)
            }
        } else {
            Button(onClick = {
                if (config1.isEmpty() || config2.isEmpty()) {
                    statusText = "هر دو کانفیگ رو Paste کن"
                    return@Button
                }
                dl1 = -1.0; ul1 = -1.0; dl2 = -1.0; ul2 = -1.0
                onStateChange(SpeedTestState.TESTING)
                scope.launch {
                    // Test config 1
                    statusText = "تست کانفیگ 1... (V2RayNG: کانفیگ 1 وصل باشه)"
                    testingSlot = 1; progress = 0f
                    dl1 = runDownloadTest(server, 8000, { _, _, _, p -> progress = p }) { testingSlot == 1 }
                    ul1 = runUploadTest(server, 6000, { _, _, _, p -> progress = p }) { testingSlot == 1 }

                    // Wait for user to switch config
                    statusText = "الان کانفیگ 2 رو توی V2RayNG وصل کن..."
                    testingSlot = 0
                    delay(5000) // 5 sec pause to switch

                    // Test config 2
                    statusText = "تست کانفیگ 2..."
                    testingSlot = 2; progress = 0f
                    dl2 = runDownloadTest(server, 8000, { _, _, _, p -> progress = p }) { testingSlot == 2 }
                    ul2 = runUploadTest(server, 6000, { _, _, _, p -> progress = p }) { testingSlot == 2 }

                    testingSlot = 0; onStateChange(SpeedTestState.DONE)
                    statusText = ""
                }
            }, modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = config1.isNotEmpty() && config2.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = HdnGold, contentColor = Color.Black,
                    disabledContainerColor = HdnDarkBorder),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(4.dp)) {
                Text("⚔ START TEST", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 2.sp)
            }
        }

        // === Winner ===
        if (dl1 >= 0 && dl2 >= 0) {
            Spacer(Modifier.height(14.dp))
            val total1 = dl1 + ul1
            val total2 = dl2 + ul2
            val winner = if (total1 >= total2) 1 else 2
            val winnerColor = if (winner == 1) HdnCyan else HdnPurple

            Card(colors = CardDefaults.cardColors(containerColor = winnerColor.copy(0.1f)), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WINNER", color = HdnGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Config $winner", color = winnerColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("#1", color = HdnCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${String.format("%.1f", total1)}", color = if (winner == 1) HdnGold else HdnGray, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("Mbps", color = HdnGrayDark, fontSize = 8.sp)
                        }
                        Text("vs", color = HdnGrayDark, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("#2", color = HdnPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${String.format("%.1f", total2)}", color = if (winner == 2) HdnGold else HdnGray, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("Mbps", color = HdnGrayDark, fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}
