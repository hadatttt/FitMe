package com.pbl6.fitme.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.model.Product

class ProductAdapter(private val items: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val txtTitle: TextView = view.findViewById(R.id.tvProductName)
        // Nếu muốn hiển thị giá, cần lấy từ bảng product_variant
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtTitle.text = item.productName
        // Nếu có trường imageUrl, dùng Glide/Picasso để load ảnh
        // Glide.with(holder.imgProduct.context).load(item.imageUrl).into(holder.imgProduct)
        // Hiện tại dùng ảnh tạm
        holder.imgProduct.setImageResource(R.drawable.ic_splash)
    }

    override fun getItemCount() = items.size
}
