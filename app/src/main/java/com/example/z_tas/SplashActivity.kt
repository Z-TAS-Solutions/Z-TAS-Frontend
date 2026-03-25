package com.example.z_tas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val shimmerLayout = findViewById<ShimmerFrameLayout>(R.id.shimmer_layout)

        // Alpha shimmer: only a white shine line passes over logo
        val shimmer = Shimmer.AlphaHighlightBuilder()
            .setBaseAlpha(1.0f)          // logo fully visible
            .setHighlightAlpha(0.3f)     // shine line is semi-transparent
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setAutoStart(true)
            .setRepeatCount(3)            // number of passes
            .setDuration(3000L)           // slow shimmer
            .build()

        shimmerLayout.setShimmer(shimmer)

        // Wait 2-3 seconds before opening LoginActivity
        shimmerLayout.postDelayed({
            // Check auth state here if applicable, but defaulting to LoginActivity
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }, 2500) // 2.5 seconds total before moving to app
    }
}