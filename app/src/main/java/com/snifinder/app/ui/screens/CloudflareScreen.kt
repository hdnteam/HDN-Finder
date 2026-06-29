package com.snifinder.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snifinder.app.model.ConfigData
import com.snifinder.app.model.SpeedResult
import com.snifinder.app.model.SpeedStatus
import com.snifinder.app.ui.theme.*
import com.snifinder.app.ui.components.*
import com.snifinder.app.util.CloudflareIpProvider
import com.snifinder.app.util.ConfigParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

data class CfIpResult(
    val ip: String,
    val latency: Long,      // TLS handshake ms
    val downloadSpeed: Double, // KB/s
    val uploadSpeed: Double,   // KB/s
    val status: CfStatus
) {
    fun formattedDownload(): String = when {
        downloadSpeed >= 1024 -> String.format("%.1f MB/s", downloadSpeed / 1024.0)
        downloadSpeed > 0 -> String.format("%.0f KB/s", downloadSpeed)
        else -> "—"
    }
    fun formattedUpload(): String = when {
        uploadSpeed >= 1024 -> String.format("%.1f MB/s", uploadSpeed / 1024.0)
        uploadSpeed > 0 -> String.format("%.0f KB/s", uploadSpeed)
        else -> "—"
    }
}

enum class CfStatus { SUCCESS, TIMEOUT, FAILED }

enum class CfSortMode { DOWNLOAD, UPLOAD, PING }

enum class CfTestMode {
    SPEED,    // Full download + upload speed test
    LATENCY   // Quick TLS handshake (real delay) only
}

