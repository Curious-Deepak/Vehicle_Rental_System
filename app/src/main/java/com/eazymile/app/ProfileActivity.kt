package com.eazymile.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.eazymile.app.adapters.ProfileMenuAdapter
import com.eazymile.app.databinding.ActivityProfileBinding
import com.eazymile.app.models.Booking
import com.eazymile.app.models.ProfileMenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var bookingsRef: DatabaseReference
    private var completedRides = 0
    private var totalHours = 0.0
    private var co2Saved = 0.0
    private val co2PerHour = 0.12

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
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Database Url
        bookingsRef = FirebaseDatabase.getInstance("https://eazy-mile-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("bookings")

        setupUI()
        loadBookingsOnce()
    }

    private fun setupUI() {
        setupBottomNavigation()
        setupProfileMenu()
        setupSignOutButton()
        showLoadingState()
    }

//  Loads Bookings Data
    private fun loadBookingsOnce() {
        Log.d("ProfileActivity", "LOADING FROM YOUR DATABASE...")

        bookingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("ProfileActivity", "FOUND ${snapshot.childrenCount} BOOKINGS!")

                completedRides = 0
                totalHours = 0.0
                co2Saved = 0.0

                if (snapshot.childrenCount == 0L) {
                    Log.w("ProfileActivity", "No bookings")
                    updateStatsUI()
                    return
                }

                // PROCESS YOUR EXACT DATA
                for (bookingSnapshot in snapshot.children) {
                    Log.d("ProfileActivity", "PROCESSING: ${bookingSnapshot.key}")

                    // MANUAL PARSING
                    val status = bookingSnapshot.child("status").getValue(String::class.java) ?: ""
                    val duration = bookingSnapshot.child("duration").getValue(String::class.java) ?: ""

                    Log.d("ProfileActivity", "Status: '$status' | Duration: '$duration'")

                    // YOUR STATUS IS "completed" (lowercase)
                    if (status.lowercase() == "completed") {
                        completedRides++

                        // PARSE YOUR "XX hours YY min" FORMAT
                        val (hours, minutes) = parseYourDuration(duration)
                        val rideHours = hours + (minutes / 60.0)

                        totalHours += rideHours
                        co2Saved += rideHours * co2PerHour

                        Log.d("ProfileActivity", "Ride #$completedRides: $rideHours hours | CO2: ${rideHours * co2PerHour}")
                    }
                }

                Log.d("ProfileActivity", "STATS: $completedRides rides | ${String.format("%.1f", totalHours)} hrs | ${String.format("%.2f", co2Saved)} kg")
                updateStatsUI()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileActivity", "ERROR: ${error.message}")
                showErrorState()
            }
        })
    }

//  Duration Parsing
    private fun parseYourDuration(duration: String): Pair<Double, Double> {
        Log.d("ProfileActivity", "PARSING YOUR FORMAT: '$duration'")

        if (duration.isBlank()) return Pair(0.0, 0.0)


        val hoursMatch = Regex("(\\d+)\\s*hours?").find(duration)
        val minutesMatch = Regex("(\\d+)\\s*min").find(duration)

        val hours = hoursMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val minutes = minutesMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        Log.d("ProfileActivity", "PARSED: $hours hours + $minutes minutes")
        return Pair(hours, minutes)
    }

    private fun showLoadingState() {
        binding.tvTotalRides.text = "0"
        binding.tvTotalDistance.text = "Loading..."
        binding.tvCO2Saved.text = "Loading..."
    }

    private fun showErrorState() {
        binding.tvTotalRides.text = "Error"
        binding.tvTotalDistance.text = "Error"
        binding.tvCO2Saved.text = "Error"
    }

    private fun updateStatsUI() {
        binding.tvTotalRides.text = completedRides.toString()
        binding.tvTotalDistance.text = String.format("%.1f hrs", totalHours)
        binding.tvCO2Saved.text = String.format("%.2f kg", co2Saved)
    }

    // Bottom Navigation Setup
    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_profile
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, HomeActivity::class.java)); true }
                R.id.nav_fleet -> { startActivity(Intent(this, FleetActivity::class.java)); true }
                R.id.nav_bookings -> { startActivity(Intent(this, BookingsActivity::class.java)); true }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun setupProfileMenu() {
        val menuList = listOf(
            ProfileMenuItem("Payment Gateway", R.drawable.ic_payment),
            ProfileMenuItem("Ride History", R.drawable.ic_clock)
        )
        val adapter = ProfileMenuAdapter(menuList) {}
        binding.rvProfileMenu.layoutManager = LinearLayoutManager(this)
        binding.rvProfileMenu.adapter = adapter
    }

//   Sign Out Button
    private fun setupSignOutButton() {
        binding.btnSignOut.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}