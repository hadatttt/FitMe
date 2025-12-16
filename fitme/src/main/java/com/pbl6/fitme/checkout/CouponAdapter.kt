package com.pbl6.fitme.checkout

import com.pbl6.fitme.databinding.ItemVoucherSmallBinding
import com.pbl6.fitme.model.Coupon
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class CouponAdapter: BaseRecyclerViewAdapter<Coupon, ItemVoucherSmallBinding>() {
    override fun bindData(
        binding: ItemVoucherSmallBinding,
        item: Coupon,
        position: Int
    ) {
        binding.txtVoucherCondition.text = item.code
    }
}