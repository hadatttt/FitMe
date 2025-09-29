package com.pbl6.fitme.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R

data class Product(
    val title: String,
    val price: Double,
    val imageRes: Int
)

class ProductAdapter(private val items: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        val txtPrice: TextView = view.findViewById(R.id.txtPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtTitle.text = item.title
        holder.txtPrice.text = item.price.toString()
        holder.imgProduct.setImageResource(R.drawable.ic_splash) // ảnh tạm
    }

    override fun getItemCount() = items.size
}
