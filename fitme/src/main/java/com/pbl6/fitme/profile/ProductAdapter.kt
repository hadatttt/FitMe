package com.pbl6.fitme.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // <-- Import Glide
import com.pbl6.fitme.R
import com.pbl6.fitme.model.Product

class ProductAdapter(private val items: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val txtTitle: TextView = view.findViewById(R.id.tvProductName)
        // TODO: Thêm các view khác nếu cần, ví dụ như giá tiền
        // val txtPrice: TextView = view.findViewById(R.id.tvProductPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtTitle.text = item.productName

        // Use mainImageUrl convenience property
        val imageUrl = item.mainImageUrl
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(holder.imgProduct.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_splash)
                .error(R.drawable.ic_splash)
                .into(holder.imgProduct)
        } else {
            holder.imgProduct.setImageResource(R.drawable.ic_splash)
        }

        // TODO: Hiển thị giá tiền của biến thể đầu tiên (nếu có)
        // if (item.variants.isNotEmpty()) {
        //     holder.txtPrice.text = "${item.variants[0].price} đ"
        // }
    }

    override fun getItemCount() = items.size
}