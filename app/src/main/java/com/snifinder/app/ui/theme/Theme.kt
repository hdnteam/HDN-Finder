package com.snifinder.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// === HDN Finder — Modern Dark Pro Theme ===
// Lighter dark theme with better contrast and readability

// Backgrounds - lighter and more readable
val HdnDarkBg = Color(0xFF121620)          // Main background - dark navy (not pure black)
val HdnDarkSurface = Color(0xFF1A2030)     // Surface - slightly lighter
val HdnDarkCard = Color(0xFF212B3E)        // Cards - clearly visible
val HdnDarkCardHover = Color(0xFF2A3650)   // Elevated/hover
val HdnDarkBorder = Color(0xFF2E3B52)      // Borders - visible
val HdnDarkBorderLight = Color(0xFF3D4E6A) // Active borders

// Primary - Bright Blue (more visible than cyan)
val HdnCyan = Color(0xFF4FC3F7)
val HdnCyanLight = Color(0xFF81D4FA)
val HdnCyanDim = Color(0xFF0288D1)

// Accent - Bright Green
val HdnGreen = Color(0xFF66FF99)
val HdnGreenDim = Color(0xFF00E676)

// Secondary - Soft Purple
val HdnPurple = Color(0xFFCE93D8)
val HdnPurpleDim = Color(0xFFAB47BC)

// Warning - Bright Orange
val HdnOrange = Color(0xFFFFB74D)
val HdnOrangeDim = Color(0xFFFFA000)

// Status
val HdnRed = Color(0xFFFF6B6B)
val HdnGold = Color(0xFFFFE57F)

// Text - high contrast
val HdnWhite = Color(0xFFF5F7FA)
val HdnGray = Color(0xFFA0B0C4)
val HdnGrayLight = Color(0xFFBFCFDF)
val HdnGrayDark = Color(0xFF6B7D94)

private val DarkColorScheme = darkColorScheme(
    primary = HdnCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1A3A4D),
    onPrimaryContainer = HdnCyanLight,
    secondary = HdnGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1A4030),
    onSecondaryContainer = HdnGreen,
    tertiary = HdnPurple,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF2D1A3D),
    onTertiaryContainer = HdnPurple,
    background = HdnDarkBg,
    onBackground = HdnWhite,
    surface = HdnDarkSurface,
    onSurface = HdnWhite,
    surfaceVariant = HdnDarkCard,
    onSurfaceVariant = HdnGray,
    outline = HdnDarkBorder,
    outlineVariant = HdnDarkBorderLight,
    error = HdnRed,
    onError = Color.White
)

@Composable
fun SniFinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
