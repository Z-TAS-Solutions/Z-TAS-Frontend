package com.example.z_tas

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.jvm.java

class VerifyCodeActivity : AppCompatActivity() {

    // Define views at class level to access them in functions
    private lateinit var tvResend: TextView
    private lateinit var tvTimer: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_code2)

        // 1. Initialize Views
        tvResend = findViewById(R.id.tvResendCode)
        tvTimer = findViewById(R.id.tvTimer)
        val btnVerify = findViewById<Button>(R.id.btnVerifyCode)
        val btnBack = findViewById<ImageButton>(R.id.btnBackVerify)
        val btnEditPhone = findViewById<ImageButton>(R.id.btnEditNumber)

        val otpBoxes = arrayOf(
            findViewById<EditText>(R.id.otp1),
            findViewById<EditText>(R.id.otp2),
            findViewById<EditText>(R.id.otp3),
            findViewById<EditText>(R.id.otp4)
        )

        // 2. Start Timer immediately
        startResendTimer()

        // 3. OTP Focus Management
        setupOtpFocus(otpBoxes)

        // 4. Click Listeners
        tvResend.setOnClickListener {
            // Logic to trigger SMS resend goes here
            startResendTimer()
        }

        btnEditPhone.setOnClickListener {
            finish() // Goes back to the phone input screen
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnVerify.setOnClickListener {
            startActivity(Intent(this, VerifyCodeActivity::class.java))
        }
    }

    private fun startResendTimer() {
        // Disable clicking and fade color while timer runs
        tvResend.isEnabled = false
        tvResend.setTextColor(Color.parseColor("#40FFFFFF"))

        object : CountDownTimer(30000, 1000) { // 30 seconds
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                tvTimer.text = String.format("00:%02d", secondsRemaining)
            }

            override fun onFinish() {
                // Re-enable and highlight when done
                tvTimer.text = "00:00"
                tvResend.isEnabled = true
                tvResend.setTextColor(Color.parseColor("#00C4FF"))
            }
        }.start()
    }

    private fun setupOtpFocus(otpBoxes: Array<EditText>) {
        for (i in otpBoxes.indices) {
            otpBoxes[i].addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // Move forward if digit entered
                    if (s?.length == 1 && i < otpBoxes.size - 1) {
                        otpBoxes[i + 1].requestFocus()
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Optional: Move backward if deleted (requires custom KeyListener)
                }
            })
        }
    }
}