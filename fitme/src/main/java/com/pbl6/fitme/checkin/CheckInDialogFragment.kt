package com.pbl6.fitme.checkin

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dailycheckin.utils.DailyCheckIn
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentCheckInDialogBinding
import hoang.dqm.codebase.utils.singleClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class CheckInDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentCheckInDialogBinding
    private var adapter: CheckInAdapter? = null
    private var currentWeekStartIndex = 0

    private val viewModel: CheckInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCheckInDialogBinding.inflate(inflater, container, false)


        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenTransparentDialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CheckInAdapter()
        binding.rvCheckIn.adapter = adapter
        binding.rvCheckIn.layoutManager = GridLayoutManager(requireContext(), 3)

        lifecycleScope.launch(Dispatchers.IO) {
            val fullList = DailyCheckIn.getListCheckIn(requireContext())
            val today = LocalDate.now()
            val indexToday = fullList.indexOfFirst { it.date == today }

            val currentCycle = if (indexToday >= 0) indexToday / 7 else 0
            currentWeekStartIndex = currentCycle * 7
            val endIndex = minOf(currentWeekStartIndex + 7, fullList.size)

            val currentWeekList = if (currentWeekStartIndex < fullList.size) {
                fullList.subList(currentWeekStartIndex, endIndex).toList()
            } else {
                emptyList()
            }

            withContext(Dispatchers.Main) {
                adapter?.setOffset(currentWeekStartIndex)
                adapter?.updateListCheckIn(currentWeekList)
                if (currentWeekList.find { it.date == today }?.isChecked == true) {
                    disableClaimButton()
                }
            }
        }
        binding.btnClaim.singleClick {
            val todayDate = LocalDate.now()
            val todayItem = adapter?.dataList?.find { it.date == todayDate }
            if (todayItem?.isChecked == true) {
                disableClaimButton()
                return@singleClick
            }
            callCheckInApi(todayDate)
            updateUiAfterCheckIn()
        }

        binding.btnClose.singleClick {
            dialog?.dismiss()
        }
    }


    private fun callCheckInApi(date: LocalDate) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // TODO: Viết logic gọi API POST Check-in tại đây
                // Ví dụ:
                // val response = repository.postCheckIn(date.toString())
                // if (response.isSuccessful) { ... }

                // Log kiểm tra
                // Log.d("CheckIn", "Calling API for date: $date")

                // Lưu trạng thái vào local để lần sau mở app hiển thị đúng (Tùy chọn)
                DailyCheckIn.checkIn(requireContext(), date)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnClaim.isEnabled = true
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateUiAfterCheckIn() {
        disableClaimButton()
        adapter?.checkInNow()
        adapter?.notifyDataSetChanged()

        lifecycleScope.launch {
            delay(1000)
            if (isAdded && !isDetached && dialog?.isShowing == true) {
                dialog?.dismiss()
            }
        }
    }

    private fun disableClaimButton() {
        binding.btnClaim.isEnabled = false
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    protected fun adjustInsetsForNavigation(viewBottom: View) {
        ViewCompat.setOnApplyWindowInsetsListener(viewBottom) { view, insets ->
            try {
                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                val systemInsets = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
                val newBottomMargin = systemInsets.bottom
                if (params.bottomMargin != newBottomMargin) {
                    params.bottomMargin = newBottomMargin
                    view.layoutParams = params
                    view.requestLayout()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@setOnApplyWindowInsetsListener insets
        }
    }
}