package com.pbl6.fitme.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pbl6.fitme.R
import com.pbl6.fitme.model.CartItem
import hoang.dqm.codebase.utils.singleClick
import java.util.UUID

class CartProductAdapter(
    private val items: MutableList<CartItem>,
    private val variantMap: Map<UUID, com.pbl6.fitme.model.ProductVariant>,
    private val productMap: Map<UUID, com.pbl6.fitme.model.Product>,
    private val selectedVariantIds: Set<UUID>, // Thêm biến này để kiểm tra trạng thái chọn
    private val listener: OnCartActionListener
) : RecyclerView.Adapter<CartProductAdapter.VH>() {

    interface OnCartActionListener {
        fun onRemove(position: Int)
        fun onIncrease(position: Int)
        fun onDecrease(position: Int)
        // Thêm hàm lắng nghe sự kiện check
        fun onSelectionChanged(position: Int, isSelected: Boolean)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val chkSelect: CheckBox = view.findViewById(R.id.chkSelect_cart) // Checkbox mới
        val txtTitle: TextView = view.findViewById(R.id.txtTitle_cart)
        val txtDetail: TextView = view.findViewById(R.id.txtDetail_cart)
        val txtPrice: TextView = view.findViewById(R.id.txtPrice_cart)
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct_cart)
        val txtQuantity: TextView = view.findViewById(R.id.txtQty)
        val btnIncrease: View = view.findViewById(R.id.btnPlus)
        val btnDecrease: View = view.findViewById(R.id.btnMinus)
        val btnRemove: View = view.findViewById(R.id.btnRemoveCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_cart, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cartItem = items[position]
        val variant = variantMap[cartItem.variantId]
        val product = variant?.let { productMap[it.productId] }

        holder.txtTitle.text = product?.productName ?: "Unknown"
        holder.txtDetail.text = variant?.let { "${it.color} - Size: ${it.size}" } ?: ""
        holder.txtPrice.text = variant?.price?.let { "\$${String.format("%.2f", it)}" } ?: ""
        holder.txtQuantity.text = cartItem.quantity.toString()

        val imageUrl = product?.images?.firstOrNull()?.imageUrl
        Glide.with(holder.imgProduct.context)
            .load(imageUrl ?: R.drawable.ic_splash)
            .placeholder(R.drawable.ic_splash)
            .error(R.drawable.ic_splash)
            .into(holder.imgProduct)

        // --- Xử lý CheckBox ---
        // 1. Gỡ listener cũ để tránh lỗi khi scroll RecyclerView
        holder.chkSelect.setOnCheckedChangeListener(null)

        // 2. Set trạng thái hiện tại dựa trên danh sách ID đã chọn
        holder.chkSelect.isChecked = selectedVariantIds.contains(cartItem.variantId)

        // 3. Gán listener mới
        holder.chkSelect.setOnCheckedChangeListener { _, isChecked ->
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                listener.onSelectionChanged(pos, isChecked)
            }
        }
        // ----------------------

        holder.btnIncrease.singleClick {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) listener.onIncrease(pos)
        }

        holder.btnDecrease.singleClick {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) listener.onDecrease(pos)
        }

        holder.btnRemove.singleClick {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) listener.onRemove(pos)
        }
    }

    override fun getItemCount(): Int = items.size
}