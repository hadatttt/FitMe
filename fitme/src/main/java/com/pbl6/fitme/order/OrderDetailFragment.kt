package com.pbl6.fitme.order

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentOrderDetailBinding
import com.pbl6.fitme.model.Order
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.repository.UserRepository // 1. Import UserRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.text.SimpleDateFormat
import java.util.Locale

class OrderDetailFragment : BaseFragment<FragmentOrderDetailBinding, OrderDetailViewModel>() {
    private val mainRepository = MainRepository()
    private val userRepository = UserRepository() // 2. Khai báo UserRepository

    private val displayFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    override fun initView() {
        binding.recyclerOrderItems.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun initListener() {
        binding.ivBack.singleClick {
            popBackStack()
        }
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
                    bindOrder(order)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindOrder(order: Order) {
        // --- 1. Kiểm tra Context an toàn (Tránh Crash nếu thoát màn hình sớm) ---
        val context = context ?: return

        // --- Order Info ---
        binding.txtOrderId.text = "Order ID: #${order.orderId}"

        val dateStr = order.createdAt ?: order.orderDate
        val displayDate = try {
            if (dateStr != null) {
                val date = apiDateFormat.parse(dateStr)
                displayFormatter.format(date)
            } else "N/A"
        } catch (e: Exception) {
            dateStr ?: "N/A"
        }
        binding.txtOrderDate.text = "Date: $displayDate"
        binding.txtStatus.text = "Status: ${order.status ?: order.orderStatus ?: "N/A"}"

        binding.txtCreatedAt.text = "Created: ${order.createdAt ?: "N/A"}"
        binding.txtUpdatedAt.text = "Updated: ${order.updatedAt ?: "N/A"}"

        // --- User Email ---
        val sessionEmail = SessionManager.getInstance().getUserEmail(context)
        binding.txtUserEmail.text = "User: ${order.userEmail ?: sessionEmail ?: "N/A"}"

        // --- Address Info ---
        val ship = order.shippingAddress
        val addrParts = listOfNotNull(
            ship.addressLine1.takeIf { it.isNotBlank() },
            ship.addressLine2.takeIf { it.isNotBlank() },
            ship.city.takeIf { it.isNotBlank() },
            ship.stateProvince.takeIf { it.isNotBlank() },
            ship.postalCode.takeIf { it.isNotBlank() },
            ship.country.takeIf { it.isNotBlank() }
        )
        binding.txtAddress.text = "Country: " + if (addrParts.isEmpty()) "N/A" else addrParts.joinToString(", ")

        try {
            val details = order.shippingAddressDetails ?: ""
            binding.txtShippingAddressDetails.text = "Address: $details"
            binding.txtShippingAddressDetails.visibility = if (details.isBlank()) View.GONE else View.VISIBLE
        } catch (_: Exception) { }


        // --- LOGIC HIỂN THỊ TÊN/SĐT ---
        // Ưu tiên 1: Lấy từ User Profile (gọi API sau)
        // Ưu tiên 2: Lấy từ Shipping Address trong Order
        // Ưu tiên 3: Lấy từ Session (Local)

        val session = SessionManager.getInstance()
        val fallbackName = ship.recipientName.takeIf { !it.isNullOrBlank() }
            ?: session.getRecipientName(context) ?: ""

        val fallbackPhone = ship.phone.takeIf { !it.isNullOrBlank() }
            ?: session.getRecipientPhone(context) ?: ""

        // Gán giá trị ban đầu để User đỡ thấy trống
        binding.txtCustomerName.text = "Recipient Name: ${fallbackName.ifBlank { "Checking..." }}"
        binding.txtPhoneNumber.text = "Phone number: ${fallbackPhone.ifBlank { "Checking..." }}"

        // --- GỌI API LẤY USER DETAIL (Đã Fix an toàn) ---
        val token = session.getAccessToken(context)
        val userId = session.getUserId(context)

        if (!token.isNullOrBlank()) {
            userRepository.getUserDetail(token, userId.toString()) { userProfile ->
                // Kiểm tra lại context và isAdded để tránh Crash nếu user đã thoát
                activity?.runOnUiThread {
                    if (!isAdded || getContext() == null) return@runOnUiThread

                    userProfile?.let { user ->
                        // Chỉ cập nhật nếu dữ liệu API trả về hợp lệ
                        if (!user.fullName.isNullOrBlank()) {
                            binding.txtCustomerName.text = "Recipient Name: ${user.fullName}"
                        }
                        if (!user.phone.isNullOrBlank()) {
                            binding.txtPhoneNumber.text = "Phone number: ${user.phone}"
                        }
                    }
                }
            }
        }

        // --- Order Items ---
        val items = when {
            !order.orderItems.isNullOrEmpty() -> order.orderItems
            !order.items.isNullOrEmpty() -> order.items
            else -> emptyList()
        }
        val adapter = OrderItemsAdapter(items)
        binding.recyclerOrderItems.adapter = adapter

        // --- Payment Summary ---
        val subtotal = order.subtotal ?: 0.0
        val shippingFee = order.shippingFee ?: 0.0
        val total = order.totalAmount ?: (subtotal + shippingFee)

        binding.txtSubtotal.text = "\$${String.format("%.2f", subtotal)}"
        binding.txtShippingFee.text = "\$${String.format("%.2f", shippingFee)}"
        binding.txtTotal.text = "\$${String.format("%.2f", total)}"
        binding.txtDiscount.text = "\$${String.format("%.2f", order.discountAmount ?: 0.0)}"
        binding.txtCouponCode.text = "Coupon: ${order.couponCode ?: "-"}"
        binding.txtOrderNotes.text = "Notes: ${order.orderNotes ?: "-"}"

        // Payment Method & Status Color (Giữ nguyên logic cũ của bạn)
        val rawMethod = when {
            !order.paymentMethod.isNullOrBlank() -> order.paymentMethod
            !order.paymentStatus.isNullOrBlank() -> order.paymentStatus
            else -> null
        }
        val paymentMethodText = when (rawMethod?.uppercase()) {
            "COD", "CASHONDELIVERY", "CASH_ON_DELIVERY" -> "Cash on Delivery"
            "VNPAY", "VN_PAY" -> "VNPay"
            "MOMO" -> "Momo"
            else -> rawMethod ?: "N/A"
        }
        binding.txtPaymentMethod.text = "Payment Method: $paymentMethodText"
        binding.txtPaymentMethod.visibility = View.VISIBLE

        val statusColor = when(order.status?.uppercase() ?: order.orderStatus?.uppercase()) {
            "PENDING" -> R.color.status_pending
            "CONFIRMED" -> R.color.status_confirmed
            "PROCESSING" -> R.color.status_processing
            "SHIPPED" -> R.color.status_shipped
            "DELIVERED" -> R.color.status_delivered
            "CANCELLED" -> R.color.status_cancelled
            else -> R.color.maincolor
        }
        // Dùng Context an toàn
        binding.txtStatus.setTextColor(resources.getColor(statusColor, null))
    }
}