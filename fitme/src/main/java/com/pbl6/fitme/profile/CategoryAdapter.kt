package com.pbl6.fitme.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.model.Category

class CategoryAdapter(private var categories: List<Category>) : // Thay đổi: val -> var
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.imgItem)
        val name: TextView = itemView.findViewById(R.id.txtName)
        val container: FrameLayout = itemView.findViewById(R.id.container)

        init {
            itemView.setOnClickListener {
                val prevPos = selectedPosition
                selectedPosition = bindingAdapterPosition
                notifyItemChanged(prevPos)
                notifyItemChanged(selectedPosition)
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
        // Nếu có trường imageUrl, dùng Glide/Picasso để load ảnh
        // Glide.with(holder.img.context).load(category.imageUrl).into(holder.img)
        // Hiện tại dùng ảnh tạm
        holder.img.setImageResource(R.drawable.ic_splash)

        if (position == selectedPosition) {
            holder.container.setBackgroundResource(R.drawable.bg_bluelight_circle)
        } else {
            holder.container.setBackgroundResource(R.drawable.bg_outer_circle)
        }
    }

    override fun getItemCount(): Int = categories.size

    fun getSelectedCategory(): String? {
        return if (selectedPosition != RecyclerView.NO_POSITION) categories[selectedPosition].categoryName else null
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