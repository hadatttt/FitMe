package com.pbl6.fitme.wishlist

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.session.SessionManager
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
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()

    override fun initView() {
        // Hiện toolbar
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab wish
        highlightSelectedTab(R.id.wish_id)

        // Setup RecyclerView
        binding.rvWishlist.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        // Chuẩn hóa truyền dữ liệu cho Adapter
        // Giả sử bạn đã có productMap và variantMap từ MainRepository
        adapter = WishlistProductAdapter(
            wishlistItems,
            productMap,
            variantMap,
            object : WishlistProductAdapter.OnWishlistActionListener {
                override fun onRemove(position: Int) {
                    if (position !in wishlistItems.indices) return
                    val item = wishlistItems[position]
                    val token = SessionManager.getInstance().getAccessToken(requireContext())
                    if (token.isNullOrBlank()) {
                        android.widget.Toast.makeText(requireContext(), "Vui lòng đăng nhập", android.widget.Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Call API to remove wishlist item, then update UI on success
                    wishlistRepository.removeWishlistItem(token, item.wishlistItemId.toString()) { success ->
                        activity?.runOnUiThread {
                            if (success) {
                                wishlistItems.removeAt(position)
                                adapter.notifyItemRemoved(position)
                                updateWishlistView()
                            } else {
                                android.widget.Toast.makeText(requireContext(), "Xóa mục yêu thích thất bại", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun onAddToCart(position: Int) {
                    if (position !in wishlistItems.indices) return
                    val item = wishlistItems[position]
                    val product = productMap[item.productId]
                    if (product == null) {
                        // product not loaded yet
                        return
                    }

                    // Navigate to ProductDetailFragment and request to auto-open add-to-cart
                    val bundle = android.os.Bundle().apply {
                        putString("productId", product.productId.toString())
                        putBoolean("autoAddToCart", true)
                    }
                    try {
                        navigate(R.id.productDetailFragment, bundle)
                    } catch (ex: Exception) {
                        android.util.Log.e("WishlistFragment", "Failed to navigate to product detail", ex)
                    }
                }
            }
        )
        binding.rvWishlist.adapter = adapter

        // Lấy dữ liệu wishlist từ API
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        // Prefer userEmail stored in SessionManager because backend expects userEmail for wishlist APIs
        val userEmail = SessionManager.getInstance().getUserEmail(requireContext())
        val fetchCallback: (List<com.pbl6.fitme.model.WishlistItem>?) -> Unit = { result ->
            wishlistItems.clear()
            productMap.clear()
            variantMap.clear()

            if (result != null) {
                wishlistItems.addAll(result)
            }

            // If there are no wishlist items, update UI immediately
            val productIds = wishlistItems.map { it.productId.toString() }.distinct()
            if (productIds.isEmpty()) {
                adapter.notifyDataSetChanged()
                updateWishlistView()
//                return@fetchCallback
            }

            // Fetch product details for each productId to display product name/price/image
            var remaining = productIds.size
            productIds.forEach { pid ->
                mainRepository.getProductById(token ?: "",  pid) { product ->
                    if (product != null) {
                        productMap[product.productId] = product
                        product.variants.forEach { v ->
                            variantMap[v.variantId] = v
                        }
                    }
                    remaining -= 1
                    if (remaining <= 0) {
                        adapter.notifyDataSetChanged()
                        updateWishlistView()
                    }
                }
            }
        }

        if (!userEmail.isNullOrBlank()) {
            // We have explicit user email saved in session
            wishlistRepository.getWishlistByProfile(token, userEmail, fetchCallback)
        } else {
            // Fallback: repository will try to extract email from JWT token if available
            wishlistRepository.getWishlist(token, fetchCallback)
        }

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
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.cart_id, R.id.person_id)
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
