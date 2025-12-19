package com.eazymile.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eazymile.app.databinding.ItemProfileMenuBinding
import com.eazymile.app.models.ProfileMenuItem


class ProfileMenuAdapter(
    private val menuItems: List<ProfileMenuItem>,
    private val onItemClick: (ProfileMenuItem) -> Unit
) : RecyclerView.Adapter<ProfileMenuAdapter.ProfileMenuViewHolder>() {

    inner class ProfileMenuViewHolder(private val binding: ItemProfileMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProfileMenuItem) {
            binding.tvMenuTitle.text = item.title
            binding.ivMenuIcon.setImageResource(item.iconRes)
            // No click actions (purely display)
            binding.root.isClickable = false
            binding.root.isFocusable = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileMenuViewHolder {
        val binding = ItemProfileMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProfileMenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileMenuViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    override fun getItemCount(): Int = menuItems.size
}
