package com.pbl6.fitme.profile

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentProfileBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.setDraggableWithClick
import hoang.dqm.codebase.utils.singleClick

class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>() {

    override fun initView() {
        // Hiện toolbar trong Activity
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab person trong toolbar
        highlightSelectedTab(R.id.person_id)

        // ===== Data mẫu =====
        val topProducts = listOf("Bag", "Watch", "Shirt", "Shoes", "Dress")
        val stories = listOf("Story1", "Story2", "Story3")
        val productList = listOf(
            Product("Bag", "$25.00"),
            Product("Watch", "$17.00"),
            Product("Shirt", "$12.00")
        )

        // Setup RecyclerViews
        setupRecyclerViewCategory(binding.rvTopProducts, topProducts)
        setupRecyclerViewCategory(binding.rvStories, stories)
        setupRecyclerViewProduct(binding.rvNewItems, productList)
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }
        binding.flCart.setDraggableWithClick {
            hideToolbar()
            popBackStack()
        }
        // ===== Button trong Profile =====
        binding.btnMyActivity.singleClick {
            // TODO: Navigate to MyActivity screen
        }
        binding.btnSetting.singleClick {
            // TODO: Navigate to Settings screen
        }
        binding.btnNotification.singleClick {
            // TODO: Show Notification list
        }
        binding.btnVoucher.singleClick {
            // TODO: Show Voucher screen
        }
        binding.ivSeeAllNewItems.singleClick {
            // TODO: Show full product list
        }
        binding.ivSeeAllNotification.singleClick {
            // TODO: Show full notification list
        }

        // ===== Toolbar click =====
        requireActivity().findViewById<View>(R.id.home_id).singleClick {
            highlightSelectedTab(R.id.home_id)
            // TODO: Navigate to HomeFragment
        }
        requireActivity().findViewById<View>(R.id.wish_id).singleClick {
            highlightSelectedTab(R.id.wish_id)
            // TODO: Navigate to WishFragment
        }
        requireActivity().findViewById<View>(R.id.filter_id).singleClick {
            highlightSelectedTab(R.id.filter_id)
            // TODO: Navigate to FilterFragment
        }
        requireActivity().findViewById<View>(R.id.cart_id).singleClick {
            highlightSelectedTab(R.id.cart_id)
            // TODO: Navigate to CartFragment
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            highlightSelectedTab(R.id.person_id)
            // TODO: Navigate to ProfileFragment (current)
        }
    }

    override fun initData() {
        // TODO: Load data từ ViewModel
    }

    // ===== Toolbar Helpers =====
    private fun highlightSelectedTab(selectedId: Int) {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.filter_id, R.id.cart_id, R.id.person_id)
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

    // ===== RecyclerView Helpers =====
    private fun setupRecyclerViewCategory(rv: RecyclerView, data: List<String>) {
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = CategoryAdapter(data)
    }

    private fun setupRecyclerViewProduct(rv: RecyclerView, data: List<Product>) {
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = ProductAdapter(data)
    }
    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}
