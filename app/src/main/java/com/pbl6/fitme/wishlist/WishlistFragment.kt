package com.pbl6.fitme.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.toolbar.ToolBarFragment
class WishlistFragment : ToolBarFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wishlist, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Gọi menu chung
        setupBottomNavigation(view)

        // Dữ liệu mẫu
        val sampleItems = listOf(
            WishlistProduct("Bag", "$25.00", "Pink", "M"),
            WishlistProduct("Watch", "$17.00", "Black", "L"),
            WishlistProduct("Shirt", "$12.00", "Blue", "S")
        )
        setupRecyclerView(view.findViewById(R.id.rvWishlist), sampleItems)
    }

    private fun setupRecyclerView(rv: RecyclerView, data: List<WishlistProduct>) {
        rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        rv.adapter = WishlistProductAdapter(data)
    }
}