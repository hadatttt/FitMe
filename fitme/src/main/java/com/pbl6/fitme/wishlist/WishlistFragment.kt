package com.pbl6.fitme.wishlist

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentWishlistBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class WishlistFragment : BaseFragment<FragmentWishlistBinding, WishlistViewModel>() {

    private val wishlistItems = mutableListOf<WishlistProduct>()
    private lateinit var adapter: WishlistProductAdapter

    override fun initView() {
        // Hiện toolbar
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab wish
        highlightSelectedTab(R.id.wish_id)

        // Setup RecyclerView
        binding.rvWishlist.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        // Dữ liệu mẫu
        wishlistItems.addAll(
            listOf(
                WishlistProduct("Bag", "$25.00", "Pink", "M"),
                WishlistProduct("Watch", "$17.00", "Black", "L"),
                WishlistProduct("Shirt", "$12.00", "Blue", "S")
            )
        )

        adapter = WishlistProductAdapter(wishlistItems, object :
            WishlistProductAdapter.OnWishlistActionListener {
            override fun onRemove(position: Int) {
                if (position in wishlistItems.indices) {
                    wishlistItems.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    updateWishlistView()
                }
            }

            override fun onAddToCart(position: Int) {
                // TODO: Thêm sản phẩm này vào Cart
            }
        })

        binding.rvWishlist.adapter = adapter
        updateWishlistView()
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }

        // ===== Toolbar click =====
        requireActivity().findViewById<View>(R.id.home_id).singleClick {
            highlightSelectedTab(R.id.home_id)
            // TODO: Navigate to HomeFragment
            navigate(R.id.action_wishlistFragment_to_homeFragment)
        }
        requireActivity().findViewById<View>(R.id.wish_id).singleClick {
            highlightSelectedTab(R.id.wish_id)
            // Stay in WishlistFragment
        }
        requireActivity().findViewById<View>(R.id.filter_id).singleClick {
            highlightSelectedTab(R.id.filter_id)
            // TODO: Navigate to FilterFragment
        }
        requireActivity().findViewById<View>(R.id.cart_id).singleClick {
            highlightSelectedTab(R.id.cart_id)
            // TODO: Navigate to CartFragment
            navigate(R.id.action_wishlistFragment_to_cartFragment)
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            highlightSelectedTab(R.id.person_id)
            // TODO: Navigate to ProfileFragment
            navigate(R.id.action_wishlistFragment_to_profileFragment)
        }
    }

    override fun initData() {
        // TODO: Load data từ ViewModel thay cho dữ liệu mẫu
    }

    // ===== Helpers =====
    private fun updateWishlistView() {
        binding.txtWishlist.text = "Wishlist (${wishlistItems.size})"
        if (wishlistItems.isEmpty()) {
            binding.rvWishlist.visibility = View.GONE
            binding.emptyViewWl.visibility = View.VISIBLE
        } else {
            binding.rvWishlist.visibility = View.VISIBLE
            binding.emptyViewWl.visibility = View.GONE
        }
    }

    private fun highlightSelectedTab(selectedId: Int) {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.filter_id, R.id.cart_id, R.id.person_id)
        ids.forEach { id ->
            val view = requireActivity().findViewById<View>(id)
            if (id == selectedId) {
                view.setBackgroundResource(R.drawable.bg_selected_tab)
            } else {
                view.setBackgroundColor(
                    resources.getColor(android.R.color.transparent, null)
                )
            }
        }
    }

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}
