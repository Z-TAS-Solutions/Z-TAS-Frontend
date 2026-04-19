package com.ztas.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.ztas.app.network.DeleteAccountRequest
import com.ztas.app.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private val userApi = RetrofitClient.userApi
    private val sessionApi = RetrofitClient.sessionApi
    private val passkeyCredentialManager by lazy { CredentialManager.create(this) }

    private val pickProfileImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { persistProfileImageFromUri(uri) }
                .onSuccess { applySavedProfilePhoto() }
                .onFailure {
                    Log.e(TAG, "Profile image save failed", it)
                    Toast.makeText(this, "Could not save photo", Toast.LENGTH_SHORT).show()
                }
        }
    }

    companion object {
        private const val TAG = "ProfileActivity"
    }

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

        // Set up click listeners for the cards
        findViewById<ImageView>(R.id.settingsIcon)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageView>(R.id.backArrow)?.setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.changePasswordCard).setOnClickListener {
            // Unchanged for now
        }

        findViewById<LinearLayout>(R.id.activeSessionsCard).setOnClickListener {
            showActiveSessionsDialog()
        }

        findViewById<LinearLayout>(R.id.signOutCard).setOnClickListener {
            showSignOutDialog()
        }

        findViewById<LinearLayout>(R.id.deleteAccountCard).setOnClickListener {
            showDeleteAccount()
        }

        findViewById<FrameLayout>(R.id.profilePicFrame)?.setOnClickListener {
            pickProfileImage.launch("image/*")
        }

        applySavedProfilePhoto()
        applyCachedIdentity()
        loadProfileData()
    }

    /**
     * Renders username + email immediately from local cache so the header is never empty
     * (even if /user/profile is slow or returns 404). Real values overwrite these once the API responds.
     */
    private fun applyCachedIdentity() {
        val cachedEmail = AuthPreferences.cachedEmail(this)
        val emailView = findViewById<TextView>(R.id.useremail)
        val nameView = findViewById<TextView>(R.id.username)

        if (cachedEmail.isNotBlank()) {
            emailView.text = cachedEmail
        }
        val cachedName = AuthPreferences.cachedDisplayName(this).trim()
        if (cachedName.isNotBlank()) {
            nameView.text = cachedName
        } else if (cachedEmail.isNotBlank()) {
            nameView.text = ProfileDisplayName.displayNameFromEmail(cachedEmail)
        }
    }

    private fun authHeaderOrNull(): String? = AuthPreferences.bearerOrNull(this)

    private fun applySavedProfilePhoto() {
        val path = AuthPreferences.profileImagePath(this) ?: return
        val f = File(path)
        if (f.exists()) {
            findViewById<ImageView>(R.id.profilePic).setImageURI(Uri.fromFile(f))
        }
    }

    private fun persistProfileImageFromUri(uri: Uri) {
        val dest = File(filesDir, "profile_avatar.jpg")
        contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open image")
        AuthPreferences.setProfileImagePath(this, dest.absolutePath)
    }

    private fun loadProfileData() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val token = authHeaderOrNull()
                if (token == null) {
                    Toast.makeText(this@ProfileActivity, "Not signed in", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val response = withContext(Dispatchers.IO) {
                    userApi.getProfile(token)
                }

                val raw = response.body()?.string()
                val profile = raw?.let { UserProfileJson.parse(it) }

                if (response.isSuccessful && profile != null) {
                    val resolved =
                        ProfileDisplayName.headerName(this@ProfileActivity, profile.name, profile.email)
                    findViewById<TextView>(R.id.username).text = resolved
                    val emailForCheck =
                        profile.email.ifBlank { AuthPreferences.cachedEmail(this@ProfileActivity) }
                    ProfileDisplayName.persistIfRichLabel(this@ProfileActivity, resolved, emailForCheck)
                    if (profile.email.isNotBlank()) {
                        findViewById<TextView>(R.id.useremail).text = profile.email
                    }
                    findViewById<TextView>(R.id.status).text = "STATUS: ${profile.status.uppercase()}"
                    findViewById<TextView>(R.id.activeDevicesCount).text =
                        "${profile.activeDevices} devices connected"
                    findViewById<TextView>(R.id.lastSync).text = getLastSyncText(profile.lastLogin)
                    findViewById<TextView>(R.id.biometricStatus).text = profile.status.uppercase()
                    findViewById<TextView>(R.id.securityLevel).text = profile.securityLevel.uppercase()
                } else {
                    Log.e(TAG, "Failed to load profile: ${response.code()} body=$raw")
                    // Keep cached username/email already shown by applyCachedIdentity().
                    // Only surface a toast for unexpected errors, not for 404 "no profile" cases.
                    if (response.code() != 404) {
                        Toast.makeText(this@ProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile", e)
                Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLastSyncText(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestampMs

        // If the timestamp is 0 or negative (which means it wasn't returned or is invalid), say "Just now"
        if (timestampMs <= 0L) return "Just now"

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

        // Temporarily, we will set the static XML text fields to dynamic data by fetching from API
        // Then we'll update the values. Our XML has two static devices. We will populate as many as we can fit.
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val token = authHeaderOrNull() ?: return@launch
                val response = withContext(Dispatchers.IO) {
                    sessionApi.getSessions(token, limit = 5)
                }

                if (response.isSuccessful) {
                    val sessions = response.body()?.data?.sessions.orEmpty()
                    // In a real scenario, this dialog should use a RecyclerView or Compose for dynamic counts.
                    // For now, let's keep it simple: we know our XML has two hardcoded blocks. 
                    // This serves as an immediate visual update without massive UI changes to the static XML.
                    Log.d(TAG, "Loaded ${sessions.size} sessions")
                } else {
                    Log.e(TAG, "Failed to load sessions: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sessions", e)
            }
        }

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        signOutOtherBtn.setOnClickListener {
            // Disable button during network call
            signOutOtherBtn.isEnabled = false
            
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val token = authHeaderOrNull() ?: return@launch
                    val response = withContext(Dispatchers.IO) {
                        sessionApi.logoutOtherDevices(token)
                    }

                    if (response.isSuccessful && response.body() != null) {
                        val terminated = response.body()!!.data.sessionsTerminated
                        Toast.makeText(this@ProfileActivity, "$terminated other devices signed out", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        // Reload profile to refresh device count
                        loadProfileData()
                    } else {
                        Log.e(TAG, "Logout others failed: ${response.code()}")
                        Toast.makeText(this@ProfileActivity, "Failed to sign out other devices", Toast.LENGTH_SHORT).show()
                        signOutOtherBtn.isEnabled = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error logging out others", e)
                    Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    signOutOtherBtn.isEnabled = true
                }
            }
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
            AuthPreferences.clear(this)
            Toast.makeText(this, "Successfully signed out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
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
            deleteBtn.isEnabled = false

            lifecycleScope.launch {
                try {
                    val emailOrNull = AuthPreferences.cachedEmail(this@ProfileActivity).trim().ifEmpty { null }
                    val passkeyOutcome = WebAuthnLoginFlow.authenticate(
                        activity = this@ProfileActivity,
                        credentialManager = passkeyCredentialManager,
                        emailOrNull = emailOrNull
                    )
                    when (passkeyOutcome) {
                        is WebAuthnLoginFlow.PasskeyOutcome.Error -> {
                            Toast.makeText(this@ProfileActivity, passkeyOutcome.message, Toast.LENGTH_LONG).show()
                            deleteBtn.isEnabled = true
                        }
                        is WebAuthnLoginFlow.PasskeyOutcome.Success -> {
                            val bearer = WebAuthnLoginFlow.bearerHeaderForToken(passkeyOutcome.data.token)
                            val response = withContext(Dispatchers.IO) {
                                val req = DeleteAccountRequest("")
                                val del = userApi.deleteAccountDelete(token = bearer, request = req)
                                if (del.code() == 404) {
                                    userApi.deleteAccountPost(token = bearer, request = req)
                                } else {
                                    del
                                }
                            }
                            if (response.isSuccessful) {
                                Toast.makeText(this@ProfileActivity, "Account deleted successfully", Toast.LENGTH_LONG).show()
                                dialog.dismiss()
                                AuthPreferences.clear(this@ProfileActivity)
                                startActivity(Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                                finish()
                            } else {
                                val errorBody = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
                                Log.e(TAG, "Delete account failed: ${response.code()} - $errorBody")
                                val message = when {
                                    errorBody.contains("password", ignoreCase = true) ->
                                        "Delete failed: backend still requires password confirmation."
                                    errorBody.contains("unauthorized", ignoreCase = true) ||
                                        response.code() == 401 ->
                                        "Delete failed: passkey was accepted, but the delete API rejected authorization."
                                    errorBody.isNotBlank() ->
                                        "Delete failed: $errorBody"
                                    else ->
                                        "Failed to delete account (${response.code()})"
                                }
                                Toast.makeText(this@ProfileActivity, message, Toast.LENGTH_LONG).show()
                                deleteBtn.isEnabled = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting account", e)
                    Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    deleteBtn.isEnabled = true
                }
            }
        }

        dialog.show()
    }
}
