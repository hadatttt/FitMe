package com.pbl6.fitme.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.profile.ProductAdapter
import com.pbl6.fitme.toolbar.ToolBarFragment
import com.pbl6.fitme.untils.singleClick

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
        val topProducts = listOf("Bag", "Watch", "Shirt", "Shoes", "Dress")
        val newItems = listOf("Item1", "Item2", "Item3", "Item4", "Item5", "Item6")

        // Setup RecyclerView
        setupRecyclerView(view.findViewById(R.id.rvRecentlyViewed), topProducts)
        setupRecyclerView(view.findViewById(R.id.rvWishlist), newItems)
    }

    private fun setupRecyclerView(rv: RecyclerView, data: List<String>) {
        rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = ProductAdapter(data)
    }
}