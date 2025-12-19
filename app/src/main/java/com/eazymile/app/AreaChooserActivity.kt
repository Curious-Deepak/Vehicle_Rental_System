package com.eazymile.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.eazymile.app.adapters.AreaAdapter
import com.eazymile.app.models.Area
import com.google.firebase.database.*

class AreaChooserActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AreaAdapter
    private lateinit var searchLayout: TextInputLayout
    private lateinit var searchText: TextInputEditText
    private lateinit var database: DatabaseReference

    private val allAreas = mutableListOf<Area>()

    // Firebase listener reference for proper cleanup
    private lateinit var areaListener: ValueEventListener

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
        setContentView(R.layout.activity_area_chooser)

        // Enable Firebase local persistence once
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Already enabled - ignore
        }

        // Firebase reference
        database = FirebaseDatabase.getInstance().getReference("areas")
        database.keepSynced(true)
        fetchAreasFromFirebase()

        recyclerView = findViewById(R.id.recycler_view_areas)
        searchLayout = findViewById(R.id.search_layout)
        searchText = findViewById(R.id.search_text)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AreaAdapter(allAreas.toMutableList(), this) { selectedArea ->
            // Send data and open HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("selected_area", selectedArea.name)
            // Optional: clear previous HomeActivity instance so you don't stack activities
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
            // closes AreaChooserActivity
        }
        recyclerView.adapter = adapter

        // Setup search box
        setupSearchBox()
    }

    private fun fetchAreasFromFirebase() {
        areaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allAreas.clear()
                for (areaSnapshot in snapshot.children) {
                    val id = areaSnapshot.child("id").getValue(String::class.java) ?: ""
                    val name = areaSnapshot.child("name").getValue(String::class.java) ?: ""
                    val evCycle = areaSnapshot.child("vehicles/evCycle").getValue(Int::class.java) ?: 0
                    val scooter = areaSnapshot.child("vehicles/scooter").getValue(Int::class.java) ?: 0
                    val bike = areaSnapshot.child("vehicles/bike").getValue(Int::class.java) ?: 0

                    val area = Area(id, name, evCycle, scooter, bike)
                    allAreas.add(area)
                }
                adapter.updateData(allAreas.toMutableList())
            }

            override fun onCancelled(error: DatabaseError) {
                // log or handle error - Optionally
            }
        }

        // Attach listener
        database.addValueEventListener(areaListener)
    }

    private fun setupSearchBox() {
        searchLayout.startIconDrawable =
            AppCompatResources.getDrawable(this, R.drawable.ic_search)
        searchLayout.endIconDrawable =
            AppCompatResources.getDrawable(this, R.drawable.ic_arrow_forward)

        searchText.clearFocus()
        searchText.isCursorVisible = false
        searchText.isFocusable = false
        searchText.isFocusableInTouchMode = false

        searchText.setOnClickListener {
            searchText.isFocusableInTouchMode = true
            searchText.isFocusable = true
            searchText.isCursorVisible = true
            searchText.requestFocus()
        }

        searchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim().lowercase()
                val filtered = if (query.isEmpty()) {
                    allAreas
                } else {
                    allAreas.filter { it.name.lowercase().contains(query) }
                }
                adapter.updateData(filtered.toMutableList())
            }
        })

        searchText.setOnEditorActionListener { _, actionId, _ ->
            actionId == EditorInfo.IME_ACTION_SEARCH
        }
    }

    // Remove Firebase listener to prevent finalizer warnings
    override fun onDestroy() {
        super.onDestroy()
        if (::areaListener.isInitialized) {
            database.removeEventListener(areaListener)
        }
    }
}
