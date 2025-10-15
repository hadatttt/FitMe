package com.pbl6.fitme.profile

import com.bumptech.glide.Glide
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.ItemProductBinding
import com.pbl6.fitme.model.Product
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class ProductAdapter : BaseRecyclerViewAdapter<Product, ItemProductBinding>() {

    override fun bindData(binding: ItemProductBinding, item: Product, position: Int) {
        binding.tvProductName.text = item.productName
        val imageUrl = item.mainImageUrl
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(binding.root.context) // Lấy context từ view gốc của binding
                .load(imageUrl)
                .placeholder(R.drawable.ic_splash)
                .error(R.drawable.ic_splash)
                .into(binding.imgProduct)
        } else {
            binding.imgProduct.setImageResource(R.drawable.ic_splash)
        }
    }
}