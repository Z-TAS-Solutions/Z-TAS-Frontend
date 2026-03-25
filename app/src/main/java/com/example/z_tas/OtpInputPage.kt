package com.example.z_tas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OtpInputPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_input_page)

        val btnContinue = findViewById<Button>(R.id.btnContinue)
        val etOtpPassword = findViewById<EditText>(R.id.etOtpPassword)
        val etOtpConfirm = findViewById<EditText>(R.id.etOtpConfirm)
        val btnBack = findViewById<ImageButton>(R.id.btnBackVerify)

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnContinue.setOnClickListener {
            val otp = etOtpPassword.text.toString().trim()
            val confirmOtp = etOtpConfirm.text.toString().trim()

            if (otp.isEmpty()) {
                etOtpPassword.error = "Enter the one-time password"
                return@setOnClickListener
            }

            if (confirmOtp.isEmpty()) {
                etOtpConfirm.error = "Re-enter to confirm"
                return@setOnClickListener
            }

            if (otp != confirmOtp) {
                etOtpConfirm.error = "Passwords do not match"
                return@setOnClickListener
            }

            // OTP verified, navigate to Passkey activation
            Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PasskeyActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}