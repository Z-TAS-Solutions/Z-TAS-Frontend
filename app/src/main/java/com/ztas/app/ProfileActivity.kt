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
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import com.ztas.app.network.AssertionResponseBody
import com.ztas.app.network.BeginLoginRequest
import com.ztas.app.network.DeleteAccountRequest
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
    private val webAuthnApi = RetrofitClient.webAuthnApi
    private lateinit var credentialManager: CredentialManager

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

        credentialManager = CredentialManager.create(this)

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
        val cachedName = AuthPreferences.cachedName(this)
        val emailView = findViewById<TextView>(R.id.useremail)
        val nameView = findViewById<TextView>(R.id.username)

        if (cachedEmail.isNotBlank()) {
            emailView.text = cachedEmail
        }
        // Prefer the real name captured at registration; fall back to an
        // email-derived guess only if no name was ever stored.
        nameView.text = when {
            cachedName.isNotBlank() -> cachedName
            cachedEmail.isNotBlank() -> displayNameFromEmail(cachedEmail)
            else -> nameView.text
        }
    }

    /** Turns "ravi.kumar@example.com" → "Ravi Kumar" as a friendly fallback. */
    private fun displayNameFromEmail(email: String): String {
        val local = email.substringBefore('@', missingDelimiterValue = email)
        if (local.isBlank()) return "User"
        return local
            .split('.', '_', '-', '+')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
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
                    if (profile.name.isNotBlank()) {
                        findViewById<TextView>(R.id.username).text = profile.name
                        AuthPreferences.setCachedName(this@ProfileActivity, profile.name)
                    }
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
            val originalText = deleteBtn.text
            deleteBtn.text = "VERIFYING…"

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val token = authHeaderOrNull()
                    val email = AuthPreferences.cachedEmail(this@ProfileActivity)
                    if (token == null || email.isBlank()) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Session expired. Please sign in again.",
                            Toast.LENGTH_SHORT
                        ).show()
                        deleteBtn.text = originalText
                        deleteBtn.isEnabled = true
                        return@launch
                    }

                    // Step 1: re-authenticate with the user's passkey before destroying the account.
                    val verified = verifyWithPasskey(email)
                    if (!verified) {
                        deleteBtn.text = originalText
                        deleteBtn.isEnabled = true
                        return@launch
                    }

                    deleteBtn.text = "DELETING…"

                    // Step 2: actually delete. Password is omitted now that the user proved
                    // ownership with their passkey.
                    val response = withContext(Dispatchers.IO) {
                        userApi.deleteAccount(
                            token = token,
                            request = DeleteAccountRequest()
                        )
                    }

                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Account deleted successfully",
                            Toast.LENGTH_LONG
                        ).show()
                        dialog.dismiss()
                        AuthPreferences.clear(this@ProfileActivity)
                        startActivity(Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    } else {
                        val errBody = response.errorBody()?.string().orEmpty()
                        Log.e(TAG, "Delete account failed: ${response.code()} body=$errBody")
                        val msg = when (response.code()) {
                            401, 403 -> "Passkey verification expired. Please try again."
                            else -> "Failed to delete account. Please try again."
                        }
                        Toast.makeText(this@ProfileActivity, msg, Toast.LENGTH_LONG).show()
                        deleteBtn.text = originalText
                        deleteBtn.isEnabled = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting account", e)
                    Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    deleteBtn.text = originalText
                    deleteBtn.isEnabled = true
                }
            }
        }

        dialog.show()
    }

    // ───────────────────────────────────────────────────────────────────
    // WebAuthn re-authentication for sensitive actions (e.g. delete account)
    // ───────────────────────────────────────────────────────────────────

    /**
     * Runs the same passkey ceremony used at login (`/webauthn/login/begin` →
     * CredentialManager → `/webauthn/login/finish`) purely as a re-authentication
     * gate. Returns `true` only when the server validates the assertion.
     */
    private suspend fun verifyWithPasskey(email: String): Boolean {
        return try {
            val begin = withContext(Dispatchers.IO) {
                webAuthnApi.beginLogin(BeginLoginRequest(email = email))
            }
            if (!begin.isSuccessful || begin.body() == null) {
                val err = begin.errorBody()?.string().orEmpty()
                Log.e(TAG, "Passkey begin failed: ${begin.code()} — $err")
                Toast.makeText(this, "Couldn't start passkey check. Please try again.", Toast.LENGTH_LONG).show()
                return false
            }

            val parsed = parseLoginBeginPayload(begin.body()!!.string()) ?: run {
                Log.e(TAG, "Passkey begin: could not parse server response")
                Toast.makeText(this, "Passkey check failed (bad server response).", Toast.LENGTH_LONG).show()
                return false
            }

            val getJson = JSONObject().apply {
                put("challenge", parsed.challenge)
                put("rpId", parsed.rpId)
                put("timeout", parsed.timeout)
                put("userVerification", parsed.userVerification)
                put("allowCredentials", parsed.allowCredentials)
            }.toString()

            val getRequest = GetCredentialRequest(listOf(GetPublicKeyCredentialOption(getJson)))
            val result = credentialManager.getCredential(this@ProfileActivity, getRequest)
            val credential = result.credential as? PublicKeyCredential ?: run {
                Toast.makeText(this, "Unexpected credential type.", Toast.LENGTH_SHORT).show()
                return false
            }

            val assertionJson = JSONObject(credential.authenticationResponseJson)
            val responseObj = assertionJson.getJSONObject("response")
            val finishRequest = FinishLoginRequest(
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

            val finish = withContext(Dispatchers.IO) {
                webAuthnApi.finishLogin(sessionToken = parsed.sessionToken, request = finishRequest)
            }
            if (!finish.isSuccessful) {
                val err = finish.errorBody()?.string().orEmpty()
                Log.e(TAG, "Passkey finish failed: ${finish.code()} — $err")
                Toast.makeText(this, "Passkey verification failed. Please try again.", Toast.LENGTH_LONG).show()
                return false
            }
            true
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Passkey verification cancelled / failed", e)
            Toast.makeText(this, "Passkey verification was cancelled.", Toast.LENGTH_SHORT).show()
            false
        } catch (e: Exception) {
            Log.e(TAG, "Passkey verification error", e)
            Toast.makeText(this, "Passkey error: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private data class ParsedLoginBegin(
        val sessionToken: String,
        val challenge: String,
        val rpId: String,
        val timeout: Long,
        val userVerification: String,
        val allowCredentials: JSONArray
    )

    /** Mirror of [LoginActivity.parseLoginBeginPayload] kept local so the two flows can evolve independently. */
    private fun parseLoginBeginPayload(raw: String): ParsedLoginBegin? {
        return try {
            val root = JSONObject(raw)
            val data = root.optJSONObject("data")
            val sessionToken = sequenceOf(
                data?.optString("session_token").orEmpty(),
                data?.optString("sessionToken").orEmpty(),
                root.optString("session_token"),
                root.optString("sessionToken")
            ).firstOrNull { it.isNotEmpty() } ?: return null

            val candidates = mutableListOf<JSONObject>()
            fun add(obj: JSONObject?) { if (obj != null) candidates.add(obj) }
            data?.let { d ->
                add(d.optJSONObject("assertion_data"))
                add(d.optJSONObject("assertionData"))
                add(d.optJSONObject("assertion"))
                add(d.optJSONObject("publicKey"))
                add(d.optJSONObject("options"))
            }
            add(root.optJSONObject("assertion_data"))
            add(root.optJSONObject("publicKey"))

            for (assertion in candidates) {
                val opts = assertion.optJSONObject("response")
                    ?: assertion.optJSONObject("publicKey")
                    ?: assertion
                buildParsedBegin(opts, sessionToken)?.let { return it }
            }
            data?.let { buildParsedBegin(it, sessionToken) }
        } catch (e: Exception) {
            Log.e(TAG, "parseLoginBeginPayload", e)
            null
        }
    }

    private fun buildParsedBegin(options: JSONObject, sessionToken: String): ParsedLoginBegin? {
        val challenge = options.optString("challenge").ifEmpty { options.optString("Challenge") }
        if (challenge.isEmpty()) return null
        var rpId = options.optString("rpId").ifEmpty { options.optString("rp_id") }
        if (rpId.isEmpty()) rpId = options.optJSONObject("rp")?.optString("id").orEmpty()
        if (rpId.isEmpty()) return null
        val timeout = if (options.has("timeout") && !options.isNull("timeout"))
            options.optLong("timeout", 120_000L).takeIf { it > 0 } ?: 120_000L
        else 120_000L
        val uv = options.optString("userVerification").ifEmpty {
            options.optString("user_verification")
        }.ifEmpty { "required" }
        val allow = options.optJSONArray("allowCredentials")
            ?: options.optJSONArray("allow_credentials")
            ?: JSONArray()
        return ParsedLoginBegin(sessionToken, challenge, rpId, timeout, uv, allow)
    }
}
