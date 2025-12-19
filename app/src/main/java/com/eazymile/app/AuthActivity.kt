package com.eazymile.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import android.view.animation.Animation
import android.view.animation.ScaleAnimation


class AuthActivity : AppCompatActivity() {

    private lateinit var etOtp1: EditText
    private lateinit var etOtp2: EditText
    private lateinit var etOtp3: EditText
    private lateinit var etOtp4: EditText
    private lateinit var btnVerifyOtp: MaterialButton
    private lateinit var btnGoBack: MaterialButton
    private lateinit var tvResendOtp: TextView
    private lateinit var tvOtpTimer: TextView
    private lateinit var tvAuthSubtitle: TextView

    private var otpTimer: CountDownTimer? = null

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
        setContentView(R.layout.activity_auth)

        // Initialize views
        etOtp1 = findViewById(R.id.etOtp1)
        etOtp2 = findViewById(R.id.etOtp2)
        etOtp3 = findViewById(R.id.etOtp3)
        etOtp4 = findViewById(R.id.etOtp4)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)
        btnGoBack = findViewById(R.id.btnGoBack)
        tvResendOtp = findViewById(R.id.ResendOtp)
        tvOtpTimer = findViewById(R.id.OtpTimer)
        tvAuthSubtitle = findViewById(R.id.authSubtitle)

        val mobileNumber = intent.getStringExtra("MOBILE_NUMBER") ?: "+91 XXXXXXXX"
        tvAuthSubtitle.text = getString(R.string.auth_message, mobileNumber)

        setupOtpInputs()
        startOtpTimer()

        // Verify OTP button
        btnVerifyOtp.setOnClickListener {
            val otp = etOtp1.text.toString() +
                    etOtp2.text.toString() +
                    etOtp3.text.toString() +
                    etOtp4.text.toString()

            // TODO: backend validation
            val correctOtp = "2345"

            if (otp == correctOtp) {
                startActivity(Intent(this, AreaChooserActivity::class.java))
                finish()
            } else {
                clearOtpFields()
                shakeOtpFields()
            }
        }

        // Go back button
        btnGoBack.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupOtpInputs() {
        val otpFields = listOf(etOtp1, etOtp2, etOtp3, etOtp4)
        otpFields.forEachIndexed { index, editText ->

            // Move forward on input
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < otpFields.size - 1) {
                        otpFields[index + 1].requestFocus()
                    }
                    enableVerifyButton()
                }
            })

            // Move backward on backspace
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && index > 0) {
                        otpFields[index - 1].text.clear()
                        otpFields[index - 1].requestFocus()
                    }
                }
                false
            }
        }
    }

    private fun enableVerifyButton() {
        val isFilled = etOtp1.text.isNotEmpty() &&
                etOtp2.text.isNotEmpty() &&
                etOtp3.text.isNotEmpty() &&
                etOtp4.text.isNotEmpty()
        btnVerifyOtp.isEnabled = isFilled
    }

    private fun startOtpTimer() {
        otpTimer?.cancel()
        tvResendOtp.isEnabled = false

        otpTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) % 60
                val minutes = (millisUntilFinished / 1000) / 60
                tvOtpTimer.text = getString(R.string.otp_timer_format, minutes, seconds)
                tvOtpTimer.setTextColor(resources.getColor(R.color.black, theme))
            }

            override fun onFinish() {
                tvOtpTimer.text = getString(R.string.otp_time_end_message)
                tvOtpTimer.textSize = 14f
                tvOtpTimer.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))

                btnVerifyOtp.isEnabled = false
                tvResendOtp.isEnabled = true
                tvResendOtp.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))

                val toColor = btnVerifyOtp.backgroundTintList?.defaultColor
                    ?: resources.getColor(R.color.primary_green, theme)
                val colorAnimator = android.animation.ValueAnimator.ofArgb(
                    resources.getColor(android.R.color.holo_red_dark, theme),
                    toColor
                )
                colorAnimator.duration = 500
                colorAnimator.addUpdateListener { animator ->
                    tvResendOtp.setTextColor(animator.animatedValue as Int)
                }
                colorAnimator.start()
            }
        }.start()

        tvResendOtp.setOnClickListener {
            if (!tvResendOtp.isEnabled) return@setOnClickListener
            tvResendOtp.animate().alpha(0.5f).setDuration(100).withEndAction {
                tvResendOtp.animate().alpha(1f).duration = 100
            }
            tvResendOtp.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
            startOtpTimer()
            clearOtpFields()
            btnVerifyOtp.isEnabled = true
        }
    }

    private fun clearOtpFields() {
        etOtp1.text.clear()
        etOtp2.text.clear()
        etOtp3.text.clear()
        etOtp4.text.clear()
        etOtp1.requestFocus()
        enableVerifyButton()
    }

    private fun shakeOtpFields() {
        val otpFields = listOf(etOtp1, etOtp2, etOtp3, etOtp4)
        otpFields.forEach { editText ->
            editText.animate()
                .translationX(25f)
                .setDuration(50)
                .withEndAction {
                    editText.animate()
                        .translationX(-25f)
                        .setDuration(50)
                        .withEndAction {
                            editText.animate().translationX(0f).duration = 50
                        }
                }
        }
    }

    override fun onDestroy() {
        otpTimer?.cancel()
        super.onDestroy()
    }
}
