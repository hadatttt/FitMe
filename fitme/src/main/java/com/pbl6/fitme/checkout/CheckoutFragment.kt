package com.pbl6.fitme.checkout

import android.view.View
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.pbl6.fitme.model.Order
import com.pbl6.fitme.model.OrderItem
import com.pbl6.fitme.session.SessionManager
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.model.CartItem
import com.pbl6.fitme.databinding.FragmentCheckoutBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class CheckoutFragment : BaseFragment<FragmentCheckoutBinding, CheckoutViewModel>() {
    private var total: Double = 0.0
    private var shippingFee: Double = 0.0
    private lateinit var checkoutProductAdapter: CheckoutProductAdapter
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private var dataLoaded: Boolean = false

    private enum class PaymentMethod {
        MOMO, VNPAY, COD
    }

    private var selectedPaymentMethod: PaymentMethod = PaymentMethod.COD

    private var productMap: Map<java.util.UUID, com.pbl6.fitme.model.Product> = emptyMap()
    private var variantMap: Map<java.util.UUID, com.pbl6.fitme.model.ProductVariant> = emptyMap()
    private var cartItems: List<CartItem> = emptyList()

    override fun initView() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        highlightSelectedTab(R.id.cart_id)

        binding.rvCart.layoutManager = LinearLayoutManager(requireContext())
        checkoutProductAdapter = CheckoutProductAdapter(emptyMap(), emptyMap())
        binding.rvCart.adapter = checkoutProductAdapter

        try {
            binding.txtPaymentMethod.text = when (selectedPaymentMethod) {
                PaymentMethod.MOMO -> "MOMO"
                PaymentMethod.VNPAY -> "VNPay"
                PaymentMethod.COD -> "Cash on Delivery"
            }
        } catch (_: Exception) { }
    }

    override fun onResume() {
        super.onResume()
        val hasItems = binding.rvCart.adapter?.itemCount ?: 0
        if (!dataLoaded || hasItems == 0) {
            initData()
        }

        viewModel.getCurrentOrderId()?.let { orderId ->
            val token = SessionManager.getInstance().getAccessToken(requireContext())
            if (!token.isNullOrBlank()) {
                mainRepository.getOrderById(token, orderId.toString()) { order ->
                    activity?.runOnUiThread {
                        if (order != null) {
                            viewModel.clearCurrentOrderId()
                            try {
                                val bundle = android.os.Bundle()
                                bundle.putString("order_id", order.orderId ?: "")
                                navigate(R.id.orderDetailFragment, bundle)
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }
    }

    override fun initListener() {
        binding.btnEditAddress.singleClick {
            navigate(R.id.shippingAddressFragment)
        }
        binding.btnEditContact.singleClick {
            navigate(R.id.contactInforFragment)
        }
        binding.btnEditPayment.singleClick {
            showPaymentMethodDialog()
        }

        binding.btnCheckout.singleClick {
            val token = SessionManager.getInstance().getAccessToken(requireContext())
            if (token.isNullOrBlank()) {
                Toast.makeText(requireContext(), "You must be logged in to checkout", Toast.LENGTH_LONG).show()
                return@singleClick
            }

            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "No items to checkout", Toast.LENGTH_SHORT).show()
                return@singleClick
            }

            if (binding.txtShippingAddress.text.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Please add a shipping address", Toast.LENGTH_LONG).show()
                return@singleClick
            }

            binding.btnCheckout.isEnabled = false

            val loginResponse = SessionManager.getInstance().getLoginResponse(requireContext())
            val userEmail = loginResponse?.result?.email?.takeIf { !it.isNullOrBlank() }
                ?: SessionManager.getInstance().getUserEmail(requireContext())
            if (userEmail.isNullOrBlank()) {
                binding.btnCheckout.isEnabled = true
                Toast.makeText(requireContext(), "Missing user email. Please log in again.", Toast.LENGTH_LONG).show()
                return@singleClick
            }

            val recipientNameFromSession = SessionManager.getInstance().getRecipientName(requireContext())
                ?.takeIf { it.isNotBlank() }
            val fallbackRecipient = SessionManager.getInstance().getUserEmail(requireContext())
                ?.substringBefore("@")
            val recipient = recipientNameFromSession ?: fallbackRecipient ?: "Customer"

            val shippingAddress = com.pbl6.fitme.model.ShippingAddress(
                addressId = "",
                userId = "",
                recipientName = recipient,
                phone = "",
                addressLine1 = binding.txtShippingAddress?.text?.toString() ?: "",
                addressLine2 = "",
                city = "",
                stateProvince = "",
                postalCode = "",
                country = "Vietnam",
                isDefault = false,
                addressType = "SHIPPING"
            )

            if (shippingAddress.addressLine1.isBlank()) {
                binding.btnCheckout.isEnabled = true
                Toast.makeText(requireContext(), "Please add a shipping address", Toast.LENGTH_LONG).show()
                return@singleClick
            }

            val unified = createUnifiedVariantMap()
            val orderItems = cartItems.map { ci ->
                val v = unified[ci.variantId]
                val price = v?.price ?: 0.0
                OrderItem(
                    productId = v?.productId?.toString() ?: "",
                    quantity = ci.quantity,
                    unitPrice = price,
                    color = v?.color,
                    size = v?.size,
                    variantId = null,
                    productName = null,
                    variantDetails = null,
                    totalPrice = null,
                    productImageUrl = null
                )
            }

            val subtotal = total
            val shippingMoney = shippingFee
            val discount = 0.0
            val totalAmount = total + shippingFee

            val order = Order(
                userEmail = userEmail,
                shippingAddressId = "",
                shippingAddress = shippingAddress,
                couponId = "",
                orderItems = orderItems,
                subtotal = subtotal,
                totalAmount = totalAmount,
                discountAmount = discount,
                shippingFee = shippingMoney,
                orderNotes = ""
            )

            mainRepository.createOrder(token, order) { createdOrder ->
                activity?.runOnUiThread {
                    if (createdOrder == null) {
                        binding.btnCheckout.isEnabled = true
                        Toast.makeText(requireContext(), "Failed to create order", Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }

                    when (selectedPaymentMethod) {
                        PaymentMethod.MOMO -> {

                        }

                        PaymentMethod.VNPAY -> {
                            mainRepository.createVNPayPayment(
                                token,
                                createdOrder.orderId?.toString() ?: "",
                                userEmail
                            ) { vnResp ->
                                activity?.runOnUiThread {
                                    binding.btnCheckout.isEnabled = true
                                    if (vnResp?.paymentUrl.isNullOrBlank()) {
                                        Toast.makeText(requireContext(), "Failed to start VNPay", Toast.LENGTH_LONG).show()
                                    } else {
                                        try {
                                            val uuid = createdOrder.orderId?.let { java.util.UUID.fromString(it) }
                                            viewModel.setCurrentOrderId(uuid)
                                        } catch (_: Exception) { }

                                        // Navigate to in-app WebView for VNPay instead of external browser
                                        try {
                                            val bundle = android.os.Bundle()
                                            bundle.putString("payment_url", vnResp!!.paymentUrl)
                                            bundle.putString("order_id", createdOrder.orderId?.toString() ?: "")
                                            navigate(R.id.paymentWebViewFragment, bundle)
                                        } catch (ex: Exception) {
                                            // Fallback to external browser if navigation fails
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vnResp!!.paymentUrl))
                                            startActivity(intent)
                                        }
                                    }
                                }
                            }
                        }

                        PaymentMethod.COD -> {
                            binding.btnCheckout.isEnabled = true
                            Toast.makeText(requireContext(), "Order placed successfully (COD)", Toast.LENGTH_LONG).show()
                            try {
                                val bundle = android.os.Bundle()
                                bundle.putString("order_status", "confirming") // Trạng thái pending/confirming
                                navigate(R.id.ordersFragment, bundle)
                            } catch (e: Exception) {
                                android.util.Log.e("CheckoutFragment", "Failed to navigate to orders list", e)
                            }
                        }
                    }
                }
            }
        }

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

        // Toolbar navigation
        val tabIds = listOf(
            R.id.home_id to R.id.homeFragment,
            R.id.wish_id to R.id.wishlistFragment,
            R.id.filter_id to R.id.filterFragment,
            R.id.cart_id to R.id.cartFragment,
            R.id.person_id to R.id.profileFragment
        )

        tabIds.forEach { (tabId, dest) ->
            requireActivity().findViewById<View>(tabId).singleClick {
                highlightSelectedTab(tabId)
                navigate(dest)
            }
        }

        // Back arrow in toolbar
        try {
            binding.ivBack.singleClick {
                // Go back to previous screen and hide toolbar
                val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
                toolbar.visibility = View.GONE
                popBackStack()
            }
        } catch (_: Exception) { }
    }

    override fun initData() {
        // Use arguments to support buy-now flow
        val args = arguments
        val buyProductId = args?.getString("buy_now_product_id")
        val buyVariantId = args?.getString("buy_now_variant_id")
        val buyQuantity = args?.getInt("buy_now_quantity") ?: 1
        val cartIndices = args?.getIntegerArrayList("cart_item_indices")
        val cartVariantIds = args?.getStringArrayList("cart_variant_ids")
        val cartVariantQuantities = args?.getIntegerArrayList("cart_variant_quantities")

        // Load products and variants then decide whether we show cart items or buy-now item
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())

        mainRepository.getProducts(token ?: "") { products ->
            activity?.runOnUiThread {
                productMap = products?.associateBy { it.productId } ?: emptyMap()
                android.util.Log.d("CheckoutFragment", "args buyProductId=$buyProductId buyVariantId=$buyVariantId cart_variant_ids=$cartVariantIds cart_indices=$cartIndices")
                android.util.Log.d("CheckoutFragment", "initial productMap.size=${productMap.size}")
                mainRepository.getProductVariants(token ?: "") { variants ->
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
                            val list = mutableListOf<com.pbl6.fitme.model.CartItem>()
                            val productIdsToFetch = mutableSetOf<java.util.UUID>()
                            cartVariantIds.forEach { vidStr ->
                                try {
                                    val vid = java.util.UUID.fromString(vidStr)
                                    val qty = cartVariantQuantities?.getOrNull(list.size) ?: 1
                                    val item = com.pbl6.fitme.model.CartItem(
                                        cartItemId = java.util.UUID.randomUUID(),
                                        addedAt = null,
                                        quantity = qty,
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
                            val cartId = com.pbl6.fitme.session.SessionManager.getInstance().getOrCreateCartId(requireContext()).toString()
                            mainRepository.getCartItems(token ?: "", cartId) { items ->
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
                            val cartId = com.pbl6.fitme.session.SessionManager.getInstance().getOrCreateCartId(requireContext()).toString()
                            mainRepository.getCartItems(token ?: "", cartId) { items ->
                                activity?.runOnUiThread {
                                    // If server cart items are unavailable, fall back to local items
                                    val fallback = com.pbl6.fitme.session.SessionManager.getInstance().getLocalCartItems(requireContext())
                                    cartItems = items ?: (fallback ?: emptyList())
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
        binding.rvCart.visibility = View.VISIBLE
        dataLoaded = true
    }

    private fun createUnifiedVariantMap(): Map<java.util.UUID, com.pbl6.fitme.model.ProductVariant> {
        val unified = variantMap.toMutableMap()
        productMap.values.forEach { product ->
            product.variants.forEach { v ->
                unified[v.variantId] = v
            }
        }
        return unified
    }

    private fun showPaymentMethodDialog() {
        val options = arrayOf("Card", "VNPay", "Cash on Delivery")
        val checkedItem = when (selectedPaymentMethod) {
            PaymentMethod.MOMO -> 0
            PaymentMethod.VNPAY -> 1
            PaymentMethod.COD -> 2
        }

        var tempSelection = selectedPaymentMethod

        AlertDialog.Builder(requireContext())
            .setTitle("Select payment method")
            .setSingleChoiceItems(options, checkedItem) { _, which ->
                tempSelection = when (which) {
                    0 -> PaymentMethod.MOMO
                    1 -> PaymentMethod.VNPAY
                    else -> PaymentMethod.COD
                }
            }
            .setPositiveButton("OK") { dialog, _ ->
                selectedPaymentMethod = tempSelection
                binding.txtPaymentMethod.text = when (selectedPaymentMethod) {
                    PaymentMethod.MOMO -> "MOMO"
                    PaymentMethod.VNPAY -> "VNPay"
                    PaymentMethod.COD -> "Cash on Delivery"
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
