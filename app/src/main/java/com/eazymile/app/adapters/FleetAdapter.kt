package com.eazymile.app.adapters

import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.eazymile.app.R
import com.eazymile.app.models.Fleet
import com.google.android.material.bottomsheet.BottomSheetDialog

class FleetAdapter(
    private var vehicleList: MutableList<Fleet>,
    private val onBookClick: (Fleet) -> Unit
) : RecyclerView.Adapter<FleetAdapter.VehicleViewHolder>() {

    inner class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleTitle: TextView = itemView.findViewById(R.id.vehicleTitle)
        val vehicleImage: ImageView = itemView.findViewById(R.id.vehicleImage)
        val priceDetail: TextView = itemView.findViewById(R.id.priceDetail)
        val hourPrice: TextView = itemView.findViewById(R.id.hourPrice)
        val zeroDepositText: TextView = itemView.findViewById(R.id.zeroDepositText)
        val kmPackageText: TextView = itemView.findViewById(R.id.kmPackageText)
        val bookNowButton: Button = itemView.findViewById(R.id.bookNowButton)
        val vehicleInfoIcon: ImageView = itemView.findViewById(R.id.vehicleInfoIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fleet, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicleList[position]

        // Highlight brand part in red
        val fullName = "${vehicle.brand} | ${vehicle.model}"
        val spannable = SpannableString(fullName)
        val separatorIndex = fullName.indexOf(" | ")
        if (separatorIndex != -1) {
            spannable.setSpan(
                ForegroundColorSpan("#F31010".toColorInt()),
                0,
                separatorIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        holder.vehicleTitle.text = spannable

        // Prices
        holder.priceDetail.text = vehicle.pricePerDay?.let { "From ₹$it/Day" } ?: "N/A"
        holder.hourPrice.text = vehicle.pricePerHour?.let { "Hourly - ₹$it/h" } ?: "N/A"

        // Image
        Glide.with(holder.itemView.context)
            .load(vehicle.imgUrl)
            .placeholder(R.drawable.sample_bike)
            .error(R.drawable.sample_bike)
            .into(holder.vehicleImage)

        // Static texts
        holder.zeroDepositText.text = vehicle.zeroDeposit ?: "Zero Deposit"
        holder.kmPackageText.text = vehicle.kmPackages ?: "KM Packages"

        // Handle Book button state based on status
        if (vehicle.status.equals("Booked", true)) {
            holder.bookNowButton.isEnabled = false
            holder.bookNowButton.text = "Booked"
            holder.bookNowButton.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
            )
        } else {
            holder.bookNowButton.isEnabled = true
            holder.bookNowButton.text = "Book Now"
            holder.bookNowButton.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.context, R.color.bookColor)
            )
        }

        // Book Now click
        holder.bookNowButton.setOnClickListener {
            if (!vehicle.status.equals("Booked", true)) {
                onBookClick(vehicle)
            }
        }

        // Info icon click - show bottom sheet
        holder.vehicleInfoIcon.setOnClickListener {
            val bottomSheetView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.fleet_bottom_sheet, null)
            val tvBrand = bottomSheetView.findViewById<TextView>(R.id.tvBrand)
            val tvModel = bottomSheetView.findViewById<TextView>(R.id.tvModel)
            val tvType = bottomSheetView.findViewById<TextView>(R.id.tvType)
            val tvBrandValue = bottomSheetView.findViewById<TextView>(R.id.tvBrandValue)
            val tvModelValue = bottomSheetView.findViewById<TextView>(R.id.tvModelValue)
            val tvRange = bottomSheetView.findViewById<TextView>(R.id.tvRange)
            val tvPower = bottomSheetView.findViewById<TextView>(R.id.tvPower)
            val tvBattery = bottomSheetView.findViewById<TextView>(R.id.tvBatteryLevel)
            val btnClose = bottomSheetView.findViewById<ImageView>(R.id.ivCancel)
            val btnBookNow = bottomSheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBookNowBottom)

            tvBrand.text = vehicle.brand ?: "N/A"
            tvModel.text = "| ${vehicle.model ?: "N/A"}"
            tvType.text = vehicle.type ?: "N/A"
            tvBrandValue.text = vehicle.brand ?: "N/A"
            tvModelValue.text = vehicle.model ?: "N/A"
            tvRange.text = vehicle.rangeMileage ?: "N/A"
            tvPower.text = vehicle.power ?: "N/A"
            tvBattery.text = vehicle.batteryLevel ?: "N/A"

            // Bottom sheet Book button state
            if (vehicle.status.equals("Booked", true)) {
                btnBookNow.isEnabled = false
                btnBookNow.text = "Booked"
                btnBookNow.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
                )
            } else {
                btnBookNow.isEnabled = true
                btnBookNow.text = "Book Now"
                btnBookNow.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.context, R.color.bookColor)
                )
            }

            val bottomSheetDialog = BottomSheetDialog(holder.itemView.context)
            bottomSheetDialog.setContentView(bottomSheetView)
            btnClose.setOnClickListener { bottomSheetDialog.dismiss() }

            // Bottom sheet Book click
            btnBookNow.setOnClickListener {
                if (!vehicle.status.equals("Booked", true)) {
                    bottomSheetDialog.dismiss()
                    onBookClick(vehicle)
                }
            }

            bottomSheetDialog.show()
        }
    }

    override fun getItemCount(): Int = vehicleList.size

    // Update adapter list
    fun updateList(newList: List<Fleet>) {
        vehicleList.clear()
        vehicleList.addAll(newList)
        notifyDataSetChanged()
    }
}
