package com.pbl6.fitme.profile

import com.bumptech.glide.Glide
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.ItemProductBinding
import com.pbl6.fitme.model.Product
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class ProductAdapter : BaseRecyclerViewAdapter<Product, ItemProductBinding>() {

    override fun bindData(binding: ItemProductBinding, item: Product, position: Int) {
        binding.tvProductName.text = item.productName
        // Show the lowest variant price if available
        val min = item.minPrice
        binding.tvProductPrice.text = if (min != null) {
            // Format theo USD
            val fmt = NumberFormat.getCurrencyInstance(Locale.US)
            fmt.currency = Currency.getInstance("USD")
            fmt.format(min)
        } else {
            "-"
        }

        val imageUrl = item.mainImageUrl
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(binding.root.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_splash)
                .error(R.drawable.ic_splash)
                .into(binding.imgProduct)
        } else {
            binding.imgProduct.setImageResource(R.drawable.ic_splash)
        }
    }
}