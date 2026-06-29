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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snifinder.app.model.SniResult
import com.snifinder.app.model.SniStatus
import com.snifinder.app.model.SpeedResult
import com.snifinder.app.model.SpeedStatus
import com.snifinder.app.ui.components.*
import com.snifinder.app.ui.theme.*
import com.snifinder.app.viewmodel.MainViewModel
import com.snifinder.app.viewmodel.ScanMode
import com.snifinder.app.viewmodel.SortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            ConfigSection(uiState, viewModel)
            Spacer(Modifier.height(14.dp))
            ScanSection(uiState, viewModel)
            Spacer(Modifier.height(14.dp))

            if (uiState.scanMode == ScanMode.SPEED_TEST && uiState.speedResults.isNotEmpty()) {
                SpeedResults(uiState, viewModel, context)
            } else if (uiState.scanMode == ScanMode.LATENCY && uiState.latencyResults.isNotEmpty()) {
                LatencyResults(uiState.latencyResults, viewModel, context)
            }
            Spacer(Modifier.height(16.dp))
        }
        HdnFooter()
    }
}

@Composable
private fun ConfigSection(uiState: com.snifinder.app.viewmodel.UiState, viewModel: MainViewModel) {
    HdnGlowCard(glowColor = HdnCyan) {
        Column(Modifier.padding(18.dp)) {
            SectionTitle("🔐", "کانفیگ")
            Spacer(Modifier.height(4.dp))
            Text("VLESS • VMess • Trojan • SS • Hysteria2 | Reality • gRPC • WS",
                color = HdnGrayDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.configInput, onValueChange = { viewModel.updateConfigInput(it) },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                placeholder = { Text("کانفیگ خود را paste کنید...", color = HdnGrayDark) },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = HdnGreen),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HdnCyan.copy(0.6f), unfocusedBorderColor = HdnDarkBorder,
                    cursorColor = HdnCyan, focusedContainerColor = HdnDarkBg.copy(0.5f), unfocusedContainerColor = HdnDarkBg.copy(0.5f)),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            HdnButton("تجزیه کانفیگ", onClick = { viewModel.parseConfig() }, enabled = uiState.configInput.isNotBlank(),
                icon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) })

            uiState.parseError?.let {
                Spacer(Modifier.height(8.dp))
                Text("⚠ $it", color = HdnRed, fontSize = 11.sp)
            }

            uiState.parsedConfig?.let { config ->
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(HdnGreen.copy(alpha = 0.06f))
                    .border(1.dp, HdnGreen.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(12.dp)) {
                    Column {
                        Text("✅ شناسایی شد", fontWeight = FontWeight.Bold, color = HdnGreen, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        InfoRow("Protocol", config.protocol.name)
                        InfoRow("Transport", config.transport.name)
                        InfoRow("Security", config.security)
                        InfoRow("Server", "${config.server}:${config.port}")
                        InfoRow("SNI", config.originalSni)
                        if (config.flow.isNotEmpty()) InfoRow("Flow", config.flow)
                        if (config.fp.isNotEmpty()) InfoRow("Fingerprint", config.fp)
                        if (config.pbk.isNotEmpty()) InfoRow("PublicKey", config.pbk.take(12) + "...")
                        if (config.sid.isNotEmpty()) InfoRow("ShortID", config.sid)
                        if (config.path.isNotEmpty()) InfoRow("Path", config.path)
                        if (config.host.isNotEmpty()) InfoRow("Host", config.host)
                        if (config.alpn.isNotEmpty()) InfoRow("ALPN", config.alpn)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanSection(uiState: com.snifinder.app.viewmodel.UiState, viewModel: MainViewModel) {
    HdnCard {
        Column(Modifier.padding(18.dp)) {
            SectionTitle("⚙", "تنظیمات اسکن")
            Spacer(Modifier.height(14.dp))

            // Scan mode
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HdnChip("⚡ سرعت واقعی", uiState.scanMode == ScanMode.SPEED_TEST, HdnCyan) { viewModel.setScanMode(ScanMode.SPEED_TEST) }
                HdnChip("🏓 Latency فقط", uiState.scanMode == ScanMode.LATENCY, HdnPurple) { viewModel.setScanMode(ScanMode.LATENCY) }
            }

            Spacer(Modifier.height(14.dp))

            // Custom list
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("لیست سفارشی", color = HdnWhite, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Switch(checked = uiState.useCustomList, onCheckedChange = { viewModel.toggleCustomList(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = HdnDarkBg, checkedTrackColor = HdnCyan,
                        uncheckedThumbColor = HdnGrayDark, uncheckedTrackColor = HdnDarkBorder))
            }
            if (uiState.useCustomList) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = uiState.customSniInput, onValueChange = { viewModel.updateCustomSniInput(it) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    placeholder = { Text("هر SNI در یک خط", color = HdnGrayDark) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = HdnWhite),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HdnPurple.copy(0.6f), unfocusedBorderColor = HdnDarkBorder,
                        cursorColor = HdnCyan, focusedContainerColor = HdnDarkBg.copy(0.5f), unfocusedContainerColor = HdnDarkBg.copy(0.5f)),
                    shape = RoundedCornerShape(10.dp))
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Timeout:", color = HdnGray, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text("${uiState.timeoutSeconds}s", color = HdnCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Slider(value = uiState.timeoutSeconds.toFloat(), onValueChange = { viewModel.updateTimeout(it.toInt()) },
                valueRange = 3f..30f, steps = 26,
                colors = SliderDefaults.colors(thumbColor = HdnCyan, activeTrackColor = HdnCyan, inactiveTrackColor = HdnDarkBorder))

            Spacer(Modifier.height(10.dp))
            if (uiState.isScanning) {
                HdnButton("Stop", onClick = { viewModel.stopScan() }, color = HdnRed,
                    icon = { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) })
            } else {
                HdnButton(
                    if (uiState.scanMode == ScanMode.SPEED_TEST) "شروع تست سرعت" else "شروع تست Latency",
                    onClick = { viewModel.startScan() }, enabled = uiState.parsedConfig != null,
                    icon = { Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)) })
            }

            if (uiState.isScanning || uiState.progress > 0) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { if (uiState.totalCount > 0) uiState.progress.toFloat() / uiState.totalCount else 0f },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = HdnCyan, trackColor = HdnDarkBorder)
                Spacer(Modifier.height(4.dp))
                Text("${uiState.progress} / ${uiState.totalCount}", Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center, color = HdnGrayDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun SpeedResults(uiState: com.snifinder.app.viewmodel.UiState, viewModel: MainViewModel, context: Context) {
    HdnGlowCard(glowColor = HdnGold) {
        Column(Modifier.padding(18.dp)) {
            SectionTitle("⚡", "نتایج")
            val s = uiState.speedResults.count { it.status == SpeedStatus.SUCCESS }
            Text("$s موفق از ${uiState.speedResults.size}", color = HdnGray, fontSize = 11.sp)

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HdnChip("↓ دانلود", uiState.sortMode == SortMode.DOWNLOAD, HdnGreen) { viewModel.setSortMode(SortMode.DOWNLOAD) }
                HdnChip("↑ آپلود", uiState.sortMode == SortMode.UPLOAD, HdnCyan) { viewModel.setSortMode(SortMode.UPLOAD) }
                HdnChip("🏓 پینگ", uiState.sortMode == SortMode.PING, HdnPurple) { viewModel.setSortMode(SortMode.PING) }
            }
            Spacer(Modifier.height(10.dp))

            uiState.speedResults.forEachIndexed { idx, result ->
                SpeedItem(result, idx + 1, viewModel, context)
                if (idx < uiState.speedResults.size - 1) HorizontalDivider(Modifier.padding(vertical = 3.dp), color = HdnDarkBorder.copy(0.5f))
            }
        }
    }
}

@Composable
private fun SpeedItem(result: SpeedResult, rank: Int, viewModel: MainViewModel, context: Context) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        RankBadge(rank, result.status == SpeedStatus.SUCCESS)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(result.sni, color = HdnWhite, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            when (result.status) {
                SpeedStatus.SUCCESS -> Row {
                    Text("↓${result.formattedDownloadSpeed()}", color = HdnGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text("↑${result.formattedUploadSpeed()}", color = HdnCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text("${result.handshakeMs}ms", color = HdnGrayDark, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                SpeedStatus.TIMEOUT -> Text("⏱ TIMEOUT", color = HdnRed, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                SpeedStatus.FAILED -> Text("✖ FAILED", color = HdnRed, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                else -> Text("...", color = HdnGray)
            }
        }
        CopyButtons(result.sni, result.status == SpeedStatus.SUCCESS, viewModel, context)
    }
}

@Composable
private fun LatencyResults(results: List<SniResult>, viewModel: MainViewModel, context: Context) {
    HdnGlowCard(glowColor = HdnPurple) {
        Column(Modifier.padding(18.dp)) {
            SectionTitle("🏓", "نتایج Latency")
            val s = results.count { it.status == SniStatus.SUCCESS }
            Text("$s موفق — کمترین پینگ اول", color = HdnGray, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))

            results.forEachIndexed { idx, result ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    RankBadge(idx + 1, result.status == SniStatus.SUCCESS)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(result.sni, color = HdnWhite, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(when (result.status) {
                            SniStatus.SUCCESS -> "${result.latency} ms"
                            SniStatus.TIMEOUT -> "⏱ TIMEOUT"
                            SniStatus.FAILED -> "✖ FAILED"
                            else -> "..."
                        }, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            color = when (result.status) {
                                SniStatus.SUCCESS -> when { result.latency < 200 -> HdnGreen; result.latency < 500 -> HdnOrange; else -> HdnRed }
                                else -> HdnRed
                            })
                    }
                    CopyButtons(result.sni, result.status == SniStatus.SUCCESS, viewModel, context)
                }
                if (idx < results.size - 1) HorizontalDivider(Modifier.padding(vertical = 3.dp), color = HdnDarkBorder.copy(0.5f))
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int, isSuccess: Boolean) {
    val color = when {
        rank == 1 && isSuccess -> HdnGold
        rank == 2 && isSuccess -> Color(0xFFC0C0C0)
        rank == 3 && isSuccess -> Color(0xFFCD7F32)
        isSuccess -> HdnCyan
        else -> HdnRed
    }
    Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
        .background(color.copy(alpha = 0.1f))
        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center) {
        Text("$rank", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color)
    }
}

@Composable
private fun CopyButtons(sni: String, showConfig: Boolean, viewModel: MainViewModel, context: Context) {
    IconButton(onClick = {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("sni", sni))
        Toast.makeText(context, "SNI copied!", Toast.LENGTH_SHORT).show()
    }, modifier = Modifier.size(32.dp)) {
        Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp), tint = HdnGrayDark)
    }
    if (showConfig) {
        IconButton(onClick = {
            val c = viewModel.getConfigWithSni(sni)
            if (c != null) {
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("config", c))
                Toast.makeText(context, "Config + SNI copied!", Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Share, null, Modifier.size(14.dp), tint = HdnCyan)
        }
    }
}
