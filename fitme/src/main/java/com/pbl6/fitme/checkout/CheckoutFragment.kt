package com.pbl6.fitme.checkout

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentCheckoutBinding
import com.pbl6.fitme.model.*
import com.pbl6.fitme.repository.AddressRepository
import com.pbl6.fitme.repository.CouponRepository
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.repository.UserRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.util.UUID
import kotlin.math.min

class CheckoutFragment : BaseFragment<FragmentCheckoutBinding, CheckoutViewModel>() {
    private var total: Double = 0.0
    private var shippingFee: Double = 0.0
    private var isUsePoints: Boolean = false
    private var currentUserPoints: Int = 0
    private val EXCHANGE_RATE = 25000.0

    private var discountAmount: Double = 0.0
    private var selectedCoupon: Coupon? = null
    private val couponRepository = CouponRepository()
    private val userRepository = UserRepository()
    private lateinit var couponAdapter: CouponAdapter

    private lateinit var checkoutProductAdapter: CheckoutProductAdapter
    private val mainRepository = MainRepository()
    private val addressRepository = AddressRepository()
    private var dataLoaded: Boolean = false

    private var useraddress: ShippingAddress? = null

    private enum class PaymentMethod {
        MOMO, VNPAY, COD
    }

    private var selectedPaymentMethod: PaymentMethod = PaymentMethod.COD

    private var productMap: Map<UUID, Product> = emptyMap()
    private var variantMap: Map<UUID, ProductVariant> = emptyMap()
    private var cartItems: List<CartItem> = emptyList()

    override fun initView() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        highlightSelectedTab(R.id.cart_id)

        binding.rvCart.layoutManager = LinearLayoutManager(requireContext())
        checkoutProductAdapter = CheckoutProductAdapter(emptyMap(), emptyMap())
        binding.rvCart.adapter = checkoutProductAdapter

        couponAdapter = CouponAdapter()
        binding.rvVoucherHorizontal.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvVoucherHorizontal.adapter = couponAdapter

        loadCoupons()
        fetchUserPoints()

        val email = SessionManager.getInstance().getUserEmail(requireContext())
        val token = SessionManager.getInstance().getAccessToken(requireContext()).toString()

        if (!email.isNullOrBlank()) {
            addressRepository.getUserAddresses(token, email) { listAddress ->
                activity?.runOnUiThread {
                    if (!listAddress.isNullOrEmpty()) {
                        val defaultAddress = listAddress.find { it.isDefault } ?: listAddress.first()
                        useraddress = defaultAddress
                        binding.txtShippingAddress.text = "${defaultAddress.addressLine1}, ${defaultAddress.addressLine2}"
                    } else {
                        binding.txtShippingAddress.text = "Please add a shipping address"
                        useraddress = null
                    }
                }
            }
        }

        try {
            val session = SessionManager.getInstance()
            val recipient = session.getRecipientName(requireContext()) ?: ""
            val phone = ""
            val userEmail2 = session.getUserEmail(requireContext()) ?: ""
            if (recipient.isNotBlank()) binding.txtRecipientName.text = "Recipient Name: $recipient"
            if (phone.isNotBlank()) binding.txtPhone.text = "Phone: $phone"
            if (userEmail2.isNotBlank()) binding.txtEmail.text = "Email: $userEmail2"
        } catch (_: Exception) {}

