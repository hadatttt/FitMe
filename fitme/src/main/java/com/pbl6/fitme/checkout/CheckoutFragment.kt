package com.pbl6.fitme.checkout

import android.view.View
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.bumptech.glide.Glide
import com.pbl6.fitme.model.Order
import com.pbl6.fitme.model.OrderItem
import com.pbl6.fitme.session.SessionManager
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.model.CartItem
import com.pbl6.fitme.databinding.FragmentCheckoutBinding
import com.pbl6.fitme.model.ShippingAddress
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.util.UUID

class CheckoutFragment : BaseFragment<FragmentCheckoutBinding, CheckoutViewModel>() {
    private var total: Double = 0.0
    private var shippingFee: Double = 0.0
    private lateinit var checkoutProductAdapter: CheckoutProductAdapter
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private val addressRepository = com.pbl6.fitme.repository.AddressRepository()
    private var dataLoaded: Boolean = false

    // FIX: Đổi từ lateinit sang nullable để tránh crash nếu chưa load xong
    private var useraddress: ShippingAddress? = null

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

        val email = SessionManager.getInstance().getUserEmail(requireContext())
        val token = SessionManager.getInstance().getAccessToken(requireContext()).toString()

        if (!email.isNullOrBlank()) {
            addressRepository.getUserAddresses(token, email) { listAddress ->
                activity?.runOnUiThread {
                    if (!listAddress.isNullOrEmpty()) {
                        val defaultAddress =
                            listAddress.find { it.isDefault } ?: listAddress.first()
                        useraddress = defaultAddress
                        binding.txtShippingAddress.text =
                            "${defaultAddress.addressLine1}, ${defaultAddress.addressLine2}"
                    } else {
                        binding.txtShippingAddress.text = "Please add a shipping address"
                        useraddress = null
                    }
                }
            }
        }
        try {
            binding.txtPaymentMethod.text = when (selectedPaymentMethod) {
                PaymentMethod.MOMO -> "MOMO"
                PaymentMethod.VNPAY -> "VNPay"
                PaymentMethod.COD -> "Cash on Delivery"
            }
        } catch (_: Exception) {
        }
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
                            } catch (_: Exception) {
                            }
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
                Toast.makeText(
                    requireContext(),
                    "You must be logged in to checkout",
                    Toast.LENGTH_LONG
                ).show()
                return@singleClick
            }

            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "No items to checkout", Toast.LENGTH_SHORT).show()
                return@singleClick
            }

            // FIX: Kiểm tra useraddress null trước khi truy cập thuộc tính của nó
            val currentAddress = useraddress
            if (currentAddress == null) {
                binding.btnCheckout.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Please add/select a shipping address",
                    Toast.LENGTH_LONG
                ).show()
                // Thử load lại địa chỉ nếu chưa có
                val email = SessionManager.getInstance().getUserEmail(requireContext())
                if (!email.isNullOrBlank()) {
                    addressRepository.getUserAddresses(token, email) { listAddress ->
                        activity?.runOnUiThread {
                            if (!listAddress.isNullOrEmpty()) {
                                useraddress =
                                    listAddress.find { it.isDefault } ?: listAddress.first()
                                binding.txtShippingAddress.text =
                                    "${useraddress?.addressLine1}, ${useraddress?.addressLine2}"
                            }
                        }
                    }
                }
                return@singleClick
            }

            if (binding.txtShippingAddress.text.isNullOrBlank() || currentAddress.addressLine1.isBlank()) {
                Toast.makeText(requireContext(), "Please add a shipping address", Toast.LENGTH_LONG)
                    .show()
                return@singleClick
            }

            binding.btnCheckout.isEnabled = false

            val loginResponse = SessionManager.getInstance().getLoginResponse(requireContext())
            val userEmail = loginResponse?.result?.email?.takeIf { !it.isNullOrBlank() }
                ?: SessionManager.getInstance().getUserEmail(requireContext())

            val subtotal = total
            val shippingMoney = shippingFee
            val discount = 0.0
            val rawTotal = total + shippingFee

            val totalAmount = try {
                if (selectedPaymentMethod == PaymentMethod.VNPAY) {
                    val bd = java.math.BigDecimal(rawTotal.toString())
                    val rounded = bd.setScale(0, java.math.RoundingMode.HALF_UP).toDouble()
                    if (rounded != rawTotal) {
                        activity?.runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Total rounded to \$${String.format("%.0f", rounded)} for VNPay",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    rounded
                } else {
                    rawTotal
                }
            } catch (ex: Exception) {
                rawTotal
            }

            // Build order items from cart items (convert variant/product IDs to strings)
            val orderItems = cartItems.map { cartItem ->
                val unified = createUnifiedVariantMap()
                val variant = unified[cartItem.variantId]
                val product = variant?.let { productMap[it.productId] }
                com.pbl6.fitme.model.OrderItem(
                    productId = product?.productId?.toString(),
                    quantity = cartItem.quantity,
                    unitPrice = variant?.price ?: 0.0,
                    color = variant?.color,
                    size = variant?.size
                )
            }

            val order = Order(
                userEmail = userEmail?:"",
                shippingAddressId = "",
                shippingAddress = currentAddress, // Sử dụng biến safe đã check null
                couponId = "",
                orderItems = orderItems,
                subtotal = subtotal,
                totalAmount = totalAmount,
                discountAmount = discount,
                shippingFee = shippingMoney,
                orderNotes = "",
                paymentMethod = when (selectedPaymentMethod) {
                    PaymentMethod.MOMO -> "MOMO"
                    PaymentMethod.VNPAY -> "VNPAY"
                    PaymentMethod.COD -> "COD"
                }
            )

            mainRepository.createOrder(token, order) { createdOrder ->
                activity?.runOnUiThread {
                    if (createdOrder == null) {
                        binding.btnCheckout.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            "Failed to create order",
                            Toast.LENGTH_LONG
                        ).show()
                        return@runOnUiThread
                    }
                    when (selectedPaymentMethod) {
                        PaymentMethod.MOMO -> {
                            val exchangeRate = 25000L
                            val amountVndLong = try {
                                java.math.BigDecimal(totalAmount.toString())
                                    .multiply(java.math.BigDecimal.valueOf(exchangeRate))
                                    .setScale(0, java.math.RoundingMode.HALF_UP)
                                    .longValueExact()
                            } catch (ex: Exception) {
                                (totalAmount * exchangeRate).toLong()
                            }

                            mainRepository.createMomoPayment(
                                token,
                                amountVndLong,
                                userEmail ?: "",
                                createdOrder.orderId?.toString()
                            ) { momoResp ->
                                activity?.runOnUiThread {
                                    android.util.Log.d("CheckoutFragment", "MomoResponse: $momoResp")
                                    binding.btnCheckout.isEnabled = true
                                    if (momoResp == null) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Failed to start Momo payment",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@runOnUiThread
                                    }

                                    if (momoResp.resultCode != null && momoResp.resultCode != 0) {
                                        val msg = momoResp.message
                                            ?: "Momo payment failed (code=${momoResp.resultCode})"
                                        try {
                                            AlertDialog.Builder(requireContext())
                                                .setTitle("Momo Payment")
                                                .setMessage(msg)
                                                .setPositiveButton("OK", null)
                                                .show()
                                        } catch (_: Exception) {
                                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG)
                                                .show()
                                        }
                                        return@runOnUiThread
                                    }

                                    try {
                                        val uuid =
                                            createdOrder.orderId?.let { java.util.UUID.fromString(it) }; viewModel.setCurrentOrderId(
                                            uuid
                                        )
                                    } catch (_: Exception) {
                                    }

                                    val deeplink = momoResp.deeplink
                                    val payUrl = momoResp.payUrl
                                    val qrCodeUrl = momoResp.qrCodeUrl

                                    // Prefer payUrl (HTML) so we can inject QR into the page when available
                                    if (!payUrl.isNullOrBlank()) {
                                        try {
                                            val bundle = android.os.Bundle()
                                            bundle.putString("payment_url", payUrl)
                                            bundle.putString("order_id", createdOrder.orderId?.toString() ?: "")
                                            bundle.putString("payment_method", "MOMO")
                                            if (!qrCodeUrl.isNullOrBlank()) {
                                                bundle.putString("qr_url", qrCodeUrl)
                                                bundle.putString("amount_text", "Amount (VND): ${amountVndLong}")
                                            }
                                            navigate(R.id.paymentWebViewFragment, bundle)
                                        } catch (ex: Exception) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(payUrl))
                                                startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(requireContext(), "Unable to open payment link", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else if (!qrCodeUrl.isNullOrBlank()) {
                                        try {
                                            // Navigate to PaymentWebViewFragment and let it inject the QR into the page
                                            val bundle = android.os.Bundle()
                                            bundle.putString("payment_url", "about:blank")
                                            bundle.putString("qr_url", qrCodeUrl)
                                            bundle.putString("amount_text", "Amount (VND): ${amountVndLong}")
                                            bundle.putString("order_id", createdOrder.orderId?.toString() ?: "")
                                               bundle.putString("payment_method", "MOMO")
                                            navigate(R.id.paymentWebViewFragment, bundle)
                                        } catch (ex: Exception) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrCodeUrl))
                                                startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(requireContext(), "Unable to open payment link", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else if (!deeplink.isNullOrBlank()) {
                                        if (deeplink.startsWith("http")) {
                                            try {
                                                val bundle = android.os.Bundle()
                                                bundle.putString("payment_url", deeplink)
                                                bundle.putString("order_id", createdOrder.orderId?.toString() ?: "")
                                                   bundle.putString("payment_method", "MOMO")
                                                navigate(R.id.paymentWebViewFragment, bundle)
                                            } catch (ex: Exception) {
                                                Toast.makeText(requireContext(), "Unable to open in-app payment. Please try again.", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            try {
                                                AlertDialog.Builder(requireContext())
                                                    .setTitle("Open Momo App")
                                                    .setMessage("This payment will open the Momo app. Continue?")
                                                    .setPositiveButton("Open") { _, _ ->
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deeplink))
                                                            startActivity(intent)
                                                        } catch (ex: Exception) {
                                                            Toast.makeText(requireContext(), "Unable to open external app", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                    .setNegativeButton("Cancel", null)
                                                    .show()
                                            } catch (ex: Exception) {
                                                Toast.makeText(requireContext(), "Unable to open external app", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(requireContext(), "Momo returned no usable payment link", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }

                        PaymentMethod.VNPAY -> {
                            mainRepository.createVNPayPayment(
                                token,
                                createdOrder.orderId?.toString() ?: "",
                                userEmail ?: ""
                            ) { vnResp ->
                                activity?.runOnUiThread {
                                    binding.btnCheckout.isEnabled = true
                                    if (vnResp?.paymentUrl.isNullOrBlank()) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Failed to start VNPay",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        try {
                                            val uuid =
                                                createdOrder.orderId?.let {
                                                    java.util.UUID.fromString(
                                                        it
                                                    )
                                                }
                                            viewModel.setCurrentOrderId(uuid)
                                        } catch (_: Exception) {
                                        }

                                        try {
                                            val bundle = android.os.Bundle()
                                            bundle.putString("payment_url", vnResp?.paymentUrl ?: "")
                                            bundle.putString(
                                                "order_id",
                                                createdOrder.orderId?.toString() ?: ""
                                            )
                                            bundle.putString("payment_method", "VNPAY")
                                            navigate(R.id.paymentWebViewFragment, bundle)
                                        } catch (ex: Exception) {
                                            val intent =
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(vnResp!!.paymentUrl)
                                                )
                                            startActivity(intent)
                                        }
                                    }
                                }
                            }
                        }

                        PaymentMethod.COD -> {
                            // For COD we rely on server to create the Payment record when the order is created.
                            binding.btnCheckout.isEnabled = true
                            Toast.makeText(
                                requireContext(),
                                "Order placed successfully (COD)",
                                Toast.LENGTH_LONG
                            ).show()
                            try {
                                val bundle = android.os.Bundle()
                                bundle.putString("order_status", "confirming")
                                navigate(R.id.ordersFragment, bundle)
                            } catch (e: Exception) {
                                android.util.Log.e(
                                    "CheckoutFragment",
                                    "Failed to navigate to orders list",
                                    e
                                )
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

            val tabIds = listOf(
                R.id.home_id to R.id.homeFragment,
                R.id.wish_id to R.id.wishlistFragment,
                R.id.cart_id to R.id.cartFragment,
                R.id.person_id to R.id.profileFragment
            )

            tabIds.forEach { (tabId, dest) ->
                requireActivity().findViewById<View>(tabId).singleClick {
                    highlightSelectedTab(tabId)
                    navigate(dest)
                }
            }

            try {
                binding.ivBack.singleClick {
                    val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
                    toolbar.visibility = View.GONE
                    popBackStack()
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun initData() {
        val args = arguments
        val buyProductId = args?.getString("buy_now_product_id")
        val buyVariantId = args?.getString("buy_now_variant_id")
        val buyQuantity = args?.getInt("buy_now_quantity") ?: 1
        val cartIndices = args?.getIntegerArrayList("cart_item_indices")
        val cartVariantIds = args?.getStringArrayList("cart_variant_ids")
        val cartVariantQuantities = args?.getIntegerArrayList("cart_variant_quantities")

        val token = SessionManager.getInstance().getAccessToken(requireContext())

        mainRepository.getProducts(token ?: "") { products ->
            activity?.runOnUiThread {
                productMap = products?.associateBy { it.productId } ?: emptyMap()

                mainRepository.getProductVariants(token ?: "") { variants ->
                    activity?.runOnUiThread {
                        variantMap = variants?.associateBy { it.variantId } ?: emptyMap()

                        if (!buyProductId.isNullOrBlank() && !buyVariantId.isNullOrBlank()) {
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

                            val variant = cartItems.firstOrNull()?.let { variantMap[it.variantId] }
                            val neededProductId = variant?.productId
                            if (neededProductId != null && !productMap.containsKey(neededProductId) && !token.isNullOrBlank()) {
                                mainRepository.getProductById(
                                    token,
                                    neededProductId.toString()
                                ) { fetched ->
                                    activity?.runOnUiThread {
                                        if (fetched != null) {
                                            productMap =
                                                productMap + mapOf(fetched.productId to fetched)
                                        }
                                        checkoutProductAdapter = CheckoutProductAdapter(
                                            createUnifiedVariantMap(),
                                            productMap
                                        )
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
                                checkoutProductAdapter =
                                    CheckoutProductAdapter(createUnifiedVariantMap(), productMap)
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
                                } catch (_: Exception) {
                                }
                            }
                            cartItems = list

                            if (productIdsToFetch.isNotEmpty() && !token.isNullOrBlank()) {
                                var remaining = productIdsToFetch.size
                                productIdsToFetch.forEach { pid ->
                                    mainRepository.getProductById(
                                        token,
                                        pid.toString()
                                    ) { fetched ->
                                        activity?.runOnUiThread {
                                            if (fetched != null) {
                                                productMap =
                                                    productMap + mapOf(fetched.productId to fetched)
                                            }
                                            remaining -= 1
                                            if (remaining <= 0) {
                                                checkoutProductAdapter = CheckoutProductAdapter(
                                                    createUnifiedVariantMap(),
                                                    productMap
                                                )
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
                                checkoutProductAdapter =
                                    CheckoutProductAdapter(createUnifiedVariantMap(), productMap)
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
                            val cartId = SessionManager.getInstance()
                                .getOrCreateCartId(requireContext()).toString()
                            mainRepository.getCartItems(token ?: "", cartId) { items ->
                                activity?.runOnUiThread {
                                    val allItems = items ?: emptyList()
                                    val selected = cartIndices.mapNotNull { idx ->
                                        try {
                                            allItems[idx]
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    cartItems = selected
                                    checkoutProductAdapter = CheckoutProductAdapter(
                                        createUnifiedVariantMap(),
                                        productMap
                                    )
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
                            val cartId = SessionManager.getInstance()
                                .getOrCreateCartId(requireContext()).toString()
                            mainRepository.getCartItems(token ?: "", cartId) { items ->
                                activity?.runOnUiThread {
                                    val fallback = SessionManager.getInstance()
                                        .getLocalCartItems(requireContext())
                                    cartItems = items ?: (fallback ?: emptyList())
                                    checkoutProductAdapter = CheckoutProductAdapter(
                                        createUnifiedVariantMap(),
                                        productMap
                                    )
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
        binding.txtTotal.text = "Total \$${String.format("%.2f", finalTotal)}"
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
        val options = arrayOf("Momo", "VNPay", "Cash on Delivery")
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
}