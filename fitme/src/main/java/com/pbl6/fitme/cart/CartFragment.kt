package com.pbl6.fitme.cart

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentCartBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.util.UUID

class CartFragment : BaseFragment<FragmentCartBinding, CartViewModel>() {

    private val cartItems = mutableListOf<com.pbl6.fitme.model.CartItem>()
    private lateinit var cartAdapter: CartProductAdapter
    private val cartRepository = com.pbl6.fitme.repository.CartRepository()
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private val addressRepository = com.pbl6.fitme.repository.AddressRepository()
    private var accessToken: String? = null

    // Map lưu thông tin sản phẩm
    private var productMap: MutableMap<UUID, com.pbl6.fitme.model.Product> = mutableMapOf()
    private var variantMap: MutableMap<UUID, com.pbl6.fitme.model.ProductVariant> = mutableMapOf()

    // Set lưu các variantId đang được tích chọn
    private val selectedVariantIds = mutableSetOf<UUID>()

    override fun initView() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        highlightSelectedTab(R.id.cart_id)

        binding.rvCart.layoutManager = LinearLayoutManager(requireContext())

        accessToken = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
        val token = accessToken

        if (!token.isNullOrBlank()) {
            val email = com.pbl6.fitme.session.SessionManager.getInstance().getUserEmail(requireContext())

            if (!email.isNullOrBlank()) {
                addressRepository.getUserAddresses(token, email) { listAddress ->
                    activity?.runOnUiThread {
                        if (!listAddress.isNullOrEmpty()) {
                            val defaultAddress = listAddress.find { it.isDefault } ?: listAddress.first()

                            binding.txtShippingAddress.text = "${defaultAddress.addressLine1},${defaultAddress.addressLine2}"
                        } else {
                            binding.txtShippingAddress.text = "Please add a shipping address"
                        }
                    }
                }
            }
            mainRepository.getProducts(token) { products ->
                activity?.runOnUiThread {
                    productMap.clear()
                    products?.forEach { p -> productMap[p.productId] = p }

                    mainRepository.getProductVariants(token) { variants ->
                        activity?.runOnUiThread {
                            variantMap.clear()
                            variants?.forEach { v -> variantMap[v.variantId] = v }

                            val cartId = com.pbl6.fitme.session.SessionManager.getInstance().getOrCreateCartId(requireContext()).toString()
                            cartRepository.getCart(token, cartId) { items ->
                                activity?.runOnUiThread {
                                    cartItems.clear()
                                    val fallback = com.pbl6.fitme.session.SessionManager.getInstance().getLocalCartItems(requireContext())
                                    if (items != null) cartItems.addAll(items)
                                    else if (fallback != null) cartItems.addAll(fallback)

                                    // Mặc định chọn tất cả các item khi mới load
                                    selectedVariantIds.clear()
                                    cartItems.forEach { selectedVariantIds.add(it.variantId) }

                                    val missingVariantIds = mutableListOf<UUID>()
                                    cartItems.forEach { ci ->
                                        val existingProduct = variantMap[ci.variantId]?.let { v -> productMap[v.productId] }
                                        if (existingProduct == null) missingVariantIds.add(ci.variantId)
                                    }

                                    if (missingVariantIds.isEmpty()) {
                                        setupCartAdapter()
                                    } else {
                                        var remaining = missingVariantIds.size
                                        missingVariantIds.forEach { vid ->
                                            mainRepository.getProductByVariantId(token, vid.toString()) { product ->
                                                activity?.runOnUiThread {
                                                    product?.let { p -> productMap[p.productId] = p }
                                                    try {
                                                        val foundVariant = product?.variants?.find { it.variantId == vid }
                                                        if (foundVariant != null) variantMap[foundVariant.variantId] = foundVariant
                                                    } catch (_: Exception) { }

                                                    remaining -= 1
                                                    if (remaining <= 0) setupCartAdapter()
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
            selectedVariantIds, // Truyền danh sách đang chọn vào adapter
            object : CartProductAdapter.OnCartActionListener {

                // Xử lý sự kiện tích chọn checkbox
                override fun onSelectionChanged(position: Int, isSelected: Boolean) {
                    if (position in cartItems.indices) {
                        val variantId = cartItems[position].variantId
                        if (isSelected) {
                            selectedVariantIds.add(variantId)
                        } else {
                            selectedVariantIds.remove(variantId)
                        }
                        // Tính lại tiền ngay lập tức
                        updateCartView()
                    }
                }

                override fun onRemove(position: Int) {
                    if (position in cartItems.indices) {
                        val itemToRemove = cartItems[position]
                        val t = accessToken
                        if (!t.isNullOrBlank()) {
                            cartRepository.removeCartItem(t, itemToRemove.cartItemId.toString()) { success ->
                                activity?.runOnUiThread {
                                    if (success) {
                                        // Xóa khỏi danh sách chọn nếu có
                                        selectedVariantIds.remove(itemToRemove.variantId)
                                        cartItems.removeAt(position)
                                        cartAdapter.notifyItemRemoved(position)
                                        cartAdapter.notifyItemRangeChanged(position, cartItems.size)
                                        updateCartView()
                                    }
                                }
                            }
                        } else {
                            selectedVariantIds.remove(itemToRemove.variantId)
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

    private fun updateCartView() {
        binding.thump.text = "Cart (${cartItems.size})"

        if (cartItems.isEmpty()) {
            binding.rvCart.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            binding.txtTotal.text = "Total $0.00"
            setCheckoutEnabled(false)
        } else {
            binding.rvCart.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE

            // --- TÍNH TỔNG TIỀN CHỈ CÁC ITEM ĐƯỢC CHỌN ---
            val total = cartItems.sumOf { cartItem ->
                if (selectedVariantIds.contains(cartItem.variantId)) {
                    val variant = variantMap[cartItem.variantId]
                    (variant?.price ?: 0.0) * cartItem.quantity
                } else {
                    0.0
                }
            }
            binding.txtTotal.text = "Total $%.2f".format(total)

            setCheckoutEnabled(selectedVariantIds.isNotEmpty())
        }
    }

    private fun setCheckoutEnabled(isEnabled: Boolean) {
        binding.btnCheckout.isEnabled = isEnabled
        if (isEnabled) {
            binding.btnCheckout.setBackgroundColor(resources.getColor(R.color.maincolor, null))
            binding.btnCheckout.setTextColor(resources.getColor(android.R.color.white, null))
        } else {
            binding.btnCheckout.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            binding.btnCheckout.setTextColor(resources.getColor(android.R.color.white, null))
        }
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }

        try {
            binding.ivBack.singleClick {
                hideToolbar()
                popBackStack()
            }
        } catch (_: Exception) { }

        binding.btnEditAddress.singleClick {
            navigate(R.id.shippingAddressFragment)
        }

        binding.btnCheckout.singleClick {
            val selectedItems = cartItems.filter { selectedVariantIds.contains(it.variantId) }

            if (selectedItems.isNotEmpty()) {

                // --- 1) Gửi item qua checkout như cũ ---
                val variantIds = ArrayList<String>()
                val variantQuantities = ArrayList<Int>()

                selectedItems.forEach { ci ->
                    variantIds.add(ci.variantId.toString())
                    variantQuantities.add(ci.quantity)
                }

                val bundle = Bundle().apply {
                    putStringArrayList("cart_variant_ids", variantIds)
                    putIntegerArrayList("cart_variant_quantities", variantQuantities)
                }

                navigate(R.id.checkoutFragment, bundle)

                val token = accessToken
                if (!token.isNullOrBlank()) {
                    selectedItems.forEach { item ->
                        cartRepository.removeCartItem(token, item.cartItemId.toString()) { }
                    }
                }

                cartItems.removeAll(selectedItems)
                selectedVariantIds.removeAll(selectedItems.map { it.variantId }.toSet())

                cartAdapter.notifyDataSetChanged()
                updateCartView()

            } else {
                Toast.makeText(context, "Please select items to checkout", Toast.LENGTH_SHORT).show()
            }
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
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            highlightSelectedTab(R.id.person_id)
            navigate(R.id.profileFragment)
        }
    }

    override fun initData() { }

    private fun highlightSelectedTab(selectedId: Int) {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.cart_id, R.id.person_id)
        ids.forEach { id ->
            val view = requireActivity().findViewById<View>(id)
            if (id == selectedId) {
                view.setBackgroundResource(R.drawable.bg_selected_tab)
            } else {
                view.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            }
        }
    }

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}