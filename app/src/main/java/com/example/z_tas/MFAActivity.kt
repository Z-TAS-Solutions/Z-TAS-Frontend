package com.example.z_tas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.z_tas.R

class MFAActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MfaNavigationWrapper()
        }
    }
}

enum class MfaScreenState {
    APPROVAL,
    RESULT_APPROVED,
    RESULT_DENIED
}

@Composable
fun MfaNavigationWrapper() {
    var screenState by remember { mutableStateOf(MfaScreenState.APPROVAL) }
    val context = LocalContext.current
    
    val deepNavy = colorResource(id = R.color.deep_navy)

    Box(modifier = Modifier.fillMaxSize().background(deepNavy)) {
        
        MfaCyberBackground()

        when (screenState) {
            MfaScreenState.APPROVAL -> {
                SessionApprovalScreen(
                    onApprove = { screenState = MfaScreenState.RESULT_APPROVED },
                    onDeny = { screenState = MfaScreenState.RESULT_DENIED }
                )
            }
            MfaScreenState.RESULT_APPROVED -> {
                ResultScreen(
                    isApproved = true,
                    onBackToHome = {
                        val intent = Intent(context, HomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    }
                )
            }
            MfaScreenState.RESULT_DENIED -> {
                ResultScreen(
                    isApproved = false,
                    onBackToHome = {
                        val intent = Intent(context, HomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun SessionApprovalScreen(onApprove: () -> Unit, onDeny: () -> Unit) {
    val cardBg = colorResource(id = R.color.card_background)
    val ztasCyan = colorResource(id = R.color.ztas_cyan)
    val successGreen = colorResource(id = R.color.success_green)
    val criticalRed = colorResource(id = R.color.critical_red)
    val textPrimary = colorResource(id = R.color.text_primary)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, ztasCyan.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A2332)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security Lock",
                        tint = ztasCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "NEW ACCESS REQUEST",
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Details Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(label = "Device", value = "Windows PC")
                    DetailRow(label = "Location", value = "Colombo")
                    DetailRow(label = "Time", value = "2:15")
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                Button(
                    onClick = onApprove,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = successGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("APPROVE", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDeny,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = criticalRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("DENY", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ResultScreen(isApproved: Boolean, onBackToHome: () -> Unit) {
    val successGreen = colorResource(id = R.color.success_green)
    val criticalRed = colorResource(id = R.color.critical_red)
    val cardBg = colorResource(id = R.color.card_background)
    val textPrimary = colorResource(id = R.color.text_primary)
    val textSecondary = colorResource(id = R.color.text_secondary)
    val ztasCyan = colorResource(id = R.color.ztas_cyan)

    val statusColor = if (isApproved) successGreen else criticalRed
    val icon = if (isApproved) Icons.Default.Check else Icons.Default.Close
    val statusText = if (isApproved) "ACCESS APPROVED" else "ACCESS DENIED"
    val subtitle = if (isApproved) "This device has been successfully verified" else "Unauthorized access attempt blocked"

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // status iicon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A2332))
                        .border(2.dp, statusColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Status Icon",
                        tint = statusColor,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    statusText,
                    color = textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    subtitle,
                    color = textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = BorderStroke(1.dp, ztasCyan),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ztasCyan)
                ) {
                    Text("BACK TO HOME", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    val textPrimary = colorResource(id = R.color.text_primary)
    val textSecondary = colorResource(id = R.color.text_secondary)
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", color = textSecondary, fontSize = 14.sp)
        Text(value, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MfaCyberBackground() {
    val deepNavy = colorResource(id = R.color.deep_navy)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = deepNavy)
        drawMfaGridPattern(this, size.width, size.height)
    }
}

fun drawMfaGridPattern(scope: DrawScope, width: Float, height: Float) {
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MFAApprovalPreview() {
    MfaNavigationWrapper()
}
