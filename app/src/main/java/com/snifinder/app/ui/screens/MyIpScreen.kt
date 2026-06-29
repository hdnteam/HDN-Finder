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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snifinder.app.ui.theme.*
import com.snifinder.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class IpInfo(
    val ip: String = "",
    val country: String = "",
    val countryCode: String = "",
    val regionName: String = "",
    val city: String = "",
    val zip: String = "",
    val lat: String = "",
    val lon: String = "",
    val timezone: String = "",
    val isp: String = "",
    val org: String = "",
    val asn: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@Composable
fun MyIpScreen() {
    var myIpInfo by remember { mutableStateOf(IpInfo()) }
    var customIpInfo by remember { mutableStateOf(IpInfo()) }
    var manualIp by remember { mutableStateOf("") }
    var isLoadingMy by remember { mutableStateOf(false) }
    var isLoadingCustom by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Auto-fetch my IP on first load
    LaunchedEffect(Unit) {
        isLoadingMy = true
        myIpInfo = fetchIpDetails(null)
        isLoadingMy = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            // === My IP Section ===
            HdnGlowCard(glowColor = HdnCyan) {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("🌐", "My IP Address")
                    Spacer(Modifier.height(14.dp))

                    if (isLoadingMy) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = HdnCyan, strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("در حال دریافت...", color = HdnGray)
                        }
                    } else if (myIpInfo.ip.isNotEmpty()) {
                        // Detect VPN status
                        val isVpnOn = remember {
                            try {
                                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                                val net = cm?.activeNetwork
                                val caps = cm?.getNetworkCapabilities(net)
                                caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true
                            } catch (e: Exception) { false }
                        }

                        // Big IP display
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isVpnOn) {
                                Text("🟢 VPN فعال — IP خروجی VPN:", color = HdnGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("آی‌پی خروجی شما (ISP)", color = HdnGray, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(myIpInfo.ip, color = HdnCyan, fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)

                            // Warning if VPN is on but IP looks Iranian
                            if (isVpnOn && (myIpInfo.country.contains("IR") || myIpInfo.country.contains("Iran", true))) {
                                Spacer(Modifier.height(8.dp))
                                Card(colors = CardDefaults.cardColors(containerColor = HdnRed.copy(0.1f)), shape = RoundedCornerShape(8.dp)) {
                                    Text("⚠ VPN فعاله ولی IP هنوز ایرانه!\nV2RayNG → Settings → VPN Mode فعال کن\nیا Per-App Proxy → HDN Finder تیک بزن",
                                        color = HdnRed, fontSize = 10.sp, modifier = Modifier.padding(10.dp), lineHeight = 15.sp)
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Button(onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("ip", myIpInfo.ip))
                                Toast.makeText(context, "IP copied!", Toast.LENGTH_SHORT).show()
                            }, colors = ButtonDefaults.buttonColors(containerColor = HdnCyan.copy(0.12f), contentColor = HdnCyan),
                                shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy IP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Detailed info table
                        if (myIpInfo.country.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = HdnDarkBorder)
                            Spacer(Modifier.height(10.dp))
                            IpDetailTable(myIpInfo)
                        }
                    }

                    myIpInfo.error?.let {
                        Text("⚠ $it", color = HdnRed, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(12.dp))
                    HdnButton("بروزرسانی", onClick = {
                        isLoadingMy = true
                        scope.launch {
                            myIpInfo = fetchIpDetails(null)
                            isLoadingMy = false
                        }
                    }, enabled = !isLoadingMy, color = HdnDarkBorderLight,
                        icon = { Icon(Icons.Default.Refresh, null, Modifier.size(16.dp)) })
                }
            }

            Spacer(Modifier.height(16.dp))

            // === Custom IP Lookup Section ===
            HdnCard {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("🔍", "IP Lookup")
                    Spacer(Modifier.height(4.dp))
                    Text("آی‌پی وارد کن تا اطلاعات کامل بگیری", color = HdnGrayDark, fontSize = 11.sp)
                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = manualIp, onValueChange = { manualIp = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 1.1.1.1 or 8.8.8.8", color = HdnGrayDark) },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = HdnGreen),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HdnPurple.copy(0.6f), unfocusedBorderColor = HdnDarkBorder,
                            cursorColor = HdnCyan, focusedContainerColor = HdnDarkBg.copy(0.5f), unfocusedContainerColor = HdnDarkBg.copy(0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    HdnButton("Lookup", onClick = {
                        if (manualIp.isBlank()) return@HdnButton
                        isLoadingCustom = true
                        scope.launch {
                            customIpInfo = fetchIpDetails(manualIp.trim())
                            isLoadingCustom = false
                        }
                    }, enabled = manualIp.isNotBlank() && !isLoadingCustom, color = HdnPurple,
                        icon = {
                            if (isLoadingCustom) CircularProgressIndicator(Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            else Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                        })

                    // Custom IP result
                    if (customIpInfo.ip.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = HdnDarkBorder)
                        Spacer(Modifier.height(10.dp))

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(customIpInfo.ip, color = HdnPurple, fontSize = 18.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("ip", customIpInfo.ip))
                                Toast.makeText(context, "IP copied!", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp), tint = HdnPurple)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        IpDetailTable(customIpInfo)
                    }
                    customIpInfo.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("⚠ $it", color = HdnRed, fontSize = 12.sp)
                    }
                }
            }
        }
        HdnFooter()
    }
}

