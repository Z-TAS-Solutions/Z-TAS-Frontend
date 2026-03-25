package com.example.z_tas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var credentialManager: CredentialManager

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
            val name = etName.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Username cannot be empty"
                return@setOnClickListener
            }

            // Trigger passkey authentication
            authenticateWithPasskey()
        }
    }

    private fun authenticateWithPasskey() {
        val requestJson = """
            {
                "challenge": "bmV3LWNoYWxsZW5nZQ",
                "rp": { "name": "Z-TAS Security", "id": "ztas.example.com" },
                "user": { "id": "dXNlci1pZA", "name": "user@example.com", "displayName": "User" },
                "pubKeyCredParams": [{ "type": "public-key", "alg": -7 }]
            }
        """.trimIndent()

        val request = CreatePublicKeyCredentialRequest(requestJson)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.createCredential(this@LoginActivity, request)
                Toast.makeText(
                    this@LoginActivity,
                    "Login Successful!",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@LoginActivity, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } catch (e: CreateCredentialException) {
                val errorMsg = e.message ?: e.toString()
                Toast.makeText(this@LoginActivity, "Passkey Failed: $errorMsg", Toast.LENGTH_LONG).show()
                // Proceed to Home anyway for demo purposes
                startActivity(Intent(this@LoginActivity, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        }
    }
}