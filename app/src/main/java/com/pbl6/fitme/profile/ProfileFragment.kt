package com.pbl6.fitme.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.toolbar.ToolBarFragment
import com.pbl6.fitme.untils.singleClick

class ProfileFragment : ToolBarFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val setting: ImageView = view.findViewById(R.id.btnSetting)
        val notification: ImageView = view.findViewById(R.id.btnNotification)
        val voucher: ImageView = view.findViewById(R.id.btnVoucher)
        val btnSeeAll = view.findViewById<View>(R.id.iv_see_all_new_items)
        val btnSeeAllNotification = view.findViewById<View>(R.id.iv_see_all_notification)
        val btnMyActivity = view.findViewById<Button>(R.id.btnMyActivity)

        btnMyActivity.singleClick {

        }
        setting.singleClick {

        }
        notification.singleClick {

        }
        voucher.singleClick {

        }
        btnSeeAll.singleClick {

        }
        btnSeeAllNotification.singleClick {

        }

        // Gọi menu chung
        setupBottomNavigation(view)

        // Dữ liệu mẫu
        val topProducts = listOf("Bag", "Watch", "Shirt", "Shoes", "Dress")
        val stories = listOf("Story1", "Story2", "Story3")
        val newItems = listOf("Item1", "Item2", "Item3", "Item4", "Item5", "Item6")

        // Setup RecyclerView
        setupRecyclerView(view.findViewById(R.id.rvTopProducts), topProducts)
        setupRecyclerView(view.findViewById(R.id.rvStories), stories)
        setupRecyclerView(view.findViewById(R.id.rvNewItems), newItems)
    }

    private fun setupRecyclerView(rv: RecyclerView, data: List<String>) {
        rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = ProductAdapter(data)
    }
}
