package com.example.z_tas

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class RegistrationPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_page)

        // All input fields
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etNIC = findViewById<EditText>(R.id.etNIC)
        val etPhone = findViewById<EditText>(R.id.etPhone)

        val registerButton = findViewById<Button>(R.id.btnRegister)

        registerButton.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val nic = etNIC.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            val nameRegex = Regex("^[A-Za-z ]+$")

            if (name.isEmpty()) { etName.error = "Name is required"; return@setOnClickListener }
            if (!nameRegex.matches(name)) { etName.error = "Name should contain only letters"; return@setOnClickListener }

            if (email.isEmpty()) { etEmail.error = "Email required"; return@setOnClickListener }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.error = "Enter a valid email"; return@setOnClickListener }

            if (nic.isEmpty()) { etNIC.error = "NIC number is required"; return@setOnClickListener }

            if (phone.isEmpty()) { etPhone.error = "Phone number required"; return@setOnClickListener }
            if (!phone.matches(Regex("^[0-9]{10}$"))) { etPhone.error = "Enter a valid 10 digit phone number"; return@setOnClickListener }

            // Navigate to OTP confirmation page
            val intent = Intent(this, OtpInputPage::class.java)
            startActivity(intent)
            finish()
        }

        // Login link
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        val fullText = "Already have an account? Log in"

        val spannableString = SpannableString(fullText)

        val startIndex = fullText.indexOf("Log in")
        val endIndex = startIndex + "Log in".length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                val intent = Intent(this@RegistrationPage, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
            }
        }

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(
            ForegroundColorSpan(Color.parseColor("#00C4FF")),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvLoginLink.text = spannableString
        tvLoginLink.movementMethod = LinkMovementMethod.getInstance()
    }
}