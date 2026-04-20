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
import androidx.lifecycle.lifecycleScope
import com.ztas.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var credentialManager: CredentialManager

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
     * WebAuthn login via [WebAuthnLoginFlow]; on success saves session and opens home.
     */
    private fun authenticateWithPasskey(email: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                when (
                    val out = WebAuthnLoginFlow.authenticate(
                        activity = this@LoginActivity,
                        credentialManager = credentialManager,
                        emailOrNull = email
                    )
                ) {
                    is WebAuthnLoginFlow.PasskeyOutcome.Error -> {
                        Toast.makeText(
                            this@LoginActivity,
                            out.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is WebAuthnLoginFlow.PasskeyOutcome.Success -> {
                        val finishData = out.data
                        Log.d(TAG, "Login success — userId=${finishData.userId}, role=${finishData.role}")

                        AuthPreferences.saveSession(
                            this@LoginActivity,
                            accessToken = finishData.token,
                            userId = finishData.userId,
                            email = finishData.email,
                            role = finishData.role
                        )
                        if (finishData.displayName.isNotBlank()) {
                            AuthPreferences.setCachedDisplayName(
                                this@LoginActivity,
                                finishData.displayName,
                                emailForCheck = finishData.email
                            )
                            if (!DisplayNameHints.isEmailLocalHandle(finishData.displayName, finishData.email)) {
                                AuthPreferences.setCachedName(this@LoginActivity, finishData.displayName)
                            }
                        }

                        runCatching {
                            withContext(Dispatchers.IO) {
                                val bearer = AuthPreferences.bearerOrNull(this@LoginActivity) ?: return@withContext
                                val resp = RetrofitClient.userApi.getProfile(bearer)
                                if (!resp.isSuccessful) return@withContext
                                val raw = resp.body()?.string() ?: return@withContext
                                val profile = UserProfileJson.parse(raw) ?: return@withContext
                                val profileEmail = profile.email.ifBlank { finishData.email }
                                val nm = profile.name.trim()
                                if (nm.isNotEmpty() && !DisplayNameHints.isEmailLocalHandle(nm, profileEmail)) {
                                    AuthPreferences.setCachedDisplayName(
                                        this@LoginActivity,
                                        nm,
                                        emailForCheck = profileEmail
                                    )
                                    AuthPreferences.setCachedName(this@LoginActivity, nm)
                                }
                            }
                        }.onFailure { Log.w(TAG, "Profile fetch after login (display name cache)", it) }

                        Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                }
            } finally {
                onComplete()
            }
        }
    }
}
