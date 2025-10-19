package com.pbl6.fitme.order

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentOrdersBinding
import com.pbl6.fitme.model.Order
import com.pbl6.fitme.model.OrderItem
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
        binding.recyclerOrders.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun initListener() {
        onBackPressed { popBackStack() }

    }

    override fun initData() {
        val status = arguments?.getString("order_status") ?: "all"
        binding.apply {
            // Set title according to status
            // (Fragment layout has static title; we can optionally set a subtitle)
        }

        // TODO: Replace with real API call. For now, show mocked orders.
        val mocked = createMockOrders()

        // Compute counts for each status
        val counts = mocked.groupingBy { it.orderStatus.lowercase() }.eachCount()

        fun labelWithCount(base: String, key: String) = "$base (${counts[key] ?: 0})"

        binding.tvTabPending.text = labelWithCount("Confirming", "confirming")
        binding.tvTabPacking.text = labelWithCount("Packing", "packing")
        binding.tvTabProcessing.text = labelWithCount("Delivery", "delivering")
        binding.tvTabDelivered.text = labelWithCount("Received", "received")
        binding.tvTabReturned.text = labelWithCount("Returned", "returned")
        binding.tvTabCancelled.text = labelWithCount("Cancelled", "cancelled")

        // Convert incoming status names from ProfileFragment to internal keys
        val initialKey = when (status.lowercase()) {
            "confirming" -> "confirming"
            "packing" -> "packing"
            "delivering" -> "delivering"
            "received" -> "received"
            else -> "all"
        }

        var currentKey = initialKey

        fun applyFilter(key: String) {
            currentKey = key
            val filtered = if (key == "all") mocked else mocked.filter { it.orderStatus.equals(key, ignoreCase = true) }
            val adapter = OrdersAdapter()
            adapter.setList(filtered)
            binding.recyclerOrders.adapter = adapter
            // update tab visuals
            selectTab(key)
            // update empty / list visibility
            updateOrderView()
        }

        // Tab click listeners
        binding.tvTabPending.singleClick { applyFilter("confirming")     }
        binding.tvTabPacking.singleClick { applyFilter("packing") }
        binding.tvTabProcessing.singleClick { applyFilter("delivering") }
        binding.tvTabDelivered.singleClick { applyFilter("received") }
        binding.tvTabReturned.singleClick { applyFilter("returned") }
        binding.tvTabCancelled.singleClick { applyFilter("cancelled") }

        // Apply initial filter
        applyFilter(currentKey)
    }

    private fun selectTab(key: String) {
        val selectedBg = resources.getColor(android.R.color.holo_blue_light, null)
        val selectedText = resources.getColor(android.R.color.black, null)
        val normalBg = android.R.color.transparent
        val normalText = resources.getColor(android.R.color.black, null)

        fun apply(tvKey: String, tv: View) {
            val isSelected = (tvKey == key)
            tv.setBackgroundColor(if (isSelected) selectedBg else resources.getColor(normalBg, null))
            if (tv is android.widget.TextView) tv.setTextColor(if (isSelected) selectedText else normalText)
        }

        apply("confirming", binding.tvTabPending)
        apply("packing", binding.tvTabPacking)
        apply("delivering", binding.tvTabProcessing)
        apply("received", binding.tvTabDelivered)
        apply("returned", binding.tvTabReturned)
        apply("cancelled", binding.tvTabCancelled)
    }

    private fun createMockOrders(): List<Order> {
        // Create simple mock orders; in real app call repository.getOrders(token, ...)
        val item = OrderItem(orderItemId = java.util.UUID.randomUUID(), quantity = 1, totalPrice = 50.0, unitPrice = 50.0, orderId = java.util.UUID.randomUUID(), variantId = java.util.UUID.randomUUID())
        val o1 = Order(orderId = java.util.UUID.randomUUID(), createdAt = "2025-10-01T10:00:00Z", orderStatus = "confirming", totalAmount = 50.0, updatedAt = "2025-10-01T10:00:00Z", userId = java.util.UUID.randomUUID(), items = listOf(item))
        val o2 = Order(orderId = java.util.UUID.randomUUID(), createdAt = "2025-10-03T10:00:00Z", orderStatus = "confirming", totalAmount = 120.0, updatedAt = "2025-10-03T10:00:00Z", userId = java.util.UUID.randomUUID(), items = listOf(item))
        val o3 = Order(orderId = java.util.UUID.randomUUID(), createdAt = "2025-10-05T10:00:00Z", orderStatus = "delivering", totalAmount = 200.0, updatedAt = "2025-10-05T10:00:00Z", userId = java.util.UUID.randomUUID(), items = listOf(item))
        val o4 = Order(orderId = java.util.UUID.randomUUID(), createdAt = "2025-10-07T10:00:00Z", orderStatus = "received", totalAmount = 80.0, updatedAt = "2025-10-07T10:00:00Z", userId = java.util.UUID.randomUUID(), items = listOf(item))
        return listOf(o1, o2, o3, o4)
    }
    private fun updateOrderView() {
        val count = binding.recyclerOrders.adapter?.itemCount ?: 0
        if (count == 0) {
            binding.recyclerOrders.visibility = View.GONE
            binding.emptyViewOrder.visibility = View.VISIBLE
        } else {
            binding.recyclerOrders.visibility = View.VISIBLE
            binding.emptyViewOrder.visibility = View.GONE
        }
    }
    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}