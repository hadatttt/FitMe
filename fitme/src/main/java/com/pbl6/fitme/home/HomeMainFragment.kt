package com.pbl6.fitme.home

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentHomeBinding
import com.pbl6.fitme.profile.*
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigateWithoutAnimation
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class HomeMainFragment : BaseFragment<FragmentHomeBinding, HomeMainViewModel>() {

    override fun initView() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        highlightSelectedTab(R.id.home_id)
        setupRecyclerViews()
    }
    private fun setupRecyclerViews() {
        binding.rvItems.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvItems.adapter = ProductAdapter(getDummyProducts())
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack(R.id.loginFragment)
        }
        binding.ivCamera.singleClick {
        }
        requireActivity().findViewById<View>(R.id.wish_id).singleClick {
            navigateWithoutAnimation(R.id.wishlistFragment, isPop = true)
        }
        requireActivity().findViewById<View>(R.id.filter_id).singleClick {

        }
        requireActivity().findViewById<View>(R.id.cart_id).singleClick {
            navigateWithoutAnimation(R.id.cartFragment, isPop = true)
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            navigateWithoutAnimation(R.id.profileFragment, isPop = true)
        }
    }


    override fun initData() { }
    private fun highlightSelectedTab(selectedId: Int) {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.filter_id, R.id.cart_id, R.id.person_id)
        ids.forEach { id ->
            val view = requireActivity().findViewById<View>(id)
            if (id == selectedId) {
                view.setBackgroundResource(R.drawable.bg_selected_tab)
            } else {
                view.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), android.R.color.transparent)
                )
            }
        }
    }

    private fun getDummyCategories(): List<Category> {
        return listOf(
            Category("Dresses", R.drawable.ic_launcher_foreground),
        Category("Pants", R.drawable.ic_launcher_foreground),
        Category("Skirts", R.drawable.ic_launcher_foreground),
        Category("Shorts", R.drawable.ic_launcher_foreground),
        Category("Jackets", R.drawable.ic_launcher_foreground),
        )
    }

    private fun getDummyProducts(): List<Product> {
        return listOf(
            Product("White Top", 17.0, R.drawable.ic_splash),
            Product("Yellow Set", 25.0, R.drawable.ic_splash),
            Product("Pink Dress", 30.0, R.drawable.ic_splash),
            Product("Shopping Girl", 25.0, R.drawable.ic_splash)
        )
    }
    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}
