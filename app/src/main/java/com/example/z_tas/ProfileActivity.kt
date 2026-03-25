package com.example.z_tas

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_host)

        // Initialize Compose Bottom Nav
        val composeView = findViewById<ComposeView>(R.id.compose_bottom_nav)
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ZtasNavBar(selectedIndex = 1)
            }
        }

        // sample user's data
        val userProfile = UserProfile(
            username = "Username",
            email = "for.example@gmail.com",
            status = "ACTIVE",
            activeDevices = 2,
            biometricEngineStatus = "ONLINE",
            lastSync = getLastSyncText(),
            securityLevel = "HIGH",
        )

        // find the username TextView and set it
        findViewById<TextView>(R.id.username).text = userProfile.username

        findViewById<TextView>(R.id.useremail).text = userProfile.email

        // combining "STATUS: " with the actual status
        findViewById<TextView>(R.id.status).text = "STATUS: ${userProfile.status}"

        // active devices count
        findViewById<TextView>(R.id.activeDevicesCount).text =
            "${userProfile.activeDevices} devices connected"

        // biometric status
        findViewById<TextView>(R.id.biometricStatus).text =
            userProfile.biometricEngineStatus

        // last sync time
        findViewById<TextView>(R.id.lastSync).text = userProfile.lastSync

        // Settings click listener
        findViewById<ImageView>(R.id.settingsIcon)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Back arrow click listener
        findViewById<ImageView>(R.id.backArrow)?.setOnClickListener {
            finish()
        }

        // Add new passkey click listener
        findViewById<LinearLayout>(R.id.changePasswordCard).setOnClickListener {
            Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Active sessions click listener
        findViewById<LinearLayout>(R.id.activeSessionsCard).setOnClickListener {
            showActiveSessionsDialog()
        }

        // Sign out
        findViewById<LinearLayout>(R.id.signOutCard).setOnClickListener {
            showSignOutDialog()
        }

        // Delete account
        findViewById<LinearLayout>(R.id.deleteAccountCard).setOnClickListener {
            showDeleteAccount()
        }
    }

    private fun getLastSyncText(): String {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val lastOpenTime = prefs.getLong("lastOpenTime", 0L)
        val now = System.currentTimeMillis()

        // Save current time for next calculation
        prefs.edit().putLong("lastOpenTime", now).apply()

        if (lastOpenTime == 0L) return "Just now"

        val diffMs = now - lastOpenTime
        val diffMin = diffMs / 60_000
        val diffHr = diffMs / 3_600_000
        val diffDay = diffMs / 86_400_000

        return when {
            diffMin < 1 -> "Just now"
            diffMin < 60 -> "${diffMin}m ago"
            diffHr < 24 -> "${diffHr}h ago"
            else -> "${diffDay}d ago"
        }
    }

    private fun showActiveSessionsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(
            R.layout.active_sessions_dialog,
            null
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val closeBtn = dialogView.findViewById<Button>(R.id.closesignout_id)
        val signOutOtherBtn = dialogView.findViewById<Button>(R.id.signout_otherid)

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        signOutOtherBtn.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Other devices signed out", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showSignOutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(
            R.layout.signout_dialog,
            null
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_buttonid)
        val signOutBtn = dialogView.findViewById<Button>(R.id.signout_buttonid)

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        signOutBtn.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Successfully signed out", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showDeleteAccount() {
        val dialogView = LayoutInflater.from(this).inflate(
            R.layout.delete_account_dialog,
            null
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_button)
        val deleteBtn = dialogView.findViewById<Button>(R.id.dialog_delete_btn)

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        deleteBtn.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmationDialog()
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(
            R.layout.delete_confirmation_dialog,
            null
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_confirm_button)
        val deleteBtn = dialogView.findViewById<Button>(R.id.delete_confirm_button)

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        deleteBtn.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }
}
