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
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.z_tas.network.RegisterRequest
import com.example.z_tas.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistrationPage : AppCompatActivity() {

    private val userApi = RetrofitClient.userApi

    companion object {
        private const val TAG = "RegistrationPage"
    }

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

            // Disable button during API call
            registerButton.isEnabled = false
            registerUser(name, email, nic, phone) {
                registerButton.isEnabled = true
            }
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

    /**
     * Calls POST /admin/users/register/new and on success navigates
     * to OtpInputPage with the userId.
     */
    private fun registerUser(
        name: String,
        email: String,
        nic: String,
        phone: String,
        onComplete: () -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    userApi.register(RegisterRequest(name, email, nic, phone))
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "Registration success: ${body.message}, userId=${body.userId}")

                    Toast.makeText(
                        this@RegistrationPage,
                        body.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to OTP page, passing userId
                    val intent = Intent(this@RegistrationPage, OtpInputPage::class.java).apply {
                        putExtra("USER_ID", body.userId)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Registration failed: ${response.code()} — $errorBody")
                    Toast.makeText(
                        this@RegistrationPage,
                        "Registration failed: $errorBody",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error", e)
                Toast.makeText(
                    this@RegistrationPage,
                    "An error occurred: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                onComplete()
            }
        }
    }
}