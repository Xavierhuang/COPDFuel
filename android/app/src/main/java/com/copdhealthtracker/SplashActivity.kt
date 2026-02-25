package com.copdhealthtracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.copdhealthtracker.databinding.ActivitySplashBinding
import com.copdhealthtracker.utils.AppApplication

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            val app = application as AppApplication
            val target = if (app.copdAuth.getCurrentUser() != null) MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, target))
            finish()
        }, 2000)
    }
}
