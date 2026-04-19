package com.ztas.app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.lifecycle.lifecycleScope
import com.ztas.app.network.AttestationResponseBody
import com.ztas.app.network.BeginRegisterRequest
import com.ztas.app.network.FinishRegisterRequest
import com.ztas.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class PasskeyActivity : AppCompatActivity() {

    private lateinit var credentialManager: CredentialManager
    private val webAuthnApi = RetrofitClient.webAuthnApi
    private var userId: String = ""
    private var userEmail: String = ""
    private var userPhone: String = ""
    private var isPasskeyRegistrationInProgress = false

    companion object {
        private const val TAG = "PasskeyActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activate_passkey)

        credentialManager = CredentialManager.create(this)

        // Receive user details from OtpInputPage
        userId = intent.getStringExtra("USER_ID") ?: ""
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        userPhone = intent.getStringExtra("USER_PHONE") ?: ""
        if (userId.isEmpty()) {
            Log.w(TAG, "No USER_ID received — passkey registration will fail")
        }
        if (userEmail.isEmpty()) {
            Log.w(TAG, "No USER_EMAIL received — begin register may fail")
        }
        Log.d(TAG, "Passkey context: userId=$userId email=$userEmail phoneFromRegister=$userPhone")
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToOtpPage()
            }
        })

        // Point 1
        setupFeature(
            R.id.feature1,
            "Biometric Authentication",
            "Use your fingerprint or face to sign in instantly."
        )

        // Point 2
        setupFeature(
            R.id.feature2,
            "No Passwords Required",
            "Eliminate the risk of forgotten or stolen passwords."
        )

        // Point 3
        setupFeature(
            R.id.feature3,
            "Military-grade Encryption",
            "Your data is protected by end-to-end AES-256 security."
        )

        val btnActivate = findViewById<View>(R.id.btnActivate)
        btnActivate.setOnClickListener {
            btnActivate.isEnabled = false
            registerPasskey {
                btnActivate.isEnabled = true
            }
        }
    }

    /**
     * Full WebAuthn passkey registration flow:
     *  1. POST /webauthn/register/begin  → credential creation options
     *  2. CredentialManager.createCredential() → user does biometric
     *  3. POST /webauthn/register/finish → send attestation to backend
     */
    private fun registerPasskey(onComplete: () -> Unit) {
        if (isPasskeyRegistrationInProgress) {
            Toast.makeText(
                this@PasskeyActivity,
                "Passkey setup is already in progress.",
                Toast.LENGTH_SHORT
            ).show()
            onComplete()
            return
        }
        isPasskeyRegistrationInProgress = true
        lifecycleScope.launch {
            try {
                if (isFinishing || isDestroyed) {
                    return@launch
                }
                if (looksLikePhoneNumber(userId)) {
                    Log.e(TAG, "Invalid custom_id for passkey flow (looks like phone): $userId")
                    Toast.makeText(
                        this@PasskeyActivity,
                        "Passkey setup blocked: registration returned an invalid user identifier. Please register again.",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                    return@launch
                }
                // ── Step 1: Begin Registration ───────────────────────
                val beginResponse = withContext(Dispatchers.IO) {
                    webAuthnApi.beginRegister(
                        BeginRegisterRequest(
                            customId = userId,
                            userId = userId,
                            email = userEmail
                        )
                    )
                }

                if (!beginResponse.isSuccessful || beginResponse.body() == null) {
                    val errorBody = beginResponse.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Begin register failed: ${beginResponse.code()} — $errorBody")
                    Toast.makeText(
                        this@PasskeyActivity,
                        "Passkey setup failed: ${apiErrorUserMessage(errorBody)}",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                    return@launch
                }

                val beginBody = beginResponse.body()!!
                val sessionToken = beginBody.data?.sessionToken.orEmpty()
                if (sessionToken.isBlank()) {
                    Toast.makeText(
                        this@PasskeyActivity,
                        "Passkey setup failed: missing session token from server.",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                    return@launch
                }
                val requestJson = buildCreateCredentialJson(beginBody)
                Log.d(TAG, "CredentialManager create request JSON: $requestJson")
                Log.d(TAG, "Begin register rp.id=${beginBody.data?.creationData?.publicKey?.rp?.id ?: beginBody.rp?.id}")

                // ── Step 2: CredentialManager — user authenticates ───
                val credential = createCredentialWithFallback(requestJson)
                if (credential !is androidx.credentials.CreatePublicKeyCredentialResponse) {
                    Toast.makeText(this@PasskeyActivity, "Unexpected credential type", Toast.LENGTH_SHORT).show()
                    onComplete()
                    return@launch
                }

                // Parse the registration response JSON
                val registrationJson = JSONObject(credential.registrationResponseJson)
                Log.d(TAG, "Registration JSON: $registrationJson")

                val responseObj = registrationJson.getJSONObject("response")
                val clientDataBase64Url = responseObj.getString("clientDataJSON")
                parseClientData(clientDataBase64Url)?.let { clientData ->
                    Log.d(
                        TAG,
                        "clientDataJSON decoded: origin=${clientData.optString("origin")}, type=${clientData.optString("type")}"
                    )
                }

                // ── Step 3: Finish Registration ──────────────────────
                val finishRequest = FinishRegisterRequest(
                    id = registrationJson.getString("id"),
                    rawId = registrationJson.getString("rawId"),
                    response = AttestationResponseBody(
                        attestationObject = responseObj.getString("attestationObject"),
                        clientDataJSON = responseObj.getString("clientDataJSON")
                    ),
                    type = registrationJson.optString("type", "public-key")
                )

                // Log the full request being sent to the backend
                Log.d(TAG, "╔══ Finish Register Request ══════════════════")
                Log.d(TAG, "║ sessionToken=$sessionToken")
                Log.d(TAG, "║ id=${finishRequest.id}")
                Log.d(TAG, "║ rawId=${finishRequest.rawId}")
                Log.d(TAG, "║ type=${finishRequest.type}")
                Log.d(TAG, "║ attestationObject=${finishRequest.response.attestationObject.take(80)}...")
                Log.d(TAG, "║ clientDataJSON=${finishRequest.response.clientDataJSON}")
                Log.d(TAG, "╚════════════════════════════════════════════")

                val finishResponse = withContext(Dispatchers.IO) {
                    webAuthnApi.finishRegister(sessionToken, finishRequest)
                }

                if (!finishResponse.isSuccessful || finishResponse.body() == null) {
                    val errorBody = finishResponse.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "╔══ Finish Register FAILED ══════════════════")
                    Log.e(TAG, "║ HTTP ${finishResponse.code()}")
                    Log.e(TAG, "║ Headers: ${finishResponse.headers()}")
                    Log.e(TAG, "║ Error body: $errorBody")
                    Log.e(TAG, "╚════════════════════════════════════════════")
                    val userMessage = when {
                        errorBody.contains("Error validating origin", ignoreCase = true) ||
                            errorBody.contains("invalid credential response", ignoreCase = true) ->
                            "Passkey save failed: backend origin validation failed. Ask backend to allow Android WebAuthn origin and match rp.id with the same verified domain."
                        errorBody.contains("invalid NIC", ignoreCase = true) ->
                            "Invalid NIC format. Go back to registration, enter a valid Sri Lankan NIC (9 digits + V/X or 12 digits), then complete OTP and passkey again."
                        else ->
                            "Passkey save failed: ${apiErrorUserMessage(errorBody)}"
                    }
                    Toast.makeText(this@PasskeyActivity, userMessage, Toast.LENGTH_LONG).show()
                    onComplete()
                    return@launch
                }

                val finishBody = finishResponse.body()!!
                Log.d(TAG, "Passkey registered: ${finishBody.message}, credentialId=${finishBody.credentialId}")

                Toast.makeText(this@PasskeyActivity, "Passkey Registered Successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@PasskeyActivity, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()

            } catch (e: CreatePublicKeyCredentialDomException) {
                Log.e(TAG, "Credential creation DOM error", e)
                val msg = if (e.message?.contains("RP ID cannot be validated", ignoreCase = true) == true) {
                    "Passkey setup failed: RP ID is invalid for this app. Ask backend to use your verified domain (not localhost/IP) and configure Digital Asset Links."
                } else {
                    "Passkey creation failed: ${e.message}"
                }
                Toast.makeText(this@PasskeyActivity, msg, Toast.LENGTH_LONG).show()
            } catch (e: CreateCredentialException) {
                Log.e(TAG, "Credential creation failed", e)
                Toast.makeText(
                    this@PasskeyActivity,
                    "Passkey creation failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during passkey registration", e)
                Toast.makeText(
                    this@PasskeyActivity,
                    "An error occurred: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isPasskeyRegistrationInProgress = false
                onComplete()
            }
        }
    }

    /**
     * Converts [BeginRegisterResponse] to the JSON format expected by
     * [CreatePublicKeyCredentialRequest].
     */
    private fun buildCreateCredentialJson(
        begin: com.ztas.app.network.BeginRegisterResponse
    ): String {
        val options = begin.data?.creationData?.publicKey
            ?: com.ztas.app.network.BeginRegisterPublicKeyOptions(
                challenge = begin.challenge,
                rp = begin.rp,
                user = begin.user,
                pubKeyCredParams = begin.pubKeyCredParams,
                timeout = begin.timeout,
                attestation = begin.attestation
            )

        val challenge = options.challenge
        val rp = options.rp
        val user = options.user
        val pubKeyCredParams = options.pubKeyCredParams
        val timeout = options.timeout
        val attestation = options.attestation

        if (challenge.isNullOrBlank() || rp == null || user == null || pubKeyCredParams.isNullOrEmpty()) {
            throw IllegalStateException("Passkey begin response missing required publicKey fields")
        }

        val json = JSONObject()
        json.put("challenge", challenge)
        json.put("timeout", timeout ?: 300000L)
        json.put("attestation", attestation ?: "none")

        // rp
        val rpJson = JSONObject()
        rpJson.put("id", rp.id)
        rpJson.put("name", rp.name)
        json.put("rp", rpJson)

        // user
        val userJson = JSONObject()
        userJson.put("id", user.id)
        userJson.put("name", user.name)
        userJson.put("displayName", user.displayName)
        json.put("user", userJson)

        // pubKeyCredParams
        val paramsArray = JSONArray()
        for (param in pubKeyCredParams) {
            val paramObj = JSONObject()
            paramObj.put("type", param.type)
            paramObj.put("alg", param.alg)
            paramsArray.put(paramObj)
        }
        json.put("pubKeyCredParams", paramsArray)

        options.authenticatorSelection?.let { selection ->
            val selectionJson = JSONObject()
            selection.requireResidentKey?.let { selectionJson.put("requireResidentKey", it) }
            selection.residentKey?.let { selectionJson.put("residentKey", it) }
            selection.userVerification?.let { selectionJson.put("userVerification", it) }
            if (selectionJson.length() > 0) {
                json.put("authenticatorSelection", selectionJson)
            }
        }
        options.hints?.let { hints ->
            if (hints.isNotEmpty()) {
                val hintsArray = JSONArray()
                hints.forEach { hintsArray.put(it) }
                json.put("hints", hintsArray)
            }
        }

        return json.toString()
    }

    private suspend fun createCredentialWithFallback(
        requestJson: String
    ): androidx.credentials.CreateCredentialResponse {
        return try {
            val createRequest = CreatePublicKeyCredentialRequest(requestJson)
            credentialManager.createCredential(this@PasskeyActivity, createRequest)
        } catch (e: CreatePublicKeyCredentialDomException) {
            val shouldRetryWithSimplifiedRequest =
                e.message?.contains("incoming request cannot be validated", ignoreCase = true) == true
            if (!shouldRetryWithSimplifiedRequest) {
                throw e
            }

            // Some providers reject stricter options. Retry once with a
            // compatibility-focused payload (attestation/hints/selection removed).
            val simplifiedRequestJson = buildSimplifiedCreateCredentialJson(requestJson)
            Log.w(TAG, "Retrying createCredential with simplified request JSON: $simplifiedRequestJson")
            val retryRequest = CreatePublicKeyCredentialRequest(simplifiedRequestJson)
            credentialManager.createCredential(this@PasskeyActivity, retryRequest)
        }
    }

    private fun buildSimplifiedCreateCredentialJson(originalRequestJson: String): String {
        val json = JSONObject(originalRequestJson)
        json.put("attestation", "none")
        json.remove("hints")
        json.remove("authenticatorSelection")
        return json.toString()
    }

    private fun setupFeature(id: Int, title: String, desc: String) {
        val root = findViewById<View>(id)
        root.findViewById<TextView>(R.id.tvFeatureTitle).text = title
        root.findViewById<TextView>(R.id.tvFeatureDesc).text = desc
    }

    private fun parseClientData(clientDataBase64Url: String): JSONObject? {
        return try {
            val decoded = Base64.decode(
                clientDataBase64Url,
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            JSONObject(String(decoded, StandardCharsets.UTF_8))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode clientDataJSON", e)
            null
        }
    }

    /** Prefer JSON `message` from Gin-style error bodies instead of dumping raw JSON. */
    private fun apiErrorUserMessage(body: String): String {
        return try {
            val o = JSONObject(body)
            o.optString("message").ifEmpty { body }
        } catch (_: Exception) {
            body
        }
    }

    private fun looksLikePhoneNumber(value: String): Boolean {
        val normalized = value.replace(Regex("[\\s-]"), "")
        return normalized.matches(Regex("^\\+94\\d{9}$")) ||
            normalized.matches(Regex("^0\\d{9}$")) ||
            normalized.matches(Regex("^94\\d{9}$")) ||
            normalized.matches(Regex("^7\\d{8}$"))
    }

    private fun navigateBackToOtpPage() {
        if (isPasskeyRegistrationInProgress) {
            Toast.makeText(
                this,
                "Please wait until passkey setup completes.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        navigateBackToOtpPage()
    }
}
