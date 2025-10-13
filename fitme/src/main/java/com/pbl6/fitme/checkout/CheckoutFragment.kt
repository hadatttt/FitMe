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
    private val mainRepository = com.pbl6.fitme.repository.MainRepository

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

    override fun initListener() {
        // Nút Pay
        binding.btnEditAddress.singleClick {

        }
        binding.btnEditContact.singleClick {

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
        // Lấy dữ liệu từ MainRepository
        mainRepository.getProducts { products ->
            productMap = products?.associateBy { it.productId } ?: emptyMap()
            mainRepository.getProductVariants { variants ->
                variantMap = variants?.associateBy { it.variantId } ?: emptyMap()
                mainRepository.getCartItems { items ->
                    cartItems = items ?: emptyList()
                    checkoutProductAdapter = CheckoutProductAdapter(variantMap, productMap)
                    binding.rvCart.adapter = checkoutProductAdapter
                    checkoutProductAdapter.submitList(cartItems)
                    total = cartItems.sumOf { cartItem ->
                        val variant = variantMap[cartItem.variantId]
                        (variant?.price ?: 0.0) * cartItem.quantity
                    }
                    updateTotalPrice()
                }
            }
        }
    }

    private fun updateTotalPrice() {
        val finalTotal = total + shippingFee
        binding.txtTotal.text = "Total $${String.format("%.2f", finalTotal)}"
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
