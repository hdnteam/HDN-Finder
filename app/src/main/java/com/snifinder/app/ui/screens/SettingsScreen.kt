package com.snifinder.app.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snifinder.app.ui.theme.*
import com.snifinder.app.ui.components.*
import com.snifinder.app.util.RemoteListUpdater
import com.snifinder.app.util.SniListProvider
import com.snifinder.app.util.CloudflareIpProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUpdating by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<String?>(null) }
    var lastSniUpdate by remember { mutableStateOf(0L) }
    var lastCfUpdate by remember { mutableStateOf(0L) }
    var sniCount by remember { mutableStateOf(SniListProvider.defaultSniList.size) }
    var cfCount by remember { mutableStateOf(CloudflareIpProvider.allCidrRanges.size) }

    var protoInfo by remember { mutableStateOf(RemoteListUpdater.getProtocolInfo(context)) }

    // Load last update info
    LaunchedEffect(Unit) {
        val (sniTime, cfTime) = RemoteListUpdater.getLastUpdateInfo(context)
        lastSniUpdate = sniTime
        lastCfUpdate = cfTime
        // Get actual counts
        val sniList = RemoteListUpdater.getSniList(context)
        sniCount = sniList.size
        val cfRanges = RemoteListUpdater.getCloudflareRanges(context)
        cfCount = cfRanges.size
        protoInfo = RemoteListUpdater.getProtocolInfo(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            // Header
            HdnCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, null, tint = HdnCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("بروزرسانی بانک اطلاعاتی", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = HdnWhite, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("لیست‌ها هر 24 ساعت خودکار بروز می‌شن", color = HdnGray, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))

                    // SNI List status
                    ListInfoCard(
                        title = "بانک SNI",
                        count = sniCount,
                        lastUpdate = lastSniUpdate,
                        icon = "🌐",
                        description = "دامنه‌ها برای تست SNI"
                    )

                    Spacer(Modifier.height(12.dp))

                    // CF Ranges status
                    ListInfoCard(
                        title = "رنج IP کلادفلر",
                        count = cfCount,
                        lastUpdate = lastCfUpdate,
                        icon = "☁",
                        description = "رنج‌های CIDR رسمی Cloudflare"
                    )

                    Spacer(Modifier.height(12.dp))

                    // Protocols status
                    ListInfoCard(
                        title = "پروتکل‌ها",
                        count = protoInfo.protocols.size,
                        lastUpdate = protoInfo.lastUpdate,
                        icon = "🔐",
                        description = "Xray: ${protoInfo.xrayVersion} | V2RayNG: ${protoInfo.v2rayNgVersion}"
                    )

                    Spacer(Modifier.height(12.dp))

                    // Network test methods
                    ListInfoCard(
                        title = "تست‌های شبکه (Net)",
                        count = protoInfo.protocols.size + 23, // protocols + ports
                        lastUpdate = protoInfo.lastUpdate,
                        icon = "📡",
                        description = "پورت‌ها + پروتکل‌ها + VPN — بروز با Xray-core"
                    )

                    Spacer(Modifier.height(16.dp))

                    // Update button
                    Button(onClick = {
                        isUpdating = true
                        updateResult = null
                        scope.launch {
                            val result = RemoteListUpdater.forceUpdate(context)
                            isUpdating = false
                            updateResult = buildString {
                                if (result.sniUpdated) append("✅ SNI: ${result.sniCount} دامنه بروز شد\n")
                                else append("⚠ SNI: بروزرسانی نشد (از لیست قبلی استفاده میشه)\n")
                                if (result.cfUpdated) append("✅ CF IP: ${result.cfCount} رنج بروز شد\n")
                                else append("⚠ CF IP: بروزرسانی نشد (از لیست قبلی استفاده میشه)\n")
                                if (result.protoUpdated) append("✅ پروتکل‌ها + تست شبکه: Xray ${result.xrayVersion} | V2RayNG ${result.v2rayNgVersion}")
                                else append("⚠ پروتکل‌ها: بروزرسانی نشد")
                            }
                            // Refresh info
                            val (sniTime, cfTime) = RemoteListUpdater.getLastUpdateInfo(context)
                            lastSniUpdate = sniTime
                            lastCfUpdate = cfTime
                            // Merge SNI: keep user's custom + add new from remote
                            val prefs = context.getSharedPreferences("hdn_finder_lists", Context.MODE_PRIVATE)
                            val existingRaw = prefs.getString("sni_list", null)
                            if (result.sniUpdated && existingRaw != null) {
                                val existingList = existingRaw.split("\n").filter { it.isNotBlank() }
                                val freshList = RemoteListUpdater.getSniList(context)
                                val merged = (existingList + freshList).distinct()
                                prefs.edit().putString("sni_list", merged.joinToString("\n")).apply()
                                sniCount = merged.size
                            } else {
                                val sniList = RemoteListUpdater.getSniList(context)
                                sniCount = sniList.size
                            }
                            val cfRanges = RemoteListUpdater.getCloudflareRanges(context)
                            cfCount = cfRanges.size
                            protoInfo = RemoteListUpdater.getProtocolInfo(context)
                        }
                    }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = !isUpdating,
                        colors = ButtonDefaults.buttonColors(containerColor = HdnCyan, contentColor = Color.Black,
                            disabledContainerColor = HdnDarkBorder), shape = RoundedCornerShape(10.dp)) {
                        if (isUpdating) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("در حال بروزرسانی...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp))
                            Text("بروزرسانی دستی", fontWeight = FontWeight.Bold)
                        }
                    }

                    updateResult?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = HdnWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // === SNI Manager ===
            SniManagerSection(context)

            Spacer(Modifier.height(16.dp))

            // Info about sources
            HdnCard {
                Column(Modifier.padding(16.dp)) {
                    Text("منابع بروزرسانی", fontWeight = FontWeight.Bold, color = HdnWhite)
                    Spacer(Modifier.height(8.dp))
                    SourceRow("SNI List", "GitHub Community Lists")
                    SourceRow("CF Ranges", "cloudflare.com/ips-v4 (رسمی)")
                    SourceRow("Protocols", "github.com/XTLS/Xray-core")
                    SourceRow("V2RayNG", "github.com/2dust/v2rayNG")
                    Spacer(Modifier.height(8.dp))
                    Text("• لیست SNI از ریپازیتوری‌های GitHub دریافت میشه", color = HdnGray, fontSize = 11.sp)
                    Text("• رنج IP کلادفلر مستقیم از سایت رسمی CF", color = HdnGray, fontSize = 11.sp)
                    Text("• پروتکل‌ها از Xray-core و V2RayNG releases", color = HdnGray, fontSize = 11.sp)
                    Text("• اگه اینترنت نباشه، از آخرین نسخه ذخیره‌شده استفاده میکنه", color = HdnGray, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Protocols list
            if (protoInfo.protocols.isNotEmpty()) {
                HdnCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("🔐 پروتکل‌های پشتیبانی شده", fontWeight = FontWeight.Bold, color = HdnWhite)
                        Spacer(Modifier.height(4.dp))
                        Text("بر اساس آخرین نسخه Xray-core و V2RayNG", color = HdnGray, fontSize = 10.sp)
                        Spacer(Modifier.height(10.dp))
                        protoInfo.protocols.forEachIndexed { idx, proto ->
                            Text("${idx + 1}. $proto", color = HdnWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // === App Update from GitHub ===
            AppUpdateSection(context)

            Spacer(Modifier.height(16.dp))

            // App info
            HdnCard {
                Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HDN Finder", color = HdnCyan, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    Text("v1.0.0", color = HdnGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    Text("POWERED BY HDNTEAM", color = HdnCyan.copy(0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    Text("hdnteam@gmail.com", color = HdnGray.copy(0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        HdnFooter()
    }
}

@Composable
fun ListInfoCard(title: String, count: Int, lastUpdate: Long, icon: String, description: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(HdnDarkBg).border(1.dp, HdnDarkBorder, RoundedCornerShape(10.dp))
        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = HdnWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(description, color = HdnGray, fontSize = 11.sp)
            if (lastUpdate > 0) {
                val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(lastUpdate))
                Text("آخرین بروزرسانی: $dateStr", color = HdnGreen.copy(0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            } else {
                Text("بروزرسانی نشده — لیست داخلی فعال", color = HdnOrange, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", color = HdnCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
            Text("آیتم", color = HdnGray, fontSize = 9.sp)
        }
    }
}

@Composable
fun SourceRow(label: String, source: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", color = HdnGray, fontSize = 12.sp)
        Text(source, color = HdnWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun SniManagerSection(context: Context) {
    var customSniText by remember { mutableStateOf("") }
    var currentCount by remember { mutableStateOf(0) }
    var showList by remember { mutableStateOf(false) }
    var currentList by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val list = RemoteListUpdater.getSniList(context)
        currentCount = list.size
        currentList = list
    }

    HdnCard(glowColor = HdnGreen) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌐", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text("مدیریت بانک SNI", fontWeight = FontWeight.Bold, color = HdnWhite, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(4.dp))
            Text("$currentCount دامنه در بانک فعلی", color = HdnGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(12.dp))

            // Add custom SNIs
            Text("اضافه کردن SNI جدید:", color = HdnGray, fontSize = 12.sp)
            Text("متن یا URL لیست وارد کنید", color = HdnGrayDark, fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = customSniText, onValueChange = { customSniText = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("هر خط یک دامنه:\ngoogle.com\ncloudflare.com\nexample.ir", color = HdnGrayDark) },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = HdnGreen),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HdnGreen.copy(0.6f), unfocusedBorderColor = HdnDarkBorder,
                    cursorColor = HdnCyan, focusedContainerColor = HdnDarkBg.copy(0.5f), unfocusedContainerColor = HdnDarkBg.copy(0.5f)),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Add button
                Button(onClick = {
                    val newSnis = customSniText.replace(",", "\n").split("\n")
                        .map { it.trim() }.filter { it.isNotBlank() && it.contains(".") }
                    if (newSnis.isNotEmpty()) {
                        scope.launch {
                            val existing = RemoteListUpdater.getSniList(context)
                            val merged = (existing + newSnis).distinct()
                            // Save merged list
                            val prefs = context.getSharedPreferences("hdn_finder_lists", Context.MODE_PRIVATE)
                            prefs.edit().putString("sni_list", merged.joinToString("\n")).apply()
                            currentCount = merged.size
                            currentList = merged
                            customSniText = ""
                            Toast.makeText(context, "${newSnis.size} دامنه اضافه شد — کل: $currentCount", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, modifier = Modifier.weight(1f),
                    enabled = customSniText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = HdnGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("اضافه کن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Reset button
                Button(onClick = {
                    scope.launch {
                        val prefs = context.getSharedPreferences("hdn_finder_lists", Context.MODE_PRIVATE)
                        prefs.edit().remove("sni_list").remove("last_update_sni").apply()
                        val list = com.snifinder.app.util.SniListProvider.defaultSniList
                        currentCount = list.size
                        currentList = list
                        Toast.makeText(context, "بازنشانی به لیست پیش‌فرض ($currentCount)", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = HdnDarkBorder, contentColor = HdnWhite),
                    shape = RoundedCornerShape(8.dp)) {
                    Text("ریست", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Import from URL
            var importUrl by remember { mutableStateOf("") }
            OutlinedTextField(
                value = importUrl, onValueChange = { importUrl = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("URL لیست SNI (txt)...", color = HdnGrayDark) },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = HdnCyan),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HdnCyan.copy(0.6f), unfocusedBorderColor = HdnDarkBorder,
                    cursorColor = HdnCyan, focusedContainerColor = HdnDarkBg.copy(0.5f), unfocusedContainerColor = HdnDarkBg.copy(0.5f)),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(Modifier.height(6.dp))
            Button(onClick = {
                if (importUrl.isBlank()) return@Button
                scope.launch {
                    Toast.makeText(context, "در حال دریافت...", Toast.LENGTH_SHORT).show()
                    try {
                        val fetched = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val url = java.net.URL(importUrl.trim())
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.connectTimeout = 10000
                            conn.readTimeout = 10000
                            val text = conn.inputStream.bufferedReader().readText()
                            conn.disconnect()
                            text.split("\n").map { it.trim() }.filter { it.isNotBlank() && it.contains(".") && !it.startsWith("#") }
                        }
                        if (fetched.isNotEmpty()) {
                            val existing = RemoteListUpdater.getSniList(context)
                            val merged = (existing + fetched).distinct()
                            val prefs = context.getSharedPreferences("hdn_finder_lists", Context.MODE_PRIVATE)
                            prefs.edit().putString("sni_list", merged.joinToString("\n")).apply()
                            currentCount = merged.size
                            currentList = merged
                            importUrl = ""
                            Toast.makeText(context, "✅ ${fetched.size} دامنه اضافه شد — کل: $currentCount", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "⚠ لیست خالی بود", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "❌ خطا: ${e.message?.take(40)}", Toast.LENGTH_SHORT).show()
                    }
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = importUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HdnPurple, contentColor = Color.White,
                    disabledContainerColor = HdnDarkBorder), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Link, null, Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Import از URL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            // Update SNI button (merges remote + keeps custom)
            Button(onClick = {
                scope.launch {
                    Toast.makeText(context, "در حال بروزرسانی...", Toast.LENGTH_SHORT).show()
                    val currentSaved = RemoteListUpdater.getSniList(context)
                    // Fetch remote and merge (keeps user's custom entries)
                    val result = RemoteListUpdater.forceUpdate(context)
                    if (result.sniUpdated) {
                        // Merge: remote + whatever user already had
                        val remoteFresh = RemoteListUpdater.getSniList(context)
                        val merged = (currentSaved + remoteFresh).distinct()
                        val prefs = context.getSharedPreferences("hdn_finder_lists", Context.MODE_PRIVATE)
                        prefs.edit().putString("sni_list", merged.joinToString("\n")).apply()
                        currentCount = merged.size
                        currentList = merged
                        Toast.makeText(context, "✅ بروز شد! کل: $currentCount (دستی‌ها حفظ شدن)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "⚠ بروزرسانی نشد — از لیست قبلی استفاده میشه", Toast.LENGTH_SHORT).show()
                    }
                }
            }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HdnCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("بروزرسانی آنلاین (بدون حذف دستی‌ها)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            // Show/Hide full list
            Row(Modifier.fillMaxWidth().clickable { showList = !showList }
                .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (showList) "▼ مخفی کردن لیست" else "▶ نمایش کل لیست ($currentCount)",
                    color = HdnCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (showList) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HdnDarkBg)
                    .border(1.dp, HdnDarkBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())) {
                    Text(
                        currentList.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n"),
                        color = HdnGrayLight, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun AppUpdateSection(context: android.content.Context) {
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val githubRepo = "hdnteam/HDN-Finder"
    val currentVersion = "1.0.0"

    HdnCard(glowColor = HdnOrange) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔄", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("آپدیت برنامه", fontWeight = FontWeight.Bold, color = HdnWhite, fontSize = 14.sp)
                    Text("نسخه فعلی: v$currentVersion", color = HdnGrayDark, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(12.dp))

            if (updateInfo.isNotEmpty()) {
                Text(updateInfo, color = if (downloadUrl.isNotEmpty()) HdnGreen else HdnGray, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
            }

            if (downloadUrl.isNotEmpty()) {
                // Download update button
                Button(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl))
                    context.startActivity(intent)
                }, modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HdnGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)) {
                    Text("دانلود آپدیت", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                // Check for update button
                Button(onClick = {
                    isChecking = true; updateInfo = ""
                    scope.launch {
                        val result = checkForUpdate(githubRepo, currentVersion)
                        updateInfo = result.first
                        downloadUrl = result.second
                        isChecking = false
                    }
                }, modifier = Modifier.fillMaxWidth().height(44.dp), enabled = !isChecking,
                    colors = ButtonDefaults.buttonColors(containerColor = HdnOrange, contentColor = Color.Black,
                        disabledContainerColor = HdnDarkBorder),
                    shape = RoundedCornerShape(10.dp)) {
                    if (isChecking) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isChecking) "بررسی..." else "بررسی آپدیت", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

suspend fun checkForUpdate(repo: String, currentVersion: String): Pair<String, String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val url = java.net.URL("https://api.github.com/repos/$repo/releases/latest")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("Accept", "application/json")
        conn.useCaches = false

        if (conn.responseCode != 200) {
            conn.disconnect()
            return@withContext "خطا در بررسی (${conn.responseCode})" to ""
        }

        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        val json = org.json.JSONObject(response)
        val latestTag = json.optString("tag_name", "").removePrefix("v")
        val body = json.optString("body", "")

        // Find APK download URL
        var apkUrl = ""
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url", "")
                    break
                }
            }
        }

        // If no APK in assets, use release page
        if (apkUrl.isEmpty()) {
            apkUrl = json.optString("html_url", "")
        }

        // Compare versions
        if (latestTag.isNotEmpty() && latestTag != currentVersion && compareVersions(latestTag, currentVersion) > 0) {
            "✅ نسخه جدید: v$latestTag\n$body".take(200) to apkUrl
        } else {
            "شما آخرین نسخه رو دارید (v$currentVersion)" to ""
        }
    } catch (e: Exception) {
        "خطا: ${e.message?.take(50)}" to ""
    }
}

private fun compareVersions(v1: String, v2: String): Int {
    val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(parts1.size, parts2.size)) {
        val p1 = parts1.getOrElse(i) { 0 }
        val p2 = parts2.getOrElse(i) { 0 }
        if (p1 != p2) return p1.compareTo(p2)
    }
    return 0
}
