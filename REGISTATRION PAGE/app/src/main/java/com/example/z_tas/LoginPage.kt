package com.example.z_tas // Ensure this matches your actual package name

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.jvm.java


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        // Find the views - These will stay red if R.id matches don't exist in activity_login.xml
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)

        // Switch to Register Page
        tvToRegister.setOnClickListener {
            val intent = Intent(this,RegistrationPage::class.java)
            startActivity(intent)
            finish() // Close login so 'back' button doesn't loop forever
        }

        // Login Action
        btnLogin.setOnClickListener {
            // Logic for secure login
            Toast.makeText(this, "Attempting Secure Login...", Toast.LENGTH_SHORT).show()
        }
    }
}