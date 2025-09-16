package com.pbl6.fitme.wishlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.untils.singleClick

data class WishlistProduct(
    val title: String,
    val price: String,
    val color: String,
    val size: String
)

class WishlistProductAdapter(
    private val items: MutableList<WishlistProduct>,
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
        holder.txtTitle.text = item.title
        holder.txtPrice.text = item.price
        holder.btnColor.text = item.color
        holder.btnSize.text = item.size
        holder.imgProduct.setImageResource(R.drawable.ic_splash)

        holder.btnRemove.singleClick {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) listener.onRemove(pos)
        }

        holder.btnAddToCart.singleClick {
            // TODO: xử lý thêm sản phẩm vào giỏ hàng
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) listener.onAddToCart(pos)
        }
    }

    override fun getItemCount() = items.size
}
