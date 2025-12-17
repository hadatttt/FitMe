package com.pbl6.fitme.checkin

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.dailycheckin.model.entity.CheckInDay
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.ItemDailyCheckInBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import java.time.LocalDate

class CheckInAdapter : BaseRecyclerViewAdapter<CheckInDay, ItemDailyCheckInBinding>() {

    private var checkIn = false
    private var startOffset: Int = 0

    fun setOffset(offset: Int) {
        this.startOffset = offset
    }

    fun updateListCheckIn(checkIn: List<CheckInDay>) {
        setList(checkIn)
    }

    fun checkInNow() {
        checkIn = true
    }

    @SuppressLint("StringFormatInvalid")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun bindData(binding: ItemDailyCheckInBinding, item: CheckInDay, position: Int) {
        val realGlobalIndex = startOffset + position

        binding.day.text = context.getString(R.string.text_day, realGlobalIndex + 1)

        when (item.isChecked) {
            true -> {
                binding.bgCheckIn.setImageResource(R.drawable.img_checked)
            }
            false -> {
                if (item.date == LocalDate.now()) {
                    if (checkIn) {
                        binding.bgCheckIn.setImageResource(R.drawable.img_checked)
                    } else {
                        binding.bgCheckIn.setImageResource(R.drawable.img_uncheck)
                    }
                } else {
                    binding.bgCheckIn.setImageResource(R.drawable.img_un_checked)
                }
            }
        }
    }
}