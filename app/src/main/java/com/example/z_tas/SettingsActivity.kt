package com.example.z_tas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogContent by remember { mutableStateOf("") }

    if (showDialog) {
        ZtasDetailDialog(
            title = dialogTitle,
            content = dialogContent,
            onDismiss = { showDialog = false }
        )
    }

    Scaffold(
        containerColor = DeepNavy,
        topBar = {
            TopAppBar(
                title = { Text("Z-TAS SECURITY", color = ZTasCyan, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontSize = 16.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy),
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ZTasCyan)
                    }
                }
            )
        },
        bottomBar = { ZtasNavBar(selectedIndex = 3) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DeepNavy)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    SettingsSectionHeader("ACCOUNT")
                }
                item {
                    SettingsRow(icon = Icons.Default.Person, title = "PROFILE DETAILS", subtitle = "Update your personal information") {
                        context.startActivity(Intent(context, ProfileActivity::class.java))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSectionHeader("PREFERENCES")
                }
                item {
                    MfaToggleRow()
                }
                item {
                    SettingsRow(icon = Icons.Default.Notifications, title = "NOTIFICATIONS", subtitle = "Push, email, SMS alerts") {
                        val intent = Intent(context, NotificationPage::class.java)
                        context.startActivity(intent)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSectionHeader("SYSTEM")
                }
                item {
                    SettingsRow(icon = Icons.Default.Info, title = "ABOUT Z-TAS", subtitle = "Version 1.0.0 Stable") {
                        dialogTitle = "About Z-TAS"
                        dialogContent = "Z-TAS (Zero-Touch Authentication System) is an advanced security framework providing multi-layered protection without user friction.\n\n" +
                                        "Key Features:\n" +
                                        "• Continuous Biometric Monitoring\n" +
                                        "• Adaptive Threat Detection\n" +
                                        "• Behavioral Risk Analysis\n" +
                                        "• Seamless Integration\n\n" +
                                        "Version: 1.0.0 Stable\n" +
                                        "Developer: Z-TAS Solutions"
                        showDialog = true
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            prefs.edit().putBoolean("isLoggedIn", false).apply()
                            val intent = Intent(context, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CriticalRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("LOG OUT", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZtasDetailDialog(title: String, content: String, onDismiss: () -> Unit) {
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
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ZTasCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                title.uppercase(),
                color = ZTasCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = ZTasCyan.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                content,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZTasCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = ZTasCyan,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        letterSpacing = 0.1.sp,
        modifier = Modifier.padding(start = 20.dp, bottom = 12.dp, top = 16.dp)
    )
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFF0D1B2A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = ZTasCyan, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    color = TextPrimary, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle, 
                    color = Color(0xFF888888), 
                    fontSize = 11.sp 
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward, 
                contentDescription = "Go", 
                tint = ZTasCyan, 
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MfaToggleRow() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    var mfaEnabled by remember { mutableStateOf(prefs.getBoolean("mfaEnabled", false)) }

    Surface(
        color = Color(0xFF0D1B2A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "MFA",
                    tint = ZTasCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ENABLE MFA",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (mfaEnabled) "Multi-Factor Auth is ON" else "Multi-Factor Auth is OFF",
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = mfaEnabled,
                onCheckedChange = {
                    mfaEnabled = it
                    prefs.edit().putBoolean("mfaEnabled", it).apply()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ZTasCyan,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF1E3A5F)
                )
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
