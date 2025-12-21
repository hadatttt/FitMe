package com.pbl6.fitme.cart

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentCartBinding
import com.pbl6.fitme.model.CartItem
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.util.UUID

class CartFragment : BaseFragment<FragmentCartBinding, CartViewModel>() {

    private val cartItems = mutableListOf<CartItem>()
    private lateinit var cartAdapter: CartProductAdapter

    // Chỉ dùng MainRepo để lấy thông tin Tên/Ảnh/Giá sản phẩm để hiển thị
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private val addressRepository = com.pbl6.fitme.repository.AddressRepository()

    // Cache thông tin sản phẩm
    private var productMap: MutableMap<UUID, com.pbl6.fitme.model.Product> = mutableMapOf()
    private var variantMap: MutableMap<UUID, com.pbl6.fitme.model.ProductVariant> = mutableMapOf()

    private val selectedVariantIds = mutableSetOf<UUID>()

    override fun initView() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        highlightSelectedTab(R.id.cart_id)

        binding.rvCart.layoutManager = LinearLayoutManager(requireContext())

        // Vẫn lấy token để load địa chỉ (nếu có) và load thông tin sản phẩm
        val token = SessionManager.getInstance().getAccessToken(requireContext())

        // 1. Load Địa chỉ (Nếu user đã login)
        if (!token.isNullOrBlank()) {
            val email = SessionManager.getInstance().getUserEmail(requireContext())
            if (!email.isNullOrBlank()) {
                addressRepository.getUserAddresses(token, email) { listAddress ->
                    activity?.runOnUiThread {
                        if (!listAddress.isNullOrEmpty()) {
                            val defaultAddress = listAddress.find { it.isDefault } ?: listAddress.first()
                            binding.txtShippingAddress.text = "${defaultAddress.addressLine1}, ${defaultAddress.addressLine2}"
                        } else {
                            binding.txtShippingAddress.text = "Please add a shipping address"
                        }
                    }
                }
            }
        } else {
            binding.txtShippingAddress.text = "Login to see address"
        }