@Composable
fun IpDetailTable(info: IpInfo) {
    Column {
        if (info.country.isNotEmpty()) IpRow("🌍 Country", "${info.country} (${info.countryCode})")
        if (info.regionName.isNotEmpty()) IpRow("📍 Region", info.regionName)
        if (info.city.isNotEmpty()) IpRow("🏙 City", info.city)
        if (info.zip.isNotEmpty()) IpRow("📮 Zip", info.zip)
        if (info.lat.isNotEmpty()) IpRow("📌 Coordinates", "${info.lat}, ${info.lon}")
        if (info.timezone.isNotEmpty()) IpRow("🕐 Timezone", info.timezone)
        if (info.isp.isNotEmpty()) IpRow("🌐 ISP", info.isp)
        if (info.org.isNotEmpty()) IpRow("🏢 Organization", info.org)
        if (info.asn.isNotEmpty()) IpRow("🔗 ASN", info.asn)
    }
}

@Composable
fun IpRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = HdnGray, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Text(value, color = HdnWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

/**
 * Fetches IP info using ip-api.com (same fields as the PHP script)
 * API: http://ip-api.com/json/{ip}?fields=status,message,country,countryCode,regionName,city,zip,lat,lon,timezone,isp,org,as,query
 * 
 * For detecting own IP (when customIp is null): uses api64.ipify.org first, then queries ip-api.com
 */
suspend fun fetchIpDetails(customIp: String?): IpInfo = withContext(Dispatchers.IO) {
    try {
        val ipToQuery: String

        if (customIp != null) {
            ipToQuery = customIp
        } else {
            // Get public IP - tries multiple services to ensure VPN IP is detected
            // These services return YOUR exit IP (the VPN server IP when connected)
            ipToQuery = detectPublicIp()
                ?: return@withContext IpInfo(isLoading = false, error = "خطا در دریافت IP — اینترنت یا VPN بررسی شود")
        }

        // Query ip-api.com for full details (use IP address directly to bypass DNS block)
        val apiUrl = URL("http://208.95.112.1/json/$ipToQuery")
        val conn = apiUrl.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.useCaches = false
        conn.setRequestProperty("Cache-Control", "no-cache")
        conn.setRequestProperty("Pragma", "no-cache")
        conn.setRequestProperty("Host", "ip-api.com")

        if (conn.responseCode != 200) {
            conn.disconnect()
            return@withContext IpInfo(ip = ipToQuery, isLoading = false, error = "API پاسخ نداد (${conn.responseCode})")
        }

        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        val json = JSONObject(response)

        if (json.optString("status") == "fail") {
            return@withContext IpInfo(ip = ipToQuery, isLoading = false,
                error = json.optString("message", "IP نامعتبر"))
        }

        IpInfo(
            ip = json.optString("query", ipToQuery),
            country = json.optString("country", ""),
            countryCode = json.optString("countryCode", ""),
            regionName = json.optString("regionName", ""),
            city = json.optString("city", ""),
            zip = json.optString("zip", ""),
            lat = json.optString("lat", ""),
            lon = json.optString("lon", ""),
            timezone = json.optString("timezone", ""),
            isp = json.optString("isp", ""),
            org = json.optString("org", ""),
            asn = json.optString("as", ""),
            isLoading = false,
            error = null
        )
    } catch (e: Exception) {
        // Primary API failed - try alternative APIs
        val ip = customIp ?: detectPublicIp()
        if (ip != null) {
            val altInfo = tryAlternativeIpApis(ip)
            if (altInfo != null) return@withContext altInfo
            IpInfo(ip = ip, isLoading = false, error = "فقط IP دریافت شد — جزئیات در دسترس نیست")
        } else {
            IpInfo(isLoading = false, error = "خطا: ${e.message?.take(50)}")
        }
    }
}

/**
 * Try multiple alternative IP info APIs for maximum detail
 */
private fun tryAlternativeIpApis(ip: String): IpInfo? {
    // 1. ipwho.is — HTTPS, free, detailed
    try {
        val url = URL("https://ipwho.is/$ip")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000; conn.readTimeout = 5000; conn.useCaches = false
        if (conn.responseCode == 200) {
            val json = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
            conn.disconnect()
            if (json.optBoolean("success", false)) {
                return IpInfo(
                    ip = json.optString("ip", ip),
                    country = json.optString("country", ""),
                    countryCode = json.optString("country_code", ""),
                    regionName = json.optString("region", ""),
                    city = json.optString("city", ""),
                    zip = json.optString("postal", ""),
                    lat = json.optDouble("latitude", 0.0).toString(),
                    lon = json.optDouble("longitude", 0.0).toString(),
                    timezone = json.optJSONObject("timezone")?.optString("id", "") ?: "",
                    isp = json.optJSONObject("connection")?.optString("isp", "") ?: "",
                    org = json.optJSONObject("connection")?.optString("org", "") ?: "",
                    asn = json.optJSONObject("connection")?.optString("asn", "") ?: "",
                    isLoading = false, error = null
                )
            }
        } else conn.disconnect()
    } catch (e: Exception) { }

    // 2. ipapi.co — HTTPS
    try {
        val url = URL("https://ipapi.co/$ip/json/")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000; conn.readTimeout = 5000; conn.useCaches = false
        conn.setRequestProperty("User-Agent", "HDNFinder/1.0")
        if (conn.responseCode == 200) {
            val json = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
            conn.disconnect()
            if (!json.has("error")) {
                return IpInfo(
                    ip = json.optString("ip", ip),
                    country = json.optString("country_name", ""),
                    countryCode = json.optString("country_code", ""),
                    regionName = json.optString("region", ""),
                    city = json.optString("city", ""),
                    zip = json.optString("postal", ""),
                    lat = json.optString("latitude", ""),
                    lon = json.optString("longitude", ""),
                    timezone = json.optString("timezone", ""),
                    isp = json.optString("org", ""),
                    org = json.optString("org", ""),
                    asn = json.optString("asn", ""),
                    isLoading = false, error = null
                )
            }
        } else conn.disconnect()
    } catch (e: Exception) { }

    // 3. ipinfo.io — HTTPS
    try {
        val url = URL("https://ipinfo.io/$ip/json")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000; conn.readTimeout = 5000; conn.useCaches = false
        if (conn.responseCode == 200) {
            val json = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
            conn.disconnect()
            val loc = json.optString("loc", "").split(",")
            val orgField = json.optString("org", "") // format: "AS13335 Cloudflare, Inc."
            val asnPart = orgField.split(" ").firstOrNull() ?: ""
            val orgPart = orgField.removePrefix(asnPart).trim()
            return IpInfo(
                ip = json.optString("ip", ip),
                country = json.optString("country", ""),
                countryCode = json.optString("country", ""),
                regionName = json.optString("region", ""),
                city = json.optString("city", ""),
                zip = json.optString("postal", ""),
                lat = if (loc.size == 2) loc[0] else "",
                lon = if (loc.size == 2) loc[1] else "",
                timezone = json.optString("timezone", ""),
                isp = orgPart,
                org = orgPart,
                asn = asnPart,
                isLoading = false, error = null
            )
        } else conn.disconnect()
    } catch (e: Exception) { }

    // 4. ip-api.com via direct IP
    try {
        val url = URL("http://208.95.112.1/json/$ip")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000; conn.readTimeout = 5000; conn.useCaches = false
        conn.setRequestProperty("Host", "ip-api.com")
        if (conn.responseCode == 200) {
            val json = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
            conn.disconnect()
            if (json.optString("status") == "success") {
                return IpInfo(
                    ip = json.optString("query", ip),
                    country = json.optString("country", ""),
                    countryCode = json.optString("countryCode", ""),
                    regionName = json.optString("regionName", ""),
                    city = json.optString("city", ""),
                    zip = json.optString("zip", ""),
                    lat = json.optString("lat", ""),
                    lon = json.optString("lon", ""),
                    timezone = json.optString("timezone", ""),
                    isp = json.optString("isp", ""),
                    org = json.optString("org", ""),
                    asn = json.optString("as", ""),
                    isLoading = false, error = null
                )
            }
        } else conn.disconnect()
    } catch (e: Exception) { }

    return null
}

/**
 * Detects public IP by trying multiple services.
 * When VPN is active, this returns the VPN exit IP.
 * Key: use fresh connections with no cache and no DNS cache.
 */
private fun detectPublicIp(): String? {
    // List of IP detection services - mix of HTTP and HTTPS for maximum compatibility
    val services = listOf(
        "https://api.ipify.org",
        "https://ipv4.icanhazip.com",
        "http://208.95.112.1/line/?fields=query",
        "https://ipecho.net/plain",
        "https://checkip.amazonaws.com",
        "https://v4.ident.me",
        "http://ifconfig.me/ip",
        "http://whatismyip.akamai.com"
    )

    for (service in services) {
        try {
            val url = URL(service)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.useCaches = false
            conn.defaultUseCaches = false
            conn.setRequestProperty("Cache-Control", "no-cache, no-store")
            conn.setRequestProperty("Pragma", "no-cache")
            conn.setRequestProperty("Connection", "close")

            if (conn.responseCode == 200) {
                val ip = conn.inputStream.bufferedReader().readText().trim()
                conn.disconnect()
                // Validate it looks like an IP
                if (ip.isNotEmpty() && (ip.contains(".") || ip.contains(":"))) {
                    // Remove any extra whitespace/newlines
                    val cleanIp = ip.lines().first().trim()
                    if (cleanIp.matches(Regex("^[0-9a-fA-F.:]+$"))) {
                        return cleanIp
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            // Try next service
        }
    }
    return null
}

@Composable
fun VpnComparisonSection(currentIpInfo: IpInfo) {
    var savedIp by remember { mutableStateOf("") }
    var savedCountry by remember { mutableStateOf("") }
    var savedIsp by remember { mutableStateOf("") }
    var isVpnActive by remember { mutableStateOf(false) }

    // Detect VPN change
    LaunchedEffect(currentIpInfo.ip) {
        if (currentIpInfo.ip.isNotEmpty()) {
            if (savedIp.isEmpty()) {
                // First IP detected - save as baseline
                savedIp = currentIpInfo.ip
                savedCountry = currentIpInfo.country
                savedIsp = currentIpInfo.isp
                isVpnActive = false
            } else if (currentIpInfo.ip != savedIp) {
                // IP changed! VPN toggled
                isVpnActive = true
            }
        }
    }

    if (savedIp.isNotEmpty() && currentIpInfo.ip.isNotEmpty()) {
        HdnCard(glowColor = if (isVpnActive) HdnGreen else HdnOrange) {
            Column(Modifier.padding(16.dp)) {
                Text("🔄 مقایسه قبل/بعد VPN", fontWeight = FontWeight.Bold, color = HdnWhite, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth()) {
                    // Before (saved)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("بدون VPN", color = HdnOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(savedIp, color = HdnWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(savedCountry, color = HdnGrayDark, fontSize = 9.sp)
                        Text(savedIsp.take(20), color = HdnGrayDark, fontSize = 8.sp)
                    }

                    // Arrow
                    Text("→", color = HdnGray, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp).align(Alignment.CenterVertically))

                    // After (current)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isVpnActive) "با VPN ✅" else "فعلی", color = if (isVpnActive) HdnGreen else HdnCyan,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(currentIpInfo.ip, color = HdnWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(currentIpInfo.country, color = HdnGrayDark, fontSize = 9.sp)
                        Text(currentIpInfo.isp.take(20), color = HdnGrayDark, fontSize = 8.sp)
                    }
                }

                if (isVpnActive) {
                    Spacer(Modifier.height(8.dp))
                    Text("✅ IP تغییر کرده — VPN فعال است", color = HdnGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else if (savedIp == currentIpInfo.ip) {
                    Spacer(Modifier.height(8.dp))
                    Text("⚠ IP تغییر نکرده — VPN غیرفعال یا Leak دارد", color = HdnOrange, fontSize = 11.sp)
                }

                Spacer(Modifier.height(8.dp))
                // Reset baseline button
                Button(onClick = {
                    savedIp = currentIpInfo.ip
                    savedCountry = currentIpInfo.country
                    savedIsp = currentIpInfo.isp
                    isVpnActive = false
                }, colors = ButtonDefaults.buttonColors(containerColor = HdnDarkBorder, contentColor = HdnWhite),
                    shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("ذخیره IP فعلی به عنوان مبنا", fontSize = 10.sp)
                }
            }
        }
    }
}
