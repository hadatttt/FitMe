package com.pbl6.fitme.filter

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentFilterBinding
import com.pbl6.fitme.model.Category
import com.pbl6.fitme.profile.CategoryAdapter

class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var colorAdapter: ColorAdapter
    private lateinit var sizeAdapter: SizeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryRecycler()
        setupColorRecycler()
        setupSizeRecycler()
        setupPriceRange()
        setupButtons()
        hideToolbar()
        binding.ivClose.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private val mainRepository = com.pbl6.fitme.repository.MainRepository()

    private fun setupCategoryRecycler() {
        mainRepository.getCategories { categories: List<Category>? ->
            if (categories != null) {
                categoryAdapter = CategoryAdapter(categories)
                binding.rvCategory.apply {
                    layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false)
                    adapter = categoryAdapter
                }
            } else {
                Toast.makeText(requireContext(), "Không lấy được danh mục", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupColorRecycler() {
        val colors = listOf(
            "#FFFFFF", "#000000", "#FF0000", "#00BCD4", "#FFEB3B", "#9C27B0"
        )
        colorAdapter = ColorAdapter(colors)
        binding.rvColors.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = colorAdapter
        }
    }
    private fun setupSizeRecycler() {
        val sizes = listOf("XS", "S", "M", "L", "XL", "2XL")

        sizeAdapter = SizeAdapter(sizes) { selectedSize ->
            //Toast.makeText(requireContext(), "Chọn size: $selectedSize", Toast.LENGTH_SHORT).show()
        }

        binding.rvSizes.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSizes.adapter = sizeAdapter
    }

    private fun setupPriceRange() {
        binding.priceRangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            binding.tvPrice.text = "$${values[0].toInt()} — $${values[1].toInt()}"
        }
        val greenColor = ColorStateList.valueOf(Color.parseColor("#004CFF")) // Màu main
        binding.priceRangeSlider.trackActiveTintList = greenColor
        binding.priceRangeSlider.thumbTintList = greenColor
        binding.priceRangeSlider.haloTintList = greenColor    }

    private fun setupButtons() {
        binding.btnClear.setOnClickListener {
            // reset filter
            categoryAdapter.clearSelection()
            colorAdapter.clearSelection()
            sizeAdapter.clearSelection()
            binding.radioGroupSort.clearCheck()
            binding.radioGroupPrice.clearCheck()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}