        // 2. LOAD CART TỪ LOCAL (Luôn luôn lấy từ local)
        loadCartFromLocal(token ?: "")
    }

    private fun loadCartFromLocal(token: String) {
        // Lấy danh sách ID và số lượng từ máy
        val localItems = SessionManager.getInstance().getLocalCartItems(requireContext())

        cartItems.clear()
        if (localItems != null) {
            cartItems.addAll(localItems)
        }

        // Mặc định chọn tất cả
        selectedVariantIds.clear()
        cartItems.forEach { selectedVariantIds.add(it.variantId) }

        if (cartItems.isEmpty()) {
            setupCartAdapter()
            return
        }

        // Tìm những item chưa có thông tin chi tiết (Tên, Ảnh...)
        val missingVariantIds = cartItems.map { it.variantId }
            .filter { vid -> !variantMap.containsKey(vid) }
            .distinct()

        if (missingVariantIds.isEmpty()) {
            setupCartAdapter()
        } else {
            // Gọi API lấy thông tin chi tiết để hiển thị (KHÔNG PHẢI API CART)
            var remaining = missingVariantIds.size
            missingVariantIds.forEach { vid ->
                mainRepository.getProductByVariantId(token, vid.toString()) { product ->
                    activity?.runOnUiThread {
                        if (product != null) {
                            productMap[product.productId] = product
                            product.variants.find { it.variantId == vid }?.let { v ->
                                variantMap[v.variantId] = v
                            }
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

    private fun setupCartAdapter() {
        if (!::cartAdapter.isInitialized) {
            cartAdapter = CartProductAdapter(
                cartItems,
                variantMap,
                productMap,
                selectedVariantIds,
                object : CartProductAdapter.OnCartActionListener {

                    // Xử lý Checkbox
                    override fun onSelectionChanged(position: Int, isSelected: Boolean) {
                        if (position in cartItems.indices) {
                            val variantId = cartItems[position].variantId
                            if (isSelected) selectedVariantIds.add(variantId)
                            else selectedVariantIds.remove(variantId)
                            updateCartView()
                        }
                    }

                    // Xóa Item -> Xóa Local
                    override fun onRemove(position: Int) {
                        if (position in cartItems.indices) {
                            val itemToRemove = cartItems[position]
                            selectedVariantIds.remove(itemToRemove.variantId)
                            cartItems.removeAt(position)

                            // Lưu danh sách mới xuống Local
                            saveCartToLocal()

                            cartAdapter.notifyItemRemoved(position)
                            cartAdapter.notifyItemRangeChanged(position, cartItems.size)
                            updateCartView()
                        }
                    }

                    // Tăng số lượng -> Lưu Local
                    override fun onIncrease(position: Int) {
                        if (position in cartItems.indices) {
                            val newItem = cartItems[position].copy(quantity = cartItems[position].quantity + 1)
                            cartItems[position] = newItem

                            saveCartToLocal()

                            cartAdapter.notifyItemChanged(position)
                            updateCartView()
                        }
                    }

                    // Giảm số lượng -> Lưu Local
                    override fun onDecrease(position: Int) {
                        if (position in cartItems.indices) {
                            val currentQty = cartItems[position].quantity
                            if (currentQty > 1) {
                                val newItem = cartItems[position].copy(quantity = currentQty - 1)
                                cartItems[position] = newItem

                                saveCartToLocal()

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
        } else {
            cartAdapter.notifyDataSetChanged()
        }
        updateCartView()
    }

    // Hàm lưu danh sách hiện tại vào SharedPreferences
    private fun saveCartToLocal() {
        SessionManager.getInstance().saveLocalCartItems(requireContext(), cartItems)
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

            // Tính tổng tiền dựa trên các item được chọn và giá trong variantMap
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
        val color = if (isEnabled) R.color.maincolor else android.R.color.darker_gray
        binding.btnCheckout.setBackgroundColor(resources.getColor(color, null))
    }

    override fun initListener() {
        onBackPressed { hideToolbar(); popBackStack() }
        try { binding.ivBack.singleClick { hideToolbar(); popBackStack() } } catch (_: Exception) { }

        binding.btnEditAddress.singleClick { navigate(R.id.shippingAddressFragment) }

        binding.btnCheckout.singleClick {
            val selectedItems = cartItems.filter { selectedVariantIds.contains(it.variantId) }
            if (selectedItems.isNotEmpty()) {
                val variantIds = ArrayList(selectedItems.map { it.variantId.toString() })
                val variantQuantities = ArrayList(selectedItems.map { it.quantity })

                val bundle = Bundle().apply {
                    putStringArrayList("cart_variant_ids", variantIds)
                    putIntegerArrayList("cart_variant_quantities", variantQuantities)
                }
                navigate(R.id.checkoutFragment, bundle)
            } else {
                Toast.makeText(context, "Please select items to checkout", Toast.LENGTH_SHORT).show()
            }
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val navIds = mapOf(
            R.id.home_id to R.id.homeFragment,
            R.id.wish_id to R.id.wishlistFragment,
            R.id.cart_id to null,
            R.id.person_id to R.id.profileFragment
        )
        navIds.forEach { (id, dest) ->
            requireActivity().findViewById<View>(id).singleClick {
                highlightSelectedTab(id)
                if (dest != null) navigate(dest)
            }
        }
    }

    override fun initData() { }

    private fun highlightSelectedTab(selectedId: Int) {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.cart_id, R.id.person_id)
        ids.forEach { id ->
            requireActivity().findViewById<View>(id).apply {
                if (id == selectedId) setBackgroundResource(R.drawable.bg_selected_tab)
                else setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            }
        }
    }

    private fun hideToolbar() {
        activity?.findViewById<View>(R.id.toolbar)?.visibility = View.GONE
    }
}