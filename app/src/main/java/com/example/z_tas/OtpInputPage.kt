package com.example.z_tas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.z_tas.network.RetrofitClient
import com.example.z_tas.network.VerifyOtpRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtpInputPage : AppCompatActivity() {

    private val userApi = RetrofitClient.userApi
    private var userId: String = ""

    companion object {
        private const val TAG = "OtpInputPage"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_input_page)

        // Receive userId from RegistrationPage
        userId = intent.getStringExtra("USER_ID") ?: ""
        if (userId.isEmpty()) {
            Log.w(TAG, "No USER_ID received — OTP verification will fail")
        }

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

            btnContinue.isEnabled = false
            verifyOtp(otp) {
                btnContinue.isEnabled = true
            }
        }
    }

    /**
     * Calls POST /admin/users/register/verifyOTP and on success
     * navigates to PasskeyActivity with the userId.
     */
    private fun verifyOtp(otp: String, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    userApi.verifyOtp(VerifyOtpRequest(userId, otp))
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    if (body.verified) {
                        Log.d(TAG, "OTP verified: ${body.message}")
                        Toast.makeText(
                            this@OtpInputPage,
                            "OTP Verified Successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to Passkey activation, passing userId
                        val intent = Intent(this@OtpInputPage, PasskeyActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@OtpInputPage,
                            "OTP verification failed. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "OTP verification failed: ${response.code()} — $errorBody")
                    Toast.makeText(
                        this@OtpInputPage,
                        "Verification failed: $errorBody",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "OTP verification error", e)
                Toast.makeText(
                    this@OtpInputPage,
                    "An error occurred: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                onComplete()
            }
        }
    }
}