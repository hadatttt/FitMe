package com.pbl6.fitme.order

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.ItemOrderBinding
import com.pbl6.fitme.model.Order
import com.pbl6.fitme.model.OrderStatus
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.singleClick

class OrdersAdapter : BaseRecyclerViewAdapter<Order, ItemOrderBinding>() {
    private val mainRepository = MainRepository()

    interface OrderStatusChangeListener {
        fun onOrderStatusChanged(orderId: String, newStatus: String)
    }

    private var statusChangeListener: OrderStatusChangeListener? = null

    fun setOrderStatusChangeListener(listener: OrderStatusChangeListener) {
        statusChangeListener = listener
    }

    // Function for general status updates (e.g., Receiving an order)
    private fun updateOrderStatus(order: Order, newStatus: String, context: Context) {
        val token = SessionManager.getInstance().getAccessToken(context)
        if (token == null) {
            Toast.makeText(context, "Please login to update order status", Toast.LENGTH_SHORT).show()
            return
        }

        mainRepository.updateOrderStatus(token, order.orderId ?: "", OrderStatus.valueOf(newStatus.uppercase())) { success ->
            (context as? FragmentActivity)?.runOnUiThread {
                if (success) {
                    Toast.makeText(context, "Order status updated successfully", Toast.LENGTH_SHORT).show()
                    statusChangeListener?.onOrderStatusChanged(order.orderId ?: "", newStatus)
                } else {
                    Toast.makeText(context, "Failed to update order status", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Dedicated function for Canceling orders (Calls the /cancel endpoint)
    private fun cancelOrder(order: Order, context: Context) {
        val token = SessionManager.getInstance().getAccessToken(context)
        if (token == null) {
            Toast.makeText(context, "Please login to cancel order", Toast.LENGTH_SHORT).show()
            return
        }

        // Assuming MainRepository has the cancelOrder function we added previously
        mainRepository.cancelOrder(token, order.orderId ?: "") { success ->
            (context as? FragmentActivity)?.runOnUiThread {
                if (success) {
                    Toast.makeText(context, "Order cancelled successfully", Toast.LENGTH_SHORT).show()
                    statusChangeListener?.onOrderStatusChanged(order.orderId ?: "", "CANCELLED")
                } else {
                    Toast.makeText(context, "Failed to cancel order", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun bindData(binding: ItemOrderBinding, item: Order, position: Int) {
        val context = binding.root.context

        binding.txtShop.text = "Shop"

        // Date display
        val displayDate = item.orderDate ?: item.createdAt ?: ""
        binding.txtProductName.text = "Date: $displayDate"

        // Total Price display
        val total = try {
            String.format("%.2f", item.totalAmount)
        } catch (e: Exception) {
            (item.totalAmount ?: 0.0).toString()
        }
        binding.txtPrice.text = "Total Price: \$$total"

        // Status Text
        binding.txtStatus.text = item.status ?: item.orderStatus ?: ""

        // Image Loading
        val imgUrl = item.items.firstOrNull()?.productImageUrl ?: item.orderItems.firstOrNull()?.productImageUrl

        if (!imgUrl.isNullOrBlank()) {
            try {
                Glide.with(context)
                    .load(imgUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_splash)
                    .error(R.drawable.ic_splash)
                    .into(binding.imgProduct)
            } catch (_: Exception) {
                binding.imgProduct.setImageResource(R.drawable.ic_splash)
            }
        } else {
            binding.imgProduct.setImageResource(R.drawable.ic_splash)
        }

        // Detail Button Click
        binding.btnDetail.singleClick {
            try {
                val activity = context as? FragmentActivity
                activity?.let {
                    val fragment = it.supportFragmentManager.findFragmentById(R.id.navHostFragment)
                    fragment?.let { navFragment ->
                        val bundle = Bundle().apply {
                            putString("order_id", item.orderId)
                        }
                        navFragment.navigate(R.id.orderDetailFragment, bundle)
                        return@singleClick
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // --- BUTTON STATE LOGIC ---

        // 1. Normalize status
        val status = (item.status ?: item.orderStatus ?: "").lowercase()

        // 2. Determine Button Label
        val stateLabel = when (status) {
            "pending", "confirmed" -> "Cancel"
            "shipped" -> "Received"
            "cancelled" -> "Reorder"
            "delivered" -> "Review"
            else -> null // Hide button for other states (e.g., Processing)
        }

        // 3. Handle Visibility and Click Actions
        if (stateLabel != null) {
            binding.btnState.visibility = View.VISIBLE
            binding.btnState.text = stateLabel

            binding.btnState.singleClick {
                when (stateLabel) {
                    "Cancel" -> {
                        // Show confirmation dialog before canceling
                        val activity = context as? FragmentActivity
                        activity?.let {
                            AlertDialog.Builder(activity)
                                .setTitle("Cancel Order")
                                .setMessage("Are you sure you want to cancel this order?")
                                .setPositiveButton("Yes") { _, _ ->
                                    // Use the dedicated cancel function
                                    cancelOrder(item, context)
                                }
                                .setNegativeButton("No", null)
                                .show()
                        }
                    }
                    "Received" -> {
                        // Confirm delivery
                        AlertDialog.Builder(context)
                            .setTitle("Confirm Receipt")
                            .setMessage("Have you received this order?")
                            .setPositiveButton("Yes") { _, _ ->
                                updateOrderStatus(item, "DELIVERED", context)
                            }
                            .setNegativeButton("No", null)
                            .show()
                    }
                    "Review" -> {
                        val activity = context as? FragmentActivity
                        val orderItem = item.orderItems.firstOrNull() ?: item.items.firstOrNull()

                        if (activity != null && orderItem != null) {
                            val bundle = Bundle().apply {
                                putString("variant_id", orderItem.variantId)
                                putString("product_name", orderItem.productName)
                                putString("product_image_url", orderItem.productImageUrl)
                            }
                            val navHost = activity.supportFragmentManager.findFragmentById(R.id.navHostFragment)
                            navHost?.navigate(R.id.reviewFragment, bundle)
                        }
                    }
                    "Reorder" -> {
                        val activity = context as? FragmentActivity
                        activity?.let {
                            val navFragment = it.supportFragmentManager.findFragmentById(R.id.navHostFragment)
                            val bundle = Bundle().apply {
                                putString("reorder_id", item.orderId)
                            }
                            navFragment?.navigate(R.id.checkoutFragment, bundle)
                        }
                    }
                }
            }
        } else {
            // Hide button if no action is available
            binding.btnState.visibility = View.GONE
        }
    }
}