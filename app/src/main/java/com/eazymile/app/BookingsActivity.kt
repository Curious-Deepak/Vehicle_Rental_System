package com.eazymile.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.eazymile.app.adapters.BookingsAdapter
import com.eazymile.app.databinding.ActivityBookingsBinding
import com.eazymile.app.models.Booking
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class BookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingsBinding
    private lateinit var bookingsAdapter: BookingsAdapter
    private val bookingsList = mutableListOf<Booking>()
    private val database = FirebaseDatabase.getInstance().reference.child("bookings")

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
        binding = ActivityBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        handleBackGesture()

        // Setup RecyclerView with dynamic buttons and proper flow
        bookingsAdapter = BookingsAdapter(bookingsList) { booking, action ->
            handleBookingAction(booking, action)
        }

        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(this@BookingsActivity)
            adapter = bookingsAdapter
        }

        // Swipe to refresh feature
        binding.swipeRefresh.setOnRefreshListener {
            loadBookingsFromDatabase {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        // Load existing bookings from Realtime Database
        loadBookingsFromDatabase()

        // Check if FleetActivity sent a new booking
        val bookingData = intent.getSerializableExtra("bookingData") as? Booking
        bookingData?.let {
            saveBookingToRealtimeDatabase(it)
        }
    }

// Load all bookings from Realtime Database
    private fun loadBookingsFromDatabase(onComplete: (() -> Unit)? = null) {
        database.get().addOnSuccessListener { snapshot ->
            bookingsList.clear()
            for (child in snapshot.children) {
                val booking = child.getValue(Booking::class.java)
                booking?.let { bookingsList.add(it) }
            }
            bookingsList.reverse() // newest first
            bookingsAdapter.notifyDataSetChanged()
            onComplete?.invoke()
        }.addOnFailureListener { e ->
            e.printStackTrace()
            onComplete?.invoke()
        }
    }

// Save booking into Firebase Realtime Database
    private fun saveBookingToRealtimeDatabase(booking: Booking) {
        val bookingId = booking.bookingId.ifEmpty { generateBookingId() }
        val bookingRef = database.child(bookingId)

        // Default status to Pending if empty
        if (booking.status.isEmpty()) booking.status = "pending"

        val bookingMap = mapOf(
            "bookingId" to bookingId,
            "fleetName" to booking.fleetName,
            "fleetModel" to booking.fleetModel,
            "date" to booking.date,
            "duration" to booking.duration,
            "status" to booking.status,
            "price" to booking.price,
            "selectedPrice" to booking.selectedPrice,
            "timestamp" to System.currentTimeMillis()
        )

        bookingRef.setValue(bookingMap)
            .addOnSuccessListener {
                bookingsList.add(0, booking)
                bookingsAdapter.notifyItemInserted(0)
            }
            .addOnFailureListener { e -> e.printStackTrace() }
    }

// Handle booking action flow : start - active - end - completed / cancel
    private fun handleBookingAction(booking: Booking, action: String) {
        val newStatus = when (action) {
            "start" -> "active"
            "end" -> "completed"
            "cancel" -> "cancelled"
            else -> booking.status
        }
        booking.status = newStatus
        bookingsAdapter.notifyDataSetChanged()

        // Update Firebase
        val bookingRef = database.child(booking.bookingId)
        bookingRef.child("status").setValue(newStatus)
            .addOnFailureListener { e -> e.printStackTrace() }
    }

// Generate booking ID based on current date and time
    private fun generateBookingId(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR) % 100
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val random = (100..999).random()
        return "BK${year}${day}$random"
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = binding.bottomNavigation
        bottomNavigation.selectedItemId = R.id.nav_bookings
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> navigateTo(HomeActivity::class.java)
                R.id.nav_fleet -> navigateTo(FleetActivity::class.java)
                R.id.nav_profile -> navigateTo(ProfileActivity::class.java)
                else -> true
            }
        }
    }

    private fun navigateTo(destination: Class<*>): Boolean {
        if (this::class.java != destination) {
            startActivity(Intent(this, destination))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
        return true
    }

    private fun handleBackGesture() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        })
    }
}
