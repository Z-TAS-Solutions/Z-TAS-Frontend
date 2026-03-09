package com.ztas.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.z_tas.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activate_passkey)

        // Point 1
        setupFeature(R.id.feature1, "Biometric Authentication",
            "Use your fingerprint or face to sign in instantly.")

        // Point 2
        setupFeature(R.id.feature2, "No Passwords Required",
            "Eliminate the risk of forgotten or stolen passwords.")

        // Point 3
        setupFeature(R.id.feature3, "Military-grade Encryption",
            "Your data is protected by end-to-end AES-256 security.")

        findViewById<View>(R.id.btnActivate).setOnClickListener {
            // Biometric logic goes here
        }
    }

    private fun setupFeature(id: Int, title: String, desc: String) {
        val root = findViewById<View>(id)
        root.findViewById<TextView>(R.id.tvFeatureTitle).text = title
        root.findViewById<TextView>(R.id.tvFeatureDesc).text = desc
    }
}