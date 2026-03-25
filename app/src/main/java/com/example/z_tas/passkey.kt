package com.example.z_tas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PasskeyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activate_passkey)

        // Point 1
        setupFeature(
                R.id.feature1,
                "Biometric Authentication",
                "Use your fingerprint or face to sign in instantly."
        )

        // Point 2
        setupFeature(
                R.id.feature2,
                "No Passwords Required",
                "Eliminate the risk of forgotten or stolen passwords."
        )

        // Point 3
        setupFeature(
                R.id.feature3,
                "Military-grade Encryption",
                "Your data is protected by end-to-end AES-256 security."
        )

        findViewById<View>(R.id.btnActivate).setOnClickListener {
            // Passkey creation will be implemented after backend integration
            Toast.makeText(this, "Passkey activation setup.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun setupFeature(id: Int, title: String, desc: String) {
        val root = findViewById<View>(id)
        root.findViewById<TextView>(R.id.tvFeatureTitle).text = title
        root.findViewById<TextView>(R.id.tvFeatureDesc).text = desc
    }
}
