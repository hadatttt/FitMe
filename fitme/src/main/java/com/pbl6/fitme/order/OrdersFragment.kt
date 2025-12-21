package com.pbl6.fitme.order

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentOrdersBinding
import com.pbl6.fitme.model.Order
import com.pbl6.fitme.model.OrderStatus
import com.pbl6.fitme.model.ShippingAddress
import com.pbl6.fitme.repository.MainRepository
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.utils.singleClick

class OrdersFragment : BaseFragment<FragmentOrdersBinding, OrdersViewModel>() {

    private val mainRepo = MainRepository()
    private var currentOrders: List<Order> = emptyList()
    private var currentStatus: OrderStatus = OrderStatus.PENDING

    // Đưa adapter ra thành property để dễ quản lý
    private val ordersAdapter by lazy { OrdersAdapter() }

    override fun initView() {
        hideToolbar()

        // Setup RecyclerView UI
        binding.recyclerOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ordersAdapter
        }

        // Logic kiểm tra login và load data ban đầu
        val initialStatus = arguments?.getString("order_status")?.let { value ->
            OrderStatus.values().find { it.value.equals(value, ignoreCase = true) }
        } ?: OrderStatus.PENDING

        // Mặc định chọn tab UI ban đầu (dù chưa có data)
        selectTab(initialStatus)

        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
        val email = com.pbl6.fitme.session.SessionManager.getInstance().getUserEmail(requireContext())

        if (token.isNullOrBlank() || email.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Please login to view orders", Toast.LENGTH_SHORT).show()
            return
        }

        binding.recyclerOrders.visibility = View.GONE
        binding.emptyViewOrder.visibility = View.GONE

