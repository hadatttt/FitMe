package com.pbl6.fitme.order

import com.bumptech.glide.Glide
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.ItemOrderBinding
import com.pbl6.fitme.model.Order
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.singleClick
import androidx.fragment.app.FragmentActivity
import com.pbl6.fitme.model.OrderStatus
import com.pbl6.fitme.repository.MainRepository
import hoang.dqm.codebase.base.activity.navigate

class OrdersAdapter : BaseRecyclerViewAdapter<Order, ItemOrderBinding>() {
    private val mainRepository = MainRepository()
    
    interface OrderStatusChangeListener {
        fun onOrderStatusChanged(orderId: String, newStatus: String)
    }
    
    private var statusChangeListener: OrderStatusChangeListener? = null
    
    fun setOrderStatusChangeListener(listener: OrderStatusChangeListener) {
        statusChangeListener = listener
    }
    
    private fun updateOrderStatus(order: Order, newStatus: String, context: android.content.Context) {
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(context)
        if (token == null) {
            android.widget.Toast.makeText(context, "Please login to update order status", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        android.util.Log.d("OrdersAdapter", "Updating order ${order.orderId} status to $newStatus")
        
        mainRepository.updateOrderStatus(token, order.orderId ?: "", OrderStatus.valueOf(newStatus.uppercase())) { success ->
            (context as? androidx.fragment.app.FragmentActivity)?.runOnUiThread {
                if (success) {
                    android.widget.Toast.makeText(context, "Order status updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                    // Notify the fragment with the specific order change
                    statusChangeListener?.onOrderStatusChanged(order.orderId ?: "", newStatus)
                } else {
                    android.widget.Toast.makeText(context, "Failed to update order status", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun bindData(binding: ItemOrderBinding, item: Order, position: Int) {
        android.util.Log.d("OrdersAdapter", "bindData position=$position orderId=${item.orderId} status=${item.status ?: item.orderStatus}")
    binding.txtShop.text = "Shop"
            // Date: prefer orderDate / createdAt
            val displayDate = item.orderDate ?: item.createdAt ?: ""
            binding.txtProductName.text = "Date: $displayDate"

            // Total: safely format
            val total = try {
                String.format("%.2f", item.totalAmount)
            } catch (e: Exception) {
                (item.totalAmount ?: 0.0).toString()
            }
            binding.txtPrice.text = "Total Price: \$${total}"

            binding.txtStatus.text = item.status ?: item.orderStatus ?: ""

            // Try to load first product image from order items
            val imgUrl = item.items.firstOrNull()?.productImageUrl
                ?: item.orderItems.firstOrNull()?.productImageUrl
            if (!imgUrl.isNullOrBlank()) {
                try {
                    Glide.with(binding.root.context)
                        .load(imgUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_splash)
                        .into(binding.imgProduct)
                } catch (_: Exception) {
                    binding.imgProduct.setImageResource(R.drawable.ic_splash)
                }
            } else {
                binding.imgProduct.setImageResource(R.drawable.ic_splash)
            }

            binding.btnDetail.singleClick {
                try {
                    val activity = binding.root.context as? FragmentActivity
                    activity?.let {
                        val fragment = it.supportFragmentManager.findFragmentById(R.id.navHostFragment)
                        fragment?.let { navFragment ->
                            val bundle = android.os.Bundle().apply {
                                putString("order_id", item.orderId)
                            }
                            navFragment.navigate(R.id.orderDetailFragment, bundle)
                            android.util.Log.d("OrdersAdapter", "Navigating to order detail for orderId=${item.orderId}")
                            return@singleClick
                        }
                        android.util.Log.e("OrdersAdapter", "Nav host fragment not found")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OrdersAdapter", "Navigation error", e)
                }
            }
    // Set state button label based on order status
    val status = (item.status ?: item.orderStatus ?: "").lowercase()
        val stateLabel = when (status) {
            "pending", "confirmed", "processing" -> "Cancel"
            "shipped" -> "Received"
            "cancelled" -> "Reorder"
            "delivered" -> "Review"
            else -> "State"
        }
        binding.btnState.text = stateLabel
        binding.btnState.singleClick {
            try {
                val ctx = binding.root.context
                when (stateLabel) {
                    "Cancel" -> {
                        // Update order status to CANCELLED
                        val activity = ctx as? FragmentActivity
                        activity?.let {
                            // Show confirmation dialog
                            android.app.AlertDialog.Builder(activity)
                                .setTitle("Cancel Order")
                                .setMessage("Are you sure you want to cancel this order?")
                                .setPositiveButton("Yes") { _, _ ->
                                    updateOrderStatus(item, "CANCELLED", ctx)
                                }
                                .setNegativeButton("No", null)
                                .show()
                        }

                    }
                    "Received" -> {
                        updateOrderStatus(item, "DELIVERED", ctx)
                    }
                    "Review" -> {
                        val activity = ctx as? FragmentActivity
                        activity?.let {
                            val sheet = ReviewBottomSheetFragment()
                            val args = android.os.Bundle()
                            args.putString("order_id", item.orderId ?: "")
                            sheet.arguments = args
                            sheet.show(it.supportFragmentManager, "ReviewBottomSheet")
                            return@singleClick
                        }
                    }
                    "Reorder" -> {
                        val activity = ctx as? FragmentActivity
                        activity?.let {
                            // Create a new order with the same items
                            val fragment = it.supportFragmentManager.findFragmentById(R.id.navHostFragment)
                            fragment?.let { navFragment ->
                                val bundle = android.os.Bundle().apply {
                                    putString("reorder_id", item.orderId)
                                }
                                navFragment.navigate(R.id.checkoutFragment, bundle)
                            }
                        }
                    }
                }
                // default / other actions can be implemented here
            } catch (e: Exception) {
                // swallow any cast/show errors
            }
        }
    }
}

