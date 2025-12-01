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
    private var variantMap: Map<java.util.UUID, com.pbl6.fitme.model.ProductVariant>,
    private var productMap: Map<java.util.UUID, com.pbl6.fitme.model.Product>
) : ListAdapter<CartItem, CheckoutProductAdapter.VH>(DiffCallback()) {

    fun updateData(
        newVariantMap: Map<java.util.UUID, com.pbl6.fitme.model.ProductVariant>,
        newProductMap: Map<java.util.UUID, com.pbl6.fitme.model.Product>
    ) {
        variantMap = newVariantMap
        productMap = newProductMap
        // data for items may have changed; ask the adapter to rebind visible views
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val txtProductName: TextView = view.findViewById(R.id.tvProductName)
        val txtVariant: TextView = view.findViewById(R.id.tvVariant)
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
        val product = variant?.let { productMap[it.productId] }

        // Title: prefer product name; show variant details in separate field
        holder.txtProductName.text = product?.productName ?: (variant?.let { "${it.color} - ${it.size}" } ?: "Unknown")
        holder.txtVariant.text = variant?.let { "${it.color} - ${it.size}" } ?: ""

        holder.txtPrice.text = variant?.price?.let { "\$${String.format("%.2f", it)}" } ?: ""
        holder.txtQuantity.text = "x${cartItem.quantity}"

        val imageUrl = product?.mainImageUrl
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(holder.imgProduct.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_splash)
                .error(R.drawable.ic_splash)
                .into(holder.imgProduct)
        } else {
            holder.imgProduct.setImageResource(R.drawable.ic_splash)
        }
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
