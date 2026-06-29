package com.snifinder.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snifinder.app.ui.theme.*
import com.snifinder.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*

data class PingResult(
    val host: String,
    val ip: String,
    val time: Long,
    val isReachable: Boolean
)

data class PingHistoryItem(
    val host: String,
    val ip: String,
    val avgLatency: Long,
    val timestamp: Long
)

@Composable
fun PingScreen() {
    var hostInput by remember { mutableStateOf("") }
    var isPinging by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<PingResult>>(emptyList()) }
    var resolvedIp by remember { mutableStateOf("") }
    var history by remember { mutableStateOf<List<PingHistoryItem>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Load history from prefs
    LaunchedEffect(Unit) {
        history = loadPingHistory(context)
    }

    DisposableEffect(Unit) { onDispose { isPinging = false } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            HdnGlowCard(glowColor = HdnCyan) {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("📡", "Ping Tool")
                    Spacer(Modifier.height(4.dp))
                    Text("پینگ مداوم — Start/Stop", color = HdnGrayDark, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = hostInput, onValueChange = { hostInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("google.com یا 8.8.8.8", color = HdnGrayDark) },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = HdnGreen),
                        singleLine = true, enabled = !isPinging,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HdnCyan.copy(0.6f), unfocusedBorderColor = HdnDarkBorder,
                            cursorColor = HdnCyan, focusedContainerColor = HdnDarkBg.copy(0.5f), unfocusedContainerColor = HdnDarkBg.copy(0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    if (isPinging) {
                        HdnButton("Stop", onClick = { isPinging = false }, color = HdnRed,
                            icon = { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) })
                    } else {
                        HdnButton("Start", onClick = {
                            if (hostInput.isBlank()) {
                                Toast.makeText(context, "آدرس وارد کنید", Toast.LENGTH_SHORT).show()
                                return@HdnButton
                            }
                            isPinging = true; results = emptyList(); resolvedIp = ""
                            scope.launch {
                                while (isPinging) {
                                    val result = doPing(hostInput.trim())
                                    if (resolvedIp.isEmpty() && result.ip.isNotEmpty()) resolvedIp = result.ip
                                    results = results + result
                                    if (results.size > 50) results = results.takeLast(50)
                                    delay(1000)
                                }
                                // Save to history when stopped
                                val successful = results.filter { it.isReachable }
                                if (successful.isNotEmpty()) {
                                    val avg = successful.map { it.time }.average().toLong()
                                    val item = PingHistoryItem(hostInput.trim(), resolvedIp, avg, System.currentTimeMillis())
                                    history = (listOf(item) + history).take(30)
                                    savePingHistory(context, history)
                                }
                            }
                        }, enabled = hostInput.isNotBlank(), icon = { Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)) })
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Live Results
            if (results.isNotEmpty()) {
                HdnCard {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("نتایج", fontWeight = FontWeight.Bold, color = HdnWhite, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.weight(1f))
                            if (isPinging) Text("● LIVE", color = HdnGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))

                        // IP + Copy
                        if (resolvedIp.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(HdnCyan.copy(alpha = 0.08f))
                                .border(1.dp, HdnCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("IP: ", color = HdnGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                Text(resolvedIp, color = HdnCyan, fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("ip", resolvedIp))
                                    Toast.makeText(context, "IP copied!", Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp), tint = HdnCyan)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        // Last 10 pings with bars
                        val displayResults = results.takeLast(10)
                        displayResults.forEachIndexed { idx, r ->
                            val actualIdx = results.size - displayResults.size + idx + 1
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("#$actualIdx", color = HdnGrayDark, fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp, modifier = Modifier.width(32.dp))
                                if (r.isReachable) {
                                    val barWidth = (r.time.coerceAtMost(500).toFloat() / 500f)
                                    Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(3.dp)).background(HdnDarkBorder)) {
                                        Box(Modifier.fillMaxHeight().fillMaxWidth(barWidth).clip(RoundedCornerShape(3.dp))
                                            .background(when { r.time < 50 -> HdnGreen; r.time < 150 -> HdnOrange; else -> HdnRed }))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("${r.time}ms", color = when { r.time < 50 -> HdnGreen; r.time < 150 -> HdnOrange; else -> HdnRed },
                                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(50.dp))
                                } else {
                                    Text("❌ timeout", color = HdnRed, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                        }

                        // Stats
                        val successful = results.filter { it.isReachable }
                        if (successful.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = HdnDarkBorder)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("Min", "${successful.minOf { it.time }}ms", HdnGreen)
                                StatItem("Avg", "${successful.map { it.time }.average().toLong()}ms", HdnCyan)
                                StatItem("Max", "${successful.maxOf { it.time }}ms", HdnRed)
                                StatItem("Sent", "${results.size}", HdnWhite)
                                StatItem("Loss", "${((results.size - successful.size) * 100) / results.size}%", HdnOrange)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Ping Chart
            if (results.size > 3) {
                Spacer(Modifier.height(14.dp))
                HdnCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("📈 نمودار پینگ", color = HdnWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(10.dp))
                        PingChart(results.takeLast(30))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // History
            if (history.isNotEmpty()) {
                HdnCard {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("📋", "تاریخچه")
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                history = emptyList()
                                savePingHistory(context, history)
                                Toast.makeText(context, "تاریخچه پاک شد", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = HdnRed)
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        history.forEach { item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                // Tap to ping
                                Row(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        hostInput = item.host
                                        isPinging = true
                                        results = emptyList()
                                        resolvedIp = ""
                                        scope.launch {
                                            while (isPinging) {
                                                val result = doPing(item.host)
                                                if (resolvedIp.isEmpty() && result.ip.isNotEmpty()) resolvedIp = result.ip
                                                results = results + result
                                                if (results.size > 50) results = results.takeLast(50)
                                                delay(1000)
                                            }
                                            val successful = results.filter { it.isReachable }
                                            if (successful.isNotEmpty()) {
                                                val avg = successful.map { it.time }.average().toLong()
                                                val newItem = PingHistoryItem(item.host, resolvedIp, avg, System.currentTimeMillis())
                                                history = (listOf(newItem) + history.filter { it.host != item.host }).take(30)
                                                savePingHistory(context, history)
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.host, color = HdnWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                                        Row {
                                            if (item.ip.isNotEmpty()) {
                                                Text(item.ip, color = HdnGrayDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            val dateStr = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                                            Text(dateStr, color = HdnGrayDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    Text("${item.avgLatency}ms", color = when {
                                        item.avgLatency < 50 -> HdnGreen
                                        item.avgLatency < 150 -> HdnOrange
                                        else -> HdnRed
                                    }, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                                // Delete single item
                                IconButton(onClick = {
                                    history = history.filter { it.timestamp != item.timestamp || it.host != item.host }
                                    savePingHistory(context, history)
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, null, Modifier.size(14.dp), tint = HdnRed.copy(alpha = 0.7f))
                                }
                            }
                            HorizontalDivider(color = HdnDarkBorder.copy(0.4f))
                        }
                    }
                }
            }
        }
        HdnFooter()
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(label, color = HdnGrayDark, fontSize = 9.sp)
    }
}

suspend fun doPing(host: String): PingResult = withContext(Dispatchers.IO) {
    try {
        val startTime = System.currentTimeMillis()
        val address = InetAddress.getByName(host)
        val reachable = address.isReachable(5000)
        val elapsed = System.currentTimeMillis() - startTime
        PingResult(host = host, ip = address.hostAddress ?: "", time = if (reachable) elapsed else -1, isReachable = reachable)
    } catch (e: Exception) {
        PingResult(host = host, ip = "", time = -1, isReachable = false)
    }
}

// === History persistence ===

private const val PING_HISTORY_PREFS = "ping_history"
private const val PING_HISTORY_KEY = "items"

fun savePingHistory(context: Context, history: List<PingHistoryItem>) {
    val prefs = context.getSharedPreferences(PING_HISTORY_PREFS, Context.MODE_PRIVATE)
    val data = history.joinToString("|") { "${it.host},${it.ip},${it.avgLatency},${it.timestamp}" }
    prefs.edit().putString(PING_HISTORY_KEY, data).apply()
}

fun loadPingHistory(context: Context): List<PingHistoryItem> {
    val prefs = context.getSharedPreferences(PING_HISTORY_PREFS, Context.MODE_PRIVATE)
    val data = prefs.getString(PING_HISTORY_KEY, "") ?: ""
    if (data.isBlank()) return emptyList()
    return data.split("|").mapNotNull { entry ->
        val parts = entry.split(",")
        if (parts.size >= 4) {
            PingHistoryItem(
                host = parts[0],
                ip = parts[1],
                avgLatency = parts[2].toLongOrNull() ?: 0,
                timestamp = parts[3].toLongOrNull() ?: 0
            )
        } else null
    }
}

@Composable
fun PingChart(results: List<PingResult>) {
    val successResults = results.map { if (it.isReachable) it.time.toFloat() else -1f }
    val maxPing = successResults.filter { it > 0 }.maxOrNull() ?: 100f

    Canvas(
        modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp))
            .background(HdnDarkBg)
    ) {
        val width = size.width
        val height = size.height
        val padding = 4f

        if (successResults.size < 2) return@Canvas

        val stepX = (width - padding * 2) / (successResults.size - 1).coerceAtLeast(1)
        val scaleY = (height - padding * 2) / maxPing.coerceAtLeast(1f)

        // Draw grid lines
        for (i in 1..3) {
            val y = height - padding - (maxPing * i / 4) * scaleY
            drawLine(HdnDarkBorder, Offset(padding, y), Offset(width - padding, y), strokeWidth = 1f)
        }

        // Draw ping line
        val path = Path()
        var started = false

        successResults.forEachIndexed { index, ping ->
            val x = padding + index * stepX
            if (ping > 0) {
                val y = height - padding - ping * scaleY
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
                // Draw dot
                val dotColor = when {
                    ping < 50 -> HdnGreen
                    ping < 150 -> HdnOrange
                    else -> HdnRed
                }
                drawCircle(dotColor, radius = 3f, center = Offset(x, y))
            } else {
                // Timeout - draw red X at top
                val y = padding + 10f
                drawCircle(HdnRed, radius = 3f, center = Offset(x, y))
            }
        }

        // Draw the line
        if (started) {
            drawPath(path, color = HdnCyan, style = Stroke(width = 2f))
        }
    }
}
