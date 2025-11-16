package com.pbl6.fitme.cart

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentCartBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class CartFragment : BaseFragment<FragmentCartBinding, CartViewModel>() {

    private val cartItems = mutableListOf<com.pbl6.fitme.model.CartItem>()
    private lateinit var cartAdapter: CartProductAdapter
    private val cartRepository = com.pbl6.fitme.repository.CartRepository()
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private var productMap: Map<java.util.UUID, com.pbl6.fitme.model.Product> = emptyMap()
    private var variantMap: Map<java.util.UUID, com.pbl6.fitme.model.ProductVariant> = emptyMap()

    override fun initView() {
        // Hiện toolbar
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab cart
        highlightSelectedTab(R.id.cart_id)

        // Setup RecyclerView
        binding.rvCart.layoutManager = LinearLayoutManager(requireContext())
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
        if (!token.isNullOrBlank()) {
            // Load dữ liệu: products -> variants -> cart
            mainRepository.getProducts(token) { products: List<com.pbl6.fitme.model.Product>? ->
                activity?.runOnUiThread {
                    productMap = products?.associateBy { it.productId } ?: emptyMap()

                    mainRepository.getProductVariants(token ?: "") { variants: List<com.pbl6.fitme.model.ProductVariant>? ->
                        activity?.runOnUiThread {
                            variantMap = variants?.associateBy { it.variantId } ?: emptyMap()

                            // Use CartRepository to fetch cart items
                            val cartId = com.pbl6.fitme.session.SessionManager.getInstance().getOrCreateCartId(requireContext()).toString()
                            cartRepository.getCart(token ?: "", cartId) { items: List<com.pbl6.fitme.model.CartItem>? ->
                                activity?.runOnUiThread {
                                    cartItems.clear()
                                    // If server cart is unavailable, fall back to local items
                                    val fallback = com.pbl6.fitme.session.SessionManager.getInstance().getLocalCartItems(requireContext())
                                    if (items != null) {
                                        cartItems.addAll(items)
                                    } else if (fallback != null) {
                                        cartItems.addAll(fallback)
                                    }

                                    cartAdapter = CartProductAdapter(
                                        cartItems,
                                        variantMap,
                                        productMap,
                                        object : CartProductAdapter.OnCartActionListener {
                                            override fun onRemove(position: Int) {
                                                if (position in cartItems.indices) {
                                                    cartItems.removeAt(position)
                                                    cartAdapter.notifyItemRemoved(position)
                                                    updateCartView()
                                                }
                                            }

                                            override fun onIncrease(position: Int) {
                                                if (position in cartItems.indices) {
                                                    cartItems[position] = cartItems[position].copy(
                                                        quantity = cartItems[position].quantity + 1
                                                    )
                                                    cartAdapter.notifyItemChanged(position)
                                                    updateCartView()
                                                }
                                            }

                                            override fun onDecrease(position: Int) {
                                                if (position in cartItems.indices) {
                                                    if (cartItems[position].quantity > 1) {
                                                        cartItems[position] = cartItems[position].copy(
                                                            quantity = cartItems[position].quantity - 1
                                                        )
                                                        cartAdapter.notifyItemChanged(position)
                                                        updateCartView()
                                                    } else {
                                                        onRemove(position)
                                                    }
                                                }
                                            }
                                        }
                                    )

                                    binding.rvCart.adapter = cartAdapter
                                    updateCartView()
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }

        binding.btnEditAddress.singleClick {
            // TODO: Navigate/Edit Address
        }
        binding.btnCheckout.singleClick {
            if (cartItems.isNotEmpty()) {
                // Pass variant ids as strings to avoid index mismatch after reload
                val variantIds = ArrayList<String>()
                cartItems.forEach { ci -> variantIds.add(ci.variantId.toString()) }
                val bundle = Bundle().apply {
                    putStringArrayList("cart_variant_ids", variantIds)
                }
               navigate(R.id.checkoutFragment, bundle)
            }
        }

        // ===== Toolbar click =====
        requireActivity().findViewById<View>(R.id.home_id).singleClick {
            highlightSelectedTab(R.id.home_id)
            navigate(R.id.homeFragment)
        }
        requireActivity().findViewById<View>(R.id.wish_id).singleClick {
            highlightSelectedTab(R.id.wish_id)
            navigate(R.id.wishlistFragment)
        }
        requireActivity().findViewById<View>(R.id.filter_id).singleClick {
            highlightSelectedTab(R.id.filter_id)
            navigate(R.id.filterFragment)
        }
        requireActivity().findViewById<View>(R.id.cart_id).singleClick {
            highlightSelectedTab(R.id.cart_id)
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            highlightSelectedTab(R.id.person_id)
            navigate(R.id.profileFragment)
        }
    }

    override fun initData() {
    }

    // ===== Helpers =====
    private fun updateCartView() {
        binding.txtCartTitle.text = "Cart (${cartItems.size})"

        if (cartItems.isEmpty()) {
            binding.rvCart.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            binding.txtTotal.text = "Total $0.00"
            binding.btnCheckout.isEnabled = false
            binding.btnCheckout.setBackgroundColor(resources.getColor(android.R.color.white, null))
            binding.btnCheckout.setTextColor(resources.getColor(android.R.color.black, null))
        } else {
            binding.rvCart.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE

            // Tính tổng tiền dựa vào variantMap
            val total = cartItems.sumOf { cartItem ->
                val variant = variantMap[cartItem.variantId]
                (variant?.price ?: 0.0) * cartItem.quantity
            }
            binding.txtTotal.text = "Total $%.2f".format(total)

            binding.btnCheckout.isEnabled = true
            binding.btnCheckout.setBackgroundColor(resources.getColor(R.color.maincolor, null))
            binding.btnCheckout.setTextColor(resources.getColor(android.R.color.white, null))
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
