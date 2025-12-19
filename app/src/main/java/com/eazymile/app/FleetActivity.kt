package com.eazymile.app

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.eazymile.app.adapters.FleetAdapter
import com.eazymile.app.databinding.ActivityFleetBinding
import com.eazymile.app.models.Booking
import com.eazymile.app.models.Fleet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class FleetActivity : AppCompatActivity() {

    // View binding for activity layout
    private lateinit var binding: ActivityFleetBinding
    // Adapter for the RecyclerView
    private lateinit var bikesAdapter: FleetAdapter
    // Flag to show ride card
    private var showRideCard: Boolean = false
    // Complete list of fleet vehicles from Firebase
    private val fullFleetList = mutableListOf<Fleet>()
    // Currently selected vehicle type filter
    private var selectedFleetType: String? = null
    // Firebase Realtime Database reference
    private lateinit var databaseRef: DatabaseReference
    // Listener for Firebase database changes
    private var valueEventListener: ValueEventListener? = null

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
        binding = ActivityFleetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        handleBackGesture()

        // Get intent extras to show ride card
        showRideCard = intent.getBooleanExtra("showRideCard", false)

        if (showRideCard) {
            binding.cardRide.visibility = View.VISIBLE
            binding.tvStartDate.text = intent.getStringExtra("startDate") ?: ""
            binding.tvDuration.text = intent.getStringExtra("duration") ?: ""
            binding.ivEditRide.setOnClickListener {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        } else {
            binding.cardRide.visibility = View.GONE
        }

        databaseRef = FirebaseDatabase.getInstance().getReference("fleet")

        // Swipe to refresh listener
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadFleetDataFromFirebase(selectedFleetType)
        }

        setupChipFilter() // Setup filter chips
        loadFleetDataFromFirebase(null)
    }

    // Update vehicle count text
    private fun updateVehicleCount(list: List<Fleet>) {
        binding.vehiclesAvailableText.text = "${list.size} Vehicles Available"
    }

    // Setup chip filter for vehicle type
    private fun setupChipFilter() {
        binding.chipGroup.isSingleSelection = true
        var lastCheckedId = R.id.chipAll
        binding.chipGroup.check(lastCheckedId)
        selectedFleetType = null

        binding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == -1) {
                group.check(lastCheckedId)
                return@setOnCheckedChangeListener
            }

            if (checkedId != lastCheckedId) {
                lastCheckedId = checkedId
                selectedFleetType = when (checkedId) {
                    R.id.chipAll -> null
                    R.id.chipBikes -> "Electric"
                    R.id.chipScooters -> "Oil"
                    else -> null
                }

                val filteredList = when (selectedFleetType) {
                    null -> fullFleetList
                    else -> fullFleetList.filter { it.type.equals(selectedFleetType, true) }
                }

                bikesAdapter.updateList(filteredList)
                updateVehicleCount(filteredList)
            }
        }
    }

    // Load fleet data from Firebase
    private fun loadFleetDataFromFirebase(fleetType: String?) {
        binding.swipeRefreshLayout.isRefreshing = true
        valueEventListener?.let { databaseRef.removeEventListener(it) }

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fleetList = mutableListOf<Fleet>()
                fullFleetList.clear()

                for (fleetSnap in snapshot.children) {
                    val batteryLevel = fleetSnap.child("Battery_Level").getValue(String::class.java) ?: "N/A"
                    val brand = fleetSnap.child("Brand").getValue(String::class.java) ?: ""
                    val imgUrl = fleetSnap.child("Img_Url").getValue(String::class.java) ?: ""
                    val model = fleetSnap.child("Model").getValue(String::class.java) ?: ""
                    val power = fleetSnap.child("Power").getValue(String::class.java) ?: ""
                    val pricePerDay = fleetSnap.child("Price_Per_Day").getValue(Double::class.java) ?: 0.0
                    val pricePerHour = fleetSnap.child("Price_Per_Hour").getValue(Double::class.java) ?: 0.0
                    val rangeMileage = fleetSnap.child("Range_Mileage").getValue(String::class.java) ?: ""
                    val status = fleetSnap.child("Status").getValue(String::class.java) ?: ""
                    val type = fleetSnap.child("Type").getValue(String::class.java) ?: ""
                    val vehicle = fleetSnap.child("Vehicle").getValue(String::class.java) ?: ""

                    val fleet = Fleet(
                        batteryLevel = batteryLevel,
                        brand = brand,
                        imgUrl = imgUrl,
                        model = model,
                        power = power,
                        pricePerDay = pricePerDay,
                        pricePerHour = pricePerHour,
                        rangeMileage = rangeMileage,
                        status = status,
                        type = type,
                        vehicle = vehicle,
                        zeroDeposit = "Zero Deposit",
                        kmPackages = "KM Packages"
                    )

                    // Filter by selected type
                    if (fleetType == null || fleet.type.equals(fleetType, true)) {
                        fleetList.add(fleet)
                    }
                    fullFleetList.add(fleet)
                }

                if (!::bikesAdapter.isInitialized) {
                    // Initialize adapter
                    bikesAdapter = FleetAdapter(fleetList.toMutableList()) { bike ->
                        if (bike.status.equals("Booked", true)) return@FleetAdapter

                        if (showRideCard) {
                            openBookingsActivity(
                                date = binding.tvStartDate.text.toString(),
                                duration = binding.tvDuration.text.toString(),
                                model = bike.model ?: "",
                                hourPrice = bike.pricePerHour.toString()
                            )
                        } else {
                            showFleetBottomSheet(bike)
                        }
                    }

                    binding.rvVehicles.apply {
                        layoutManager = LinearLayoutManager(this@FleetActivity)
                        adapter = bikesAdapter
                    }
                } else {
                    bikesAdapter.updateList(fleetList)
                }

                updateVehicleCount(fleetList)
                binding.swipeRefreshLayout.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FleetActivity", "Firebase load failed: ${error.message}")
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        databaseRef.addValueEventListener(valueEventListener as ValueEventListener)
    }

    // Show bottom sheet for fleet booking
    private fun showFleetBottomSheet(fleet: Fleet) {
        val view = layoutInflater.inflate(R.layout.home_bottom_sheet, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.setCanceledOnTouchOutside(false)

        val tvMonthYear = view.findViewById<TextView>(R.id.tvMonthYear)
        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val btnSelectHour = view.findViewById<Button>(R.id.btnSelectHour)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)
        val ivClose = view.findViewById<ImageView>(R.id.ivClose)

        tvMonthYear.text = "Booking for ${fleet.brand} ${fleet.model}"
        tvMonthYear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

        var selectedDate = getCurrentDate()
        var selectedDuration = "01 hours 00 min"

        // Handle calendar selection
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            selectedDate = sdf.format(cal.time)
        }

        // Show time picker dialog
        btnSelectHour.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            TimePickerDialog(this, { _, h, m ->
                selectedDuration = String.format("%02d hours %02d min", h, m)
                btnSelectHour.text = selectedDuration
            }, hour, minute, true).show()
        }

        // Confirm booking
        btnConfirm.setOnClickListener {
            if (selectedDate.isEmpty() || selectedDuration.isEmpty()) return@setOnClickListener
            openBookingsActivity(
                date = selectedDate,
                duration = selectedDuration,
                model = fleet.model ?: "",
                hourPrice = fleet.pricePerHour.toString()
            )
            bottomSheetDialog.dismiss()
        }

        ivClose.setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.show()
    }

    // Open BookingsActivity with booking data
    private fun openBookingsActivity(date: String, duration: String, model: String, hourPrice: String) {
        val booking = Booking(
            bookingId = generateBookingId(),
            fleetName = model,
            fleetModel = model,
            date = date,
            duration = duration,
            status = "Pending",
            price = calculateTotalCost(duration, hourPrice),
            selectedPrice = hourPrice
        )

        val intent = Intent(this, BookingsActivity::class.java)
        intent.putExtra("bookingData", booking)
        startActivity(intent)
    }

    // Generate unique booking ID
    private fun generateBookingId(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR) % 100
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val random = (100..999).random()
        return "BK$year$day$random"
    }

    // Calculate total cost for selected duration
    private fun calculateTotalCost(duration: String, hourPrice: String): String {
        val hourMatch = Regex("(\\d+)\\s*(h|hours)").find(duration)
        val minMatch = Regex("(\\d+)\\s*(m|min|minutes)").find(duration)
        val hours = hourMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val minutes = minMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val totalHours = hours + (minutes / 60.0)
        val pricePerHour = hourPrice.toDoubleOrNull() ?: 0.0
        val totalCost = totalHours * pricePerHour
        return "₹ %.2f".format(totalCost)
    }

    // Setup bottom navigation menu
    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = binding.bottomNavigation
        bottomNavigation.selectedItemId = R.id.nav_fleet
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { navigateTo(HomeActivity::class.java); true }
                R.id.nav_fleet -> true
                R.id.nav_bookings -> { navigateTo(BookingsActivity::class.java); true }
                R.id.nav_profile -> { navigateTo(ProfileActivity::class.java); true }
                else -> false
            }
        }
    }

    // Navigate to another activity
    private fun navigateTo(destination: Class<*>) {
        if (this::class.java != destination) {
            startActivity(Intent(this, destination))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    // Handle back press gesture
    private fun handleBackGesture() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        })
    }

    // Get current date as string
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    companion object {
        // Open FleetActivity from search vehicle
        fun openFromSearchVehicle(context: Context, startDate: String, duration: String) {
            val intent = Intent(context, FleetActivity::class.java)
            intent.putExtra("showRideCard", true)
            intent.putExtra("startDate", startDate)
            intent.putExtra("duration", duration)
            context.startActivity(intent)
        }
    }
}
