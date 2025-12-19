package com.eazymile.app

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.eazymile.app.databinding.ActivitySignUpBinding
import com.google.android.material.transition.platform.MaterialFade
import android.content.res.ColorStateList
import android.os.Build
import androidx.core.graphics.toColorInt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import android.view.View

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    // Off-black default color
    private var currentButtonColor: Int = "#1A1A1A".toColorInt()

    // Explicit URL for your Realtime Database
    private val database = FirebaseDatabase.getInstance(
        "https://eazy-mile-default-rtdb.asia-southeast1.firebasedatabase.app"
    ).reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Add fade transitions
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
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Restrict phone input to 10 digits only
        binding.etPhone.filters = arrayOf(InputFilter.LengthFilter(10))


        // Full Name formatting & validation
        binding.etFullName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val fullName = s.toString().trim()
                if (fullName.isNotEmpty()) {
                    val formatted = formatFullName(fullName)
                    if (formatted != fullName) {
                        binding.etFullName.setText(formatted)
                        binding.etFullName.setSelection(formatted.length)
                    }
                }
                updateButtonState()
            }
        })


        // Email watcher
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateButtonState()
            }
        })


        // Phone watcher
        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateButtonState()
            }
        })

        binding.rgGender.setOnCheckedChangeListener { _, _ ->
            updateButtonState()
        }

        binding.cbAbove18.setOnCheckedChangeListener { _, _ ->
            updateButtonState()
        }

        // Handle Create Account
        binding.btnSignUp.setOnClickListener {
            if (binding.btnSignUp.isEnabled) {
                writeNewUser() // Write data to Firebase
                val intent = Intent(this, AreaChooserActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        // Navigate to Login
        binding.tvSignIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(
                intent,
                android.app.ActivityOptions
                    .makeSceneTransitionAnimation(this)
                    .toBundle()
            )
            finish()
        }
        updateButtonState()
    }

    // Firebase Realtime Database write operation
    private fun writeNewUser() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val gender = when (binding.rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Male"
            R.id.rbFemale -> "Female"
            R.id.rbOther -> "Other"
            else -> ""
        }
        val isAbove18 = binding.cbAbove18.isChecked

        // Generate custom user ID (e.g., U2501, U2502)
        val lastUserId = database.child("metadata").child("lastUserId").get().addOnSuccessListener { snapshot ->
            val lastId = snapshot.getValue(Int::class.java) ?: 2500 // Start from 2500
            val newId = lastId + 1
            val userId = "U${newId}"

            val userData = hashMapOf(
                "userId" to userId,
                "fullName" to fullName,
                "email" to email,
                "phone" to phone,
                "gender" to gender,
                "isAbove18" to isAbove18
            )

            // Update metadata with the new lastUserId
            database.child("metadata").child("lastUserId").setValue(newId)

            // Write to Realtime Database
            database.child("users").child(userId).setValue(userData) { error, _ ->
                if (error == null) {
                    Log.d("FirebaseWrite", "User saved successfully at ID: $userId")
                } else {
                    Log.e("FirebaseWrite", "Failed to write user: ${error.message}")
                }
            }
        }.addOnFailureListener {
            Log.e("FirebaseWrite", "Failed to fetch lastUserId: ${it.message}")
        }
    }

    private fun goToNext() {
        val intent = Intent(this, AreaChooserActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun formatFullName(fullName: String): String {
        return fullName
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }

    private fun isValidFullName(fullName: String): Boolean {
        val names = fullName.split(" ").filter { it.isNotBlank() }
        if (names.size < 2) return false
        return names.all { it.matches(Regex("^[A-Z][a-z]*$")) }
    }

    private fun updateButtonState() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val genderSelected = binding.rgGender.checkedRadioButtonId != -1
        val termsAccepted = binding.cbAbove18.isChecked

        val allValid = isValidFullName(fullName) &&
                email.isNotEmpty() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                phone.length == 10 &&
                genderSelected &&
                termsAccepted

        binding.btnSignUp.isEnabled = allValid

        val targetColor = if (allValid) {
            "#22C55E".toColorInt()
        } else {
            "#F71A1A1A".toColorInt()
        }

        if (targetColor != currentButtonColor) {
            val animator = ValueAnimator.ofObject(ArgbEvaluator(), currentButtonColor, targetColor)
            animator.duration = 280
            animator.addUpdateListener {
                binding.btnSignUp.backgroundTintList =
                    ColorStateList.valueOf(it.animatedValue as Int)
            }
            animator.start()
            currentButtonColor = targetColor
        }
    }
}
