package com.example.z_tas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import kotlin.jvm.java
class OtpInputPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_input_page)

        val btnContinue = findViewById<Button>(R.id.btnContinue)
        val etOtpPhone = findViewById<EditText>(R.id.etOtpPhone)
        val btnBack = findViewById<ImageButton>(R.id.btnBackVerify)

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Professional way to go back
        }

        btnContinue.setOnClickListener {
            val phone = etOtpPhone.text.toString().trim()
            if (phone.length < 10) {
                etOtpPhone.error = "Enter a valid 10-digit number"
            } else {
                // Ensure VerifyCodeActivity is created in your project!
                val intent = Intent(this, VerifyCodeActivity::class.java)
                startActivity(intent)
            }
        }
    }
}