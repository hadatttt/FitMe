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

class WishlistProductAdapter(private val items: List<WishlistProduct>) :
    RecyclerView.Adapter<WishlistProductAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemove)
        val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        val txtPrice: TextView = view.findViewById(R.id.txtPrice)
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

        // Sự kiện nút xóa
        holder.btnRemove.singleClick {
            // TODO: xử lý xóa sản phẩm khỏi wishlist
        }

        // Sự kiện nút thêm vào giỏ
        holder.btnAddToCart.singleClick {
            // TODO: xử lý thêm sản phẩm vào giỏ hàng
        }
    }

    override fun getItemCount() = items.size
}
