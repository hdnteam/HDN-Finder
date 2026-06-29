package com.snifinder.app.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snifinder.app.ui.theme.*
import com.snifinder.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CheckHostResult(
    val node: String,
    val country: String,
    val countryFlag: String,
    val city: String,
    val time: Double,   // response time in seconds, -1 = error
    val statusCode: Int,
    val error: String?
)

enum class HostQuality { EXCELLENT, GOOD, AVERAGE, POOR, DEAD }

@Composable
fun CheckHostScreen() {
    var hostInput by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<CheckHostResult>>(emptyList()) }
    var overallQuality by remember { mutableStateOf<HostQuality?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            HdnCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, null, tint = HdnCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Check Host", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = HdnWhite, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("HTTP check از سراسر دنیا — مثل check-host.net", color = HdnGray, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = hostInput, onValueChange = { hostInput = it; errorMsg = null },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("IP یا دامنه: 1.2.3.4 یا example.com", color = HdnGray.copy(0.5f)) },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = HdnGreen),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HdnCyan, unfocusedBorderColor = HdnDarkBorder,
                            cursorColor = HdnCyan, focusedContainerColor = HdnDarkBg, unfocusedContainerColor = HdnDarkBg),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(onClick = {
                        if (hostInput.isBlank()) {
                            Toast.makeText(context, "آدرس وارد کنید", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isChecking = true
                        results = emptyList()
                        overallQuality = null
                        errorMsg = null
                        scope.launch {
                            val res = performCheckHost(hostInput.trim())
                            if (res.first != null) {
                                errorMsg = res.first
                            } else {
                                results = res.second
                                overallQuality = calculateQuality(res.second)
                            }
                            isChecking = false
                        }
                    }, modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = hostInput.isNotBlank() && !isChecking,
                        colors = ButtonDefaults.buttonColors(containerColor = HdnCyan, contentColor = Color.Black,
                            disabledContainerColor = HdnDarkBorder), shape = RoundedCornerShape(10.dp)) {
                        if (isChecking) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("در حال بررسی...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Language, null); Spacer(Modifier.width(8.dp))
                            Text("Check HTTP از کل دنیا", fontWeight = FontWeight.Bold)
                        }
                    }

                    errorMsg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("⚠ $it", color = HdnRed, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Overall quality
            if (overallQuality != null && results.isNotEmpty()) {
                HdnCard {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("کیفیت کلی", color = HdnGray, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        val (qualityText, qualityColor) = when (overallQuality) {
                            HostQuality.EXCELLENT -> "⭐ EXCELLENT" to HdnGreen
                            HostQuality.GOOD -> "✅ GOOD" to HdnGreen
                            HostQuality.AVERAGE -> "⚡ AVERAGE" to HdnOrange
                            HostQuality.POOR -> "⚠ POOR" to HdnRed
                            HostQuality.DEAD -> "❌ DEAD" to HdnRed
                            else -> "" to HdnGray
                        }
                        Text(qualityText, color = qualityColor, fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)

                        Spacer(Modifier.height(8.dp))
                        val successCount = results.count { it.error == null && it.statusCode > 0 }
                        val avgTime = results.filter { it.time > 0 }.map { it.time }.average()
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$successCount/${results.size}", color = HdnWhite, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("Reachable", color = HdnGray, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (avgTime > 0) "${String.format("%.0f", avgTime * 1000)}ms" else "—",
                                    color = HdnWhite, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("Avg Time", color = HdnGray, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Results by country
            if (results.isNotEmpty()) {
                HdnCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("نتایج از سراسر دنیا", fontWeight = FontWeight.Bold, color = HdnWhite, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(12.dp))

                        results.forEach { r ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Flag + Country
                                Text(r.countryFlag, fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(r.city.ifEmpty { r.country }, color = HdnWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(r.node, color = HdnGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                                // Result
                                if (r.error != null) {
                                    Text("❌", fontSize = 14.sp)
                                } else {
                                    Column(horizontalAlignment = Alignment.End) {
                                        val timeMs = (r.time * 1000).toLong()
                                        Text("${timeMs}ms", color = when {
                                            timeMs < 200 -> HdnGreen
                                            timeMs < 500 -> HdnOrange
                                            else -> HdnRed
                                        }, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        if (r.statusCode > 0) {
                                            Text("HTTP ${r.statusCode}", color = if (r.statusCode in 200..399) HdnGreen else HdnOrange,
                                                fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 2.dp), color = HdnDarkBorder.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
        HdnFooter()
    }
}

fun calculateQuality(results: List<CheckHostResult>): HostQuality {
    if (results.isEmpty()) return HostQuality.DEAD
    val successCount = results.count { it.error == null && it.statusCode > 0 }
    val successRate = successCount.toDouble() / results.size.toDouble()
    val avgTime = results.filter { it.time > 0 }.map { it.time }.average().let { if (it.isNaN()) 99.0 else it }

    return when {
        successRate >= 0.9 && avgTime < 0.3 -> HostQuality.EXCELLENT
        successRate >= 0.7 && avgTime < 0.6 -> HostQuality.GOOD
        successRate >= 0.5 -> HostQuality.AVERAGE
        successRate > 0.1 -> HostQuality.POOR
        else -> HostQuality.DEAD
    }
}

/**
 * Uses check-host.net API to check HTTP from worldwide nodes.
 * API format:
 * Step 1: GET https://check-host.net/check-http?host=<HOST>&max_nodes=40
 * Step 2: GET https://check-host.net/check-result/<REQUEST_ID>
 * 
 * HTTP result format per node: [[success, time, message, statusCode, ip]]
 * success: 1 = ok, 0 = error
 * time: response time in seconds
 */
suspend fun performCheckHost(host: String): Pair<String?, List<CheckHostResult>> = withContext(Dispatchers.IO) {
    try {
        // Step 1: Create check request
        val target = when {
            host.startsWith("http://") || host.startsWith("https://") -> host
            host.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) -> "http://$host"
            else -> "http://$host"
        }

        val checkUrl = URL("https://check-host.net/check-http?host=$target&max_nodes=40")
        val conn = checkUrl.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("Accept", "application/json")

        if (conn.responseCode != 200) {
            val errBody = try { conn.errorStream?.bufferedReader()?.readText() } catch (e: Exception) { null }
            conn.disconnect()
            return@withContext ("خطا: check-host پاسخ نداد (HTTP ${conn.responseCode})" to emptyList())
        }

        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        val json = JSONObject(response)
        val requestId = json.optString("request_id", "")
        val nodesObj = json.optJSONObject("nodes")

        if (requestId.isEmpty() || nodesObj == null) {
            return@withContext ("خطا: request_id یا nodes دریافت نشد" to emptyList())
        }

        // Parse node info: {"us1.node.check-host.net": ["us","USA","Los Angeles","1.2.3.4","AS123"]}
        data class NodeInfo(val countryCode: String, val country: String, val city: String)
        val nodeInfoMap = mutableMapOf<String, NodeInfo>()

        val nodeKeys = nodesObj.keys()
        while (nodeKeys.hasNext()) {
            val nodeName = nodeKeys.next()
            val nodeArr = nodesObj.optJSONArray(nodeName)
            if (nodeArr != null && nodeArr.length() >= 3) {
                nodeInfoMap[nodeName] = NodeInfo(
                    countryCode = nodeArr.optString(0, ""),
                    country = nodeArr.optString(1, ""),
                    city = nodeArr.optString(2, "")
                )
            }
        }

        // Step 2: Wait then poll results
        delay(5000) // Initial wait for checks to run

        var finalResults = listOf<CheckHostResult>()
        var attempts = 0

        while (attempts < 6) {
            attempts++

            val resultUrl = URL("https://check-host.net/check-result/$requestId")
            val resultConn = resultUrl.openConnection() as HttpURLConnection
            resultConn.connectTimeout = 15000
            resultConn.readTimeout = 15000
            resultConn.setRequestProperty("Accept", "application/json")

            if (resultConn.responseCode != 200) {
                resultConn.disconnect()
                delay(3000)
                continue
            }

            val resultResponse = resultConn.inputStream.bufferedReader().readText()
            resultConn.disconnect()

            val resultJson = JSONObject(resultResponse)
            val parsed = mutableListOf<CheckHostResult>()
            var pendingCount = 0

            val keys = resultJson.keys()
            while (keys.hasNext()) {
                val nodeName = keys.next()

                // null means still processing
                if (resultJson.isNull(nodeName)) {
                    pendingCount++
                    continue
                }

                val info = nodeInfoMap[nodeName] ?: NodeInfo("", "Unknown", "")
                val flag = countryToFlag(info.countryCode)

                try {
                    // Format: [[success, time, message, statusCode, ip]]
                    val outerArr = resultJson.optJSONArray(nodeName)
                    if (outerArr != null && outerArr.length() > 0) {
                        val innerArr = outerArr.optJSONArray(0)
                        if (innerArr != null && innerArr.length() >= 3) {
                            val success = innerArr.optInt(0, 0)
                            val time = innerArr.optDouble(1, -1.0)
                            val message = innerArr.optString(2, "")
                            val statusCode = innerArr.optString(3, "").let {
                                it.toIntOrNull() ?: 0
                            }
                            val resolvedIp = if (innerArr.length() >= 5) innerArr.optString(4, "") else ""

                            if (success == 1 || statusCode > 0) {
                                parsed.add(CheckHostResult(
                                    node = nodeName,
                                    country = info.country,
                                    countryFlag = flag,
                                    city = info.city,
                                    time = time,
                                    statusCode = statusCode,
                                    error = null
                                ))
                            } else {
                                parsed.add(CheckHostResult(
                                    node = nodeName,
                                    country = info.country,
                                    countryFlag = flag,
                                    city = info.city,
                                    time = time,
                                    statusCode = statusCode,
                                    error = message.ifEmpty { "Connection failed" }
                                ))
                            }
                        }
                    }
                } catch (e: Exception) {
                    parsed.add(CheckHostResult(
                        node = nodeName, country = info.country,
                        countryFlag = flag, city = info.city,
                        time = -1.0, statusCode = 0, error = "Parse error"
                    ))
                }
            }

            finalResults = parsed.sortedWith(compareBy {
                if (it.error == null && it.time > 0) it.time else 999.0
            })

            // If most nodes responded, we're done
            if (pendingCount == 0 || parsed.size >= nodeInfoMap.size - 3) {
                break
            }

            delay(3000)
        }

        null to finalResults

    } catch (e: java.net.SocketTimeoutException) {
        ("خطا: Timeout — اینترنت یا VPN بررسی شود" to emptyList())
    } catch (e: Exception) {
        ("خطا: ${e.message}" to emptyList())
    }
}

fun countryToFlag(countryCode: String): String {
    if (countryCode.length != 2) return "🌐"
    val first = Character.toChars(0x1F1E6 + (countryCode[0].uppercaseChar() - 'A'))
    val second = Character.toChars(0x1F1E6 + (countryCode[1].uppercaseChar() - 'A'))
    return String(first) + String(second)
}
