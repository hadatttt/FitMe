package com.pbl6.fitme.profile

import android.view.*
import android.widget.*
import com.pbl6.fitme.R
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(private val items: List<String>) :
    RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgItem: ImageView = view.findViewById(R.id.imgItem)
        val txtName: TextView = view.findViewById(R.id.txtName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.txtName.text = items[position]
        // Tạm thời set ảnh mặc định, có thể dùng Glide để load ảnh từ URL
        holder.imgItem.setImageResource(R.drawable.ic_launcher_foreground)
    }

    override fun getItemCount() = items.size
}
