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
            return
        }

        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (token.isNullOrBlank()) {
            return
        }

        loadOrderDetails(token, orderId)
    }

    private fun loadOrderDetails(token: String, orderId: String) {
        mainRepository.getOrderById(token, orderId) { order ->
            activity?.runOnUiThread {
                if (order == null) {
                    android.util.Log.e("OrderDetailFragment", "getOrderById returned null for orderId=$orderId")
                } else {
                    android.util.Log.d("OrderDetailFragment", "Order loaded: $order")
                    android.util.Log.d("OrderDetailFragment", "Payment fields: method=${order.paymentMethod} status=${order.paymentStatus} amount=${order.paymentAmount}")
                    bindOrder(order)
                }
            }
        }
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
        binding.txtUserEmail.text = "User: ${order.userEmail ?: "N/A"}"
        binding.txtCreatedAt.text = "Created: ${order.createdAt ?: "N/A"}"
        binding.txtUpdatedAt.text = "Updated: ${order.updatedAt ?: "N/A"}"

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
        // If backend provided a formatted shippingAddressDetails, show it too
        try {
            binding.txtShippingAddressDetails.text = order.shippingAddressDetails ?: ""
            binding.txtShippingAddressDetails.visibility = if ((order.shippingAddressDetails ?: "").isBlank()) View.GONE else View.VISIBLE
        } catch (_: Exception) { }

        // Order Items - prefer server `orderItems` if present, otherwise support older `items` key
        val items = when {
            !order.orderItems.isNullOrEmpty() -> order.orderItems
            !order.items.isNullOrEmpty() -> order.items
            else -> emptyList()
        }
        val adapter = OrderItemsAdapter(items)
        binding.recyclerOrderItems.adapter = adapter

        // Payment Summary
        val subtotal = order.subtotal ?: 0.0
        val shippingFee = order.shippingFee ?: 0.0
        val total = order.totalAmount ?: (subtotal + shippingFee)

        binding.txtSubtotal.text = "\$${String.format("%.2f", subtotal)}"
        binding.txtShippingFee.text = "\$${String.format("%.2f", shippingFee)}"
        binding.txtTotal.text = "\$${String.format("%.2f", total)}"
        binding.txtDiscount.text = "\$${String.format("%.2f", order.discountAmount ?: 0.0)}"
        binding.txtCouponCode.text = "Coupon: ${order.couponId ?: "-"}"
        binding.txtOrderNotes.text = "Notes: ${order.orderNotes ?: "-"}"
    // Show payment method if provided by backend
    val rawMethod = when {
        !order.paymentMethod.isNullOrBlank() -> order.paymentMethod
        !order.paymentStatus.isNullOrBlank() -> order.paymentStatus
        else -> null
    }
    val paymentMethodText = when (rawMethod?.uppercase()) {
        "COD", "CASHONDELIVERY", "CASH_ON_DELIVERY" -> "Cash on Delivery"
        "VNPAY", "VN_PAY" -> "VNPay"
        "MOMO" -> "Momo"
        "PENDING" -> "Pending"
        "COMPLETED" -> "Completed"
        "FAILED" -> "Failed"
        null -> "N/A"
        else -> rawMethod
    }

    binding.txtPaymentMethod.text = "Payment Method: $paymentMethodText"
    binding.txtPaymentMethod.visibility = View.VISIBLE

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
