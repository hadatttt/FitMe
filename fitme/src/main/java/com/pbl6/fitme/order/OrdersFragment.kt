package com.pbl6.fitme.order

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentOrdersBinding
import com.pbl6.fitme.model.Order
import com.pbl6.fitme.model.OrderItem
import com.pbl6.fitme.model.OrderStatus
import com.pbl6.fitme.model.ShippingAddress
import com.pbl6.fitme.repository.MainRepository
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.singleClick

class OrdersFragment : BaseFragment<FragmentOrdersBinding, OrdersViewModel>() {

    private val mainRepo = MainRepository()

    override fun initView() {
        // setup header back
        binding.ivBack.setOnClickListener { popBackStack() }
        hideToolbar()
        
        // Setup RecyclerView
        binding.recyclerOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            val adapterInstance = OrdersAdapter().apply {
                setOrderStatusChangeListener(object : OrdersAdapter.OrderStatusChangeListener {
                    override fun onOrderStatusChanged(orderId: String, newStatus: String) {
                        // Update just the specific order in our list
                        currentOrders = currentOrders.map { order ->
                            if (order.orderId == orderId) {
                                // Just update the status field and let other fields use their defaults or existing values
                                Order(
                                    orderId = order.orderId,
                                    userEmail = order.userEmail ?: "",
                                    userId = order.userId,
                                    orderDate = order.orderDate,
                                    createdAt = order.createdAt,
                                    updatedAt = order.updatedAt,
                                    status = newStatus,
                                    orderStatus = newStatus,
                                    shippingAddressId = order.shippingAddressId ?: "",
                                    shippingAddress = order.shippingAddress ?: ShippingAddress(),
                                    couponId = "",  // Use empty string as default since it's non-null
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
                        // Update UI
                        updateTabCounts()
                        displayOrders(currentOrders, currentStatus)
                        
                        android.util.Log.d("OrdersFragment", "Updated order $orderId to status $newStatus")
                    }
                })
            }
            adapter = adapterInstance
            android.util.Log.d("OrdersFragment", "RecyclerView adapter initialized: ${adapterInstance::class.java.simpleName}")
        }
        
        // Set click listeners for tabs
            binding.apply {
                tvTabPending.singleClick { loadOrders(OrderStatus.PENDING) }
                tvTabConfirmed.singleClick { loadOrders(OrderStatus.CONFIRMED) }
                tvTabProcessing.singleClick { loadOrders(OrderStatus.PROCESSING) }
                tvTabShipped.singleClick { loadOrders(OrderStatus.SHIPPED) }
                tvTabDelivered.singleClick { loadOrders(OrderStatus.DELIVERED) }
                tvTabCancelled.singleClick { loadOrders(OrderStatus.CANCELLED) }
            }
    }

    override fun initListener() {
        onBackPressed { popBackStack() }

    }

    private var currentOrders: List<Order> = emptyList()
    private var currentStatus: OrderStatus = OrderStatus.PENDING

    override fun initData() {
        val initialStatus = arguments?.getString("order_status")?.let { value ->
            OrderStatus.values().find { it.value == value }
        } ?: OrderStatus.PENDING
        
        // Load all orders first
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
        val email = com.pbl6.fitme.session.SessionManager.getInstance().getUserEmail(requireContext())
        
        if (token.isNullOrBlank() || email.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Please login to view orders", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        binding.recyclerOrders.visibility = View.GONE
        binding.emptyViewOrder.visibility = View.GONE

        // Get all orders first
        mainRepo.getOrdersByUser(token, email, null) { allOrders -> 
            activity?.runOnUiThread {
                if (!allOrders.isNullOrEmpty()) {
                    currentOrders = allOrders
                    // Update all tab counts first
                    updateTabCounts()
                    // Then display orders for initial status
                    displayOrders(allOrders, initialStatus)
                } else {
                    showEmptyState()
                }
            }
        }
    }

    private fun loadOrders(status: OrderStatus) {
        currentStatus = status
        // Filter from cached orders if available
        if (currentOrders.isNotEmpty()) {
            displayOrders(currentOrders, status)
            return
        }

        // If no cached orders, load from API
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
        val email = com.pbl6.fitme.session.SessionManager.getInstance().getUserEmail(requireContext())
        
        if (token.isNullOrBlank() || email.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Please login to view orders", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        binding.recyclerOrders.visibility = View.GONE
        binding.emptyViewOrder.visibility = View.GONE

        // Get all orders
        mainRepo.getOrdersByUser(token, email, null) { orders -> 
            activity?.runOnUiThread {
                if (!orders.isNullOrEmpty()) {
                    android.util.Log.d("OrdersFragment", "Loaded orders count=${orders.size} sampleStatuses=${orders.map { it.status ?: it.orderStatus }}")
                    currentOrders = orders
                    // Use consolidated display method
                    displayOrders(orders, status)
                } else {
                    android.util.Log.d("OrdersFragment", "No orders returned for user=$email")
                    showEmptyState()
                }
            }
        }
    }

    private fun displayOrders(orders: List<Order>, status: OrderStatus) {
        currentStatus = status
        currentOrders = orders

        val filteredOrders = orders.filter { order ->
            val s = (order.status ?: order.orderStatus ?: "")
            s.equals(status.name, ignoreCase = true) || s.equals(status.value, ignoreCase = true)
        }

        updateOrdersList(filteredOrders)
        selectTab(status)
    }

    private fun updateOrdersList(orders: List<Order>) {
        (binding.recyclerOrders.adapter as? OrdersAdapter)?.apply {
            setList(orders)
        }
        android.util.Log.d("OrdersFragment", "updateOrdersList called, ordersToShow=${orders.size}, adapterCount=${binding.recyclerOrders.adapter?.itemCount}")

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
            tvTabPending.text = "PENDING (${getCount(OrderStatus.PENDING)})"
            tvTabConfirmed.text = "CONFIRMED (${getCount(OrderStatus.CONFIRMED)})"
            tvTabProcessing.text = "PROCESSING (${getCount(OrderStatus.PROCESSING)})"
            tvTabShipped.text = "SHIPPED (${getCount(OrderStatus.SHIPPED)})"
            tvTabDelivered.text = "DELIVERED (${getCount(OrderStatus.DELIVERED)})"
            tvTabCancelled.text = "CANCELLED (${getCount(OrderStatus.CANCELLED)})"
        }
    }

    private fun selectTab(selectedStatus: OrderStatus) {
        binding.apply {
            val views = listOf(
                tvTabPending,
                tvTabConfirmed,
                tvTabProcessing,
                tvTabShipped,
                tvTabDelivered,
                tvTabCancelled
            )
            
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
                
                textView.setBackgroundResource(
                    if (isSelected) R.drawable.bg_selected_tab
                    else android.R.color.transparent
                )
            }
        }
    }
    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}