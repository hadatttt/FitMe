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
    private val wishlistItems = mutableListOf<com.pbl6.fitme.model.WishlistItem>()
    private val productMap = mutableMapOf<java.util.UUID, com.pbl6.fitme.model.Product>()
    private val variantMap = mutableMapOf<java.util.UUID, com.pbl6.fitme.model.ProductVariant>()
    private lateinit var adapter: WishlistProductAdapter
    private val wishlistRepository = com.pbl6.fitme.repository.WishlistRepository()

    override fun initView() {
        // Hiện toolbar
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab wish
        highlightSelectedTab(R.id.wish_id)

        // Setup RecyclerView
        binding.rvWishlist.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        // Lấy dữ liệu wishlist từ API
        wishlistRepository.getWishlist { result ->
            wishlistItems.clear()
            if (result != null) {
                wishlistItems.addAll(result)
            }
            adapter.notifyDataSetChanged()
            updateWishlistView()
        }

        // Chuẩn hóa truyền dữ liệu cho Adapter
        // Giả sử bạn đã có productMap và variantMap từ MainRepository
        adapter = WishlistProductAdapter(
            wishlistItems,
            productMap,
            variantMap,
            object : WishlistProductAdapter.OnWishlistActionListener {
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
            }
        )
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
            navigate(R.id.homeFragment)
        }
        requireActivity().findViewById<View>(R.id.wish_id).singleClick {
            highlightSelectedTab(R.id.wish_id)
        }
        requireActivity().findViewById<View>(R.id.filter_id).singleClick {
            highlightSelectedTab(R.id.filter_id)
            navigate(R.id.filterFragment)
        }
        requireActivity().findViewById<View>(R.id.cart_id).singleClick {
            highlightSelectedTab(R.id.cart_id)
            navigate(R.id.cartFragment)
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            highlightSelectedTab(R.id.person_id)
            navigate(R.id.profileFragment)
        }
    }

    override fun initData() {
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
