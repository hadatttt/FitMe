package com.pbl6.fitme.checkout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.cart.CartProduct

class CheckoutProductAdapter :
    ListAdapter<CartProduct, CheckoutProductAdapter.VH>(DiffCallback()) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val txtProductName: TextView = view.findViewById(R.id.tvProductName)
        val txtPrice: TextView = view.findViewById(R.id.tvPrice)
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val txtQuantity: TextView = view.findViewById(R.id.tvQuantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_checkout, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val product = getItem(position)
        holder.txtProductName.text = "${product.title} - ${product.detail}"
        holder.txtPrice.text = "$${String.format("%.2f", product.price)}"
        holder.txtQuantity.text = "x${product.quantity}"
        holder.imgProduct.setImageResource(product.imageResId)
    }

    class DiffCallback : DiffUtil.ItemCallback<CartProduct>() {
        override fun areItemsTheSame(oldItem: CartProduct, newItem: CartProduct): Boolean {
            return oldItem.title == newItem.title && oldItem.detail == newItem.detail
        }

        override fun areContentsTheSame(oldItem: CartProduct, newItem: CartProduct): Boolean {
            return oldItem == newItem
        }
    }
}
