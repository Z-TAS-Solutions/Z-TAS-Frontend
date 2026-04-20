package com.ztas.app

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.ztas.app.network.FcmTokenRequest
import com.ztas.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//creates the home page screen and extends componentActivity which is the base class for jetpack compose
class HomeActivity : ComponentActivity() {
    //
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for android phones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        // Log FCM token for testing and debugging
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result.orEmpty()
                Log.d("FCM", "Token: $token")
                syncFcmToken(token)
            }
        }

        setContent { HomeScreen() }
    }

    private fun syncFcmToken(fcmToken: String) {
        if (fcmToken.isBlank()) return
        val bearer = AuthPreferences.bearerOrNull(this) ?: return

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.sessionApi.updateFcmToken(
                        token = bearer,
                        request = FcmTokenRequest(fcmToken = fcmToken)
                    )
                }
                if (!response.isSuccessful) {
                    Log.e("FCM", "FCM token update failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "FCM token update exception", e)
            }
        }
    }
}
