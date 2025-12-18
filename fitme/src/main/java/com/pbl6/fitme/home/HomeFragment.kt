package com.pbl6.fitme.home

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.checkin.CheckInDialogFragment
import com.pbl6.fitme.databinding.FragmentHomeBinding
import com.pbl6.fitme.model.Category
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.profile.CategoryAdapter
import com.pbl6.fitme.profile.ProductAdapter
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.session.SessionManager
import com.pbl6.fitme.untils.AppSharePref
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.setDraggableWithClick
import hoang.dqm.codebase.utils.singleClick
import java.util.Locale

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeMainViewModel>() {

    private val mainRepository = MainRepository()
    private var allProducts: List<Product> = emptyList()
    private var allCategories: List<Category> = emptyList()
    private var currentDisplayedProducts: List<Product> = emptyList()
    private var currentGenderFilter: String = "WOMAN"
    private lateinit var productAdapter: ProductAdapter

    override fun initView() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        highlightSelectedTab(R.id.home_id)
        updateGenderTabUI(currentGenderFilter)
        setupRecyclerViews()
    }

    override fun initData() {
        val pref = AppSharePref(requireContext())
        if (!pref.isTodaySaved(requireContext())) {
            val dialog = CheckInDialogFragment()
            dialog.show(parentFragmentManager, "CheckInDialog")
            pref.saveToday(requireContext())
        }
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }

        binding.btnSlot.setDraggableWithClick {
            navigate(R.id.slotMachineGameFragment)
        }

        binding.tvTabWoman.setOnClickListener { onGenderTabSelected("WOMAN") }
        binding.tvTabMan.setOnClickListener { onGenderTabSelected("MAN") }
        binding.tvTabKid.setOnClickListener { onGenderTabSelected("KID") }

        binding.etSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivFilter.singleClick {
            showSortPopupMenu()
        }

        binding.tvAllItems.singleClick {
            resetToGenderDefaultList()
        }

        requireActivity().findViewById<View>(R.id.home_id).singleClick { highlightSelectedTab(R.id.home_id) }
        requireActivity().findViewById<View>(R.id.wish_id).singleClick {
            highlightSelectedTab(R.id.wish_id)
            navigate(R.id.wishlistFragment)
        }
        requireActivity().findViewById<View>(R.id.cart_id).singleClick {
            highlightSelectedTab(R.id.cart_id)
            navigate(R.id.cartFragment)
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            highlightSelectedTab(R.id.person_id)
            navigate(R.id.profileFragment)
        }
    }

    private fun setupRecyclerViews() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())

        if (!token.isNullOrBlank()) {
            productAdapter = ProductAdapter()
            binding.rvItems.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.rvItems.adapter = productAdapter

            productAdapter.setOnClickItemRecyclerView { product, _ ->
                val bundle = android.os.Bundle().apply {
                    putString("productId", product.productId.toString())
                }
                navigate(R.id.productDetailFragment, bundle)
            }

            mainRepository.getCategories(token) { categories: List<Category>? ->
                activity?.runOnUiThread {
                    if (categories != null) {
                        allCategories = categories
                        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                        filterCategoriesForGender(currentGenderFilter, token)
                    } else {
                        Toast.makeText(requireContext(), "Error loading categories", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            mainRepository.getProducts(token) { products: List<Product>? ->
                activity?.runOnUiThread {
                    if (products != null) {
                        allProducts = products
                        filterProductsForGender(currentGenderFilter)
                    } else {
                        Toast.makeText(requireContext(), "Error loading products", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        } else {
            Toast.makeText(requireContext(), "Please login", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadProductsByCategory(token: String, categoryId: String) {
        mainRepository.getProductsByCategory(token, categoryId) { products: List<Product>? ->
            activity?.runOnUiThread {
                if (products != null) {
                    updateCurrentList(products)
                    binding.rvItems.scrollToPosition(0)
                } else {
                    updateCurrentList(emptyList())
                }
            }
        }
    }

    private fun filterCategoriesForGender(gender: String, token: String) {
        val filteredCats = allCategories.filter { cat ->
            val name = cat.categoryName?.lowercase(Locale.ROOT)?.trim() ?: ""
            val isGenericName = name == "man" || name == "woman" || name == "kid" || name == "kids" || name == "men" || name == "women"
            if (isGenericName) return@filter false

            when (gender) {
                "WOMAN" -> Regex("\\b(woman|women|lady|girl|dress|skirt)\\b").containsMatchIn(name)
                "MAN" -> Regex("\\b(man|men|boy|gentle|shirt)\\b").containsMatchIn(name)
                "KID" -> Regex("\\b(kid|kids|baby|child|children)\\b").containsMatchIn(name)
                else -> true
            }
        }

        binding.rvCategories.adapter = CategoryAdapter(filteredCats) { selectedCategory ->
            val catId = selectedCategory.categoryId
            if (catId != null) {
                loadProductsByCategory(token, catId.toString())
            }
        }
        binding.rvCategories.scrollToPosition(0)

        if (filteredCats.isNotEmpty()) {
            val firstCatId = filteredCats[0].categoryId
            if (firstCatId != null) {
                loadProductsByCategory(token, firstCatId.toString())
            }
        } else {
            filterProductsForGender(gender)
        }
    }

    private fun filterProductsForGender(gender: String) {
        val filteredProducts = allProducts.filter { p ->
            val name = p.productName?.lowercase(Locale.ROOT)?.trim()?.replace("’", "'") ?: ""
            val isGenericName = name == "man" || name == "woman" || name == "kid" || name == "men" || name == "women"
            if (isGenericName) return@filter false

            when (gender) {
                "WOMAN" -> Regex("\\b(woman|women)('s)?\\b|\\b(dress|skirt|lady|girl)\\b").containsMatchIn(name)
                "MAN" -> Regex("\\b(man|men)('s)?\\b|\\b(boy)\\b").containsMatchIn(name)
                "KID" -> Regex("\\b(kid|kids|baby|child|children)('s)?\\b").containsMatchIn(name)
                else -> true
            }
        }
        updateCurrentList(filteredProducts)
    }

    private fun updateCurrentList(list: List<Product>) {
        currentDisplayedProducts = list
        productAdapter.setList(currentDisplayedProducts)
    }

    private fun onGenderTabSelected(gender: String) {
        if (currentGenderFilter == gender) return

        currentGenderFilter = gender
        updateGenderTabUI(gender)

        val token = SessionManager.getInstance().getAccessToken(requireContext()) ?: ""
        filterCategoriesForGender(gender, token)
    }

    private fun updateGenderTabUI(selectedGender: String) {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.black)
        val unselectedColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)

        fun updateStyle(tv: TextView, isSelected: Boolean) {
            tv.setTextColor(if (isSelected) selectedColor else unselectedColor)
            tv.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }

        updateStyle(binding.tvTabWoman, selectedGender == "WOMAN")
        updateStyle(binding.tvTabMan, selectedGender == "MAN")
        updateStyle(binding.tvTabKid, selectedGender == "KID")
    }

    private fun highlightSelectedTab(selectedId: Int) {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.cart_id, R.id.person_id)
        ids.forEach { id ->
            val view = requireActivity().findViewById<View>(id)
            if (id == selectedId) {
                view.setBackgroundResource(R.drawable.bg_selected_tab)
            } else {
                view.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            }
        }
    }

    private fun showSortPopupMenu() {
        val popup = PopupMenu(requireContext(), binding.ivFilter)
        popup.menu.add(0, 1, 0, "Name A -> Z")
        popup.menu.add(0, 2, 1, "Price low -> high")
        popup.menu.add(0, 3, 2, "Price high -> low")
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> sortCurrentListByNameAsc()
                2 -> sortCurrentListByPriceAsc()
                3 -> sortCurrentListByPriceDesc()
            }
            true
        }
        popup.show()
    }

    private fun sortCurrentListByNameAsc() {
        val sorted = currentDisplayedProducts.sortedBy { it.productName?.lowercase() }
        updateCurrentList(sorted)
    }

    private fun sortCurrentListByPriceAsc() {
        val sorted = currentDisplayedProducts.sortedBy { it.minPrice ?: Double.MAX_VALUE }
        updateCurrentList(sorted)
    }

    private fun sortCurrentListByPriceDesc() {
        val sorted = currentDisplayedProducts.sortedByDescending { it.minPrice ?: Double.MIN_VALUE }
        updateCurrentList(sorted)
    }

    private fun performSearch(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            filterProductsForGender(currentGenderFilter)
            return
        }
        val filtered = allProducts.filter { p ->
            val name = p.productName ?: ""
            name.lowercase().contains(q)
        }
        updateCurrentList(filtered)
    }

    private fun resetToGenderDefaultList() {
        filterProductsForGender(currentGenderFilter)
        Toast.makeText(requireContext(), "All $currentGenderFilter Items", Toast.LENGTH_SHORT).show()
    }

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}