package com.ztas.app

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ztas.app.network.NotificationData
import com.ztas.app.network.RetrofitClient
import com.ztas.app.network.SessionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale


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
    var sessions by remember { mutableStateOf<List<SessionData>>(emptyList()) }
    var sessionsLoading by remember { mutableStateOf(true) }
    var sessionsError by remember { mutableStateOf<String?>(null) }
    var activities by remember { mutableStateOf<List<ActivityItem>>(emptyList()) }
    var activitiesLoading by remember { mutableStateOf(true) }
    var welcomeTitle by remember { mutableStateOf("WELCOME") }
    var welcomeSubtitle by remember { mutableStateOf("SYSTEM STATUS: OPTIMAL") }

    // Fetch profile, sessions + notifications on screen load
    LaunchedEffect(Unit) {
        val bearer = AuthPreferences.bearerOrNull(context)
        if (bearer != null) {
            try {
                val profileRaw = withContext(Dispatchers.IO) {
                    RetrofitClient.userApi.getProfile(bearer).body()?.string()
                }
                val profile = profileRaw?.let { UserProfileJson.parse(it) }
                if (profile != null) {
                    val emailCheck = profile.email.ifBlank { AuthPreferences.cachedEmail(context) }
                    val resolved =
                        ProfileDisplayName.headerName(context, profile.name, profile.email)
                    welcomeTitle = "WELCOME, ${resolved.uppercase(Locale.getDefault())}"
                    welcomeSubtitle = "Signed in as $emailCheck"
                    ProfileDisplayName.persistIfRichLabel(context, resolved, emailCheck)
                } else {
                    val em = AuthPreferences.cachedEmail(context)
                    val cachedName = AuthPreferences.cachedDisplayName(context).trim()
                    if (cachedName.isNotEmpty()) {
                        welcomeTitle = "WELCOME, ${cachedName.uppercase(Locale.getDefault())}"
                        welcomeSubtitle = if (em.isNotEmpty()) "Signed in as $em" else welcomeSubtitle
                    } else if (em.isNotEmpty()) {
                        welcomeTitle =
                            "WELCOME, ${em.substringBefore('@').uppercase(Locale.getDefault())}"
                        welcomeSubtitle = "Signed in as $em"
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Profile fetch failed", e)
            }
        }

        // Sessions
        try {
            if (bearer == null) {
                sessionsLoading = false
            } else {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.sessionApi.getSessions(bearer, limit = 20)
                }
                if (response.isSuccessful) {
                    sessions = response.body()?.data?.sessions.orEmpty()
                } else {
                    sessionsError = "Failed to load sessions"
                    Log.e("HomeScreen", "Sessions error: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            sessionsError = e.message
            Log.e("HomeScreen", "Sessions fetch failed", e)
        } finally {
            sessionsLoading = false
        }

        // Notifications (preview — limit 5)
        try {
            if (bearer != null) {
                val notifResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.notificationApi.getNotifications(
                        token = bearer,
                        limit = 5
                    )
                }
                if (notifResponse.isSuccessful) {
                    activities =
                        notifResponse.body()?.data?.notifications.orEmpty().map { it.toActivityItem() }
                } else {
                    Log.e("HomeScreen", "Notifications error: ${notifResponse.code()}")
                }
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Notifications fetch failed", e)
        } finally {
            activitiesLoading = false
        }
    }

    if (showSessionsDialog) {
        ZtasSessionsDialog(
            sessions = sessions,
            isLoading = sessionsLoading,
            onDismiss = { showSessionsDialog = false },
            onForceLogoutAll = {
                AuthPreferences.clear(context)
                showSessionsDialog = false
                context.startActivity(Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
        )
    }

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
                                painter = painterResource(id = R.drawable.z_taslogo),
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
                // Welcome header (filled from /user/profile or cached email after login)
                Text(
                    welcomeTitle,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    welcomeSubtitle,
                    color = ZTasCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Security Overview Card
                SecurityMetricCard(
                    title = "ACTIVE SESSIONS",
                    value = if (sessionsLoading) "--" else String.format("%02d", sessions.size),
                    trend = if (sessionsLoading) "" else "+${sessions.count { it.current }}",
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

/**
 * Maps an API [NotificationData] to the UI's [ActivityItem].
 */
private fun NotificationData.toActivityItem(): ActivityItem {
    val titleUpper = title.uppercase()
    val icon = when {
        "FAIL" in titleUpper || "DENIED" in titleUpper -> Icons.Default.Close
        "BLOCK" in titleUpper || "WARNING" in titleUpper -> Icons.Default.Warning
        "SUCCESS" in titleUpper || "LOGIN" in titleUpper -> Icons.Default.CheckCircle
        "MFA" in titleUpper || "VERIFIED" in titleUpper -> Icons.Default.Lock
        "SESSION" in titleUpper -> Icons.Default.Refresh
        else -> Icons.Default.Notifications
    }
    val isCritical = status == "unread"
    val timeAgo = formatLastActive(timestamp)

    return ActivityItem(
        title = title,
        device = "$details • $timeAgo",
        isCritical = isCritical,
        icon = icon,
        subtitle = details
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZtasSessionsDialog(
    sessions: List<SessionData>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onForceLogoutAll: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0D1B2A))
            .border(BorderStroke(1.dp, ZTasCyan.copy(alpha = 0.5f)), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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

            if (isLoading) {
                CircularProgressIndicator(
                    color = ZTasCyan,
                    modifier = Modifier.padding(24.dp)
                )
            } else if (sessions.isEmpty()) {
                Text(
                    "No active sessions",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    sessions.forEach { session ->
                        SessionItem(
                            device = session.deviceName,
                            details = if (session.current) "Current Device" else "Last active: ${formatLastActive(session.lastActive)}",
                            isActive = session.current
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    border = BorderStroke(1.dp, ZTasCyan),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoggingOut
                ) {
                    Text("CLOSE", color = ZTasCyan, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        isLoggingOut = true
                        scope.launch {
                            try {
                                val token = AuthPreferences.bearerOrNull(context) ?: return@launch
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.sessionApi.forceLogoutAllDevices(token)
                                }
                                if (response.isSuccessful) {
                                    onForceLogoutAll()
                                } else {
                                    Log.e("HomeScreen", "Force logout failed: ${response.code()}")
                                }
                            } catch (e: Exception) {
                                Log.e("HomeScreen", "Error forcing logout", e)
                            } finally {
                                isLoggingOut = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoggingOut
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("LOG OUT ALL", color = Color.White, fontWeight = FontWeight.Bold)
                    }
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

/**
 * Formats a timestamp (millis) into a human-readable relative time string.
 */
private fun formatLastActive(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestampMs
    val diffMin = diffMs / (1000 * 60)
    val diffHour = diffMs / (1000 * 60 * 60)
    val diffDay = diffMs / (1000 * 60 * 60 * 24)

    return when {
        diffMin < 1 -> "Just now"
        diffMin < 60 -> "${diffMin}m ago"
        diffHour < 24 -> "${diffHour}h ago"
        else -> "${diffDay}d ago"
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
