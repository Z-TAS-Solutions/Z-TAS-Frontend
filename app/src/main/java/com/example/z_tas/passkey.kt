package com.example.z_tas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import com.example.z_tas.network.AttestationResponseBody
import com.example.z_tas.network.BeginRegisterRequest
import com.example.z_tas.network.FinishRegisterRequest
import com.example.z_tas.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PasskeyActivity : AppCompatActivity() {

    private lateinit var credentialManager: CredentialManager
    private val webAuthnApi = RetrofitClient.webAuthnApi
    private var userId: String = ""
    private var userEmail: String = ""

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
        if (userId.isEmpty()) {
            Log.w(TAG, "No USER_ID received — passkey registration will fail")
        }
        if (userEmail.isEmpty()) {
            Log.w(TAG, "No USER_EMAIL received — begin register may fail")
        }

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
        CoroutineScope(Dispatchers.Main).launch {
            try {
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
                    Toast.makeText(this@PasskeyActivity, "Passkey setup failed: $errorBody", Toast.LENGTH_LONG).show()
                    onComplete()
                    return@launch
                }

                val beginBody = beginResponse.body()!!
                val requestJson = buildCreateCredentialJson(beginBody)
                Log.d(TAG, "CredentialManager create request JSON: $requestJson")

                // ── Step 2: CredentialManager — user authenticates ───
                val createRequest = CreatePublicKeyCredentialRequest(requestJson)
                val result = credentialManager.createCredential(this@PasskeyActivity, createRequest)

                val credential = result
                if (credential !is androidx.credentials.CreatePublicKeyCredentialResponse) {
                    Toast.makeText(this@PasskeyActivity, "Unexpected credential type", Toast.LENGTH_SHORT).show()
                    onComplete()
                    return@launch
                }

                // Parse the registration response JSON
                val registrationJson = JSONObject(credential.registrationResponseJson)
                Log.d(TAG, "Registration JSON: $registrationJson")

                val responseObj = registrationJson.getJSONObject("response")

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

                val finishResponse = withContext(Dispatchers.IO) {
                    webAuthnApi.finishRegister(finishRequest)
                }

                if (!finishResponse.isSuccessful || finishResponse.body() == null) {
                    val errorBody = finishResponse.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Finish register failed: ${finishResponse.code()} — $errorBody")
                    Toast.makeText(this@PasskeyActivity, "Passkey save failed: $errorBody", Toast.LENGTH_LONG).show()
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
                onComplete()
            }
        }
    }

    /**
     * Converts [BeginRegisterResponse] to the JSON format expected by
     * [CreatePublicKeyCredentialRequest].
     */
    private fun buildCreateCredentialJson(
        begin: com.example.z_tas.network.BeginRegisterResponse
    ): String {
        val json = JSONObject()
        json.put("challenge", begin.challenge)
        json.put("timeout", begin.timeout)
        json.put("attestation", begin.attestation)

        // rp
        val rpJson = JSONObject()
        rpJson.put("id", begin.rp.id)
        rpJson.put("name", begin.rp.name)
        json.put("rp", rpJson)

        // user
        val userJson = JSONObject()
        userJson.put("id", begin.user.id)
        userJson.put("name", begin.user.name)
        userJson.put("displayName", begin.user.displayName)
        json.put("user", userJson)

        // pubKeyCredParams
        val paramsArray = JSONArray()
        for (param in begin.pubKeyCredParams) {
            val paramObj = JSONObject()
            paramObj.put("type", param.type)
            paramObj.put("alg", param.alg)
            paramsArray.put(paramObj)
        }
        json.put("pubKeyCredParams", paramsArray)

        return json.toString()
    }

    private fun setupFeature(id: Int, title: String, desc: String) {
        val root = findViewById<View>(id)
        root.findViewById<TextView>(R.id.tvFeatureTitle).text = title
        root.findViewById<TextView>(R.id.tvFeatureDesc).text = desc
    }
}
