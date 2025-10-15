package com.pbl6.fitme.product

import android.annotation.SuppressLint
import com.bumptech.glide.Glide
import com.pbl6.fitme.databinding.ItemProductImageBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class ProductImageAdapter : BaseRecyclerViewAdapter<String, ItemProductImageBinding>() {

    // Giữ danh sách ảnh nội bộ
    private val imageList = mutableListOf<String>()

    // Cập nhật danh sách ảnh mới
    @SuppressLint("NotifyDataSetChanged")
    fun setList(list: List<String>) {
        imageList.clear()
        imageList.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = imageList.size

    override fun bindData(binding: ItemProductImageBinding, item: String, position: Int) {
        Glide.with(binding.root.context)
            .load(imageList[position])
            .centerCrop()
            .into(binding.ivProduct)
        val total = imageList.size
        binding.tvPosition.text = "${position + 1}/$total"
    }

    override fun getItem(position: Int): String = imageList[position]
}
