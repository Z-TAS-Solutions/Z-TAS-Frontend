package com.ztas.app

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Button
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
            btnLogin.isEnabled = false
            // Discoverable begin: omit username so server uses BeginDiscoverableLogin (works with
            // Samsung Pass even when FindByEmail does not Preload Credentials — see Z-QryptGIN user_repo).
            authenticateWithPasskey {
                btnLogin.isEnabled = true
            }
        }
    }

    /**
     * WebAuthn login: discoverable `login/begin` (omit `username`) so the device passkey store (e.g. Samsung Pass)
     * can be used without the server preloading `Credentials` on `FindByEmail`.
     * Parses wrapped `{ data: { session_token, assertion_data } }` and sends `X-Session-Token` on `login/finish`.
     */
    private fun authenticateWithPasskey(onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // ── Step 1: Begin Login (discoverable) ─────────────────────
                val beginResponse = withContext(Dispatchers.IO) {
                    webAuthnApi.beginLogin(BeginLoginRequest())
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
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: unexpected server response",
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
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: unexpected server response",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                    return@launch
                }
                Log.d(TAG, "Login success — userId=${finishData.userId}, role=${finishData.role}")

                // TODO: Persist finishBody.token securely (e.g. EncryptedSharedPreferences)

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
        val role: String
    )

    /** Gin wraps `session_token` + `assertion_data` under `data` (see Z-QryptGIN `WebAuthnHandler.LoginBegin`). */
    private fun parseLoginBeginPayload(raw: String): ParsedLoginBegin? {
        return try {
            val root = JSONObject(raw)
            val data = root.optJSONObject("data") ?: return null
            val sessionToken = data.optString("session_token").ifEmpty { data.optString("sessionToken") }
            if (sessionToken.isEmpty()) return null
            val assertion = data.optJSONObject("assertion_data")
                ?: data.optJSONObject("assertionData")
                ?: return null
            val options = assertion.optJSONObject("response") ?: assertion
            val challenge = options.optString("challenge").ifEmpty { options.optString("Challenge") }
            if (challenge.isEmpty()) return null
            val rpId = options.optString("rpId").ifEmpty { options.optString("rp_id") }
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
            ParsedLoginBegin(sessionToken, challenge, rpId, timeout, uv, allow)
        } catch (e: Exception) {
            Log.e(TAG, "parseLoginBeginPayload", e)
            null
        }
    }

    private fun parseLoginFinishPayload(raw: String): ParsedLoginFinish? {
        return try {
            val root = JSONObject(raw)
            val data = root.optJSONObject("data") ?: root
            val token = data.optString("token").ifEmpty { return null }
            val userId = when {
                data.has("user_id") && !data.isNull("user_id") && data.get("user_id") is Number ->
                    data.getLong("user_id").toString()
                else -> data.optString("user_id").ifEmpty { data.optString("userId") }
            }
            val role = data.optString("role").ifEmpty { "Client" }
            ParsedLoginFinish(token, userId, role)
        } catch (e: Exception) {
            Log.e(TAG, "parseLoginFinishPayload", e)
            null
        }
    }
}