@Composable
fun CloudflareScreen() {
    var configInput by remember { mutableStateOf("") }
    var parsedConfig by remember { mutableStateOf<ConfigData?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<CfIpResult>>(emptyList()) }
    var progress by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }
    var sortMode by remember { mutableStateOf(CfSortMode.DOWNLOAD) }
    var testMode by remember { mutableStateOf(CfTestMode.SPEED) }
    var randomCount by remember { mutableStateOf(100) }
    var timeoutSec by remember { mutableStateOf(5) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var skippedRanges by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentRange by remember { mutableStateOf("") }
    var skipCurrentRange by remember { mutableStateOf(false) }
    // Range selection: all ranges with toggle
    val allRangeNames = remember {
        CloudflareIpProvider.allCidrRanges.map { "${it.base1}.${it.base2}.${it.base3}.${it.base4}/${it.mask}" }
    }
    var selectedRanges by remember { mutableStateOf(allRangeNames.toSet()) }
    var selectAll by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            // Config input
            HdnCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("☁", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Cloudflare Clean IP", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = HdnOrange, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("IP تمیز کلادفلر رو پیدا کن و جایگزین سرور کانفیگت کن",
                        color = HdnGray, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = configInput, onValueChange = { configInput = it; parseError = null },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        placeholder = { Text("کانفیگ خود را paste کنید...", color = HdnGray.copy(0.5f)) },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = HdnGreen),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HdnOrange, unfocusedBorderColor = HdnDarkBorder,
                            cursorColor = HdnOrange, focusedContainerColor = HdnDarkBg, unfocusedContainerColor = HdnDarkBg),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))

                    Button(onClick = {
                        val config = ConfigParser.parse(configInput)
                        if (config != null) { parsedConfig = config; parseError = null }
                        else { parsedConfig = null; parseError = "فرمت کانفیگ نامعتبر" }
                    }, modifier = Modifier.fillMaxWidth(), enabled = configInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = HdnOrange, contentColor = Color.Black,
                            disabledContainerColor = HdnDarkBorder), shape = RoundedCornerShape(10.dp)) {
                        Text("تجزیه کانفیگ", fontWeight = FontWeight.Bold)
                    }
                    parseError?.let { Spacer(Modifier.height(4.dp)); Text("⚠ $it", color = HdnRed, fontSize = 12.sp) }
                    parsedConfig?.let { config ->
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(HdnGreen.copy(alpha = 0.06f))
                            .border(1.dp, HdnGreen.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(12.dp)) {
                            Column {
                                Text("✅ شناسایی شد", fontWeight = FontWeight.Bold, color = HdnGreen, fontSize = 13.sp)
                                Spacer(Modifier.height(6.dp))
                                CfInfoRow("Protocol", config.protocol.name)
                                CfInfoRow("Transport", config.transport.name)
                                CfInfoRow("Security", config.security)
                                CfInfoRow("Server", "${config.server}:${config.port}")
                                CfInfoRow("SNI", config.originalSni)
                                if (config.flow.isNotEmpty()) CfInfoRow("Flow", config.flow)
                                if (config.fp.isNotEmpty()) CfInfoRow("Fingerprint", config.fp)
                                if (config.pbk.isNotEmpty()) CfInfoRow("PublicKey", config.pbk.take(12) + "...")
                                if (config.path.isNotEmpty()) CfInfoRow("Path", config.path)
                                if (config.host.isNotEmpty()) CfInfoRow("Host", config.host)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Settings
            HdnCard {
                Column(Modifier.padding(16.dp)) {
                    Text("⚙ تنظیمات", fontWeight = FontWeight.Bold, color = HdnWhite, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(12.dp))

                    // === Test Mode ===
                    Text("نوع تست:", color = HdnWhite, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = testMode == CfTestMode.SPEED,
                            onClick = { testMode = CfTestMode.SPEED },
                            label = { Text("⚡ سرعت واقعی", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HdnOrange.copy(alpha = 0.2f), selectedLabelColor = HdnOrange),
                            modifier = Modifier.weight(1f))
                        FilterChip(selected = testMode == CfTestMode.LATENCY,
                            onClick = { testMode = CfTestMode.LATENCY },
                            label = { Text("🏓 Latency (Real Delay)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HdnCyan.copy(alpha = 0.2f), selectedLabelColor = HdnCyan),
                            modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = HdnDarkBorder)
                    Spacer(Modifier.height(12.dp))

                    // === Range Selection ===
                    Text("انتخاب رنج IP:", color = HdnOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))

                    // Select All / Deselect All
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = selectAll, onCheckedChange = {
                            selectAll = it
                            selectedRanges = if (it) allRangeNames.toSet() else emptySet()
                        }, colors = CheckboxDefaults.colors(checkedColor = HdnOrange, uncheckedColor = HdnGray))
                        Spacer(Modifier.width(4.dp))
                        Text("انتخاب همه رنج‌ها (${allRangeNames.size})", color = HdnWhite, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(4.dp))

                    // Individual ranges
                    allRangeNames.forEach { range ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                        ) {
                            Checkbox(
                                checked = range in selectedRanges,
                                onCheckedChange = { checked ->
                                    selectedRanges = if (checked) selectedRanges + range else selectedRanges - range
                                    selectAll = selectedRanges.size == allRangeNames.size
                                },
                                colors = CheckboxDefaults.colors(checkedColor = HdnOrange, uncheckedColor = HdnGrayDark),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(range, color = if (range in selectedRanges) HdnWhite else HdnGrayDark,
                                fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("${selectedRanges.size} رنج انتخاب شده", color = HdnGray, fontSize = 10.sp)

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = HdnDarkBorder)
                    Spacer(Modifier.height(12.dp))

                    // Total IPs to scan (distributed across selected ranges)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("کل IP اسکن:", color = HdnGray, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("$randomCount", color = HdnOrange, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.width(8.dp))
                        Text("(${if (selectedRanges.isNotEmpty()) randomCount / selectedRanges.size else 0} از هر رنج)", color = HdnGrayDark, fontSize = 10.sp)
                    }
                    Slider(value = randomCount.toFloat(), onValueChange = { randomCount = it.toInt() },
                        valueRange = 20f..500f, steps = 23,
                        colors = SliderDefaults.colors(thumbColor = HdnOrange, activeTrackColor = HdnOrange, inactiveTrackColor = HdnDarkBorder))

                    // Concurrent connections
                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("تایم‌اوت:", color = HdnGray, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("${timeoutSec}s", color = HdnOrange, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Slider(value = timeoutSec.toFloat(), onValueChange = { timeoutSec = it.toInt() },
                        valueRange = 2f..15f, steps = 12,
                        colors = SliderDefaults.colors(thumbColor = HdnOrange, activeTrackColor = HdnOrange, inactiveTrackColor = HdnDarkBorder))

                    Spacer(Modifier.height(8.dp))

                    if (isScanning) {
                        Button(onClick = { scanJob?.cancel(); isScanning = false }, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = HdnRed, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Close, null); Spacer(Modifier.width(8.dp))
                            Text("Stop", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(onClick = {
                            val config = parsedConfig ?: return@Button
                            isScanning = true
                            results = emptyList()
                            progress = 0
                            skippedRanges = emptyList()
                            currentRange = ""
                            skipCurrentRange = false

                            scanJob = scope.launch {
                                val allResults = mutableListOf<CfIpResult>()

                                // Distribute total IPs across selected ranges
                                val selectedCidrs = CloudflareIpProvider.allCidrRanges.filter { cidr ->
                                    "${cidr.base1}.${cidr.base2}.${cidr.base3}.${cidr.base4}/${cidr.mask}" in selectedRanges
                                }
                                val perRange = if (selectedCidrs.isNotEmpty()) (randomCount / selectedCidrs.size).coerceAtLeast(3) else 10
                                val groupedIps = selectedCidrs.map { cidr ->
                                    val rangeName = "${cidr.base1}.${cidr.base2}.${cidr.base3}.${cidr.base4}/${cidr.mask}"
                                    val random = java.util.Random()
                                    val ips = (1..perRange).map {
                                        val baseIp = (cidr.base1 shl 24) or (cidr.base2 shl 16) or (cidr.base3 shl 8) or cidr.base4
                                        val hostBits = 32 - cidr.mask
                                        val hostCount = ((1 shl hostBits) - 2).coerceAtLeast(1)
                                        val hostPart = random.nextInt(hostCount) + 1
                                        val ip = baseIp or hostPart
                                        "${(ip shr 24) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 8) and 0xFF}.${ip and 0xFF}"
                                    }.distinct()
                                    rangeName to ips
                                }

                                // Calculate total
                                totalCount = groupedIps.sumOf { it.second.size }

                                // Scan each selected range
                                for ((rangeName, ips) in groupedIps) {
                                    if (!isScanning) break

                                    currentRange = rangeName
                                    skipCurrentRange = false

                                    for (ip in ips) {
                                        if (!isScanning) break

                                        // User manually skipped this range
                                        if (skipCurrentRange) {
                                            skippedRanges = skippedRanges + rangeName
                                            progress += (ips.size - ips.indexOf(ip))
                                            break
                                        }

                                        val result = when (testMode) {
                                            CfTestMode.SPEED -> testCloudflareIp(config, ip, timeoutSec * 1000)
                                            CfTestMode.LATENCY -> testCloudflareLatencyOnly(config, ip, timeoutSec * 1000)
                                        }
                                        allResults.add(result)
                                        progress = allResults.size
                                        results = sortCfResults(allResults, sortMode)
                                    }
                                }

                                currentRange = ""
                                isScanning = false
                            }
                        }, modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = parsedConfig != null && selectedRanges.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = HdnOrange, contentColor = Color.Black,
                                disabledContainerColor = HdnDarkBorder), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp))
                            Text("شروع اسکن Clean IP", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (progress > 0) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (totalCount > 0) progress.toFloat() / totalCount else 0f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = HdnOrange, trackColor = HdnDarkBorder)
                        Text("$progress / $totalCount", modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center, color = HdnGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                        // Current range + Skip button
                        if (isScanning && currentRange.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(HdnOrange.copy(alpha = 0.08f))
                                .border(1.dp, HdnOrange.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("🔍 در حال اسکن:", color = HdnGray, fontSize = 10.sp)
                                        Text(currentRange, color = HdnOrange, fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                    Button(onClick = {
                                        skipCurrentRange = true
                                        @Suppress("DEPRECATION")
                                        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                        vibrator?.vibrate(100)
                                    },
                                        colors = ButtonDefaults.buttonColors(containerColor = HdnRed, contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("⏭ Skip", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            // Show next range preview
                            val currentIdx = selectedRanges.toList().indexOf(currentRange)
                            if (currentIdx >= 0 && currentIdx < selectedRanges.size - 1) {
                                Spacer(Modifier.height(4.dp))
                                Text("→ بعدی: ${selectedRanges.toList()[currentIdx + 1]}",
                                    color = HdnGrayDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Skipped ranges list
                        if (skippedRanges.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(HdnRed.copy(alpha = 0.06f))
                                .border(1.dp, HdnRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(12.dp)) {
                                Column {
                                    Text("⏭ Skip شده (${skippedRanges.size} رنج):",
                                        color = HdnRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    skippedRanges.forEach { range ->
                                        Text("  ✕ $range", color = HdnRed.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Results
            if (results.isNotEmpty()) {
                HdnCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☁", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("نتایج Clean IP", fontWeight = FontWeight.Bold, color = HdnWhite, fontFamily = FontFamily.Monospace)
                        }
                        val successCount = results.count { it.status == CfStatus.SUCCESS }
                        Text("✅ $successCount تمیز از ${results.size}", color = HdnGray, fontSize = 11.sp)

                        Spacer(Modifier.height(8.dp))
                        // Sort
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = sortMode == CfSortMode.DOWNLOAD, onClick = {
                                sortMode = CfSortMode.DOWNLOAD; results = sortCfResults(results, sortMode)
                            }, label = { Text("↓ دانلود", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HdnGreen.copy(0.2f), selectedLabelColor = HdnGreen))
                            FilterChip(selected = sortMode == CfSortMode.UPLOAD, onClick = {
                                sortMode = CfSortMode.UPLOAD; results = sortCfResults(results, sortMode)
                            }, label = { Text("↑ آپلود", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HdnCyan.copy(0.2f), selectedLabelColor = HdnCyan))
                            FilterChip(selected = sortMode == CfSortMode.PING, onClick = {
                                sortMode = CfSortMode.PING; results = sortCfResults(results, sortMode)
                            }, label = { Text("🏓 پینگ", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HdnPurple.copy(0.2f), selectedLabelColor = HdnPurple))
                        }

                        Spacer(Modifier.height(8.dp))

                        results.take(30).forEachIndexed { idx, r ->
                            CfIpResultItem(r, idx + 1, parsedConfig, context)
                            if (idx < results.size - 1) HorizontalDivider(Modifier.padding(vertical = 3.dp), color = HdnDarkBorder)
                        }
                    }
                }
            }
        }
        HdnFooter()
    }
}

@Composable
fun CfIpResultItem(result: CfIpResult, rank: Int, config: ConfigData?, context: Context) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        val badgeColor = when {
            rank == 1 && result.status == CfStatus.SUCCESS -> HdnGold
            rank == 2 && result.status == CfStatus.SUCCESS -> Color(0xFFC0C0C0)
            rank == 3 && result.status == CfStatus.SUCCESS -> Color(0xFFCD7F32)
            result.status == CfStatus.SUCCESS -> HdnOrange
            else -> HdnRed
        }
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(0.15f)).border(1.dp, badgeColor.copy(0.4f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center) {
            Text("$rank", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = badgeColor)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(result.ip, color = HdnWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            when (result.status) {
                CfStatus.SUCCESS -> Row {
                    if (result.downloadSpeed > 0) {
                        Text("↓${result.formattedDownload()}", color = HdnGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                    }
                    if (result.uploadSpeed > 0) {
                        Text("↑${result.formattedUpload()}", color = HdnCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("${result.latency}ms", color = when {
                        result.latency < 100 -> HdnGreen
                        result.latency < 300 -> HdnOrange
                        else -> HdnRed
                    }, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                CfStatus.TIMEOUT -> Text("⏱ timeout", color = HdnRed, fontSize = 10.sp)
                CfStatus.FAILED -> Text("✖ failed", color = HdnRed, fontSize = 10.sp)
            }
        }
        // Copy IP
        IconButton(onClick = {
            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("ip", result.ip))
            Toast.makeText(context, "IP کپی شد!", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp), tint = HdnGray)
        }
        // Copy full config with this IP as server
        if (result.status == CfStatus.SUCCESS && config != null) {
            IconButton(onClick = {
                val newConfig = replaceServerIp(config, result.ip)
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("config", newConfig))
                Toast.makeText(context, "کانفیگ + IP جدید کپی شد!", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Share, null, Modifier.size(14.dp), tint = HdnOrange)
            }
        }
    }
}

@Composable
fun CfInfoRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", color = HdnGray, fontSize = 11.sp)
        Text(value, color = HdnWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

fun sortCfResults(results: List<CfIpResult>, mode: CfSortMode): List<CfIpResult> {
    return when (mode) {
        CfSortMode.DOWNLOAD -> results.sortedByDescending {
            if (it.status == CfStatus.SUCCESS) it.downloadSpeed else -1.0
        }
        CfSortMode.UPLOAD -> results.sortedByDescending {
            if (it.status == CfStatus.SUCCESS) it.uploadSpeed else -1.0
        }
        CfSortMode.PING -> results.sortedBy {
            if (it.status == CfStatus.SUCCESS && it.latency > 0) it.latency else Long.MAX_VALUE
        }
    }
}

/**
 * Replace the server/address in config with a Cloudflare clean IP
 */
fun replaceServerIp(config: ConfigData, newIp: String): String {
    return when (config.protocol) {
        com.snifinder.app.model.Protocol.VMESS -> {
            try {
                val encoded = config.rawConfig.removePrefix("vmess://")
                val decoded = String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
                val json = org.json.JSONObject(decoded)
                json.put("add", newIp)
                "vmess://" + android.util.Base64.encodeToString(json.toString().toByteArray(), android.util.Base64.NO_WRAP)
            } catch (e: Exception) { config.rawConfig }
        }
        else -> {
            // For URI-based configs (vless://, trojan://, etc.)
            // Replace host in URI
            config.rawConfig.replace("@${config.server}:", "@$newIp:")
        }
    }
}

/**
 * Test a Cloudflare IP: connect with config's SNI, measure latency + download speed
 */
suspend fun testCloudflareIp(
    config: ConfigData,
    ip: String,
    timeoutMs: Int
): CfIpResult = withContext(Dispatchers.IO) {
    try {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(com.snifinder.app.util.TrustAllManager()), null)
        val factory = sslContext.socketFactory

        val sni = config.originalSni
        val port = config.port

        // === Handshake ===
        val startTime = System.currentTimeMillis()
        val socket = factory.createSocket() as SSLSocket
        socket.soTimeout = timeoutMs
        socket.tcpNoDelay = true

        val sslParams = socket.sslParameters
        sslParams.serverNames = listOf(javax.net.ssl.SNIHostName(sni))
        socket.sslParameters = sslParams

        socket.connect(InetSocketAddress(ip, port), timeoutMs)
        socket.startHandshake()

        val handshake = System.currentTimeMillis() - startTime

        // === Download test ===
        val dlStart = System.currentTimeMillis()
        val output = socket.outputStream
        val input = socket.inputStream

        val request = "GET / HTTP/1.1\r\nHost: $sni\r\nConnection: close\r\nAccept: */*\r\n\r\n"
        output.write(request.toByteArray())
        output.flush()

        val buffer = ByteArray(16384)
        var totalDlBytes = 0L
        try {
            socket.soTimeout = 3000
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                totalDlBytes += read
                if (System.currentTimeMillis() - dlStart > 5000) break
            }
        } catch (e: Exception) { /* timeout or close */ }

        val dlTime = System.currentTimeMillis() - dlStart
        socket.close()

        val dlSpeed = if (dlTime > 0 && totalDlBytes > 0) {
            (totalDlBytes.toDouble() / 1024.0) / (dlTime.toDouble() / 1000.0)
        } else 0.0

        // === Upload test (new connection) ===
        var ulSpeed = 0.0
        try {
            val ulSocket = factory.createSocket() as SSLSocket
            ulSocket.soTimeout = timeoutMs
            ulSocket.tcpNoDelay = true
            val ulParams = ulSocket.sslParameters
            ulParams.serverNames = listOf(javax.net.ssl.SNIHostName(sni))
            ulSocket.sslParameters = ulParams
            ulSocket.connect(InetSocketAddress(ip, port), timeoutMs)
            ulSocket.startHandshake()

            val ulOut = ulSocket.outputStream
            val uploadSize = 131072 // 128KB
            val header = "POST / HTTP/1.1\r\nHost: $sni\r\nContent-Length: $uploadSize\r\nConnection: close\r\n\r\n"
            ulOut.write(header.toByteArray())

            val chunk = ByteArray(8192) { 0x58 }
            var totalUl = 0L
            val ulStart = System.currentTimeMillis()
            while (totalUl < uploadSize) {
                val toWrite = minOf(chunk.size.toLong(), uploadSize - totalUl).toInt()
                ulOut.write(chunk, 0, toWrite)
                totalUl += toWrite
            }
            ulOut.flush()
            val ulTime = System.currentTimeMillis() - ulStart
            ulSocket.close()

            ulSpeed = if (ulTime > 0 && totalUl > 0) {
                (totalUl.toDouble() / 1024.0) / (ulTime.toDouble() / 1000.0)
            } else 0.0
        } catch (e: Exception) { /* upload failed, OK */ }

        CfIpResult(ip = ip, latency = handshake, downloadSpeed = dlSpeed, uploadSpeed = ulSpeed, status = CfStatus.SUCCESS)

    } catch (e: java.net.SocketTimeoutException) {
        CfIpResult(ip = ip, latency = -1, downloadSpeed = 0.0, uploadSpeed = 0.0, status = CfStatus.TIMEOUT)
    } catch (e: Exception) {
        CfIpResult(ip = ip, latency = -1, downloadSpeed = 0.0, uploadSpeed = 0.0, status = CfStatus.FAILED)
    }
}

/**
 * Quick latency-only test: TLS handshake time (Real Delay)
 * No download/upload - just measures how fast TLS connection establishes
 */
suspend fun testCloudflareLatencyOnly(
    config: ConfigData,
    ip: String,
    timeoutMs: Int
): CfIpResult = withContext(Dispatchers.IO) {
    try {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(com.snifinder.app.util.TrustAllManager()), null)
        val factory = sslContext.socketFactory

        val sni = config.originalSni
        val port = config.port

        val startTime = System.currentTimeMillis()
        val socket = factory.createSocket() as SSLSocket
        socket.soTimeout = timeoutMs
        socket.tcpNoDelay = true

        val sslParams = socket.sslParameters
        sslParams.serverNames = listOf(javax.net.ssl.SNIHostName(sni))
        socket.sslParameters = sslParams

        socket.connect(InetSocketAddress(ip, port), timeoutMs)
        socket.startHandshake()

        val latency = System.currentTimeMillis() - startTime
        socket.close()

        CfIpResult(ip = ip, latency = latency, downloadSpeed = 0.0, uploadSpeed = 0.0, status = CfStatus.SUCCESS)

    } catch (e: java.net.SocketTimeoutException) {
        CfIpResult(ip = ip, latency = -1, downloadSpeed = 0.0, uploadSpeed = 0.0, status = CfStatus.TIMEOUT)
    } catch (e: Exception) {
        CfIpResult(ip = ip, latency = -1, downloadSpeed = 0.0, uploadSpeed = 0.0, status = CfStatus.FAILED)
    }
}
