package com.eazymile.app

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.eazymile.app.adapters.HomeAdapter
import com.eazymile.app.databinding.ActivityHomeBinding
import com.eazymile.app.models.Home
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    // ViewBinding for the activity layout
    private lateinit var binding: ActivityHomeBinding

    // Log tag
    private val TAG = "HomeActivity"

    // Launcher to handle result from AreaChooserActivity
    private val areaChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedArea = result.data?.getStringExtra("selected_area")
                if (!selectedArea.isNullOrEmpty()) {
                    binding.locationText.text = selectedArea
                }
            }
        }

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

        // Initialize ViewBinding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set greeting based on Firebase user data
        setGreetingFromFirebase()

        // Update location text if started with selected_area intent
        handleSelectedArea(intent)

        // Set bottom navigation selected item
        val bottomNavigation: BottomNavigationView = binding.bottomNavigation
        bottomNavigation.selectedItemId = R.id.nav_home

        // Set up click listeners
        binding.startDate.setOnClickListener { showHomeBottomSheet(true) } // Start date picker
        binding.endDate.setOnClickListener { showHomeBottomSheet(false) } // End time picker
        binding.locationText.setOnClickListener { shakeAndOpenAreaChooser() } // Area chooser animation
        binding.viewAll.setOnClickListener { openFleetActivity() } // Navigate to full fleet

        // Load fleet data from Firebase Realtime Database
        loadFleetDataFromRealtime()

        // Set bottom navigation item selection listener
        handleNavigation(bottomNavigation)

        // Override back button gesture
        handleBackGesture()

        // Setup search vehicle button
        setupSearchVehicleButton()
    }

//  Handles Search Vehicle button click
    private fun setupSearchVehicleButton() {
        binding.searchButton.setOnClickListener {
            val startDate = binding.startDate.text.toString()
            val duration = binding.endDate.text.toString()
            FleetActivity.openFromSearchVehicle(this, startDate, duration)
        }
    }

//  Sets greeting text from Firebase Realtime Database based on user phone
    private fun setGreetingFromFirebase() {
        val greetingTextView = binding.greeting
        val databaseRef = FirebaseDatabase.getInstance().reference
        val currentUserPhone = getCurrentUserPhone()

        if (currentUserPhone.isEmpty()) {
            greetingTextView.text = "Hi, Guest 👋"
            return
        }

        databaseRef.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                for (userSnap in snapshot.children) {
                    val phone = userSnap.child("phone").value?.toString()
                    if (phone == currentUserPhone) {
                        val fullName = userSnap.child("fullName").value?.toString() ?: "User"
                        greetingTextView.text = "Hi, $fullName 👋"
                        found = true
                        break
                    }
                }
                if (!found) greetingTextView.text = "Hi, User 👋"
            }

            override fun onCancelled(error: DatabaseError) {
                greetingTextView.text = "Hi, Guest 👋"
            }
        })
    }

//  Retrieves current user phone from shared preferences
    private fun getCurrentUserPhone(): String {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getString("phone", "") ?: ""
    }

//  Updates location text if activity is started with a selected area intent
    private fun handleSelectedArea(intent: Intent) {
        val selectedArea = intent.getStringExtra("selected_area")
        if (!selectedArea.isNullOrEmpty()) binding.locationText.text = selectedArea
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSelectedArea(intent)
    }

//  Firebase Realtime Database Fleet Loading
    private fun loadFleetDataFromRealtime() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("fleet")
        val homeList = mutableListOf<Home>()

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                homeList.clear()
                var count = 0

                for (fleetSnap in snapshot.children) {
                    if (count >= 4) break

                    val brand = fleetSnap.child("Brand").getValue(String::class.java) ?: ""
                    val modelName = fleetSnap.child("Model").getValue(String::class.java) ?: ""
                    val pricePerDay = fleetSnap.child("Price_Per_Day").getValue(Double::class.java)?.toInt().toString()
                    val imageUrl = fleetSnap.child("Img_Url").getValue(String::class.java) ?: ""

                    homeList.add(Home(brand, modelName, pricePerDay, imageUrl))
                    count++
                }

                // Set up RecyclerView with HomeAdapter
                binding.recyclerFleet.layoutManager =
                    LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)

                binding.recyclerFleet.adapter = HomeAdapter(this@HomeActivity, homeList) { home ->
                    // On click, navigate to FleetActivity
                    val intent = Intent(this@HomeActivity, FleetActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load fleet data", error.toException())
            }
        })
    }

