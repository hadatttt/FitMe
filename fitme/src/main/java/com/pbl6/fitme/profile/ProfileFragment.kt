package com.pbl6.fitme.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.utils.singleClick

class ProfileFragment : BaseFragment<>() {

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


        // Dữ liệu mẫu
        val topProducts = listOf("Bag", "Watch", "Shirt", "Shoes", "Dress")
        val stories = listOf("Story1", "Story2", "Story3")
        val productList = listOf(
            Product("Bag", "$25.00"),
            Product("Watch", "$17.00"),
            Product("Shirt", "$12.00")
        )

        // Setup RecyclerView
        setupRecyclerViewCatagory(view.findViewById(R.id.rvTopProducts), topProducts)
        setupRecyclerViewCatagory(view.findViewById(R.id.rvStories), stories)
        setupRecyclerViewProduct(view.findViewById(R.id.rvNewItems), productList)
    }

    private fun setupRecyclerViewCatagory(rv: RecyclerView, data: List<String>) {
        rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = CategoryAdapter(data)
    }
    private fun setupRecyclerViewProduct(rv: RecyclerView, data: List<Product>) {
        rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = ProductAdapter(data)
    }
}
