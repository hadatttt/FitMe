package com.pbl6.fitme.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.cart.CartProductAdapter
import com.pbl6.fitme.toolbar.ToolBarFragment
class WishlistFragment : ToolBarFragment() {

    private lateinit var adapter: WishlistProductAdapter
    private lateinit var txtTitle: TextView
    private lateinit var emptyView: View
    private lateinit var rvWishlist: RecyclerView
    private val wishlistItems = mutableListOf<WishlistProduct>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wishlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomNavigation(view)

        txtTitle = view.findViewById(R.id.txtWishlist)
        emptyView = view.findViewById(R.id.emptyView_wl)
        rvWishlist = view.findViewById(R.id.rvWishlist)

        // Dữ liệu mẫu
        wishlistItems.addAll(
            listOf(
                WishlistProduct("Bag", "$25.00", "Pink", "M"),
                WishlistProduct("Watch", "$17.00", "Black", "L"),
                WishlistProduct("Shirt", "$12.00", "Blue", "S")
            )
        )
        adapter = WishlistProductAdapter(wishlistItems, object :
            WishlistProductAdapter.OnWishlistActionListener {
            override fun onRemove(position: Int) {
                if (position in wishlistItems.indices) {
                    wishlistItems.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    adapter.notifyItemRangeChanged(position, wishlistItems.size - position)
                    updateWishlistView()
                }
            }

            override fun onAddToCart(position: Int) {
                // TODO: Thêm vào giỏ hàng
            }
        })

        rvWishlist.adapter = adapter
        setupBottomNavigation(view)
        updateWishlistView()
    }


    private fun updateWishlistView() {
        txtTitle.text = "Wishlist (${wishlistItems.size})"
        if (wishlistItems.isEmpty()) {
            rvWishlist.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            rvWishlist.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
}