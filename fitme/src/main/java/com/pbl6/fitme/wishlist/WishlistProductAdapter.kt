package com.pbl6.fitme.wishlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import hoang.dqm.codebase.utils.singleClick
import com.pbl6.fitme.model.WishlistItem

import com.bumptech.glide.Glide

class WishlistProductAdapter(
    private val items: MutableList<WishlistItem>,
    private val productMap: Map<java.util.UUID, com.pbl6.fitme.model.Product>,
    private val variantMap: Map<java.util.UUID, com.pbl6.fitme.model.ProductVariant>,
    private val listener: OnWishlistActionListener
) : RecyclerView.Adapter<WishlistProductAdapter.ViewHolder>() {

    interface OnWishlistActionListener {
        fun onRemove(position: Int)
        fun onAddToCart(position: Int)
    }
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct_wl)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemove)
        val txtTitle: TextView = view.findViewById(R.id.txtTitle_wl)
        val txtPrice: TextView = view.findViewById(R.id.txtPrice_wl)
        val btnColor: TextView = view.findViewById(R.id.btnColor)
        val btnSize: TextView = view.findViewById(R.id.btnSize)
        val btnAddToCart: ImageView = view.findViewById(R.id.btnAddToCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_wishlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val product = productMap[item.productId]
        // Nếu có variantId, bạn có thể truyền thêm vào WishlistItem hoặc map theo productId
        val variant = variantMap.values.find { it.variantId == item.productId }

        holder.txtTitle.text = product?.productName ?: "Unknown"
        holder.txtPrice.text = variant?.price?.let { "$${String.format("%.2f", it)}" } ?: ""
        holder.btnColor.text = variant?.color ?: ""
        holder.btnSize.text = variant?.size ?: ""
        Glide.with(holder.imgProduct.context)
            .load(R.drawable.ic_splash) // Nếu có imageUrl thì thay bằng product.imageUrl
            .placeholder(R.drawable.ic_splash)
            .into(holder.imgProduct)

        holder.btnRemove.singleClick {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) listener.onRemove(pos)
        }

        holder.btnAddToCart.singleClick {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) listener.onAddToCart(pos)
        }
    }

    override fun getItemCount() = items.size
}
