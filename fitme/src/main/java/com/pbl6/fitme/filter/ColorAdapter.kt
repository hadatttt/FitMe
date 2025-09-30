package com.pbl6.fitme.filter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.databinding.ItemColorBinding

class ColorAdapter(private val items: List<String>) :
    RecyclerView.Adapter<ColorAdapter.ViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class ViewHolder(val binding: ItemColorBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemColorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val colorCode = items[position]

        // card ngoài
        val card = holder.binding.cardColor
        // view màu bên trong
        val colorView = holder.binding.colorView

        colorView.setBackgroundColor(Color.parseColor(colorCode))

        // hiệu ứng chọn
        if (position == selectedPosition) {
            card.strokeWidth = 10
            card.strokeColor = Color.BLACK
        } else {
            card.strokeWidth = 0
        }

        holder.itemView.setOnClickListener {
            val oldPos = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)
        }
    }

    override fun getItemCount(): Int = items.size

    fun getSelectedColor(): String? {
        return if (selectedPosition != RecyclerView.NO_POSITION) items[selectedPosition] else null
    }

    fun clearSelection() {
        val oldPos = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (oldPos != RecyclerView.NO_POSITION) notifyItemChanged(oldPos)
    }
}
