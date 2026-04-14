package com.ztas.app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
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

        val etName = findViewById<EditText>(R.id.etName)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)

        tvToRegister.setOnClickListener {
            val intent = Intent(this, RegistrationPage::class.java)
            startActivity(intent)
            finish()
        }

        btnLogin.setOnClickListener {
            val username = etName.text.toString().trim()

            if (username.isEmpty()) {
                etName.error = "Username cannot be empty"
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            authenticateWithPasskey(username) {
                btnLogin.isEnabled = true
            }
        }
    }

    /**
     * Full WebAuthn login flow:
     *  1. POST /webauthn/login/begin  → get challenge + allowCredentials
     *  2. CredentialManager.getCredential() → user authenticates with passkey
     *  3. POST /webauthn/login/finish → send assertion, receive JWT
     */
    private fun authenticateWithPasskey(username: String, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // ── Step 1: Begin Login ──────────────────────────────────
                val beginResponse = withContext(Dispatchers.IO) {
                    webAuthnApi.beginLogin(BeginLoginRequest(username))
                }

                if (!beginResponse.isSuccessful || beginResponse.body() == null) {
                    val errorBody = beginResponse.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Begin login failed: ${beginResponse.code()} — $errorBody")
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: $errorBody",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                    return@launch
                }

                val beginBody = beginResponse.body()!!

                // Build the JSON that Android's CredentialManager expects
                val requestJson = buildGetCredentialJson(beginBody)
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
                    webAuthnApi.finishLogin(finishRequest)
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

                val finishBody = finishResponse.body()!!
                Log.d(TAG, "Login success — userId=${finishBody.userId}, role=${finishBody.role}")

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
     * Converts the [BeginLoginResponse] into the JSON format expected by
     * [GetPublicKeyCredentialOption].
     */
    private fun buildGetCredentialJson(
        begin: com.ztas.app.network.BeginLoginResponse
    ): String {
        val json = JSONObject()
        json.put("challenge", begin.challenge)
        json.put("rpId", begin.rpId)
        json.put("timeout", begin.timeout)
        json.put("userVerification", begin.userVerification)

        val allowCreds = JSONArray()
        for (cred in begin.allowCredentials) {
            val obj = JSONObject()
            obj.put("id", cred.id)
            obj.put("type", cred.type)
            allowCreds.put(obj)
        }
        json.put("allowCredentials", allowCreds)

        return json.toString()
    }
}