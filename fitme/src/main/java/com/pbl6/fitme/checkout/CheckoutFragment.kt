package com.pbl6.fitme.checkout

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.model.CartItem
import com.pbl6.fitme.databinding.FragmentCheckoutBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.singleClick

class CheckoutFragment : BaseFragment<FragmentCheckoutBinding, CheckoutViewModel>() {
    private var total: Double = 0.0
    private var shippingFee: Double = 0.0
    private lateinit var checkoutProductAdapter: CheckoutProductAdapter
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private var dataLoaded: Boolean = false

    private var productMap: Map<java.util.UUID, com.pbl6.fitme.model.Product> = emptyMap()
    private var variantMap: Map<java.util.UUID, com.pbl6.fitme.model.ProductVariant> = emptyMap()
    private var cartItems: List<CartItem> = emptyList()

    override fun initView() {
        // Hiện toolbar
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab cart
        highlightSelectedTab(R.id.cart_id)

    // Setup RecyclerView
    binding.rvCart.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onResume() {
        super.onResume()
        // If we returned from edit screens and no data is present, reload
        val hasItems = binding.rvCart.adapter?.itemCount ?: 0
        if (!dataLoaded || hasItems == 0) {
            initData()
        }
    }

    override fun initListener() {
        // Nút Pay
        binding.btnEditAddress.singleClick {
            navigate(R.id.shippingAddressFragment)
        }
        binding.btnEditContact.singleClick {
            navigate(R.id.contactInforFragment)
        }
        binding.btnEditPayment.singleClick {

        }
        binding.btnCheckout.singleClick {
            // TODO: xử lý thanh toán
        }

        // RadioGroup shipping
        binding.rgShippingOptions.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbStandard -> {
                    shippingFee = 0.0
                    binding.rbStandard.setBackgroundResource(R.drawable.bg_shipping_selected)
                    binding.rbExpress.setBackgroundResource(R.drawable.bg_shipping_unselected)
                }
                R.id.rbExpress -> {
                    shippingFee = 12.0
                    binding.rbStandard.setBackgroundResource(R.drawable.bg_shipping_unselected)
                    binding.rbExpress.setBackgroundResource(R.drawable.bg_shipping_selected)
                }
            }
            updateTotalPrice()
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
            navigate(R.id.cartFragment)
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            highlightSelectedTab(R.id.person_id)
            navigate(R.id.profileFragment)
        }
    }

    override fun initData() {
        // Use arguments to support buy-now flow
        val args = arguments
    val buyProductId = args?.getString("buy_now_product_id")
    val buyVariantId = args?.getString("buy_now_variant_id")
    val buyQuantity = args?.getInt("buy_now_quantity") ?: 1
    val cartIndices = args?.getIntegerArrayList("cart_item_indices")
    val cartVariantIds = args?.getStringArrayList("cart_variant_ids")

        // Load products and variants then decide whether we show cart items or buy-now item
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())

        mainRepository.getProducts(token ?: "") { products ->
            activity?.runOnUiThread {
                productMap = products?.associateBy { it.productId } ?: emptyMap()
                android.util.Log.d("CheckoutFragment", "args buyProductId=$buyProductId buyVariantId=$buyVariantId cart_variant_ids=$cartVariantIds cart_indices=$cartIndices")
                android.util.Log.d("CheckoutFragment", "initial productMap.size=${productMap.size}")
                mainRepository.getProductVariants { variants ->
                    activity?.runOnUiThread {
                        variantMap = variants?.associateBy { it.variantId } ?: emptyMap()
                        android.util.Log.d("CheckoutFragment", "variantMap.size=${variantMap.size}")

                        if (!buyProductId.isNullOrBlank() && !buyVariantId.isNullOrBlank()) {
                            // Build a single CartItem for buy-now
                            try {
                                val cartItem = com.pbl6.fitme.model.CartItem(
                                    cartItemId = java.util.UUID.randomUUID(),
                                    addedAt = null,
                                    quantity = buyQuantity,
                                    cartId = java.util.UUID.randomUUID(),
                                    variantId = java.util.UUID.fromString(buyVariantId)
                                )
                                cartItems = listOf(cartItem)
                            } catch (ex: Exception) {
                                cartItems = emptyList()
                            }
                            // Ensure we have product details for the variant's productId; fetch if missing
                            val variant = cartItems.firstOrNull()?.let { variantMap[it.variantId] }
                            val neededProductId = variant?.productId
                            if (neededProductId != null && !productMap.containsKey(neededProductId) && !token.isNullOrBlank()) {
                                // fetch missing product detail
                                mainRepository.getProductById(token, neededProductId.toString()) { fetched ->
                                    activity?.runOnUiThread {
                                        if (fetched != null) {
                                            productMap = productMap + mapOf(fetched.productId to fetched)
                                        }
                                        checkoutProductAdapter = CheckoutProductAdapter(createUnifiedVariantMap(), productMap)
                                        binding.rvCart.adapter = checkoutProductAdapter
                                        checkoutProductAdapter.submitList(cartItems)
                                        val unified = createUnifiedVariantMap()
                                        total = cartItems.sumOf { cartItem ->
                                            val v = unified[cartItem.variantId]
                                            (v?.price ?: 0.0) * cartItem.quantity
                                        }
                                        updateTotalPrice()
                                    }
                                }
                            } else {
                                checkoutProductAdapter = CheckoutProductAdapter(createUnifiedVariantMap(), productMap)
                                binding.rvCart.adapter = checkoutProductAdapter
                                checkoutProductAdapter.submitList(cartItems)
                                dataLoaded = true
                                val unified = createUnifiedVariantMap()
                                total = cartItems.sumOf { cartItem ->
                                    val variant = unified[cartItem.variantId]
                                    (variant?.price ?: 0.0) * cartItem.quantity
                                }
                                updateTotalPrice()
                            }
                        } else if (cartVariantIds != null && cartVariantIds.isNotEmpty()) {
                            // User navigated from Cart -> Checkout with explicit variant ids
                            // Build temporary CartItems from provided variant ids
                            val list = mutableListOf<com.pbl6.fitme.model.CartItem>()
                            val productIdsToFetch = mutableSetOf<java.util.UUID>()
                            cartVariantIds.forEach { vidStr ->
                                try {
                                    val vid = java.util.UUID.fromString(vidStr)
                                    val item = com.pbl6.fitme.model.CartItem(
                                        cartItemId = java.util.UUID.randomUUID(),
                                        addedAt = null,
                                        quantity = 1,
                                        cartId = java.util.UUID.randomUUID(),
                                        variantId = vid
                                    )
                                    list.add(item)
                                    val v = variantMap[vid]
                                    if (v != null && !productMap.containsKey(v.productId)) {
                                        productIdsToFetch.add(v.productId)
                                    }
                                } catch (_: Exception) { }
                            }
                            cartItems = list

                            if (productIdsToFetch.isNotEmpty() && !token.isNullOrBlank()) {
                                // fetch missing products, then set adapter
                                val fetchedList = mutableListOf<com.pbl6.fitme.model.Product>()
                                var remaining = productIdsToFetch.size
                                productIdsToFetch.forEach { pid ->
                                    mainRepository.getProductById(token, pid.toString()) { fetched ->
                                        activity?.runOnUiThread {
                                            if (fetched != null) {
                                                fetchedList.add(fetched)
                                                productMap = productMap + mapOf(fetched.productId to fetched)
                                            }
                                            remaining -= 1
                                            if (remaining <= 0) {
                                                checkoutProductAdapter = CheckoutProductAdapter(createUnifiedVariantMap(), productMap)
                                                binding.rvCart.adapter = checkoutProductAdapter
                                                checkoutProductAdapter.submitList(cartItems)
                                                    dataLoaded = true
                                                val unified = createUnifiedVariantMap()
                                                total = cartItems.sumOf { cartItem ->
                                                    val variant = unified[cartItem.variantId]
                                                    (variant?.price ?: 0.0) * cartItem.quantity
                                                }
                                                updateTotalPrice()
                                            }
                                        }
                                    }
                                }
                            } else {
                                // no missing products or cannot fetch: show what we have
                                checkoutProductAdapter = CheckoutProductAdapter(createUnifiedVariantMap(), productMap)
                                binding.rvCart.adapter = checkoutProductAdapter
                                checkoutProductAdapter.submitList(cartItems)
                                                dataLoaded = true
                                        val unified = createUnifiedVariantMap()
                                        total = cartItems.sumOf { cartItem ->
                                            val variant = unified[cartItem.variantId]
                                            (variant?.price ?: 0.0) * cartItem.quantity
                                        }
                                updateTotalPrice()
                            }
                        } else if (cartIndices != null && cartIndices.isNotEmpty()) {
                            // Backwards compatible: indices provided
                            mainRepository.getCartItems { items ->
                                activity?.runOnUiThread {
                                    val allItems = items ?: emptyList()
                                    val selected = cartIndices.mapNotNull { idx ->
                                        try { allItems[idx] } catch (e: Exception) { null }
                                    }
                                    cartItems = selected
                                    checkoutProductAdapter = CheckoutProductAdapter(createUnifiedVariantMap(), productMap)
                                    binding.rvCart.adapter = checkoutProductAdapter
                                    checkoutProductAdapter.submitList(cartItems)
                                        dataLoaded = true
                                    val unified = createUnifiedVariantMap()
                                    total = cartItems.sumOf { cartItem ->
                                        val variant = unified[cartItem.variantId]
                                        (variant?.price ?: 0.0) * cartItem.quantity
                                    }
                                    updateTotalPrice()
                                }
                            }
                        } else {
                            // No buy-now and no explicit cart indices: show full cart as fallback
                            mainRepository.getCartItems { items ->
                                activity?.runOnUiThread {
                                    cartItems = items ?: emptyList()
                                    checkoutProductAdapter = CheckoutProductAdapter(createUnifiedVariantMap(), productMap)
                                    binding.rvCart.adapter = checkoutProductAdapter
                                    checkoutProductAdapter.submitList(cartItems)
                                        dataLoaded = true
                                    total = cartItems.sumOf { cartItem ->
                                        val variant = variantMap[cartItem.variantId]
                                        (variant?.price ?: 0.0) * cartItem.quantity
                                    }
                                    updateTotalPrice()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateTotalPrice() {
        val finalTotal = total + shippingFee
        binding.txtTotal.text = "Total $${String.format("%.2f", finalTotal)}"
    }

    // Build a unified variant map that includes variants from the global variant endpoint
    // and variants nested inside each Product (from productMap). This ensures we can
    // resolve a variant even if the global variant list didn't include it.
    private fun createUnifiedVariantMap(): Map<java.util.UUID, com.pbl6.fitme.model.ProductVariant> {
        val unified = variantMap.toMutableMap()
        productMap.values.forEach { product ->
            product.variants.forEach { v ->
                unified[v.variantId] = v
            }
        }
        return unified
    }

    private fun highlightSelectedTab(selectedId: Int) {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.filter_id, R.id.cart_id, R.id.person_id)
        ids.forEach { id ->
            val view = requireActivity().findViewById<View>(id)
            if (id == selectedId) {
                view.setBackgroundResource(R.drawable.bg_selected_tab)
            } else {
                view.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            }
        }
    }
}
