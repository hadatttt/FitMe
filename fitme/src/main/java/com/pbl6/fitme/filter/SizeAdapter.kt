package com.pbl6.fitme.filter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R

class SizeAdapter(
    private val sizes: List<String>,
    private val onSelectionChanged: (String?) -> Unit
) : RecyclerView.Adapter<SizeAdapter.SizeViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class SizeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sizeView: TextView = itemView.findViewById(R.id.sizeView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SizeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_size, parent, false)
        return SizeViewHolder(view)
    }

    override fun onBindViewHolder(holder: SizeViewHolder, position: Int) {
        val size = sizes[position]
        holder.sizeView.text = size

        // Đổi màu khi chọn
        if (position == selectedPosition) {
            holder.sizeView.setBackgroundResource(R.drawable.bg_bluelight_circle) // nền chọn
            holder.sizeView.setTextColor(Color.BLUE)
        } else {
            holder.sizeView.setBackgroundResource(R.drawable.bg_outer_circle)   // nền bình thường
            holder.sizeView.setTextColor(Color.BLACK)
        }

        holder.itemView.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = if (selectedPosition == position) RecyclerView.NO_POSITION else position
            notifyItemChanged(oldPosition)
            notifyItemChanged(position)

            val selectedSize = if (selectedPosition != RecyclerView.NO_POSITION) sizes[selectedPosition] else null
            onSelectionChanged(selectedSize)
        }
    }

    override fun getItemCount() = sizes.size

    fun clearSelection() {
        val oldPosition = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (oldPosition != RecyclerView.NO_POSITION) notifyItemChanged(oldPosition)
        onSelectionChanged(null)
    }

    fun getSelectedSize(): String? {
        return if (selectedPosition != RecyclerView.NO_POSITION) sizes[selectedPosition] else null
    }
}
