package com.eazymile.app.adapters


import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.eazymile.app.R
import com.eazymile.app.models.Area


class AreaAdapter(
    private var areas: MutableList<Area>,
    private val context: Context,
    private val onItemClick: (Area) -> Unit
) : RecyclerView.Adapter<AreaAdapter.AreaViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION
    private var isAnimating = false

    inner class AreaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val areaCard: MaterialCardView = itemView.findViewById(R.id.areaCard)
        private val areaName: TextView = itemView.findViewById(R.id.AreaName)
        private val areaDetails: TextView = itemView.findViewById(R.id.AreaDetails)

        fun bind(area: Area, isSelected: Boolean) {
            areaName.text = area.name
            areaDetails.text = "${area.evCycle} EV • ${area.scooter} Scooters • ${area.bike} Bikes"

            // Default selection styling
            if (isSelected) {
                areaCard.setCardBackgroundColor(context.getColor(R.color.primary_green_light))
                areaCard.strokeWidth = 4
                areaCard.strokeColor = context.getColor(R.color.primary_green)
            } else {
                areaCard.setCardBackgroundColor(Color.WHITE)
                areaCard.strokeWidth = 0
            }

            itemView.setOnClickListener {
                if (isAnimating) return@setOnClickListener

                val previousPosition = selectedPosition
                selectedPosition = bindingAdapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                // Navigate after animation
                animateGlow(areaCard) {
                    onItemClick(area)
                }
            }
        }

        private fun animateGlow(card: MaterialCardView, onAnimationEnd: () -> Unit) {
            isAnimating = true

            // Animate border color pulse
            val colorFrom = context.getColor(R.color.primary_green)
            val colorTo = context.getColor(R.color.primary_green_light)
            val borderAnimator = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo, colorFrom)
            borderAnimator.duration = 1200
            borderAnimator.addUpdateListener { animator ->
                card.strokeColor = animator.animatedValue as Int
            }

            val elevFrom = 4f
            val elevTo = 16f
            val elevAnimator = ValueAnimator.ofFloat(elevFrom, elevTo, elevFrom)
            elevAnimator.duration = 1200
            elevAnimator.addUpdateListener { animator ->
                card.cardElevation = animator.animatedValue as Float
            }

            borderAnimator.start()
            elevAnimator.start()

            Handler(Looper.getMainLooper()).postDelayed({
                isAnimating = false
                onAnimationEnd()
            }, 1600)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AreaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_area, parent, false)
        return AreaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AreaViewHolder, position: Int) {
        holder.bind(areas[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = areas.size

    // Update data when filtering
    fun updateData(newList: MutableList<Area>) {
        areas = newList
        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }
}
