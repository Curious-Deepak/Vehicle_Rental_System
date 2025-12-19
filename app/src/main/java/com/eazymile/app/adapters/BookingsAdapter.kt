package com.eazymile.app.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eazymile.app.R
import com.eazymile.app.models.Booking
import com.google.android.material.button.MaterialButton

class BookingsAdapter(
    private val bookings: List<Booking>,
    private val onActionClick: (Booking, String) -> Unit
) : RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun getItemCount(): Int = bookings.size

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBookingId: TextView = itemView.findViewById(R.id.tvBookingId)
        private val tvModel: TextView = itemView.findViewById(R.id.tvModel)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvTotalCost: TextView = itemView.findViewById(R.id.tvTotalCost)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvBookingStatus: TextView = itemView.findViewById(R.id.tvBookingStatus)
        private val ivStatusIcon: ImageView = itemView.findViewById(R.id.ivStatusIcon)
        private val llActionButtons: LinearLayout = itemView.findViewById(R.id.llActionButtons)
        private val btnStartRide: MaterialButton = itemView.findViewById(R.id.btnStartRide)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btnCancel)

        fun bind(booking: Booking) {
            // Basic info
            tvBookingId.text = booking.bookingId
            tvModel.text = booking.fleetModel
            tvDate.text = booking.date
            tvDuration.text = booking.duration
            tvTotalCost.text = booking.price
            tvStatus.text = booking.status.replaceFirstChar { it.uppercase() }
            tvBookingStatus.text = booking.status.replaceFirstChar { it.uppercase() }

            // Set UI based on status
            when (booking.status.lowercase()) {
                "pending" -> {
                    tvBookingStatus.text = "Pending"
                    tvBookingStatus.setBackgroundResource(R.drawable.status_pending_bg)
                    tvBookingStatus.setTextColor(Color.BLACK)
                    ivStatusIcon.setImageResource(R.drawable.ic_pending)
                    llActionButtons.visibility = View.VISIBLE
                    btnStartRide.text = "Start Ride"
                    btnCancel.visibility = View.VISIBLE
                }
                "active" -> {
                    tvBookingStatus.text = "Active"
                    tvBookingStatus.setBackgroundResource(R.drawable.status_active_bg)
                    tvBookingStatus.setTextColor(Color.BLACK)
                    ivStatusIcon.setImageResource(R.drawable.ic_play)
                    llActionButtons.visibility = View.VISIBLE
                    btnStartRide.text = "End Ride"
                    btnCancel.visibility = View.GONE
                }
                "completed" -> {
                    tvBookingStatus.text = "Completed"
                    tvBookingStatus.setBackgroundResource(R.drawable.status_completed_bg)
                    tvBookingStatus.setTextColor(Color.BLACK)
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                    llActionButtons.visibility = View.GONE
                }
                "cancelled" -> {
                    tvBookingStatus.text = "Cancelled"
                    tvBookingStatus.setBackgroundResource(R.drawable.status_cancelled_bg)
                    tvBookingStatus.setTextColor(Color.BLACK)
                    ivStatusIcon.setImageResource(R.drawable.ic_cancel)
                    llActionButtons.visibility = View.GONE
                }
            }

            // Handle button clicks
            btnStartRide.setOnClickListener {
                val action = if (booking.status.lowercase() == "pending") "start" else "end"
                onActionClick(booking, action)
            }

            btnCancel.setOnClickListener {
                onActionClick(booking, "cancel")
            }
        }
    }
}
