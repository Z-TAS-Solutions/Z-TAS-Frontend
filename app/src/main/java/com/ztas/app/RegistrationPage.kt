package com.ztas.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.ztas.app.network.RegisterRequest
import com.ztas.app.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RegistrationPage : AppCompatActivity() {

    private val userApi = RetrofitClient.userApi

    companion object {
        private const val TAG = "RegistrationPage"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_page)

        // Ensure content scrolls above the keyboard (works reliably across devices)
        val registrationScroll = findViewById<ScrollView>(R.id.registrationScroll)
        val baseBottomPadding = registrationScroll.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(registrationScroll) { v, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = baseBottomPadding + maxOf(imeBottom, navBottom))
            insets
        }

        // All input fields
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etNIC = findViewById<EditText>(R.id.etNIC)
        val etPhone = findViewById<EditText>(R.id.etPhone)

        val registerButton = findViewById<Button>(R.id.btnRegister)

        registerButton.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val nic = etNIC.text.toString().trim()
            val rawPhone = etPhone.text.toString()
            val phone = sanitizePhoneInput(rawPhone)

            val nameRegex = Regex("^[A-Za-z ]+$")

            if (name.isEmpty()) { etName.error = "Name is required"; return@setOnClickListener }
            if (!nameRegex.matches(name)) { etName.error = "Name should contain only letters"; return@setOnClickListener }

            if (email.isEmpty()) { etEmail.error = "Email required"; return@setOnClickListener }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.error = "Enter a valid email"; return@setOnClickListener }

            if (nic.isEmpty()) { etNIC.error = "NIC number is required"; return@setOnClickListener }
            val nicForApi = normalizeNicForApi(nic)
            if (!isValidSriLankanNic(nicForApi)) {
                etNIC.error = "Use old NIC (9 digits + V or X) or new 12-digit NIC"
                return@setOnClickListener
            }

            if (phone.isEmpty()) { etPhone.error = "Phone number required"; return@setOnClickListener }
            if (!isValidSriLankanPhone(phone)) {
                etPhone.error = "Use 07XXXXXXXX or +94XXXXXXXXX"
                return@setOnClickListener
            }
            val phoneForApi = normalizePhoneForApi(phone)
            if (!phoneForApi.matches(Regex("^07\\d{8}$"))) {
                etPhone.error = "Invalid phone number format"
                return@setOnClickListener
            }
            Log.d(TAG, "Phone normalized for register: input=$rawPhone normalized=$phoneForApi")

            // Disable inputs during API call (prevents "unresponsive" feel)
            val originalButtonText = registerButton.text
            registerButton.isEnabled = false
            registerButton.text = "REGISTERING…"
            etName.isEnabled = false
            etEmail.isEnabled = false
            etNIC.isEnabled = false
            etPhone.isEnabled = false
            registerUser(name, email, nicForApi, phoneForApi) {
                registerButton.isEnabled = true
                registerButton.text = originalButtonText
                etName.isEnabled = true
                etEmail.isEnabled = true
                etNIC.isEnabled = true
                etPhone.isEnabled = true
            }
        }

        // Login link
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        val fullText = "Already have an account? Log in"

        val spannableString = SpannableString(fullText)

        val startIndex = fullText.indexOf("Log in")
        val endIndex = startIndex + "Log in".length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                val intent = Intent(this@RegistrationPage, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
            }
        }

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(
            ForegroundColorSpan(Color.parseColor("#00C4FF")),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvLoginLink.text = spannableString
        tvLoginLink.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Calls POST /admin/users/register/new and on success navigates
     * to OtpInputPage with the userId.
     */
    private fun registerUser(
        name: String,
        email: String,
        nic: String,
        phone: String,
        onComplete: () -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    userApi.register(RegisterRequest(name, email, nic, phone))
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val backendReportedFailure =
                        body.data?.success == false ||
                            body.status.equals("failed", ignoreCase = true) ||
                            body.status.equals("error", ignoreCase = true)
                    if (backendReportedFailure) {
                        Toast.makeText(
                            this@RegistrationPage,
                            body.message ?: "Registration failed. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    val resolvedUserId = body.data?.customId ?: body.customId ?: body.userId.orEmpty()
                    val resolvedMessage =
                        body.message
                            ?: if (body.status.equals("success", ignoreCase = true)) "Registration successful." else null
                            ?: "Registration successful."

                    if (resolvedUserId.isBlank()) {
                        Log.e(TAG, "Registration response missing user id. body=$body")
                        Toast.makeText(
                            this@RegistrationPage,
                            "Registration succeeded, but the server didn't return a user id. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    Log.d(TAG, "Registration success: $resolvedMessage, userId=$resolvedUserId")

                    // Persist immediately so profile/home can show the real name even if OTP/passkey extras fail.
                    AuthPreferences.setCachedDisplayName(
                        this@RegistrationPage,
                        name,
                        emailForCheck = email
                    )
                    AuthPreferences.setCachedName(this@RegistrationPage, name)

                    Toast.makeText(
                        this@RegistrationPage,
                        resolvedMessage,
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to OTP page, passing userId
                    val intent = Intent(this@RegistrationPage, OtpInputPage::class.java).apply {
                        putExtra("USER_ID", resolvedUserId)
                        putExtra("USER_EMAIL", email)
                        putExtra("USER_PHONE", phone)
                        putExtra("USER_DISPLAY_NAME", name)
                    }
                    startActivity(intent)
                    // Do not finish(): keep Registration in the back stack so Back from OTP returns here.
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    val cleanMessage = sanitizeServerError(errorBody, response.code())
                    Log.e(TAG, "Registration failed (HTTP ${response.code()}): $cleanMessage")
                    Toast.makeText(
                        this@RegistrationPage,
                        cleanMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error", e)
                val msg = when (e) {
                    is SocketTimeoutException ->
                        "Can't reach server (timeout). Check Wi‑Fi/VPN and server status, then try again."
                    is UnknownHostException ->
                        "Can't reach server (DNS). Check internet connection and try again."
                    is ConnectException ->
                        "Server refused the connection. Check the API IP/port and firewall rules."
                    else -> "An error occurred: ${e.message ?: "unknown error"}"
                }
                Toast.makeText(this@RegistrationPage, msg, Toast.LENGTH_LONG).show()
            } finally {
                onComplete()
            }
        }
    }

    private fun sanitizeServerError(raw: String, code: Int): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "Registration failed (HTTP $code). Please try again."
        val looksLikeHtml =
            trimmed.startsWith("<!doctype", ignoreCase = true) ||
                trimmed.startsWith("<html", ignoreCase = true) ||
                trimmed.contains("<body", ignoreCase = true) ||
                Regex("<\\s*[a-zA-Z][^>]*>").containsMatchIn(trimmed)
        if (looksLikeHtml) return "Registration failed (HTTP $code). Please try again."

        // If backend sends JSON, extract message-style fields first.
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            runCatching {
                val obj = JSONObject(trimmed)
                val directMessage = obj.optString("message").trim()
                val data = obj.optJSONObject("data")
                val duplicateFields = extractDuplicateFields(directMessage, data)
                if (duplicateFields.isNotEmpty()) {
                    val pretty = duplicateFields.joinToString(", ") { it.uppercase() }
                    return "$pretty is already registered. Please use different details."
                }

                if (directMessage.isNotEmpty()) {
                    return "Registration failed: $directMessage"
                }

                val status = obj.optString("status").trim()
                val success = data?.optBoolean("success")
                if (success == false) {
                    val customId = data.optString("custom_id").trim()
                    if (customId.isNotEmpty()) {
                        return "Registration failed: $customId"
                    }
                    if (status.isNotEmpty()) {
                        return "Registration failed: $status"
                    }
                    if (code == 409) {
                        return "Email, NIC, or phone is already registered. Please use different details."
                    }
                    return "Registration failed. Please check details and try again."
                }
            }
        }

        // Strip any tags if present and collapse whitespace
        val noTags = trimmed.replace(Regex("<[^>]*>"), " ")
        val clean = noTags.replace(Regex("\\s+"), " ").trim()
        val limited = if (clean.length > 180) clean.take(180) + "…" else clean
        return "Registration failed: $limited"
    }

    private fun extractDuplicateFields(message: String, data: JSONObject?): List<String> {
        val found = linkedSetOf<String>()

        fun addIfKnown(value: String?) {
            val normalized = value?.trim()?.lowercase().orEmpty()
            when (normalized) {
                "email" -> found.add("Email")
                "nic" -> found.add("NIC")
                "phone", "phone_number", "mobile" -> found.add("Phone")
            }
        }

        val lowerMessage = message.lowercase()
        if ("email" in lowerMessage) found.add("Email")
        if ("nic" in lowerMessage) found.add("NIC")
        if ("phone" in lowerMessage || "mobile" in lowerMessage) found.add("Phone")

        if (data != null) {
            addIfKnown(data.optString("duplicate_field"))
            addIfKnown(data.optString("conflict_field"))

            val duplicateFieldsArray = data.optJSONArray("duplicate_fields")
            if (duplicateFieldsArray != null) {
                for (i in 0 until duplicateFieldsArray.length()) {
                    addIfKnown(duplicateFieldsArray.optString(i))
                }
            }

            if (data.optBoolean("email_exists", false)) found.add("Email")
            if (data.optBoolean("nic_exists", false)) found.add("NIC")
            if (data.optBoolean("phone_exists", false)) found.add("Phone")
        }

        return found.toList()
    }

    /**
     * Sri Lankan NIC: legacy **9 digits + V or X**, or **new 12-digit** numeric NIC.
     * Spaces/dashes are stripped before checks; letter suffix normalized to uppercase.
     */
    private fun sanitizeNicInput(raw: String): String =
        raw.trim().replace(Regex("[\\s-]"), "")

    private fun isValidSriLankanNic(nic: String): Boolean {
        val n = nic.trim()
        if (n.matches(Regex("\\d{9}[VvXx]"))) return true
        if (n.matches(Regex("\\d{12}"))) return true
        return false
    }

    /** Sends NIC in the shape the backend validator expects. */
    private fun normalizeNicForApi(raw: String): String {
        val n = sanitizeNicInput(raw)
        if (n.matches(Regex("\\d{9}[VvXx]"))) {
            return n.dropLast(1) + n.last().uppercaseChar()
        }
        return n
    }

    private fun isValidSriLankanPhone(phone: String): Boolean {
        // Keep strict to formats backend explicitly accepts.
        return phone.matches(Regex("^07\\d{8}$")) || phone.matches(Regex("^\\+947\\d{8}$"))
    }

    private fun normalizePhoneForApi(phone: String): String {
        // Canonicalize to one stable backend format: 07XXXXXXXX.
        // This avoids backend issues where '+' might be stripped during later flows.
        val cleaned = sanitizePhoneInput(phone)
        return when {
            cleaned.matches(Regex("^07\\d{8}$")) -> cleaned
            cleaned.matches(Regex("^\\+94\\d{9}$")) -> "0${cleaned.removePrefix("+94")}"
            cleaned.matches(Regex("^94\\d{9}$")) -> "0${cleaned.removePrefix("94")}"
            cleaned.matches(Regex("^7\\d{8}$")) -> "0$cleaned"
            else -> cleaned
        }
    }

    private fun sanitizePhoneInput(input: String): String {
        // Keep only digits and a leading '+'; strips hidden/unicode separators too.
        val filtered = buildString(input.length) {
            input.forEachIndexed { index, ch ->
                if (ch.isDigit() || (ch == '+' && index == 0)) {
                    append(ch)
                }
            }
        }
        return filtered.trim()
    }
}