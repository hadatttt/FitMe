package com.pbl6.fitme.profile

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentProfileBinding
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.setDraggableWithClick
import hoang.dqm.codebase.utils.singleClick

class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>() {
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private val recommendRepository = com.pbl6.fitme.repository.RecommendRepository()
    private lateinit var productAdapter: ProductAdapter

    override fun initView() {
        val session = SessionManager.getInstance()
        val token = session.getAccessToken(requireContext())
        Log.d("SessionManager", "AccessToken = $token")
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        highlightSelectedTab(R.id.person_id)

        setupRecyclerViews()
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật số lượng đơn hàng mỗi khi màn hình hiện lên
        updateOrderBadges()
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }
        binding.flCart.setDraggableWithClick {
            navigate(R.id.cartFragment)
        }
        binding.btnSetting.singleClick {
            navigate(R.id.settingsFragment)
        }
        binding.ivSeeAllNotification.singleClick {
        }
        requireActivity().findViewById<View>(R.id.home_id).singleClick {
            highlightSelectedTab(R.id.home_id)
            navigate(R.id.homeFragment)
        }
        requireActivity().findViewById<View>(R.id.wish_id).singleClick {
            highlightSelectedTab(R.id.wish_id)
            navigate(R.id.wishlistFragment)
        }
        requireActivity().findViewById<View>(R.id.cart_id).singleClick {
            highlightSelectedTab(R.id.cart_id)
            navigate(R.id.cartFragment)
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            highlightSelectedTab(R.id.person_id)
        }

        // Click events for Order Statuses
        binding.llStatusPending.singleClick { navigateToOrder("pending") }
        binding.llStatusConfirmed.singleClick { navigateToOrder("confirmed") }
        binding.llStatusProcessing.singleClick { navigateToOrder("processing") }
        binding.llStatusShipped.singleClick { navigateToOrder("shipped") }
        binding.llStatusDelivered.singleClick { navigateToOrder("delivered") }
        binding.llStatusCancelled.singleClick { navigateToOrder("cancelled") }
    }

    private fun navigateToOrder(status: String) {
        val bundle = android.os.Bundle().apply { putString("order_status", status) }
        navigate(R.id.ordersFragment, bundle)
    }

    override fun initData() {
    }

    private fun updateOrderBadges() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        val email = SessionManager.getInstance().getUserEmail(requireContext())

        if (!token.isNullOrBlank() && !email.isNullOrBlank()) {
            mainRepository.getOrdersByUser(token, email, null) { orders ->
                activity?.runOnUiThread {
                    if (orders != null) {
                        // Helper function to count and set text
                        fun setBadge(textView: android.widget.TextView, statusKey: String) {
                            val count = orders.count {
                                (it.status ?: it.orderStatus ?: "").equals(statusKey, ignoreCase = true)
                            }
                            if (count > 0) {
                                textView.text = count.toString()
                                textView.visibility = View.VISIBLE
                            } else {
                                textView.visibility = View.GONE
                            }
                        }

                        // Map counts to the TextViews created in XML
                        setBadge(binding.tvCountPending, "pending")
                        setBadge(binding.tvCountConfirmed, "confirmed")
                        setBadge(binding.tvCountProcessing, "processing")
                        setBadge(binding.tvCountShipped, "shipped")
                        setBadge(binding.tvCountDelivered, "delivered")
                        setBadge(binding.tvCountCancelled, "cancelled")
                    }
                }
            }
        }
    }

    private fun highlightSelectedTab(selectedId: Int) {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.cart_id, R.id.person_id)
        ids.forEach { id ->
            val view = requireActivity().findViewById<View>(id)
            if (id == selectedId) {
                view.setBackgroundResource(R.drawable.bg_selected_tab)
            } else {
                view.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), android.R.color.transparent)
                )
            }
        }
    }

    private fun setupRecyclerViews() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (!token.isNullOrBlank()) {
            productAdapter = ProductAdapter()
            binding.rvTopProducts.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
            binding.rvTopProducts.adapter = productAdapter

            productAdapter.setOnClickItemRecyclerView { product, _ ->
                val bundle = android.os.Bundle().apply {
                    putString("productId", product.productId.toString())
                }
                navigate(R.id.productDetailFragment, bundle)
            }

            val userId = SessionManager.getInstance().getUserId(requireContext())?.toString()
            if (!userId.isNullOrBlank()) {
                // Request 6 recommendations and display them
                recommendRepository.getRecommendations(requireContext(), userId, 6) { list ->
                    activity?.runOnUiThread {
                        if (!list.isNullOrEmpty()) {
                                        // Ensure at most 6 items
                                        val limited = if (list.size > 6) list.take(6) else list

                                        // If some recommended products don't include images, fetch full product details
                                        val needFetch = limited.filter { it.mainImageUrl.isNullOrBlank() }
                                        if (needFetch.isEmpty()) {
                                            productAdapter.setList(limited)
                                        } else {
                                            val mutable = limited.toMutableList()
                                            var remaining = needFetch.size
                                            needFetch.forEach { rec ->
                                                mainRepository.getProductById(token, rec.productId.toString()) { full ->
                                                    activity?.runOnUiThread {
                                                        if (full != null) {
                                                            val idx = mutable.indexOfFirst { it.productId == rec.productId }
                                                            if (idx >= 0) mutable[idx] = full
                                                        }
                                                        remaining -= 1
                                                        if (remaining <= 0) {
                                                            productAdapter.setList(mutable)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                        } else {
                            // fallback to top products if no recommendations
                            mainRepository.getProducts(token) { products: List<Product>? ->
                                activity?.runOnUiThread {
                                    if (products != null) productAdapter.setList(products.take(6))
                                }
                            }
                        }
                    }
                }
            } else {
                // No user id: show top 6 products
                mainRepository.getProducts(token) { products: List<Product>? ->
                    activity?.runOnUiThread {
                        if (products != null) productAdapter.setList(products.take(6))
                    }
                }
            }
        }
    }

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}