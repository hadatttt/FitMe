package com.pbl6.fitme.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dữ liệu mẫu
        val topProducts = listOf("Bag", "Watch", "Shirt", "Shoes")
        val stories = listOf("Story1", "Story2", "Story3")
        val newItems = listOf("Item1", "Item2", "Item3", "Item4")

        // Setup RecyclerView
        setupRecyclerView(view.findViewById(R.id.rvTopProducts), topProducts)
        setupRecyclerView(view.findViewById(R.id.rvStories), stories)
        setupRecyclerView(view.findViewById(R.id.rvNewItems), newItems)
    }

    private fun setupRecyclerView(rv: RecyclerView, data: List<String>) {
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = ProductAdapter(data)
    }
}
