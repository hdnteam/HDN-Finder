package com.snifinder.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.snifinder.app.ui.theme.*
import com.snifinder.app.ui.screens.MainScreen
import com.snifinder.app.ui.screens.PingScreen
import com.snifinder.app.ui.screens.MyIpScreen
import com.snifinder.app.ui.screens.CloudflareScreen
import com.snifinder.app.ui.screens.CheckHostScreen
import com.snifinder.app.ui.screens.SettingsScreen
import com.snifinder.app.ui.screens.NetworkScreen
import com.snifinder.app.ui.screens.SpeedTestScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.parseColor("#121620")
        window.navigationBarColor = android.graphics.Color.parseColor("#121620")

        setContent {
            SniFinderTheme {
                AppWithNavigation()
            }
        }
    }
}

enum class Screen(val title: String, val icon: ImageVector) {
    SNI_FINDER("SNI", Icons.Default.Cloud),
    CLOUDFLARE("CF IP", Icons.Default.Wifi),
    SPEED_TEST("Speed", Icons.Default.ArrowUpward),
    NETWORK("Config", Icons.Default.Tune),
    CHECK_HOST("Check-Host", Icons.Default.Language),
    PING("Ping", Icons.Default.Favorite),
    MY_IP("My IP", Icons.Default.Public)
}

@Composable
fun AppWithNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.SNI_FINDER) }
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        containerColor = HdnDarkBg,
        bottomBar = {
            NavigationBar(
                containerColor = HdnDarkSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.height(68.dp)
            ) {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title, modifier = Modifier.size(20.dp)) },
                        label = { Text(screen.title, fontSize = 9.sp, fontWeight = if (currentScreen == screen) FontWeight.Bold else FontWeight.Normal) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen; showSettings = false },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HdnCyan,
                            selectedTextColor = HdnCyan,
                            unselectedIconColor = HdnGrayDark,
                            unselectedTextColor = HdnGrayDark,
                            indicatorColor = HdnCyan.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(HdnDarkBg)
        ) {
            // Global top bar with VPN status + Settings icon
            val isVpnActive = remember {
                try {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                    val net = cm?.activeNetwork
                    val caps = cm?.getNetworkCapabilities(net)
                    caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true
                } catch (e: Exception) { false }
            }

            Row(
                Modifier.fillMaxWidth()
                    .background(HdnDarkSurface)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(HdnCyan.copy(0.12f)),
                    contentAlignment = Alignment.Center) {
                    Text("☁", fontSize = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("HDN Finder", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HdnWhite)
                    Text("Network Toolkit", color = HdnGrayDark, fontSize = 9.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.weight(1f))

                // VPN status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isVpnActive) HdnGreen.copy(0.08f) else HdnDarkCard)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (isVpnActive) HdnGreen else HdnGrayDark))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (isVpnActive) "VPN" else "VPN",
                        color = if (isVpnActive) HdnGreen else HdnGrayDark,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Settings button
                IconButton(onClick = { showSettings = !showSettings }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (showSettings) Icons.Default.Close else Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (showSettings) HdnRed else HdnGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            // Content
            Box(Modifier.fillMaxSize()) {
                if (showSettings) {
                    SettingsScreen()
                } else {
                    when (currentScreen) {
                        Screen.SNI_FINDER -> MainScreen()
                        Screen.CLOUDFLARE -> CloudflareScreen()
                        Screen.SPEED_TEST -> SpeedTestScreen()
                        Screen.NETWORK -> NetworkScreen()
                        Screen.CHECK_HOST -> CheckHostScreen()
                        Screen.PING -> PingScreen()
                        Screen.MY_IP -> MyIpScreen()
                    }
                }
            }
        }
    }
}
