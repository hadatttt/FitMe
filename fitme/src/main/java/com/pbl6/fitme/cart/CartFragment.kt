package com.pbl6.fitme.cart

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentCartBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.navigateWithoutAnimation
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class CartFragment : BaseFragment<FragmentCartBinding, CartViewModel>() {

    private val cartItems = mutableListOf<CartProduct>()
    private lateinit var cartAdapter: CartProductAdapter

    override fun initView() {
        // Hiện toolbar
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab cart
        highlightSelectedTab(R.id.cart_id)

        // Setup RecyclerView
        binding.rvCart.layoutManager = LinearLayoutManager(requireContext())

        // Data mẫu
        cartItems.addAll(
            listOf(
                CartProduct("T-Shirt", "Pink, Size M", 17.00, R.drawable.ic_splash, 1),
                CartProduct("Jeans", "Blue, Size L", 35.50, R.drawable.ic_splash, 2),
                CartProduct("Sneakers", "White, Size 42", 55.00, R.drawable.ic_splash, 1)
            )
        )

        cartAdapter = CartProductAdapter(cartItems, object : CartProductAdapter.OnCartActionListener {
            override fun onRemove(position: Int) {
                if (position in cartItems.indices) {
                    cartItems.removeAt(position)
                    cartAdapter.notifyItemRemoved(position)
                    updateCartView()
                }
            }

            override fun onIncrease(position: Int) {
                if (position in cartItems.indices) {
                    cartItems[position].quantity++
                    cartAdapter.notifyItemChanged(position)
                }
            }

            override fun onDecrease(position: Int) {
                if (position in cartItems.indices) {
                    if (cartItems[position].quantity > 1) {
                        cartItems[position].quantity--
                        cartAdapter.notifyItemChanged(position)
                    } else {
                        onRemove(position)
                    }
                }
            }
        })

        binding.rvCart.adapter = cartAdapter
        updateCartView()
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack(R.id.loginFragment)
        }

        binding.btnEditAddress.singleClick {
            // TODO: Navigate/Edit Address
        }

        requireActivity().findViewById<View>(R.id.home_id).singleClick {
            navigateWithoutAnimation(R.id.homeMainFragment)
        }
        requireActivity().findViewById<View>(R.id.wish_id).singleClick {
            navigateWithoutAnimation(R.id.wishlistFragment)
        }
        requireActivity().findViewById<View>(R.id.filter_id).singleClick {
            highlightSelectedTab(R.id.filter_id)
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            navigateWithoutAnimation(R.id.profileFragment)
        }
    }

    override fun initData() {
        // TODO: Load data từ ViewModel thay vì data mẫu
    }

    private fun updateCartView() {
        binding.txtCartTitle.text = "Cart (${cartItems.size})"
        if (cartItems.isEmpty()) {
            binding.rvCart.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.rvCart.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
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
