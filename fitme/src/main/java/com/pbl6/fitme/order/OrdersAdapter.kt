package com.pbl6.fitme.order

import com.bumptech.glide.Glide
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.ItemOrderBinding
import com.pbl6.fitme.model.Order
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.singleClick
import androidx.fragment.app.FragmentActivity

class OrdersAdapter : BaseRecyclerViewAdapter<Order, ItemOrderBinding>() {
    override fun bindData(binding: ItemOrderBinding, item: Order, position: Int) {
        binding.txtShop.text = "Shop"
        binding.txtProductName.text = "Day: ${item.createdAt}"
        binding.txtPrice.text = "Total Price: ${item.totalAmount}$"
        binding.txtStatus.text = item.orderStatus
        // placeholder image
        binding.imgProduct.setImageResource(R.drawable.ic_splash)
        binding.btnDetail.singleClick { /* TODO: navigate to order detail */ }
        // Set state button label based on order status
        val status = item.orderStatus.lowercase()
        val stateLabel = when (status) {
            "confirming", "packing" -> "Cancel"
            "delivering" -> "Received"
            "received" -> "Review"
            else -> "State"
        }
        binding.btnState.text = stateLabel
        binding.btnState.singleClick {
            try {
                if (stateLabel == "Review") {
                    val ctx = binding.root.context
                    val activity = ctx as? FragmentActivity
                    activity?.let {
                        val sheet = ReviewBottomSheetFragment()
                        val args = android.os.Bundle()
                        args.putString("order_id", item.orderId.toString())
                        sheet.arguments = args
                        sheet.show(it.supportFragmentManager, "ReviewBottomSheet")
                        return@singleClick
                    }
                }
                // default / other actions can be implemented here
            } catch (e: Exception) {
                // swallow any cast/show errors
            }
        }
    }
}

