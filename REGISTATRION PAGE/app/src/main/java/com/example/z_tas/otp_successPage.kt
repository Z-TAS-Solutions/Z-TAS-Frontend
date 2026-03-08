package com.example.z_tas

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.jvm.java

class SuccessActivity : AppCompatActivity() {

    // Delay set to 3 seconds as per Z-TAS design specs
    private val REDIRECT_DELAY = 3000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.otp_sucess_page) // Ensure this name matches your XML filename

        val btnHome = findViewById<Button>(R.id.btnHome)

        /**
         * 1. Automatic Redirect Logic
         * Uses the Main Looper to ensure the UI transition happens safely
         */
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToHome()
        }, REDIRECT_DELAY)

        /**
         * 2. Manual Navigation
         * Handles the Z-TAS "Home" button click
         */
        btnHome.setOnClickListener {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        // Intent tells Android which screen to open next
        // If HomeActivity is red, you need to create that class file!
        val intent = Intent(this, HomeActivity::class.java)

        // This 'Flag' clears the history so the user can't go 'Back' to the success screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish() // Closes the success page properly
    }
}