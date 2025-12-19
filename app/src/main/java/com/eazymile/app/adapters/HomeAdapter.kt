package com.eazymile.app.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.eazymile.app.R
import com.eazymile.app.models.Home
import com.google.android.material.button.MaterialButton

class HomeAdapter(
    private val context: Context,
    private val homeList: List<Home>,
    private val onBookClick: (Home) -> Unit = {}
) : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {

    inner class HomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val priceTag: TextView = itemView.findViewById(R.id.price_tag)
        val fleetImage: ImageView = itemView.findViewById(R.id.fleet_image)
        val brand: TextView = itemView.findViewById(R.id.brand)
        val modelName: TextView = itemView.findViewById(R.id.model_name)
        val bookButton: MaterialButton = itemView.findViewById(R.id.book_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_home, parent, false)
        return HomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = homeList[position]

        holder.brand.text = item.brand
        holder.modelName.text = item.modelName
        holder.priceTag.text = "Starting at ₹${item.pricePerDay}/day"
        // Load image from URL using Glide
        Glide.with(context)
            .load(item.imageUrl)
            .placeholder(R.drawable.sample_bike)
            .error(R.drawable.sample_bike)
            .into(holder.fleetImage)
        // Click listeners
        holder.bookButton.setOnClickListener { onBookClick(item) }
    }

    override fun getItemCount(): Int = homeList.size
}
