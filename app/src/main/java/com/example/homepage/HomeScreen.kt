package com.example.homepage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.homepage.ui.theme.HomePageTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import kotlin.random.Random
import androidx.compose.foundation.Image

// Professional Cyber Security Palette
val DeepNavy = Color(0xFF0B0E1A)
val CardBackground = Color(0xFF1A1F2E)
val AccentBlue = Color(0xFF2196F3)
val CriticalRed = Color(0xFFFF5252)
val SuccessGreen = Color(0xFF4CAF50)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8B9CB7)
val BorderSubtle = Color(0xFF2A3142)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val activities = listOf(
        ActivityItem("Failed Login Attempt", "Unknown Device • 02:05 AM", true, Icons.Default.Close, "Suspicious activity detected"),
        ActivityItem("Access Blocked", "192.168.1.105 • 02:00 AM", true, Icons.Default.Warning, "Unauthorized access prevented"),
        ActivityItem("Successful Login", "MacBook Pro • 01:50 AM", false, Icons.Default.CheckCircle, "San Francisco, CA"),
        ActivityItem("MFA Verified", "iPhone 15 Pro • 01:40 AM", false, Icons.Default.Lock, "Authentication successful")
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Professional cyber background
        ProfessionalCyberBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DeepNavy.copy(alpha = 0.95f),
                        titleContentColor = TextPrimary
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_bg), // Make sure this matches your filename!
                                contentDescription = "App Logo",
                                modifier = Modifier
                                    .size(85.dp) // Adjust size as needed
                                    .clip(RoundedCornerShape(4.dp)) // Optional: rounds logo corners
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "ZTAS Security",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Zero Trust Access",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(CriticalRed.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = CriticalRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp)
            ) {
                // Welcome header
                Text(
                    "Welcome back, Alex",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your security status is optimal",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Security Overview Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecurityMetricCard(
                        title = "Active Sessions",
                        value = "3",
                        trend = "+1",
                        modifier = Modifier.weight(1f)
                    )
                    SecurityMetricCard(
                        title = "Threats Blocked",
                        value = "12",
                        trend = "+5",
                        isNegative = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Activity",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = {}) {
                        Text(
                            "View All",
                            color = AccentBlue,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(activities) { item ->
                        ProfessionalActivityCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityMetricCard(
    title: String,
    value: String,
    trend: String,
    modifier: Modifier = Modifier,
    isNegative: Boolean = false
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground.copy(alpha = 0.7f)
        ),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    value,
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    trend,
                    color = if (isNegative) CriticalRed else SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ProfessionalCyberBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Base dark gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B0E1A),
                    Color(0xFF151923),
                    Color(0xFF0B0E1A)
                )
            )
        )

        // Subtle center glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A3D5B).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(width * 0.5f, height * 0.3f),
                radius = height * 0.5f
            ),
            center = Offset(width * 0.5f, height * 0.3f),
            radius = height * 0.5f
        )

        // Minimal stars
        drawMinimalStars(this, width, height)

        // Clean grid lines
        drawGridPattern(this, width, height)
    }
}

fun drawMinimalStars(scope: DrawScope, width: Float, height: Float) {
    val random = Random(42)
    repeat(80) {
        val x = random.nextFloat() * width
        val y = random.nextFloat() * height
        val size = random.nextFloat() * 1f + 0.5f
        val alpha = random.nextFloat() * 0.3f + 0.2f

        scope.drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = size,
            center = Offset(x, y)
        )
    }
}

fun drawGridPattern(scope: DrawScope, width: Float, height: Float) {
    val gridColor = Color(0xFF2A3142).copy(alpha = 0.3f)
    val spacing = 60f

    // Vertical lines
    var x = spacing
    while (x < width) {
        scope.drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 0.5f
        )
        x += spacing
    }

    // Horizontal lines
    var y = spacing
    while (y < height) {
        scope.drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 0.5f
        )
        y += spacing
    }
}

@Composable
fun ProfessionalActivityCard(item: ActivityItem) {
    val iconBg = when {
        item.isCritical -> CriticalRed.copy(alpha = 0.15f)
        else -> SuccessGreen.copy(alpha = 0.15f)
    }

    val iconColor = when {
        item.isCritical -> CriticalRed
        else -> SuccessGreen
    }

    val borderColor = if (item.isCritical) {
        CriticalRed.copy(alpha = 0.3f)
    } else {
        BorderSubtle
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground.copy(alpha = 0.7f)
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    item.device,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                if (item.subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        item.subtitle,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            // Status indicator
            if (item.isCritical) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CriticalRed)
                )
            }
        }
    }
}

data class ActivityItem(
    val title: String,
    val device: String,
    val isCritical: Boolean,
    val icon: ImageVector,
    val subtitle: String = ""
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomePageTheme {
        HomeScreen()
    }
}