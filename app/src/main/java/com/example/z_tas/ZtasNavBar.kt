package com.example.z_tas

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun ZtasNavBar(selectedIndex: Int) {
    val context = LocalContext.current
    val DeepNavy = Color(0xFF070B14)
    val ZTasCyan = Color(0xFF00D1FF)
    val TextSecondary = Color(0xFF8B9CB7)
    val CardBackground = Color(0xFF111721)

    NavigationBar(
            containerColor = DeepNavy,
            contentColor = ZTasCyan,
            modifier = Modifier.fillMaxWidth().background(DeepNavy)
    ) {
        NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") },
                selected = selectedIndex == 0,
                onClick = {
                    if (selectedIndex != 0) {
                        val intent =
                                Intent(context, HomeActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                }
                        context.startActivity(intent)
                    }
                },
                colors =
                        NavigationBarItemDefaults.colors(
                                selectedIconColor = DeepNavy,
                                selectedTextColor = ZTasCyan,
                                indicatorColor = ZTasCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                        )
        )
        NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = selectedIndex == 1,
                onClick = {
                    if (selectedIndex != 1) {
                        val intent =
                                Intent(context, ProfileActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                }
                        context.startActivity(intent)
                    }
                },
                colors =
                        NavigationBarItemDefaults.colors(
                                selectedIconColor = DeepNavy,
                                selectedTextColor = ZTasCyan,
                                indicatorColor = ZTasCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                        )
        )
        NavigationBarItem(
                icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
                label = { Text("Notifications") },
                selected = selectedIndex == 2,
                onClick = {
                    if (selectedIndex != 2) {
                        val intent =
                                Intent(context, NotificationPage::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                }
                        context.startActivity(intent)
                    }
                },
                colors =
                        NavigationBarItemDefaults.colors(
                                selectedIconColor = DeepNavy,
                                selectedTextColor = ZTasCyan,
                                indicatorColor = ZTasCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                        )
        )
        NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") },
                selected = selectedIndex == 3,
                onClick = {
                    if (selectedIndex != 3) {
                        val intent =
                                Intent(context, SettingsActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                }
                        context.startActivity(intent)
                    }
                },
                colors =
                        NavigationBarItemDefaults.colors(
                                selectedIconColor = DeepNavy,
                                selectedTextColor = ZTasCyan,
                                indicatorColor = ZTasCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                        )
        )
    }
}
