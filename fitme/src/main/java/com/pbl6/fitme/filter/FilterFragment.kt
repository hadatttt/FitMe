package com.pbl6.fitme.filter

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentFilterBinding
import com.pbl6.fitme.model.Category
import com.pbl6.fitme.profile.CategoryAdapter
import hoang.dqm.codebase.base.activity.BaseFragment

class FilterFragment : BaseFragment<FragmentFilterBinding, FilterViewModel>() {

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var colorAdapter: ColorAdapter
    private lateinit var sizeAdapter: SizeAdapter
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListener()
        initData()
    }

    override fun initView() {
        hideToolbar()

        val colors = listOf("#FFFFFF", "#000000", "#FF0000", "#00BCD4", "#FFEB3B", "#9C27B0")
        colorAdapter = ColorAdapter(colors)
        binding.rvColors.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = colorAdapter
        }

        val sizes = listOf("XS", "S", "M", "L", "XL", "2XL")
        sizeAdapter = SizeAdapter(sizes) {}
        binding.rvSizes.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSizes.adapter = sizeAdapter

        val greenColor = ColorStateList.valueOf(Color.parseColor("#004CFF"))
        binding.priceRangeSlider.apply {
            trackActiveTintList = greenColor
            thumbTintList = greenColor
            haloTintList = greenColor
            values = listOf(10f, 150f)
        }
        binding.tvPrice.text = "$${binding.priceRangeSlider.values[0].toInt()} — $${binding.priceRangeSlider.values[1].toInt()}"

        categoryAdapter = CategoryAdapter(emptyList())
        binding.rvCategory.apply {
            layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
    }

    override fun initListener() {
        binding.ivClose.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.priceRangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            binding.tvPrice.text = "$${values[0].toInt()} — $${values[1].toInt()}"
        }

        binding.btnClear.setOnClickListener {
            categoryAdapter.clearSelection()
            colorAdapter.clearSelection()
            sizeAdapter.clearSelection()
            binding.radioGroupSort.clearCheck()
            binding.priceRangeSlider.values = listOf(10f, 150f)
            Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show()
        }

        binding.btnApply.setOnClickListener {
            val selectedCategory = categoryAdapter.getSelectedCategory()
            val selectedColor = colorAdapter.getSelectedColor()
            val selectedSize = sizeAdapter.getSelectedSize()
            val priceRange = binding.priceRangeSlider.values

            Toast.makeText(
                requireContext(),
                "Applied filters\nCategory: $selectedCategory\nColor: $selectedColor\nSize: $selectedSize\nPrice: ${priceRange[0]} - ${priceRange[1]}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun initData() {
        mainRepository.getCategories { categories: List<Category>? ->
            if (categories != null) {
                categoryAdapter.updateData(categories)
            } else {
                Toast.makeText(requireContext(), "Không lấy được danh mục", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}