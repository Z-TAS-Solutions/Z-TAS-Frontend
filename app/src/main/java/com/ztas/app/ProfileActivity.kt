package com.ztas.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isGone
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.ztas.app.network.AssertionResponseBody
import com.ztas.app.network.FinishLoginRequest
import com.ztas.app.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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

        // Drop any previously-cached display name that turned out to be just the
        // email handle (e.g. left over from older builds) so applyCachedIdentity()
        // can fall back cleanly and a real name from /user/profile can replace it.
        AuthPreferences.clearCachedDisplayNameIfEmailHandle(this)

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
        val tsMs = normalizeEpochMillis(timestampMs)
        val diffMs = now - tsMs

        // If the timestamp is 0 or negative (which means it wasn't returned or is invalid), say "Just now"
        if (tsMs <= 0L) return "Just now"

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

    private fun normalizeEpochMillis(value: Long): Long {
        if (value <= 0L) return value
        // Heuristic: seconds since epoch are ~1e9..1e10; millis are ~1e12..1e13
        return if (value in 1..9_999_999_999L) value * 1000L else value
    }

    private fun looksGenericDeviceName(raw: String): Boolean {
        val v = raw.trim().lowercase()
        if (v.isBlank()) return true
        return v.startsWith("okhttp/") ||
            v == "android" ||
            v == "unknown" ||
            v == "mobile"
    }

    private fun localDeviceLabel(): String {
        val brand = Build.BRAND.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        return listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { "This device" }
    }

    private fun inferFriendlyDeviceLabel(rawName: String): String {
        val v = rawName.trim().lowercase()
        if (v.isBlank()) return "Other device"

        return when {
            "iphone" in v || "ipad" in v || "ios" in v -> "iPhone"
            "windows" in v -> "Windows PC"
            "mac os" in v || "macos" in v || "macintosh" in v -> "Mac"
            "linux" in v -> "Linux device"
            "android" in v ||
                "samsung" in v ||
                "oppo" in v ||
                "vivo" in v ||
                "xiaomi" in v ||
                "redmi" in v ||
                "oneplus" in v ||
                "pixel" in v ||
                "huawei" in v -> "Android phone"
            "mozilla/" in v || "chrome/" in v || "safari/" in v || "firefox/" in v -> "Web browser"
            looksGenericDeviceName(v) -> "Android phone"
            else -> rawName.trim()
        }
    }

    private fun displayDeviceName(rawName: String, isCurrent: Boolean): String {
        if (isCurrent && looksGenericDeviceName(rawName)) return localDeviceLabel()
        return inferFriendlyDeviceLabel(rawName)
    }

    private data class ParsedDeleteBegin(
        val sessionToken: String,
        val requestJson: String
    )

    private fun parseDeleteBeginPayload(raw: String): ParsedDeleteBegin? {
        return try {
            val root = JSONObject(raw)
            val data = root.optJSONObject("data") ?: root
            val sessionToken = sequenceOf(
                data.optString("sessionToken"),
                data.optString("session_token"),
                root.optString("sessionToken"),
                root.optString("session_token")
            ).map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return null

            val assertion = data.optJSONObject("assertionData")
                ?: data.optJSONObject("assertion_data")
                ?: data.optJSONObject("assertion")
                ?: run {
                    val s = data.optString("assertionData")
                    if (s.trimStart().startsWith("{")) JSONObject(s) else null
                }
                ?: return null

            val options = assertion.optJSONObject("response")
                ?: assertion.optJSONObject("publicKey")
                ?: assertion

            val challenge = options.optString("challenge").ifBlank { options.optString("Challenge") }
            var rpId = options.optString("rpId").ifBlank { options.optString("rp_id") }
            if (rpId.isBlank()) {
                rpId = options.optJSONObject("rp")?.optString("id").orEmpty()
            }
            if (challenge.isBlank() || rpId.isBlank()) return null

            val timeout = when {
                options.has("timeout") && !options.isNull("timeout") ->
                    options.optLong("timeout", 120_000L).takeIf { it > 0 }
                        ?: options.optInt("timeout", 120_000).toLong()
                else -> 120_000L
            }
            val userVerification = options.optString("userVerification").ifBlank {
                options.optString("user_verification")
            }.ifBlank { "required" }
            val allowCredentials = options.optJSONArray("allowCredentials")
                ?: options.optJSONArray("allow_credentials")
                ?: JSONArray()

            val requestJson = JSONObject().apply {
                put("challenge", challenge)
                put("rpId", rpId)
                put("timeout", timeout)
                put("userVerification", userVerification)
                put("allowCredentials", allowCredentials)
            }.toString()

            ParsedDeleteBegin(sessionToken = sessionToken, requestJson = requestJson)
        } catch (e: Exception) {
            Log.e(TAG, "parseDeleteBeginPayload failed", e)
            null
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

        val row1 = dialogView.findViewById<LinearLayout>(R.id.device_row_1)
        val row2 = dialogView.findViewById<LinearLayout>(R.id.device_row_2)
        val name1 = dialogView.findViewById<TextView>(R.id.device_name_1)
        val status1 = dialogView.findViewById<TextView>(R.id.device_status_1)
        val icon1 = dialogView.findViewById<ImageView>(R.id.device_icon_1)
        val name2 = dialogView.findViewById<TextView>(R.id.device_name_2)
        val status2 = dialogView.findViewById<TextView>(R.id.device_status_2)
        val icon2 = dialogView.findViewById<ImageView>(R.id.device_icon_2)

        fun bindRow(
            deviceNameView: TextView,
            statusView: TextView,
            iconView: ImageView,
            deviceName: String,
            isCurrent: Boolean,
            lastActive: Long
        ) {
            deviceNameView.text = deviceName.ifBlank { "Unknown device" }
            if (isCurrent) {
                statusView.text = "Current Device"
                statusView.setTextColor(android.graphics.Color.parseColor("#00ccff"))
                iconView.setColorFilter(android.graphics.Color.parseColor("#00ccff"))
            } else {
                statusView.text = "Last active: ${getLastSyncText(lastActive)}"
                statusView.setTextColor(android.graphics.Color.parseColor("#888888"))
                iconView.setColorFilter(android.graphics.Color.parseColor("#888888"))
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val token = authHeaderOrNull() ?: return@launch
                val response = withContext(Dispatchers.IO) {
                    sessionApi.getSessions(token, limit = 5)
                }

                if (response.isSuccessful) {
                    val sessions = response.body()?.data?.sessions.orEmpty()
                    val first = sessions.getOrNull(0)
                    val second = sessions.getOrNull(1)

                    if (first != null) {
                        row1.isGone = false
                        bindRow(
                            deviceNameView = name1,
                            statusView = status1,
                            iconView = icon1,
                            deviceName = displayDeviceName(first.deviceName, first.current),
                            isCurrent = first.current,
                            lastActive = first.lastActive
                        )
                    } else {
                        row1.isGone = true
                    }

                    if (second != null) {
                        row2.isGone = false
                        bindRow(
                            deviceNameView = name2,
                            statusView = status2,
                            iconView = icon2,
                            deviceName = displayDeviceName(second.deviceName, second.current),
                            isCurrent = second.current,
                            lastActive = second.lastActive
                        )
                    } else {
                        row2.isGone = true
                    }

                    // Disable "Sign Out Other" when there's no other device.
                    signOutOtherBtn.isEnabled = sessions.any { !it.current }
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
                    val bearer = authHeaderOrNull()
                    if (bearer == null) {
                        Toast.makeText(this@ProfileActivity, "Not signed in", Toast.LENGTH_SHORT).show()
                        deleteBtn.isEnabled = true
                        return@launch
                    }

                    val beginResponse = withContext(Dispatchers.IO) {
                        userApi.beginDeleteAccount(token = bearer)
                    }
                    if (!beginResponse.isSuccessful || beginResponse.body() == null) {
                        val err = runCatching { beginResponse.errorBody()?.string().orEmpty() }.getOrDefault("")
                        val msg = if (err.isNotBlank()) "Delete begin failed: $err"
                        else "Delete begin failed (${beginResponse.code()})"
                        Toast.makeText(this@ProfileActivity, msg, Toast.LENGTH_LONG).show()
                        deleteBtn.isEnabled = true
                        return@launch
                    }

                    val parsedBegin = parseDeleteBeginPayload(beginResponse.body()!!.string())
                    if (parsedBegin == null) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Delete failed: invalid passkey challenge payload.",
                            Toast.LENGTH_LONG
                        ).show()
                        deleteBtn.isEnabled = true
                        return@launch
                    }

                    val getOption = GetPublicKeyCredentialOption(parsedBegin.requestJson)
                    val getRequest = GetCredentialRequest(listOf(getOption))
                    val result = passkeyCredentialManager.getCredential(this@ProfileActivity, getRequest)
                    val credential = result.credential
                    if (credential !is PublicKeyCredential) {
                        Toast.makeText(this@ProfileActivity, "Unexpected credential type", Toast.LENGTH_LONG).show()
                        deleteBtn.isEnabled = true
                        return@launch
                    }

                    val assertionJson = JSONObject(credential.authenticationResponseJson)
                    val responseObj = assertionJson.getJSONObject("response")
                    val request = FinishLoginRequest(
                        id = assertionJson.getString("id"),
                        rawId = assertionJson.getString("rawId"),
                        response = AssertionResponseBody(
                            authenticatorData = responseObj.getString("authenticatorData"),
                            clientDataJSON = responseObj.getString("clientDataJSON"),
                            signature = responseObj.getString("signature"),
                            userHandle = responseObj.optString("userHandle", "")
                        ),
                        type = assertionJson.optString("type", "public-key")
                    )

                    val confirmResponse = withContext(Dispatchers.IO) {
                        userApi.confirmDeleteAccount(
                            token = bearer,
                            sessionToken = parsedBegin.sessionToken,
                            request = request
                        )
                    }
                    if (confirmResponse.isSuccessful) {
                        Toast.makeText(this@ProfileActivity, "Account deleted successfully", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        AuthPreferences.clear(this@ProfileActivity)
                        startActivity(Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    } else {
                        val errorBody = runCatching { confirmResponse.errorBody()?.string().orEmpty() }.getOrDefault("")
                        Log.e(TAG, "Delete confirm failed: ${confirmResponse.code()} - $errorBody")
                        val message = if (errorBody.isNotBlank()) "Delete failed: $errorBody"
                        else "Failed to delete account (${confirmResponse.code()})"
                        Toast.makeText(this@ProfileActivity, message, Toast.LENGTH_LONG).show()
                        deleteBtn.isEnabled = true
                    }
                } catch (e: GetCredentialException) {
                    Log.e(TAG, "Passkey confirmation failed", e)
                    Toast.makeText(
                        this@ProfileActivity,
                        "Passkey confirmation failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    deleteBtn.isEnabled = true
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
