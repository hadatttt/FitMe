package com.pbl6.fitme.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
data class CartProduct(
    val title: String,
    val detail: String,
    val price: Double,
    val imageResId: Int,
    var quantity: Int
)
class CartProductAdapter(
    private val items: MutableList<CartProduct>,
    private val listener: OnCartActionListener
) : RecyclerView.Adapter<CartProductAdapter.ViewHolder>() {

    interface OnCartActionListener {
        fun onRemove(position: Int)
        fun onIncrease(position: Int)
        fun onDecrease(position: Int)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemoveCart)
        val btnIncrease: ImageView = view.findViewById(R.id.btnPlus)
        val btnDecrease: ImageView = view.findViewById(R.id.btnMinus)
        val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        val txtDetail: TextView = view.findViewById(R.id.txtDetail)
        val txtPrice: TextView = view.findViewById(R.id.txtPrice)
        val txtQuantity: TextView = view.findViewById(R.id.txtQty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtTitle.text = item.title
        holder.txtDetail.text = item.detail
        holder.txtPrice.text = "$${item.price}"
        holder.txtQuantity.text = item.quantity.toString()
        holder.imgProduct.setImageResource(item.imageResId)

        holder.btnRemove.setOnClickListener { listener.onRemove(position) }
        holder.btnIncrease.setOnClickListener { listener.onIncrease(position) }
        holder.btnDecrease.setOnClickListener { listener.onDecrease(position) }
    }

    override fun getItemCount() = items.size
}
