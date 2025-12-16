package com.pbl6.fitme.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pbl6.fitme.repository.TryOnRepository
import hoang.dqm.codebase.base.viewmodel.BaseViewModel

class AiViewModel : BaseViewModel() {
    private val repository = TryOnRepository()

    // Cần public cái này để Fragment observe được
    private val _isLoading = MutableLiveData<Boolean>()

    private val _tryOnResult = MutableLiveData<Bitmap?>()
    val tryOnResult: LiveData<Bitmap?> = _tryOnResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun performVirtualTryOn(context: Context, personBitmap: Bitmap, clothBitmap: Bitmap) {
        _isLoading.value = true

        // Callback giờ nhận 2 tham số: (bytes, errorString)
        repository.virtualTryOn(context, personBitmap, clothBitmap) { resultBytes, errorString ->

            _isLoading.postValue(false) // Tắt xoay

            if (resultBytes != null) {
                // Thành công
                try {
                    val resultBitmap = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.size)
                    _tryOnResult.postValue(resultBitmap)
                } catch (e: Exception) {
                    _errorMessage.postValue("Lỗi giải mã ảnh kết quả")
                }
            } else {
                // Thất bại: Hiển thị lỗi cụ thể từ Repository trả về
                val msg = errorString ?: "Lỗi không xác định (Unknown error)"
                _errorMessage.postValue(msg)
            }
        }
    }
}