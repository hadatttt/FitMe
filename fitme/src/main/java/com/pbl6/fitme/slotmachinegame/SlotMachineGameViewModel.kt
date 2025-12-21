package com.pbl6.fitme.slotmachinegame

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pbl6.fitme.repository.UserRepository
import hoang.dqm.codebase.base.viewmodel.BaseViewModel

class SlotMachineGameViewModel : BaseViewModel() {

    private val userRepository = UserRepository()

    private val _spinsLeft = MutableLiveData<Int>()
    val spinsLeft: LiveData<Int> get() = _spinsLeft

    private val _diamondCount = MutableLiveData<Int>()
    val diamondCount: LiveData<Int> get() = _diamondCount

    fun setSpinsLeft(value: Int) {
        _spinsLeft.value = value
    }

    fun decrementSpinsLeft() {
        val currentSpins = _spinsLeft.value ?: 0
        if (currentSpins > 0) {
            _spinsLeft.value = currentSpins - 1
        }
    }

    fun increaseSpinsLeft(amount: Int = 1) {
        val currentSpins = _spinsLeft.value ?: 0
        _spinsLeft.value = currentSpins + amount
    }

    // --- HÀM LẤY ĐIỂM (GET) ---
    // Hàm này server trả về đúng số dư nên ta cập nhật bình thường
    fun fetchUserPoints(token: String, userId: String) {
        userRepository.getUserPoints(token, userId) { points ->
            if (points != null) {
                _diamondCount.postValue(points)
            }
        }
    }

    fun setDiamondCount(value: Int) {
        _diamondCount.value = value
    }

    // --- HÀM CẬP NHẬT ĐIỂM (PUT) ---
    // Sửa logic: Không lấy result từ server để update UI nữa (vì server trả về 0)
    fun incrementDiamondCount(token: String, userId: String, amount: Int) {
        val currentDiamonds = _diamondCount.value ?: 0
        val newTotal = currentDiamonds + amount

        // 1. Cập nhật UI ngay lập tức (đây là giá trị đúng)
        _diamondCount.value = newTotal

        // 2. Gọi API để lưu lên server
        userRepository.updateUserPoints(token, userId, newTotal) { serverResult ->
            // serverResult ở đây sẽ là 0 (do server trả về) hoặc null (nếu lỗi)

            if (serverResult != null) {
                // API thành công (Server đã nhận 40.0 coin)
                // KHÔNG LÀM GÌ CẢ. Giữ nguyên giá trị newTotal đã hiển thị ở bước 1.
                // Nếu bạn gán _diamondCount.value = serverResult thì nó sẽ về 0.
            } else {
                // API thất bại -> Rollback về điểm cũ
                _diamondCount.postValue(currentDiamonds)
            }
        }
    }
}