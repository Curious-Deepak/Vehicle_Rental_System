package com.eazymile.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import com.google.android.material.transition.platform.MaterialFade


class LoginActivity : AppCompatActivity() {


    private lateinit var etPhone: TextInputEditText
    private lateinit var layoutPhone: TextInputLayout
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnSignUp: MaterialButton
    private lateinit var tvSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply fade when entering login screen
        window.enterTransition = MaterialFade().apply { duration = 300 }
        window.exitTransition = MaterialFade().apply { duration = 300 }

        super.onCreate(savedInstanceState)
        // Full screen (under notch / status bar)
        if (Build.VERSION.SDK_INT >= 30) {
            // Android 11 (API 30+) only
            window.setDecorFitsSystemWindows(false)
        } else {
            // For Android 10 and below
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE }

        setContentView(R.layout.activity_login)

        // Initialize views
        etPhone = findViewById(R.id.etPhone)
        layoutPhone = findViewById(R.id.layoutPhone)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvSignUp = findViewById(R.id.tvSignUp)

        // Restrict phone input to 10 digits only
        etPhone.filters = arrayOf(InputFilter.LengthFilter(10))

        // Show error if less than 10 digits
        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length < 10) {
                    layoutPhone.error = "Enter a valid 10-digit phone number"
                } else {
                    layoutPhone.error = null
                }
            }
        })

        // Login button - Navigate to AuthActivity with SharedAxis
        btnLogin.setOnClickListener {
            val phone = etPhone.text.toString().trim()

            if (phone.length == 10) {
                layoutPhone.error = null
                val intent = Intent(this, AuthActivity::class.java)

                // Passing mobile number with +91 prefix to AuthActivity
                intent.putExtra("MOBILE_NUMBER", "+91 $phone")

                startActivity(
                    intent,
                    android.app.ActivityOptions
                        .makeSceneTransitionAnimation(this)
                        .toBundle()
                )
            } else {
                layoutPhone.error = "Phone number must be 10 digits"
            }
        }

        // SignUp TextView - Navigate to SignUpActivity with Fade
        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(
                intent,
                android.app.ActivityOptions
                    .makeSceneTransitionAnimation(this)
                    .toBundle()
            )
        }

        // Create Account button - Navigate to SignUpActivity with Fade
        btnSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(
                intent,
                android.app.ActivityOptions
                    .makeSceneTransitionAnimation(this)
                    .toBundle()
            )
        }
    }

}
