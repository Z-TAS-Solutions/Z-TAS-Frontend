package com.ztas.app

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import com.ztas.app.network.AssertionResponseBody
import com.ztas.app.network.BeginLoginRequest
import com.ztas.app.network.FinishLoginRequest
import com.ztas.app.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var credentialManager: CredentialManager
    private val webAuthnApi = RetrofitClient.webAuthnApi

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        credentialManager = CredentialManager.create(this)

        val etEmail = findViewById<EditText>(R.id.etName)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)
        val registerText = "New user? Create an account"
        val actionText = "Create an account"
        val spannable = SpannableString(registerText)
        val startIndex = registerText.indexOf(actionText)
        if (startIndex >= 0) {
            val endIndex = startIndex + actionText.length
            spannable.setSpan(
                ForegroundColorSpan(android.graphics.Color.parseColor("#00C4FF")),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            tvToRegister.text = spannable
        }

        tvToRegister.setOnClickListener {
            startActivity(Intent(this, RegistrationPage::class.java))
            // Do not finish(): keep Login in the back stack so Back returns here.
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Enter a valid email"
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            authenticateWithPasskey(email) {
                btnLogin.isEnabled = true
            }
        }
    }

    /**
     * WebAuthn login: `login/begin` with registered email, then CredentialManager, then `login/finish`
     * with `X-Session-Token` from begin. Server binds JSON `username` to email lookup.
     */
    private fun authenticateWithPasskey(email: String, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // ── Step 1: Begin Login ───────────────────────────────────
                val beginResponse = withContext(Dispatchers.IO) {
                    webAuthnApi.beginLogin(BeginLoginRequest(email = email))
                }

                if (!beginResponse.isSuccessful || beginResponse.body() == null) {
                    val errorBody = beginResponse.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Begin login failed: ${beginResponse.code()} — $errorBody")
                    val userMessage = when {
                        beginResponse.code() == 401 &&
                            errorBody.contains("User not found", ignoreCase = true) ->
                            "No account for this email. Use the email you registered with."
                        errorBody.contains("Found no credentials", ignoreCase = true) ||
                            errorBody.contains("no credentials for user", ignoreCase = true) ->
                            "No passkey for this account yet. Finish sign-up: verify email, then complete passkey setup before logging in."
                        else -> "Login failed: $errorBody"
                    }
                    Toast.makeText(
                        this@LoginActivity,
                        userMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                    return@launch
                }

                val beginJson = beginResponse.body()!!.string()
                val parsedBegin = parseLoginBeginPayload(beginJson)
                if (parsedBegin == null) {
                    Log.e(TAG, "Could not parse login/begin JSON: $beginJson")
                    val hint = serverMessageFromJson(beginJson)
                        .ifEmpty { "Could not read passkey options from server. Check Logcat tag $TAG for the raw JSON." }
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: $hint",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                    return@launch
                }

                // Build the JSON that Android's CredentialManager expects
                val requestJson = buildGetCredentialJson(parsedBegin)
                Log.d(TAG, "CredentialManager request JSON: $requestJson")

                // ── Step 2: CredentialManager — user taps fingerprint / face ─
                val getOption = GetPublicKeyCredentialOption(requestJson)
                val getRequest = GetCredentialRequest(listOf(getOption))

                val result = credentialManager.getCredential(this@LoginActivity, getRequest)

                val credential = result.credential
                if (credential !is PublicKeyCredential) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Unexpected credential type",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete()
                    return@launch
                }

                // Parse the assertion response JSON from CredentialManager
                val assertionJson = JSONObject(credential.authenticationResponseJson)
                Log.d(TAG, "Assertion JSON: $assertionJson")

                val responseObj = assertionJson.getJSONObject("response")

                // ── Step 3: Finish Login ─────────────────────────────────
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

                val finishResponse = withContext(Dispatchers.IO) {
                    webAuthnApi.finishLogin(
                        sessionToken = parsedBegin.sessionToken,
                        request = finishRequest
                    )
                }

                if (!finishResponse.isSuccessful || finishResponse.body() == null) {
                    val errorBody = finishResponse.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Finish login failed: ${finishResponse.code()} — $errorBody")
                    Toast.makeText(
                        this@LoginActivity,
                        "Login verification failed: $errorBody",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                    return@launch
                }

                val finishJson = finishResponse.body()!!.string()
                val finishData = parseLoginFinishPayload(finishJson)
                if (finishData == null) {
                    Log.e(TAG, "Could not parse login/finish JSON: $finishJson")
                    val hint = serverMessageFromJson(finishJson)
                        .ifEmpty { "Could not read token from server. Check Logcat tag $TAG for the raw JSON." }
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: $hint",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                    return@launch
                }
                Log.d(TAG, "Login success — userId=${finishData.userId}, role=${finishData.role}")

                AuthPreferences.saveSession(
                    this@LoginActivity,
                    accessToken = finishData.token,
                    userId = finishData.userId,
                    email = finishData.email,
                    role = finishData.role
                )

                Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()

            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential retrieval failed", e)
                Toast.makeText(
                    this@LoginActivity,
                    "Passkey authentication failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during login", e)
                Toast.makeText(
                    this@LoginActivity,
                    "An error occurred: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                onComplete()
            }
        }
    }

    /**
     * Converts Z-QryptGIN `login/begin` assertion options into the JSON [GetPublicKeyCredentialOption] expects.
     */
    private fun buildGetCredentialJson(begin: ParsedLoginBegin): String {
        val json = JSONObject()
        json.put("challenge", begin.challenge)
        json.put("rpId", begin.rpId)
        json.put("timeout", begin.timeout)
        json.put("userVerification", begin.userVerification)
        json.put("allowCredentials", begin.allowCredentials)
        return json.toString()
    }

    private data class ParsedLoginBegin(
        val sessionToken: String,
        val challenge: String,
        val rpId: String,
        val timeout: Long,
        val userVerification: String,
        val allowCredentials: JSONArray
    )

    private data class ParsedLoginFinish(
        val token: String,
        val userId: String,
        val role: String,
        val email: String
    )

    private fun serverMessageFromJson(raw: String): String {
        return runCatching {
            JSONObject(raw).optString("message").trim()
        }.getOrDefault("")
    }

    /**
     * Parses `login/begin` body: supports Gin `data.session_token` + `assertion_data`, JSON-string
     * assertion blobs, `publicKey` / `options` envelopes, and `rp.id` for rpId.
     */
    private fun parseLoginBeginPayload(raw: String): ParsedLoginBegin? {
        return try {
            val root = JSONObject(raw)
            val data = root.optJSONObject("data")
            val sessionToken = sequenceOf(
                data?.optString("session_token").orEmpty(),
                data?.optString("sessionToken").orEmpty(),
                root.optString("session_token"),
                root.optString("sessionToken")
            ).firstOrNull { it.isNotEmpty() } ?: ""
            if (sessionToken.isEmpty()) return null

            val optionRoots = mutableListOf<JSONObject>()
            fun add(obj: JSONObject?) {
                if (obj != null) optionRoots.add(obj)
            }
            data?.let { d ->
                add(d.optJSONObject("assertion_data"))
                add(d.optJSONObject("assertionData"))
                add(d.optJSONObject("assertion"))
                add(d.optJSONObject("publicKey"))
                add(d.optJSONObject("options"))
                for (key in arrayOf("assertion_data", "assertionData")) {
                    val s = d.optString(key)
                    if (s.isNotBlank() && s.trimStart().startsWith("{")) {
                        runCatching { add(JSONObject(s)) }
                    }
                }
            }
            add(root.optJSONObject("assertion_data"))
            add(root.optJSONObject("publicKey"))

            for (assertion in optionRoots) {
                val options = assertion.optJSONObject("response")
                    ?: assertion.optJSONObject("publicKey")
                    ?: assertion
                parsedBeginFromWebAuthnOptions(options, sessionToken)?.let { return it }
            }
            data?.let { parsedBeginFromWebAuthnOptions(it, sessionToken) }
        } catch (e: Exception) {
            Log.e(TAG, "parseLoginBeginPayload", e)
            null
        }
    }

    private fun parsedBeginFromWebAuthnOptions(
        options: JSONObject,
        sessionToken: String
    ): ParsedLoginBegin? {
        if (sessionToken.isEmpty()) return null
        val challenge = options.optString("challenge").ifEmpty { options.optString("Challenge") }
        if (challenge.isEmpty()) return null
        var rpId = options.optString("rpId").ifEmpty { options.optString("rp_id") }
        if (rpId.isEmpty()) {
            rpId = options.optJSONObject("rp")?.optString("id").orEmpty()
        }
        if (rpId.isEmpty()) return null
        val timeout = when {
            options.has("timeout") && !options.isNull("timeout") ->
                options.optLong("timeout", 120_000L).takeIf { it > 0 }
                    ?: options.optInt("timeout", 120_000).toLong()
            else -> 120_000L
        }
        val uv = options.optString("userVerification").ifEmpty {
            options.optString("user_verification")
        }.ifEmpty { "required" }
        val allow = options.optJSONArray("allowCredentials")
            ?: options.optJSONArray("allow_credentials")
            ?: JSONArray()
        return ParsedLoginBegin(sessionToken, challenge, rpId, timeout, uv, allow)
    }

    private fun parseLoginFinishPayload(raw: String): ParsedLoginFinish? {
        return try {
            val root = JSONObject(raw)
            val data = root.optJSONObject("data") ?: root
            var token = data.optString("token").ifEmpty { data.optString("access_token") }
                .ifEmpty { data.optString("accessToken") }
            if (token.isEmpty()) {
                token = data.optJSONObject("data")?.optString("token").orEmpty()
                    .ifEmpty { data.optJSONObject("data")?.optString("access_token").orEmpty() }
            }
            if (token.isEmpty()) return null
            val userId = when {
                data.has("user_id") && !data.isNull("user_id") && data.get("user_id") is Number ->
                    data.getLong("user_id").toString()
                else -> data.optString("user_id").ifEmpty { data.optString("userId") }
            }
            val role = data.optString("role").ifEmpty { "Client" }
            val email = data.optString("email")
            ParsedLoginFinish(token, userId, role, email)
        } catch (e: Exception) {
            Log.e(TAG, "parseLoginFinishPayload", e)
            null
        }
    }
}