package com.pbl6.fitme.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import hoang.dqm.codebase.utils.singleClick


class CartFragment : Fragment() {
    private lateinit var txtCartTitle: TextView
    private val cartItems = mutableListOf<CartProduct>()
    private lateinit var rvCart: RecyclerView
    private lateinit var emptyView: View
    private lateinit var cartAdapter: CartProductAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_cart, container, false)


        rvCart = view.findViewById(R.id.rvCart)
        emptyView = view.findViewById(R.id.emptyView)
        txtCartTitle = view.findViewById(R.id.txtCartTitle)

        val editAddress = view.findViewById<ImageView>(R.id.btnEditAddress)
        editAddress.singleClick {
            // Xử lý khi nhấn nút "Edit Address"
        }

        rvCart.layoutManager = LinearLayoutManager(requireContext())


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
                    cartAdapter.notifyItemRangeChanged(position, cartItems.size - position)
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


        rvCart.adapter = cartAdapter
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