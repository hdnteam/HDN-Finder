package com.snifinder.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snifinder.app.ui.theme.*

@Composable
fun HdnCard(
    modifier: Modifier = Modifier,
    glowColor: Color? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HdnDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

@Composable
fun HdnGlowCard(
    glowColor: Color = HdnCyan,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = glowColor.copy(0.1f), spotColor = glowColor.copy(0.15f)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HdnDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box {
            // Top accent bar
            Box(
                Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Brush.horizontalGradient(
                        listOf(glowColor.copy(0.0f), glowColor.copy(0.7f), glowColor.copy(0.0f))
                    ))
            )
            content()
        }
    }
}

@Composable
fun HdnFooter() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/vpnxub"))
                context.startActivity(intent)
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(HdnGreen.copy(0.8f)))
            Spacer(Modifier.width(8.dp))
            Text("HDNTEAM", color = HdnGreen.copy(0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Spacer(Modifier.width(6.dp))
            Text("•", color = HdnGrayDark, fontSize = 8.sp)
            Spacer(Modifier.width(6.dp))
            Text("hdnteam@gmail.com", color = HdnGrayDark, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun SectionTitle(icon: String, title: String, color: Color = HdnCyan) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 16.sp) }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HdnWhite)
    }
}

@Composable
fun HdnChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor.copy(alpha = 0.15f),
            selectedLabelColor = accentColor,
            labelColor = HdnGray,
            containerColor = HdnDarkSurface
        )
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("$label ", color = HdnGray, fontSize = 12.sp)
        Text(value, color = HdnWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HdnButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = HdnCyan,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.Black,
            disabledContainerColor = HdnDarkBorder,
            disabledContentColor = HdnGrayDark
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
    ) {
        icon?.invoke()
        if (icon != null) Spacer(Modifier.width(10.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
