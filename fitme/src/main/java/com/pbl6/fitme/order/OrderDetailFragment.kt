package com.pbl6.fitme.order

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentOrderDetailBinding
import com.pbl6.fitme.model.Order
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import java.text.SimpleDateFormat
import java.util.Locale

class OrderDetailFragment : BaseFragment<FragmentOrderDetailBinding, OrderDetailViewModel>() {
    private val mainRepository = MainRepository()
    private val displayFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    override fun initView() {
        // Hide main toolbar
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE

        // Setup RecyclerView
        binding.recyclerOrderItems.layoutManager = LinearLayoutManager(requireContext())

        // Setup back button
        binding.ivBack.setOnClickListener {
            popBackStack()
        }
    }

    override fun initListener() {
        // Add any additional listeners here
    }

    override fun initData() {
        val orderId = arguments?.getString("order_id")
        if (orderId.isNullOrBlank()) {
            showError("Order not found")
            return
        }

        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (token.isNullOrBlank()) {
            showError("Please login to view order details")
            return
        }

        loadOrderDetails(token, orderId)
    }

    private fun loadOrderDetails(token: String, orderId: String) {
        mainRepository.getOrderById(token, orderId) { order ->
            activity?.runOnUiThread {
                if (order == null) {
                    showError("Failed to load order")
                } else {
                    bindOrder(order)
                }
            }
        }
    }

    private fun showError(message: String) {
        // You might want to add a proper error view in your layout
        binding.tvTitle.text = message
    }

    private fun bindOrder(order: Order) {
        // Order Info Card
        binding.txtOrderId.text = "Order ID: #${order.orderId}"
        // Parse and format the date string
        val dateStr = order.createdAt ?: order.orderDate
        val displayDate = try {
            if (dateStr != null) {
                val date = apiDateFormat.parse(dateStr)
                displayFormatter.format(date)
            } else "N/A"
        } catch (e: Exception) {
            android.util.Log.e("OrderDetailFragment", "Date parsing error", e)
            dateStr ?: "N/A"
        }
        binding.txtOrderDate.text = "Date: $displayDate"
        binding.txtStatus.text = "Status: ${order.status ?: order.orderStatus ?: "N/A"}"

        // Delivery Info Card — use nested ShippingAddress from model
        val ship = order.shippingAddress
        binding.txtCustomerName.text = ship.recipientName.ifBlank { "N/A" }
        binding.txtPhoneNumber.text = ship.phone.ifBlank { "N/A" }
        // Compose a readable address from available fields
        val addrParts = listOfNotNull(
            ship.addressLine1.takeIf { it.isNotBlank() },
            ship.addressLine2.takeIf { it.isNotBlank() },
            ship.city.takeIf { it.isNotBlank() },
            ship.stateProvince.takeIf { it.isNotBlank() },
            ship.postalCode.takeIf { it.isNotBlank() },
            ship.country.takeIf { it.isNotBlank() }
        )
        binding.txtAddress.text = if (addrParts.isEmpty()) "N/A" else addrParts.joinToString(", ")

        // Order Items
        val items = (order.items ?: order.orderItems ?: emptyList())
        val adapter = OrderItemsAdapter(items)
        binding.recyclerOrderItems.adapter = adapter

        // Payment Summary
        val subtotal = order.subtotal ?: 0.0
        val shippingFee = order.shippingFee ?: 0.0
        val total = order.totalAmount ?: (subtotal + shippingFee)

        binding.txtSubtotal.text = "$${String.format("%.2f", subtotal)}"
        binding.txtShippingFee.text = "$${String.format("%.2f", shippingFee)}"
        binding.txtTotal.text = "$${String.format("%.2f", total)}"
    // Order model doesn't include a paymentMethod field in this client model.
    // Show placeholder or derive from server response when available.
    binding.txtPaymentMethod.text = "Payment Method: N/A"

        // Set status color based on order status
        val statusColor = when(order.status?.uppercase() ?: order.orderStatus?.uppercase()) {
            "PENDING" -> R.color.status_pending
            "CONFIRMED" -> R.color.status_confirmed
            "PROCESSING" -> R.color.status_processing
            "SHIPPED" -> R.color.status_shipped
            "DELIVERED" -> R.color.status_delivered
            "CANCELLED" -> R.color.status_cancelled
            else -> R.color.maincolor
        }
        binding.txtStatus.setTextColor(resources.getColor(statusColor, null))
    }
}
