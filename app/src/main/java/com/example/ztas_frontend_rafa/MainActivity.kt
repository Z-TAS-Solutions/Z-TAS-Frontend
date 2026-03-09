package com.example.ztas_frontend_rafa

import ProfileFragment
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ztas_frontend_rafa.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .commit()
        }
    }
}
