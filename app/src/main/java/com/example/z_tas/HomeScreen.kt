package com.example.z_tas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.example.z_tas.R


val DeepNavy = Color(0xFF070B14)      
val CardBackground = Color(0xFF111721) 
val ZTasCyan = Color(0xFF00D1FF)       
val CriticalRed = Color(0xFFFF5252)
val SuccessGreen = Color(0xFF4CAF50)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8B9CB7)
val BorderCyanSubtle = Color(0xFF00D1FF).copy(alpha = 0.3f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var showSessionsDialog by remember { mutableStateOf(false) }

    if (showSessionsDialog) {
        ZtasSessionsDialog(onDismiss = { showSessionsDialog = false })
    }

    val activities = listOf(
        ActivityItem("Failed Login Attempt", "Unknown Device • 02:05 AM", true, Icons.Default.Close, "Suspicious activity detected"),
        ActivityItem("Access Blocked", "192.168.1.105 • 02:00 AM", true, Icons.Default.Warning, "Unauthorized access prevented"),
        ActivityItem("Successful Login", "MacBook Pro • 01:50 AM", false, Icons.Default.CheckCircle, "San Francisco, CA"),
        ActivityItem("MFA Verified", "iPhone 15 Pro • 01:40 AM", false, Icons.Default.Lock, "Authentication successful")
    )

    Box(
        modifier = Modifier.fillMaxSize().background(DeepNavy)
    ) {
        ProfessionalCyberBackground()

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { ZtasNavBar(selectedIndex = 0) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DeepNavy.copy(alpha = 0.95f),
                        titleContentColor = ZTasCyan 
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.offset(x = (-11).dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_bg),
                                contentDescription = "App Logo",
                                modifier = Modifier
                                    .width(75.dp)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(0.dp))
                            Column {
                                Text(
                                    "Z-TAS SECURITY", 
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    "ZERO TOUCH AUTHENTICATION",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val intent = Intent(context, NotificationPage::class.java)
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(ZTasCyan.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = ZTasCyan,
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
                    "WELCOME, USERNAME", 
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    "SYSTEM STATUS: OPTIMAL", // Industrial status text
                    color = ZTasCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Security Overview Card
                SecurityMetricCard(
                    title = "ACTIVE SESSIONS",
                    value = "03",
                    trend = "+1",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showSessionsDialog = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "RECENT ACTIVITY",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    TextButton(onClick = {
                        val intent = Intent(context, NotificationPage::class.java)
                        context.startActivity(intent)
                    }) {
                        Text(
                            "VIEW ALL",
                            color = ZTasCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
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
    isNegative: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.height(100.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1B2A)
        ),
        border = BorderStroke(1.dp, Color(0xFF1E3A5F))
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    value,
                    color = ZTasCyan, // Value now Cyan
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    trend,
                    color = if (isNegative) CriticalRed else ZTasCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProfessionalCyberBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = DeepNavy)
        drawGridPattern(this, size.width, size.height)
    }
}

fun drawGridPattern(scope: DrawScope, width: Float, height: Float) {
    val gridColor = Color(0xFF2A3142).copy(alpha = 0.2f)
    val spacing = 80f
    var x = 0f
    while (x < width) {
        scope.drawLine(gridColor, Offset(x, 0f), Offset(x, height), 0.5f)
        x += spacing
    }
    var y = 0f
    while (y < height) {
        scope.drawLine(gridColor, Offset(0f, y), Offset(width, y), 0.5f)
        y += spacing
    }
}

@Composable
fun ProfessionalActivityCard(item: ActivityItem) {
    // Icons now use the Notification Page style: Cyan on Dark Circle
    val iconBg = Color(0xFF1A2332)
    val iconColor = if (item.isCritical) CriticalRed else ZTasCyan

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1B2A)
        ),
        border = BorderStroke(1.dp, Color(0xFF1E3A5F))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape) // Circle icons like notifications
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title.uppercase(), // All caps
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
                Text(
                    item.device,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Unread Dot like the notification page
            if (item.isCritical) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ZTasCyan)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZtasSessionsDialog(onDismiss: () -> Unit) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.75f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0D1B2A))
            .border(BorderStroke(1.dp, ZTasCyan.copy(alpha = 0.5f)), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(ZTasCyan.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = ZTasCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "ACTIVE SESSIONS",
                color = ZTasCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = ZTasCyan.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SessionItem("Adam's Samsung S24", "Current Device", true)
                SessionItem("John's iPhone", "Last active: 5 hours ago", false)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    border = BorderStroke(1.dp, ZTasCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("CLOSE", color = ZTasCyan, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("LOG OUT ALL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SessionItem(device: String, details: String, isActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(BorderStroke(1.dp, ZTasCyan.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) SuccessGreen else Color.Gray)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(device, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                details, 
                color = if (isActive) ZTasCyan else Color.White.copy(alpha = 0.5f), 
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
