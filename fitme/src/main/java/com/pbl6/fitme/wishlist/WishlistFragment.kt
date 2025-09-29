package com.pbl6.fitme.wishlist

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentWishlistBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.navigateWithoutAnimation
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
        highlightSelectedTab(R.id.wish_id)
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
            popBackStack(R.id.loginFragment)
        }
        requireActivity().findViewById<View>(R.id.home_id).singleClick {
            navigateWithoutAnimation(R.id.homeMainFragment, isPop = true)
        }
        requireActivity().findViewById<View>(R.id.filter_id).singleClick {
        }
        requireActivity().findViewById<View>(R.id.cart_id).singleClick {
            navigateWithoutAnimation(R.id.cartFragment,isPop = true)
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            navigateWithoutAnimation(R.id.profileFragment,isPop = true)
        }
    }

    override fun initData() {
    }

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
