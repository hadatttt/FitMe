package com.pbl6.fitme.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.toolbar.ToolBarFragment

class CartFragment : ToolBarFragment() {

    private lateinit var rvCart: RecyclerView
    private lateinit var emptyView: View
    private lateinit var txtCartTitle: TextView

    private val cartItems = mutableListOf<CartProduct>() // Dữ liệu mẫu
    private lateinit var cartAdapter: CartProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_cart, container, false)

        rvCart = view.findViewById(R.id.rvCart)
        emptyView = view.findViewById(R.id.emptyView)
        txtCartTitle = view.findViewById(R.id.txtCartTitle)

        rvCart.layoutManager = LinearLayoutManager(requireContext())

        cartItems.addAll(
            listOf(
                CartProduct(
                    title = "T-Shirt",
                    detail = "Pink, Size M",
                    price = 17.00,
                    imageResId = R.drawable.ic_splash,
                    quantity = 1
                ),
                CartProduct(
                    title = "Jeans",
                    detail = "Blue, Size L",
                    price = 35.50,
                    imageResId = R.drawable.ic_splash,
                    quantity = 2
                ),
                CartProduct(
                    title = "Sneakers",
                    detail = "White, Size 42",
                    price = 55.00,
                    imageResId = R.drawable.ic_splash,
                    quantity = 1
                )
            )
        )

        cartAdapter = CartProductAdapter(cartItems, object : CartProductAdapter.OnCartActionListener {
            override fun onRemove(position: Int) {
                cartItems.removeAt(position)
                cartAdapter.notifyItemRemoved(position)
                updateCartView()
            }

            override fun onIncrease(position: Int) {
                cartItems[position].quantity++
                cartAdapter.notifyItemChanged(position)
            }

            override fun onDecrease(position: Int) {
                if (cartItems[position].quantity > 1) {
                    cartItems[position].quantity--
                    cartAdapter.notifyItemChanged(position)
                }
            }
        })

        rvCart.adapter = cartAdapter

        setupBottomNavigation(view)
        updateCartView()
        return view
    }

    private fun updateCartView() {
        txtCartTitle.text = "Cart (${cartItems.size})"
        if (cartItems.isEmpty()) {
            rvCart.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            rvCart.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
}
