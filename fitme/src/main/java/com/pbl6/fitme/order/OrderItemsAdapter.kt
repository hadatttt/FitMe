package com.pbl6.fitme.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.ItemOrderItemBinding
import com.pbl6.fitme.model.OrderItem

class OrderItemsAdapter(private val items: List<OrderItem>) : RecyclerView.Adapter<OrderItemsAdapter.VH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemOrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.txtName.text = item.productName ?: "Product"
        holder.binding.txtQty.text = "Qty: ${item.quantity}"
        holder.binding.txtPrice.text = "$${String.format("%.2f", item.unitPrice ?: 0.0)}"
        val img = item.productImageUrl
        if (!img.isNullOrBlank()) {
            try {
                Glide.with(holder.binding.root.context).load(img).centerCrop().placeholder(R.drawable.ic_splash).into(holder.binding.img)
            } catch (_: Exception) { holder.binding.img.setImageResource(R.drawable.ic_splash) }
        } else holder.binding.img.setImageResource(R.drawable.ic_splash)
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemOrderItemBinding) : RecyclerView.ViewHolder(binding.root)
}
