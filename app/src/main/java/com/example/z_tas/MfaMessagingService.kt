package com.example.z_tas

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MfaMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "FCM Token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.data["title"] ?: message.notification?.title ?: "MFA Approval Required"
        val body = message.data["body"] ?: message.notification?.body ?: "Confirm your identity for Z-TAS"
        val requestId = message.data["requestId"] ?: ""

        showMfaNotification(title, body, requestId)
    }

    private fun showMfaNotification(title: String, body: String, requestId: String) {
        val channelId = "mfa_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // create the channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MFA Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Multi-Factor Authentication approval requests"
            }
            notificationManager.createNotificationChannel(channel)
        }


        val approveIntent = Intent(this, MfaActionReceiver::class.java).apply {
            action = "MFA_APPROVE"
            putExtra("requestId", requestId)
        }
        val approvePending = PendingIntent.getBroadcast(
            this, 0, approveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val denyIntent = Intent(this, MfaActionReceiver::class.java).apply {
            action = "MFA_DENY"
            putExtra("requestId", requestId)
        }
        val denyPending = PendingIntent.getBroadcast(
            this, 1, denyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap opens MFA approval screen // TEMP: PREVIEW ONLY - change back to HomeActivity::class.java
        val tapIntent = Intent(this, MFAActivity::class.java)
        val tapPending = PendingIntent.getActivity(
            this, 2, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Custom notification layout
        val customView = RemoteViews(packageName, R.layout.notification_mfa)
        customView.setTextViewText(R.id.notif_title, title)
        customView.setTextViewText(R.id.notif_body, body)
        customView.setTextViewText(R.id.notif_device, "Windows PC • Colombo")

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.lock_icon)
            .setCustomContentView(customView)
            .setCustomBigContentView(customView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .addAction(R.drawable.lock_icon, "APPROVE", approvePending)
            .addAction(R.drawable.lock_icon, "DENY", denyPending)
            .build()

        notificationManager.notify(1001, notification)
    }
}