        updatePaymentMethodText()
    }

    private fun fetchUserPoints() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        val userId = SessionManager.getInstance().getUserId(requireContext())?.toString()

        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            userRepository.getUserPoints(token, userId) { points ->
                activity?.runOnUiThread {
                    currentUserPoints = points ?: 0
                    updateTotalPrice()
                }
            }
        }
    }

    private fun loadCoupons() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (!token.isNullOrBlank()) {
            couponRepository.getAllCoupons(token) { coupons ->
                activity?.runOnUiThread {
                    if (coupons != null) {
                        couponAdapter.setList(coupons.filter { it.isActive })
                    }
                }
            }
        }
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.btnEditAddress.singleClick { navigate(R.id.shippingAddressFragment) }
        binding.btnEditContact.singleClick { navigate(R.id.contactInforFragment) }
        binding.btnEditPayment.singleClick { showPaymentMethodDialog() }

        binding.switchUseCoin.setOnCheckedChangeListener { _, isChecked ->
            isUsePoints = isChecked
            updateTotalPrice()
        }

        couponAdapter.setOnClickItemRecyclerView { coupon, _ ->
            checkAndApplyCoupon(coupon)
        }

        binding.btnApplyVoucher.singleClick {
            val code = binding.etVoucherCode.text.toString().trim()
            val token = SessionManager.getInstance().getAccessToken(requireContext())
            if (!token.isNullOrBlank() && code.isNotBlank()) {
                couponRepository.getCouponByCode(token, code) { coupon ->
                    activity?.runOnUiThread {
                        if (coupon != null && coupon.isActive) {
                            checkAndApplyCoupon(coupon)
                        } else {
                            Toast.makeText(requireContext(), "Invalid coupon", Toast.LENGTH_SHORT).show()
                            selectedCoupon = null
                            discountAmount = 0.0
                            updateTotalPrice()
                        }
                    }
                }
            }
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

            val currentAddress = useraddress
            if (currentAddress == null) {
                binding.btnCheckout.isEnabled = true
                Toast.makeText(requireContext(), "Please add/select a shipping address", Toast.LENGTH_LONG).show()
                return@singleClick
            }

            if (binding.txtShippingAddress.text.isNullOrBlank() || useraddress?.addressLine1?.isBlank() == true) {
                Toast.makeText(requireContext(), "Please add a shipping address", Toast.LENGTH_LONG).show()
                return@singleClick
            }

            binding.btnCheckout.isEnabled = false

            val loginResponse = SessionManager.getInstance().getLoginResponse(requireContext())
            val userEmail = loginResponse?.result?.email?.takeIf { !it.isNullOrBlank() }
                ?: SessionManager.getInstance().getUserEmail(requireContext()) ?: ""

            val finalCalculations = calculateFinalTotal()
            val totalAmount = finalCalculations.first

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
                userEmail = userEmail,
                shippingAddressId = "",
                shippingAddress = currentAddress,
                couponCode = selectedCoupon?.code ?: "",
                discountAmount = discountAmount,
                usePoints = isUsePoints,
                orderItems = orderItems,
                subtotal = total,
                totalAmount = totalAmount,
                shippingFee = shippingFee,
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
                        Toast.makeText(requireContext(), "Failed to create order", Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }
                    when (selectedPaymentMethod) {
                        PaymentMethod.MOMO -> {
                            handleMomoPayment(token, createdOrder, totalAmount, userEmail)
                        }
                        PaymentMethod.VNPAY -> {
                            handleVNPayPayment(token, createdOrder, userEmail)
                        }
                        PaymentMethod.COD -> {
                            handleCODPayment()
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

        setupBottomNav()
    }

    private fun checkAndApplyCoupon(coupon: Coupon) {
        if (total < coupon.minimumOrderAmount) {
            Toast.makeText(requireContext(), "Order must be at least $${coupon.minimumOrderAmount}", Toast.LENGTH_LONG).show()
            selectedCoupon = null
            discountAmount = 0.0
            binding.etVoucherCode.setText("")
        } else {
            selectedCoupon = coupon
            binding.etVoucherCode.setText(coupon.code)
            Toast.makeText(requireContext(), "Coupon applied!", Toast.LENGTH_SHORT).show()
        }
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        val (finalTotal, pointDiscountUsd) = calculateFinalTotal()
        val potentialDiscountUsd = currentUserPoints / EXCHANGE_RATE

        binding.txtTotal.text = "Total \$${String.format("%.2f", finalTotal)}"

        if (isUsePoints) {
            val displayedDiscount = if (pointDiscountUsd > 0) pointDiscountUsd else 0.0
            binding.txtCoinBalance.text = "Points applied: -$${String.format("%.2f", displayedDiscount)} (${currentUserPoints} pts)"
            binding.txtCoinBalance.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            binding.txtCoinBalance.text = "Use ${currentUserPoints} points (-$${String.format("%.2f", potentialDiscountUsd)})"
            binding.txtCoinBalance.setTextColor(resources.getColor(hoang.dqm.codebase.R.color.gray_ac, null))
        }

        binding.switchUseCoin.isEnabled = currentUserPoints > 0
        binding.rvCart.visibility = View.VISIBLE
        dataLoaded = true
    }

    private fun calculateFinalTotal(): Pair<Double, Double> {
        discountAmount = 0.0
        selectedCoupon?.let { coupon ->
            if (total >= coupon.minimumOrderAmount) {
                if (coupon.discountType == "PERCENTAGE") {
                    var calcDiscount = total * (coupon.discountValue / 100)
                    if (coupon.maximumDiscountAmount > 0) {
                        calcDiscount = minOf(calcDiscount, coupon.maximumDiscountAmount)
                    }
                    discountAmount = calcDiscount
                } else {
                    discountAmount = coupon.discountValue
                }
            } else {
                selectedCoupon = null
                binding.etVoucherCode.setText("")
                Toast.makeText(requireContext(), "Coupon removed (min spend not met)", Toast.LENGTH_SHORT).show()
            }
        }

        val subTotalAfterCoupon = total - discountAmount
        var pointDiscountUsd = 0.0

        if (isUsePoints && currentUserPoints > 0) {
            val potentialUsd = currentUserPoints / EXCHANGE_RATE
            pointDiscountUsd = minOf(potentialUsd, subTotalAfterCoupon)
        }

        var tempTotal = subTotalAfterCoupon - pointDiscountUsd
        if (tempTotal < 0) tempTotal = 0.0

        val finalTotal = tempTotal + shippingFee
        return Pair(finalTotal, pointDiscountUsd)
    }

    override fun initData() {
        val args = arguments
        val buyProductId = args?.getString("buy_now_product_id")
        val buyVariantId = args?.getString("buy_now_variant_id")
        val buyQuantity = args?.getInt("buy_now_quantity") ?: 1
        val cartIndices = args?.getIntegerArrayList("cart_item_indices")
        val cartVariantIds = args?.getStringArrayList("cart_variant_ids")
        val cartVariantQuantities = args?.getIntegerArrayList("cart_variant_quantities")
        val reorderId = args?.getString("reorder_id")

        val token = SessionManager.getInstance().getAccessToken(requireContext())

        mainRepository.getProducts(token ?: "") { products ->
            activity?.runOnUiThread {
                productMap = products?.associateBy { it.productId } ?: emptyMap()

                mainRepository.getProductVariants(token ?: "") { variants ->
                    activity?.runOnUiThread {
                        variantMap = variants?.associateBy { it.variantId } ?: emptyMap()

                        if (!reorderId.isNullOrBlank()) {
                            if (!token.isNullOrBlank()) {
                                mainRepository.getOrderById(token, reorderId) { oldOrder ->
                                    activity?.runOnUiThread {
                                        if (oldOrder != null) {
                                            val reorderItems = mutableListOf<CartItem>()
                                            val items = if (oldOrder.items.isNotEmpty()) oldOrder.items else oldOrder.orderItems

                                            items.forEach { item ->
                                                try {
                                                    val vIdString = item.variantId
                                                    if (!vIdString.isNullOrBlank()) {
                                                        reorderItems.add(
                                                            CartItem(
                                                                cartItemId = UUID.randomUUID(),
                                                                addedAt = null,
                                                                quantity = item.quantity,
                                                                cartId = UUID.randomUUID(),
                                                                variantId = UUID.fromString(vIdString)
                                                            )
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }

                                            cartItems = reorderItems
                                            loadProductsForCartItems(token, cartItems)
                                        } else {
                                            Toast.makeText(requireContext(), "Could not load order details", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                        else if (!buyProductId.isNullOrBlank() && !buyVariantId.isNullOrBlank()) {
                            try {
                                val cartItem = CartItem(
                                    cartItemId = UUID.randomUUID(), addedAt = null, quantity = buyQuantity,
                                    cartId = UUID.randomUUID(), variantId = UUID.fromString(buyVariantId)
                                )
                                cartItems = listOf(cartItem)
                                loadProductsForCartItems(token, cartItems)
                            } catch (ex: Exception) { cartItems = emptyList() }
                        }
                        else if (cartVariantIds != null && cartVariantIds.isNotEmpty()) {
                            val list = mutableListOf<CartItem>()
                            cartVariantIds.forEachIndexed { index, vidStr ->
                                try {
                                    val vid = UUID.fromString(vidStr)
                                    val qty = cartVariantQuantities?.getOrNull(index) ?: 1
                                    list.add(CartItem(UUID.randomUUID(), null, qty, UUID.randomUUID(), vid))
                                } catch (_: Exception) {}
                            }
                            cartItems = list
                            loadProductsForCartItems(token, cartItems)
                        }
                        else if (cartIndices != null && cartIndices.isNotEmpty()) {
                            val cartId = SessionManager.getInstance().getOrCreateCartId(requireContext()).toString()
                            mainRepository.getCartItems(token ?: "", cartId) { items ->
                                activity?.runOnUiThread {
                                    val allItems = items ?: emptyList()
                                    cartItems = cartIndices.mapNotNull { idx -> allItems.getOrNull(idx) }
                                    loadProductsForCartItems(token, cartItems)
                                }
                            }
                        }
                        else {
                            val cartId = SessionManager.getInstance().getOrCreateCartId(requireContext()).toString()
                            mainRepository.getCartItems(token ?: "", cartId) { items ->
                                activity?.runOnUiThread {
                                    cartItems = items ?: SessionManager.getInstance().getLocalCartItems(requireContext()) ?: emptyList()
                                    loadProductsForCartItems(token, cartItems)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadProductsForCartItems(token: String?, items: List<CartItem>) {
        val productIdsToFetch = items.mapNotNull {
            variantMap[it.variantId]?.productId
        }.filter { !productMap.containsKey(it) }.toSet()

        if (productIdsToFetch.isNotEmpty() && !token.isNullOrBlank()) {
            var remaining = productIdsToFetch.size
            productIdsToFetch.forEach { pid ->
                mainRepository.getProductById(token, pid.toString()) { fetched ->
                    activity?.runOnUiThread {
                        if (fetched != null) productMap = productMap + mapOf(fetched.productId to fetched)
                        remaining -= 1
                        if (remaining <= 0) displayCartItems()
                    }
                }
            }
        } else {
            displayCartItems()
        }
    }

    private fun displayCartItems() {
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

    private fun handleMomoPayment(token: String, createdOrder: Order, totalAmount: Double, userEmail: String?) {
        val amountVndLong = (totalAmount * EXCHANGE_RATE).toLong()
        mainRepository.createMomoPayment(token, amountVndLong, userEmail ?: "", createdOrder.orderId?.toString()) { momoResp ->
            activity?.runOnUiThread {
                binding.btnCheckout.isEnabled = true
                if (momoResp?.payUrl != null) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(momoResp.payUrl))
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Momo failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleVNPayPayment(token: String, createdOrder: Order, userEmail: String?) {
        mainRepository.createVNPayPayment(token, createdOrder.orderId?.toString() ?: "", userEmail ?: "") { vnResp ->
            activity?.runOnUiThread {
                binding.btnCheckout.isEnabled = true
                if (vnResp?.paymentUrl != null) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vnResp.paymentUrl))
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "VNPay failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleCODPayment() {
        binding.btnCheckout.isEnabled = true
        Toast.makeText(requireContext(), "Order placed successfully (COD)", Toast.LENGTH_LONG).show()
        try {
            val bundle = android.os.Bundle()
            bundle.putString("order_status", "confirming")
            navigate(R.id.ordersFragment, bundle)
        } catch (_: Exception) {}
    }

    private fun createUnifiedVariantMap(): Map<UUID, ProductVariant> {
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
                updatePaymentMethodText()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePaymentMethodText() {
        binding.txtPaymentMethod.text = when (selectedPaymentMethod) {
            PaymentMethod.MOMO -> "MOMO"
            PaymentMethod.VNPAY -> "VNPay"
            PaymentMethod.COD -> "Cash on Delivery"
        }
    }

    private fun setupBottomNav() {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.cart_id, R.id.person_id)
        val dests = listOf(R.id.homeFragment, R.id.wishlistFragment, R.id.cartFragment, R.id.profileFragment)
        ids.forEachIndexed { index, id ->
            requireActivity().findViewById<View>(id).singleClick {
                highlightSelectedTab(id)
                navigate(dests[index])
            }
        }
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