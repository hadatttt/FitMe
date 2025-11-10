package com.pbl6.fitme.register

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.pbl6.fitme.R
import androidx.core.widget.addTextChangedListener
import hoang.dqm.codebase.base.activity.navigate
import com.pbl6.fitme.databinding.FragmentRegisterBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.utils.singleClick
import com.pbl6.fitme.network.RegisterRequest
import com.pbl6.fitme.repository.AuthRepository
import hoang.dqm.codebase.base.activity.popBackStack
import java.util.Calendar
import java.util.Locale

class RegisterFragment : BaseFragment<FragmentRegisterBinding, RegisterViewModel>() {
    private val authRepository = AuthRepository()
    var isPasswordVisible = true

    // Biến để lưu trữ giá trị ngày sinh và giới tính
    private var selectedDateOfBirth: String? = null
    private var selectedGender: String? = null

    override fun initView() {
        // Thiết lập Spinner cho Gender
        setupGenderSpinner()

        // Khởi tạo Text Watcher
        // Đoạn này có thể bỏ đi vì không dùng: binding.etEmail.addTextChangedListener { s -> editTextValue1 = s.toString() }
    }

    override fun initListener() {
        binding.ivTogglePassword.singleClick {
            togglePasswordVisibility()
        }

        binding.tvCancel.singleClick {
            popBackStack()
        }

        binding.btnDone.singleClick {
            handleRegistration()
        }

        // Bắt sự kiện click để mở Date Picker cho trường Date of Birth
        binding.etDateOfBirth.singleClick {
            showDatePicker()
        }
        // Thêm listener cho Spinner nếu cần xử lý thay đổi
        // binding.spGender.onItemSelectedListener = ...
    }

    override fun initData() {
        // Có thể load dữ liệu mặc định ở đây nếu cần
    }

    // region Logic UI
    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            binding.etPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.ivTogglePassword.setImageResource(R.drawable.eye)
        } else {
            binding.etPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.ivTogglePassword.setImageResource(R.drawable.eye_slash)
        }
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
    }

    private fun setupGenderSpinner() {
        val genderOptions = arrayOf(
            "Select Gender", // Placeholder
            "Male",
            "Female",
            "Other"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            genderOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spGender.adapter = adapter

        if (genderOptions.isNotEmpty()) {

            selectedGender = genderOptions[0]
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Định dạng ngày (YYYY-MM-DD)
                selectedDateOfBirth = String.format(Locale.US, "%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.etDateOfBirth.setText(selectedDateOfBirth)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }
    // endregion

    // region Logic Đăng ký
    private fun handleRegistration() {
        // 1. Lấy dữ liệu từ các View
        val username = binding.etUsername.text.toString().trim()
        val firstName = binding.etFirstname.text.toString().trim()
        val lastName = binding.etLastname.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val dateOfBirth = binding.etDateOfBirth.text.toString().trim()
        // Lấy giá trị từ Spinner (tránh lấy "Gender" nếu đó là placeholder)
        val gender = binding.spGender.selectedItem.toString()

        // 2. Kiểm tra dữ liệu (Validation)
        if (username.isEmpty()) {
            binding.etUsername.error = "Username is required"
            return
        }
        if (firstName.isEmpty()) {
            binding.etFirstname.error = "First name is required"
            return
        }
        if (lastName.isEmpty()) {
            binding.etLastname.error = "Last name is required"
            return
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Valid email is required"
            return
        }
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return
        }
        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone number is required"
            return
        }
        if (dateOfBirth.isEmpty()) {
            binding.etDateOfBirth.error = "Date of birth is required"
            return
        }
        if (gender.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select gender", Toast.LENGTH_SHORT).show()
            return
        }

        val fullName = "$firstName $lastName"

        val registerRequest = RegisterRequest(
            username = username,
            password = password,
            email = email,
            fullName = fullName,
            dateOfBirth = dateOfBirth,
            phone = phone,
            avatarUrl = null, // Giữ nguyên null
            gender = gender
        )

        Log.d("Auth", "Register Payload: $registerRequest")
        // Hiển thị loading (nếu có)

        authRepository.register(registerRequest) { response ->
            // Ẩn loading (nếu có)
            if (response != null ) {
                Toast.makeText(requireContext(), "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                navigate(R.id.loginFragment)
            } else {
                Toast.makeText(requireContext(), "Đăng ký thất bại. Vui lòng thử lại.", Toast.LENGTH_LONG).show()
                Log.e("Auth", "Register failed.")
            }
        }
    }
    // endregion
}