// Bottom Navigation
    private fun handleNavigation(bottomNavigation: BottomNavigationView) {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_fleet -> navigateTo(FleetActivity::class.java)
                R.id.nav_bookings -> navigateTo(BookingsActivity::class.java)
                R.id.nav_profile -> navigateTo(ProfileActivity::class.java)
                else -> false
            }
        }
    }

// Helper to navigate to a specific activity
    private fun navigateTo(destination: Class<*>): Boolean {
        startActivity(Intent(this, destination))
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        return true
    }

// Back Gesture
    private fun handleBackGesture() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        })
    }

// Area Chooser Animation
    private fun shakeAndOpenAreaChooser() {
        val shake = TranslateAnimation(0f, 10f, 0f, 0f)
        shake.duration = 100
        shake.repeatCount = 3
        shake.repeatMode = Animation.REVERSE
        binding.locationText.startAnimation(shake)
        binding.locationIcon?.startAnimation(shake)

        binding.locationText.postDelayed({
            val intent = Intent(this, AreaChooserActivity::class.java)
            areaChooserLauncher.launch(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 300)
    }

// Opens full FleetActivity
    private fun openFleetActivity() {
        startActivity(Intent(this, FleetActivity::class.java))
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

//  Home Bottom Sheet
    private fun showHomeBottomSheet(isStartDate: Boolean) {
        val view = layoutInflater.inflate(R.layout.home_bottom_sheet, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
        bottomSheetDialog.setCanceledOnTouchOutside(false)

        val closeBtn = view.findViewById<ImageView>(R.id.ivClose)
        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val tvMonthYear = view.findViewById<TextView>(R.id.tvMonthYear)
        val tvSelectHourLabel = view.findViewById<TextView>(R.id.tvSelectHourLabel)
        val btnSelectHour = view.findViewById<Button>(R.id.btnSelectHour)
        val confirmBtn = view.findViewById<Button>(R.id.btnConfirm)

        var selectedDate: String? = null
        var selectedTime: String? = null

        if (isStartDate) {
            // Setup calendar view for start date
            calendarView.visibility = CalendarView.VISIBLE
            tvMonthYear.visibility = TextView.VISIBLE
            tvSelectHourLabel.visibility = TextView.GONE
            btnSelectHour.visibility = Button.GONE

            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val cal = Calendar.getInstance()
            calendarView.date = cal.timeInMillis

            var userInteracted = false
            calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                userInteracted = true
                val tempCal = Calendar.getInstance()
                tempCal.set(year, month, dayOfMonth)
                tvMonthYear.text = sdf.format(tempCal.time)
                selectedDate = "$dayOfMonth/${month + 1}/$year"
            }

            calendarView.setOnScrollChangeListener { _, _, _, _, _ ->
                if (userInteracted) {
                    val scrollCal = Calendar.getInstance()
                    scrollCal.timeInMillis = calendarView.date
                    tvMonthYear.text = sdf.format(scrollCal.time)
                }
            }
        } else {
            // Setup time picker for end date
            calendarView.visibility = CalendarView.GONE
            tvMonthYear.visibility = TextView.GONE
            tvSelectHourLabel.visibility = TextView.VISIBLE
            btnSelectHour.visibility = Button.VISIBLE

            btnSelectHour.setOnClickListener {
                val timePicker = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        selectedTime = String.format("%02d hours %02d min", hourOfDay, minute)
                        btnSelectHour.text = selectedTime
                    },
                    6, 30, true
                )
                timePicker.setTitle("Select Time")
                timePicker.show()
            }
        }

        // Confirm button sets selected date/time to text fields
        confirmBtn.setOnClickListener {
            if (isStartDate && selectedDate != null) binding.startDate.setText(selectedDate)
            else binding.endDate.setText(selectedTime ?: "Select Time")
            bottomSheetDialog.dismiss()
        }

        // Close button dismisses bottom sheet
        closeBtn.setOnClickListener { bottomSheetDialog.dismiss() }
    }
}