        // Gọi API
        mainRepo.getOrdersByUser(token, email, null) { allOrders ->
            activity?.runOnUiThread {
                if (!allOrders.isNullOrEmpty()) {
                    currentOrders = allOrders
                    updateTabCounts()
                    displayOrders(allOrders, initialStatus)
                } else {
                    showEmptyState()
                    selectTab(initialStatus)
                }
            }
        }
    }

    override fun initListener() {
        // 1. System Navigation Listeners
        onBackPressed { popBackStack() }
        binding.ivBack.singleClick { popBackStack(R.id.homeFragment) }

        // 2. Tab Click Listeners (Moved from initView)
        binding.apply {
            tvTabPending.singleClick { loadOrders(OrderStatus.PENDING) }
            tvTabConfirmed.singleClick { loadOrders(OrderStatus.CONFIRMED) }
            tvTabProcessing.singleClick { loadOrders(OrderStatus.PROCESSING) }
            tvTabShipped.singleClick { loadOrders(OrderStatus.SHIPPED) }
            tvTabDelivered.singleClick { loadOrders(OrderStatus.DELIVERED) }
            tvTabCancelled.singleClick { loadOrders(OrderStatus.CANCELLED) }
        }

        // 3. Adapter Action Listeners
        ordersAdapter.setOrderStatusChangeListener(object : OrdersAdapter.OrderStatusChangeListener {
            override fun onOrderStatusChanged(orderId: String, newStatus: String) {
                handleOrderStatusChange(orderId, newStatus)
            }
        })
    }

    override fun initData() { }

    // Tách logic xử lý khi status thay đổi ra hàm riêng cho gọn initListener
    private fun handleOrderStatusChange(orderId: String, newStatus: String) {
        currentOrders = currentOrders.map { order ->
            if (order.orderId == orderId) {
                order.copy(
                    status = newStatus,
                    orderStatus = newStatus,
                    // Lưu ý: data class Order nên dùng copy() nếu có thể để ngắn gọn hơn,
                    // nhưng nếu structure class Order của bạn không hỗ trợ copy chuẩn thì giữ nguyên cách map thủ công như cũ:
                    userEmail = order.userEmail ?: "",
                    userId = order.userId,
                    orderDate = order.orderDate,
                    createdAt = order.createdAt,
                    updatedAt = order.updatedAt,
                    shippingAddressId = order.shippingAddressId ?: "",
                    shippingAddress = order.shippingAddress ?: ShippingAddress(),
                    couponCode = order.couponCode,
                    orderItems = order.orderItems ?: emptyList(),
                    items = order.items ?: emptyList(),
                    subtotal = order.subtotal ?: 0.0,
                    totalAmount = order.totalAmount ?: 0.0,
                    discountAmount = order.discountAmount ?: 0.0,
                    shippingFee = order.shippingFee ?: 0.0,
                    orderNotes = order.orderNotes ?: ""
                )
            } else {
                order
            }
        }
        updateTabCounts()
        displayOrders(currentOrders, currentStatus)
    }

    private fun loadOrders(status: OrderStatus) {
        currentStatus = status
        if (currentOrders.isNotEmpty()) {
            displayOrders(currentOrders, status)
            return
        }
    }

    private fun displayOrders(orders: List<Order>, status: OrderStatus) {
        currentStatus = status
        val filteredOrders = orders.filter { order ->
            val s = (order.status ?: order.orderStatus ?: "")
            s.equals(status.name, ignoreCase = true) || s.equals(status.value, ignoreCase = true)
        }
        updateOrdersList(filteredOrders)
        selectTab(status)
    }

    private fun updateOrdersList(orders: List<Order>) {
        ordersAdapter.setList(orders)
        if (orders.isEmpty()) {
            showEmptyState()
        } else {
            binding.recyclerOrders.visibility = View.VISIBLE
            binding.emptyViewOrder.visibility = View.GONE
        }
    }

    private fun showEmptyState() {
        binding.recyclerOrders.visibility = View.GONE
        binding.emptyViewOrder.visibility = View.VISIBLE
    }

    private fun updateTabCounts() {
        fun getCount(status: OrderStatus) = currentOrders.count { order ->
            val s = (order.status ?: order.orderStatus ?: "")
            s.equals(status.name, ignoreCase = true) || s.equals(status.value, ignoreCase = true)
        }

        binding.apply {
            tvTabPending.text = "Pending (${getCount(OrderStatus.PENDING)})"
            tvTabConfirmed.text = "Confirmed (${getCount(OrderStatus.CONFIRMED)})"
            tvTabProcessing.text = "Processing (${getCount(OrderStatus.PROCESSING)})"
            tvTabShipped.text = "Shipped (${getCount(OrderStatus.SHIPPED)})"
            tvTabDelivered.text = "Delivered (${getCount(OrderStatus.DELIVERED)})"
            tvTabCancelled.text = "Cancelled (${getCount(OrderStatus.CANCELLED)})"
        }
    }

    private fun selectTab(selectedStatus: OrderStatus) {
        binding.apply {
            val views = listOf(tvTabPending, tvTabConfirmed, tvTabProcessing, tvTabShipped, tvTabDelivered, tvTabCancelled)

            var selectedView: View? = null

            views.forEach { textView ->
                val isSelected = when(textView) {
                    tvTabPending -> selectedStatus == OrderStatus.PENDING
                    tvTabConfirmed -> selectedStatus == OrderStatus.CONFIRMED
                    tvTabProcessing -> selectedStatus == OrderStatus.PROCESSING
                    tvTabShipped -> selectedStatus == OrderStatus.SHIPPED
                    tvTabDelivered -> selectedStatus == OrderStatus.DELIVERED
                    tvTabCancelled -> selectedStatus == OrderStatus.CANCELLED
                    else -> false
                }

                if (isSelected) selectedView = textView

                textView.setBackgroundResource(
                    if (isSelected) R.drawable.bg_selected_tab
                    else android.R.color.transparent
                )
            }

            try {
                if (selectedView != null) {
                    val scrollX = selectedView!!.left - (binding.scrollTabs.width / 2) + (selectedView!!.width / 2)
                    binding.scrollTabs.smoothScrollTo(scrollX, 0)
                }
            } catch (e: Exception) {
                // Ignore if view not laid out yet
            }
        }
    }

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}