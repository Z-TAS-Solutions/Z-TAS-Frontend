package com.ztas.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MfaActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getStringExtra("requestId") ?: ""

        when (intent.action) {
            "MFA_APPROVE" -> {
                Toast.makeText(context, "MFA Approved ✓", Toast.LENGTH_SHORT).show()
                
            }
            "MFA_DENY" -> {
                Toast.makeText(context, "MFA Denied ✗", Toast.LENGTH_SHORT).show()
            
            }
        }

        // Dismiss notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(1001)
    }
}
