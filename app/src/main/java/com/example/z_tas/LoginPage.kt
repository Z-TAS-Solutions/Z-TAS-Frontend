package com.example.z_tas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        val etName = findViewById<EditText>(R.id.etName)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)

        // Switch to Register Page
        tvToRegister.setOnClickListener {
            val intent = Intent(this, RegistrationPage::class.java)
            startActivity(intent)
            finish()
        }

        // Login Action
        btnLogin.setOnClickListener {

            val name = etName.text.toString().trim()

            // Regex: only letters allowed
            val namePattern = Regex("^[A-Za-z]+$")

            if (name.isEmpty()) {
                etName.error = "Name cannot be empty"
                return@setOnClickListener
            }

            if (!namePattern.matches(name)) {
                etName.error = "Name should contain only letters"
                return@setOnClickListener
            }

            // If validation passes
            Toast.makeText(this, "Attempting Secure Login...", Toast.LENGTH_SHORT).show()
        }
    }
}