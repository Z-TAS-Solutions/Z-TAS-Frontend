package com.ztas.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import com.ztas.app.network.RetrofitClient
import com.ztas.app.network.VerifyOtpRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtpInputPage : AppCompatActivity() {

    private val userApi = RetrofitClient.userApi
    private var userId: String = ""
    private var userEmail: String = ""
    private var userPhone: String = ""
    private var isVerifying = false

    companion object {
        private const val TAG = "OtpInputPage"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_input_page)

        // Receive userId from RegistrationPage
        userId = intent.getStringExtra("USER_ID") ?: ""
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        userPhone = intent.getStringExtra("USER_PHONE") ?: ""
        if (userId.isEmpty()) {
            Log.w(TAG, "No USER_ID received — OTP verification will fail")
        }

        val btnContinue = findViewById<Button>(R.id.btnContinue)
        val etOtpPassword = findViewById<EditText>(R.id.etOtpPassword)
        val etOtpConfirm = findViewById<EditText>(R.id.etOtpConfirm)
        val btnBack = findViewById<ImageButton>(R.id.btnBackVerify)

        // Auto-advance to confirm once all digits are entered
        val expectedLen = 6
        etOtpPassword.addTextChangedListener { editable ->
            val value = editable?.toString().orEmpty().trim()
            if (value.length == expectedLen) {
                etOtpConfirm.requestFocus()
                etOtpConfirm.setSelection(etOtpConfirm.text?.length ?: 0)
            }
        }

        // On confirm completion, close keyboard (and makes screen feel responsive)
        etOtpConfirm.imeOptions = EditorInfo.IME_ACTION_DONE
        etOtpConfirm.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(v)
                maybeAutoSubmit(etOtpPassword, etOtpConfirm, btnContinue, expectedLen)
                true
            } else false
        }
        etOtpConfirm.addTextChangedListener { editable ->
            val value = editable?.toString().orEmpty().trim()
            if (value.length == expectedLen) {
                hideKeyboard(etOtpConfirm)
                maybeAutoSubmit(etOtpPassword, etOtpConfirm, btnContinue, expectedLen)
            }
        }

        btnBack.setOnClickListener {
            navigateBackToRegistration()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToRegistration()
            }
        })

        btnContinue.setOnClickListener {
            if (isVerifying) return@setOnClickListener

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

            isVerifying = true
            btnContinue.isEnabled = false
            verifyOtp(otp) {
                isVerifying = false
                btnContinue.isEnabled = true
            }
        }
    }

    private fun maybeAutoSubmit(
        etOtpPassword: EditText,
        etOtpConfirm: EditText,
        btnContinue: Button,
        expectedLen: Int
    ) {
        val otp = etOtpPassword.text?.toString().orEmpty().trim()
        val confirmOtp = etOtpConfirm.text?.toString().orEmpty().trim()
        if (otp.length != expectedLen || confirmOtp.length != expectedLen) return
        if (otp != confirmOtp) return
        if (!btnContinue.isEnabled) return
        if (isVerifying) return

        isVerifying = true
        btnContinue.isEnabled = false
        verifyOtp(otp) {
            isVerifying = false
            btnContinue.isEnabled = true
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
                    userApi.verifyOtp(
                        VerifyOtpRequest(
                            customId = userId,
                            userId = userId,
                            otp = otp
                        )
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val isVerified =
                        body.verified ||
                            body.data?.success == true ||
                            body.status.equals("success", ignoreCase = true)
                    val resolvedMessage =
                        body.data?.message?.takeIf { it.isNotBlank() }
                            ?: body.message.takeIf { it.isNotBlank() }
                            ?: "OTP verified."
                    val uiMessage =
                        if (resolvedMessage.contains("already verified", ignoreCase = true)) {
                            "OTP verified successfully."
                        } else {
                            resolvedMessage
                        }

                    if (isVerified) {
                        Log.d(TAG, "OTP verified: $resolvedMessage")
                        Toast.makeText(
                            this@OtpInputPage,
                            uiMessage,
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to Passkey activation, passing userId
                        val intent = Intent(this@OtpInputPage, PasskeyActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            putExtra("USER_EMAIL", userEmail)
                            putExtra("USER_PHONE", userPhone)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@OtpInputPage,
                            resolvedMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    val cleanMessage = sanitizeServerError(errorBody, response.code())
                    Log.e(TAG, "OTP verification failed: ${response.code()} — $errorBody")
                    Toast.makeText(
                        this@OtpInputPage,
                        cleanMessage,
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

    private fun hideKeyboard(view: android.view.View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    private fun navigateBackToRegistration() {
        if (isVerifying) {
            Toast.makeText(
                this,
                "Please wait until verification completes.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        navigateBackToRegistration()
    }

    private fun sanitizeServerError(raw: String, code: Int): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "Verification failed (HTTP $code). Please try again."
        val looksLikeHtml =
            trimmed.startsWith("<!doctype", ignoreCase = true) ||
                trimmed.startsWith("<html", ignoreCase = true) ||
                trimmed.contains("<body", ignoreCase = true) ||
                Regex("<\\s*[a-zA-Z][^>]*>").containsMatchIn(trimmed)
        if (looksLikeHtml) return "Verification failed (HTTP $code). Please try again."

        // Handle backend JSON errors like:
        // {"message":"registration info not found or expired","status":"error"}
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            runCatching {
                val msg = org.json.JSONObject(trimmed).optString("message").trim()
                if (msg.isNotEmpty()) {
                    if (msg.contains("not found or expired", ignoreCase = true)) {
                        return "OTP session expired. Please register again to get a new OTP."
                    }
                    return "Verification failed: $msg"
                }
            }
        }

        val noTags = trimmed.replace(Regex("<[^>]*>"), " ")
        val clean = noTags.replace(Regex("\\s+"), " ").trim()
        val limited = if (clean.length > 180) clean.take(180) + "…" else clean
        return "Verification failed: $limited"
    }
}