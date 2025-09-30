package com.pbl6.fitme.checkout

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.cart.CartProduct
import com.pbl6.fitme.databinding.FragmentCheckoutBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.singleClick

class CheckoutFragment : BaseFragment<FragmentCheckoutBinding, CheckoutViewModel>() {
    private var total: Double = 0.0
    private var shippingFee: Double = 0.0
    private lateinit var checkoutProductAdapter: CheckoutProductAdapter

    override fun initView() {
        // Hiện toolbar
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab cart
        highlightSelectedTab(R.id.cart_id)

        // Setup RecyclerView
        checkoutProductAdapter = CheckoutProductAdapter()
        binding.rvCart.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCart.adapter = checkoutProductAdapter
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
        // Nhận dữ liệu từ CartFragment
        val products = arguments?.getSerializable("cart_items") as? ArrayList<CartProduct> ?: arrayListOf()

        // Đổ dữ liệu vào adapter
        checkoutProductAdapter.submitList(products)

        // Tính tổng tiền sản phẩm
        total = products.sumOf { it.price * it.quantity }

        // Hiển thị tổng cộng (mặc định Standard shipping)
        updateTotalPrice()
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
