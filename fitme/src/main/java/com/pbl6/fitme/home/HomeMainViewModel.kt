package com.pbl6.fitme.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pbl6.fitme.model.Category
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.util.Locale

class HomeMainViewModel : BaseViewModel() {
    private val mainRepository = MainRepository()

    private val _obsCategories = MutableLiveData<List<Category>>()
    val obsCategories: LiveData<List<Category>> = _obsCategories

    private val _obsProducts = MutableLiveData<List<Product>>()
    val obsProducts: LiveData<List<Product>> = _obsProducts

    private val _obsCurrentGender = MutableLiveData<String>("WOMAN")
    val obsCurrentGender: LiveData<String> = _obsCurrentGender

    private var allProducts: List<Product> = emptyList()
    private var allCategories: List<Category> = emptyList()

    fun fetchData(token: String) {
        if (allProducts.isNotEmpty() && allCategories.isNotEmpty()) {
            applyGenderFilter(_obsCurrentGender.value ?: "WOMAN")
            return
        }

        mainRepository.getCategories(token) { categories ->
            if (categories != null) {
                allCategories = categories
                checkAndFilterData()
            }
        }

        mainRepository.getProducts(token) { products ->
            if (products != null) {
                allProducts = products
                checkAndFilterData()
            }
        }
    }

    private fun checkAndFilterData() {
        if (allCategories.isNotEmpty() && allProducts.isNotEmpty()) {
            applyGenderFilter(_obsCurrentGender.value ?: "WOMAN")
        }
    }

    fun changeGenderFilter(gender: String, token: String) {
        if (_obsCurrentGender.value == gender) return
        _obsCurrentGender.postValue(gender)

        if (allCategories.isEmpty() || allProducts.isEmpty()) {
            fetchData(token)
        } else {
            applyGenderFilter(gender)
        }
    }

    private fun applyGenderFilter(gender: String) {
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
        _obsCategories.postValue(filteredCats)

        if (filteredCats.isNotEmpty()) {
            val firstCatId = filteredCats[0].categoryId
            if (firstCatId != null) {
                // Note: Logic tải sản phẩm theo category đầu tiên cần token,
                // nhưng ở đây ta có thể đơn giản hóa bằng cách lọc local hoặc
                // truyền token vào hàm này nếu cần gọi API.
                // Để giữ code clean, tạm thời ta lọc local theo gender trước.
                filterProductsByGenderLocal(gender)
            }
        } else {
            filterProductsByGenderLocal(gender)
        }
    }

    private fun filterProductsByGenderLocal(gender: String) {
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
        _obsProducts.postValue(filteredProducts)
    }

    fun loadProductsByCategory(token: String, categoryId: String) {
        mainRepository.getProductsByCategory(token, categoryId) { products ->
            if (products != null) {
                _obsProducts.postValue(products)
            } else {
                _obsProducts.postValue(emptyList())
            }
        }
    }

    fun searchProduct(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            filterProductsByGenderLocal(_obsCurrentGender.value ?: "WOMAN")
            return
        }
        val filtered = allProducts.filter { p ->
            val name = p.productName ?: ""
            name.lowercase().contains(q)
        }
        _obsProducts.postValue(filtered)
    }

    fun sortProducts(sortType: Int) {
        val currentList = _obsProducts.value ?: return
        val sortedList = when (sortType) {
            1 -> currentList.sortedBy { it.productName?.lowercase() }
            2 -> currentList.sortedBy { it.minPrice ?: Double.MAX_VALUE }
            3 -> currentList.sortedByDescending { it.minPrice ?: Double.MIN_VALUE }
            else -> currentList
        }
        _obsProducts.postValue(sortedList)
    }

    fun resetToDefaultGenderList() {
        filterProductsByGenderLocal(_obsCurrentGender.value ?: "WOMAN")
    }
}