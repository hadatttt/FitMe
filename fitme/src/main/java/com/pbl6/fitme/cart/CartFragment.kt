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
    private var accessToken: String? = null
    private var productMap: MutableMap<java.util.UUID, com.pbl6.fitme.model.Product> = mutableMapOf()
    private var variantMap: MutableMap<java.util.UUID, com.pbl6.fitme.model.ProductVariant> = mutableMapOf()

    override fun initView() {
        // Hiện toolbar
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab cart
        highlightSelectedTab(R.id.cart_id)

        // Setup RecyclerView
        binding.rvCart.layoutManager = LinearLayoutManager(requireContext())
        accessToken = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
        val token = accessToken
        if (!token.isNullOrBlank()) {
            // Load dữ liệu: products -> variants -> cart
            mainRepository.getProducts(token) { products: List<com.pbl6.fitme.model.Product>? ->
                activity?.runOnUiThread {
                    productMap.clear()
                    products?.forEach { p -> productMap[p.productId] = p }

                    mainRepository.getProductVariants(token ?: "") { variants: List<com.pbl6.fitme.model.ProductVariant>? ->
                        activity?.runOnUiThread {
                            variantMap.clear()
                            variants?.forEach { v -> variantMap[v.variantId] = v }

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

                                    // Ensure we have Product objects for each cart item variant.
                                    val missingVariantIds = mutableListOf<java.util.UUID>()
                                    cartItems.forEach { ci ->
                                        val existingProduct = variantMap[ci.variantId]?.let { v -> productMap[v.productId] }
                                        if (existingProduct == null) {
                                            missingVariantIds.add(ci.variantId)
                                        }
                                    }

                                    if (missingVariantIds.isEmpty()) {
                                        // All products available -> initialize adapter
                                        setupCartAdapter()
                                    } else {
                                        // Fetch product for each missing variant from backend
                                        var remaining = missingVariantIds.size
                                        missingVariantIds.forEach { vid ->
                                            mainRepository.getProductByVariantId(token ?: "", vid.toString()) { product ->
                                                activity?.runOnUiThread {
                                                    product?.let { p -> productMap[p.productId] = p }
                                                    // Also, if variant info missing, try to populate from product variants
                                                    try {
                                                        val foundVariant = product?.variants?.find { it.variantId == vid }
                                                        if (foundVariant != null) variantMap[foundVariant.variantId] = foundVariant
                                                    } catch (ex: Exception) {
                                                        // ignore
                                                    }

                                                    remaining -= 1
                                                    if (remaining <= 0) {
                                                        setupCartAdapter()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private fun setupCartAdapter() {
                                    cartAdapter = CartProductAdapter(
            cartItems,
            variantMap,
            productMap,
            object : CartProductAdapter.OnCartActionListener {
                override fun onRemove(position: Int) {
                    if (position in cartItems.indices) {
                        // Call backend to remove item first
                        val cartItemId = cartItems[position].cartItemId.toString()
                        val t = accessToken
                        if (!t.isNullOrBlank()) {
                            cartRepository.removeCartItem(t, cartItemId) { success ->
                                activity?.runOnUiThread {
                                    if (success) {
                                        cartItems.removeAt(position)
                                        cartAdapter.notifyItemRemoved(position)
                                        updateCartView()
                                    } else {
                                        // Optionally show toast; keep item
                                    }
                                }
                            }
                        } else {
                            // No token: remove locally
                            cartItems.removeAt(position)
                            cartAdapter.notifyItemRemoved(position)
                            updateCartView()
                        }
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

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }

        // Back arrow click
        try {
            binding.ivBack.singleClick {
                hideToolbar()
                popBackStack()
            }
        } catch (_: Exception) { }

        binding.btnEditAddress.singleClick {
            // TODO: Navigate/Edit Address
        }
        binding.btnCheckout.singleClick {
            if (cartItems.isNotEmpty()) {
                // Pass variant ids as strings to avoid index mismatch after reload
                val variantIds = ArrayList<String>()
                val variantQuantities = ArrayList<Int>()
                cartItems.forEach { ci ->
                    variantIds.add(ci.variantId.toString())
                    variantQuantities.add(ci.quantity)
                }
                val bundle = Bundle().apply {
                    putStringArrayList("cart_variant_ids", variantIds)
                    putIntegerArrayList("cart_variant_quantities", variantQuantities)
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
        binding.thump.text = "Cart (${cartItems.size})"

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
