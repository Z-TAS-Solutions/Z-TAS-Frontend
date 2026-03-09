package com.example.z_tas

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class RegistrationPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_page)

        // 1. Setup Registration Button
        val registerButton = findViewById<Button>(R.id.btnRegister)
        registerButton.setOnClickListener {
            Toast.makeText(this, "Registration Started!", Toast.LENGTH_SHORT).show()
        }

        // 2. Setup Clickable Login Link
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        val fullText = "Already have an account? Log in"
        val spannableString = SpannableString(fullText)

        val startIndex = fullText.indexOf("Log in")
        val endIndex = startIndex + "Log in".length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                // Navigates to LoginActivity
                //val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
            }
        }

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#00C4FF")), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvLoginLink.text = spannableString
        tvLoginLink.movementMethod = LinkMovementMethod.getInstance()
    }
}