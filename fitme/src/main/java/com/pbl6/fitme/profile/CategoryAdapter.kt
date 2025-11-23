package com.pbl6.fitme.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pbl6.fitme.R
import com.pbl6.fitme.model.Category

class CategoryAdapter(
    private var categories: List<Category>,
    private val onSelectionChanged: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.imgItem)
        val name: TextView = itemView.findViewById(R.id.txtName)

        init {
            itemView.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener
                val prevPos = selectedPosition
                selectedPosition = currentPos
                notifyItemChanged(prevPos)
                notifyItemChanged(selectedPosition)
                onSelectionChanged(categories[selectedPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.name.text = category.categoryName

        Glide.with(holder.itemView.context)
            .load(category.imageUrl)
            .into(holder.img)
        if (position == selectedPosition) {
            holder.img.setBackgroundResource(R.drawable.bg_selected)
        } else {
            holder.img.setBackgroundResource(R.drawable.bg_outer_circle)
        }
    }

    override fun getItemCount(): Int = categories.size

    fun getSelectedCategory(): Category? {
        return if (selectedPosition != RecyclerView.NO_POSITION) categories[selectedPosition] else null
    }

    fun clearSelection() {
        val oldPos = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (oldPos != RecyclerView.NO_POSITION) notifyItemChanged(oldPos)
    }

    fun updateData(newCategories: List<Category>) {
        this.categories = newCategories
        clearSelection()
        notifyDataSetChanged()
    }
}