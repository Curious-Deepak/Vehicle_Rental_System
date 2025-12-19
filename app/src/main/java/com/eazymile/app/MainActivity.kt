package com.eazymile.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.eazymile.app.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private var isPulsing = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen (under notch / status bar)
        if (Build.VERSION.SDK_INT >= 30) {
            // Android 11 (API 30+) only
            window.setDecorFitsSystemWindows(false)
        } else {
            // For Android 10 and below
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        // Start pulsing animation
        startPulseAnimation()
        setupClickListeners()
    }

    private fun startPulseAnimation() {
        val pulseRunnable = object : Runnable {
            override fun run() {
                if (isPulsing) {
                    binding.btnStart.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(400)
                        .withEndAction {
                            binding.btnStart.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(400)
                                .withEndAction {
                                    handler.postDelayed(this, 100)
                                }.start()
                        }.start()
                }
            }
        }
        handler.post(pulseRunnable)
    }

    private fun setupClickListeners() {
        binding.btnStart.setOnClickListener { button ->
            // Stop pulsing
            isPulsing = false
            handler.removeCallbacksAndMessages(null)

            // Micro bounce on click
            button.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
                button.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()

            // Navigate to LoginActivity after animation
            button.postDelayed({
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }, 200)
        }
    }
}
