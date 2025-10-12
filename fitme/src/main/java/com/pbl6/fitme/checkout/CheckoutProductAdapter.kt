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
import com.pbl6.fitme.model.CartItem
import com.bumptech.glide.Glide

class CheckoutProductAdapter(
    private val variantMap: Map<java.util.UUID, com.pbl6.fitme.model.ProductVariant>,
    private val productMap: Map<java.util.UUID, com.pbl6.fitme.model.Product>
) : ListAdapter<CartItem, CheckoutProductAdapter.VH>(DiffCallback()) {

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
        val cartItem = getItem(position)
        val variant = variantMap[cartItem.variantId]
        val product = variant?.let { productMap[it.variantId] }

        holder.txtProductName.text = product?.productName ?: "Unknown"
        holder.txtPrice.text = variant?.price?.let { "$${String.format("%.2f", it)}" } ?: ""
        holder.txtQuantity.text = "x${cartItem.quantity}"
        Glide.with(holder.imgProduct.context)
            .load(R.drawable.ic_splash) // Nếu có imageUrl thì thay bằng product.imageUrl
            .placeholder(R.drawable.ic_splash)
            .into(holder.imgProduct)
    }

    class DiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.cartItemId == newItem.cartItemId
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}
