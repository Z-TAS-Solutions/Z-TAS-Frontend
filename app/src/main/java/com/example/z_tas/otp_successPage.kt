package com.example.z_tas

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.otp_sucess_page)

        val btnHome = findViewById<Button>(R.id.btnHome)

        /* REDIRECT DISABLED:
           Waiting for merge with friend's Home page code.
        */

        // The timer is commented out to prevent crashes
        /*
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToHome()
        }, 3000L)
        */

        // Temporary: Just show a message when clicking Home until the merge
        btnHome.setOnClickListener {
            Toast.makeText(this, "Home page coming soon after merge!", Toast.LENGTH_SHORT).show()
        }
    }

    // Function is kept but empty so it doesn't cause errors
    private fun navigateToHome() {
        // val intent = Intent(this, HomeActivity::class.java)
        // startActivity(intent)
        // finish()
    }
}