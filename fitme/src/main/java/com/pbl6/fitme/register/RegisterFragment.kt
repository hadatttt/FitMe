package com.pbl6.fitme.register

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import com.pbl6.fitme.R
import androidx.core.widget.addTextChangedListener
import hoang.dqm.codebase.base.activity.navigate
import com.pbl6.fitme.databinding.FragmentRegisterBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.utils.singleClick
import androidx.activity.result.contract.ActivityResultContracts
import com.pbl6.fitme.network.RegisterRequest
import com.pbl6.fitme.repository.AuthRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.popBackStack
import kotlin.toString
import java.text.SimpleDateFormat
import java.util.*

class RegisterFragment : BaseFragment<FragmentRegisterBinding, RegisterViewModel>() {
    private var editTextValue1: String = ""
    private val authRepository = AuthRepository()
    var isPasswordVisible = true
    private var selectedDateOfBirth: String = ""
    private var selectedGender: String = ""
//    private val cameraLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                val data: Intent? = result.data
//                val photo: Bitmap? = data?.extras?.get("data") as? Bitmap
//                if (photo != null) {
//                    binding.ivUpload.setImageBitmap(photo)
//                } else {
//                    Toast.makeText(requireContext(), "Cannot capture image", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    private val requestCameraPermission =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//            if (granted) {
//                openCamera()
//            } else {
//                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
//            }
//        }
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    private val phoneRegex = "^\\+?[0-9]{9,15}$".toRegex()
    private val dateRegex = "^\\d{4}-\\d{2}-\\d{2}$".toRegex()

    // Helper function to validate date format and age
    private fun isValidDateOfBirth(dateString: String): Pair<Boolean, String?> {
        if (dateString.isBlank()) {
            return Pair(false, "Date of birth is required")
        }

        // Check format YYYY-MM-DD
        if (!dateString.matches(dateRegex)) {
            return Pair(false, "Invalid date format")
        }

        try {
            val parts = dateString.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            // Validate month range
            if (month < 1 || month > 12) {
                return Pair(false, "Month must be between 1 and 12")
            }

            // Validate day range
            val daysInMonth = when (month) {
                2 -> if (isLeapYear(year)) 29 else 28
                4, 6, 9, 11 -> 30
                else -> 31
            }
            if (day < 1 || day > daysInMonth) {
                return Pair(false, "Day is invalid for the given month")
            }

            // Check if date is not in the future
            val calendar = Calendar.getInstance()
            val today = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val inputDate = Calendar.getInstance().apply {
                set(year, month - 1, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            if (inputDate.after(today)) {
                return Pair(false, "Date of birth cannot be in the future")
            }

            // Check age (minimum 13 years old)
            val birthCalendar = Calendar.getInstance().apply {
                time = inputDate
            }
            val currentDate = Calendar.getInstance()
            var age = currentDate.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

            // Adjust age if birthday hasn't occurred this year
            if (currentDate.get(Calendar.MONTH) < birthCalendar.get(Calendar.MONTH) ||
                (currentDate.get(Calendar.MONTH) == birthCalendar.get(Calendar.MONTH) &&
                 currentDate.get(Calendar.DAY_OF_MONTH) < birthCalendar.get(Calendar.DAY_OF_MONTH))
            ) {
                age--
            }

            if (age < 13) {
                return Pair(false, "You must be at least 13 years old")
            }

            return Pair(true, null)
        } catch (e: Exception) {
            return Pair(false, "Invalid date of birth")
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    // Helper to set hidden error text for UI tests (Appium) and internal checks
    private fun setHiddenError(message: String?) {
        // binding.tvHiddenError is generated from tv_hidden_error id
        try {
            binding.tvHiddenError.text = message ?: ""
        } catch (e: Exception) {
            Log.d("RegisterFragment", "tvHiddenError not found in binding: ${e.message}")
        }
    }

    private fun setupDatePicker() {
        binding.etDateOfBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Format: YYYY-MM-DD
                    val formattedDate = String.format(
                        "%04d-%02d-%02d",
                        selectedYear,
                        selectedMonth + 1,
                        selectedDay
                    )
                    binding.etDateOfBirth.setText(formattedDate)
                    selectedDateOfBirth = formattedDate

                    // Validate date immediately
                    val (isValid, errorMsg) = isValidDateOfBirth(formattedDate)
                    if (!isValid) {
                        binding.etDateOfBirth.error = errorMsg
                        setHiddenError(errorMsg)
                    } else {
                        binding.etDateOfBirth.error = null
                        setHiddenError(null)
                    }
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }
    }

    private fun setupGenderSpinner() {
        val genderOptions = arrayOf("Select Gender", "Male", "Female", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = adapter

        binding.spinnerGender.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedGender = if (position == 0) "" else genderOptions[position]
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedGender = ""
            }
        }
    }
    override fun initView() {
        setupValidation()
        setupDatePicker()
        setupGenderSpinner()
    }

    private fun setupValidation() {
        // Username validation
        binding.etUsername.addTextChangedListener {
            val username = it.toString().trim()
            when {
                username.isEmpty() -> {
                    binding.etUsername.error = "Username is required"
                    setHiddenError("Username is required")
                }
                username.length < 3 -> {
                    binding.etUsername.error = "Username must be at least 3 characters"
                    setHiddenError("Username must be at least 3 characters")
                }
                else -> {
                    binding.etUsername.error = null
                    setHiddenError(null)
                }
            }
        }

        // First name validation
        binding.etFirsname.addTextChangedListener {
            val firstName = it.toString().trim()
            when {
                firstName.isEmpty() -> {
                    binding.etFirsname.error = "First name is required"
                    setHiddenError("First name is required")
                }
                firstName.length < 2 -> {
                    binding.etFirsname.error = "First name is too short"
                    setHiddenError("First name is too short")
                }
                else -> {
                    binding.etFirsname.error = null
                    setHiddenError(null)
                }
            }
        }

        // Last name validation
        binding.etLast.addTextChangedListener {
            val lastName = it.toString().trim()
            when {
                lastName.isEmpty() -> {
                    binding.etLast.error = "Last name is required"
                    setHiddenError("Last name is required")
                }
                lastName.length < 2 -> {
                    binding.etLast.error = "Last name is too short"
                    setHiddenError("Last name is too short")
                }
                else -> {
                    binding.etLast.error = null
                    setHiddenError(null)
                }
            }
        }

        // Email validation
        binding.etEmail.addTextChangedListener {
            val email = it.toString().trim()
            when {
                email.isEmpty() -> {
                    binding.etEmail.error = "Email is required"
                    setHiddenError("Email is required")
                }
                !email.matches(emailRegex) -> {
                    binding.etEmail.error = "Please enter a valid email address"
                    setHiddenError("Please enter a valid email address")
                }
                else -> {
                    binding.etEmail.error = null
                    setHiddenError(null)
                }
            }
        }

        // Password validation
        binding.etPassword.addTextChangedListener {
            val password = it.toString()
            when {
                password.isEmpty() -> {
                    binding.etPassword.error = "Password is required"
                    setHiddenError("Password is required")
                }
                password.length < 6 -> {
                    binding.etPassword.error = "Password must be at least 6 characters"
                    setHiddenError("Password must be at least 6 characters")
                }
                !password.any { it.isDigit() } -> {
                    binding.etPassword.error = "Password must contain at least one number"
                    setHiddenError("Password must contain at least one number")
                }
                !password.any { it.isLetter() } -> {
                    binding.etPassword.error = "Password must contain at least one letter"
                    setHiddenError("Password must contain at least one letter")
                }
                else -> {
                    binding.etPassword.error = null
                    setHiddenError(null)
                }
            }
        }

        // Confirm Password validation
        binding.etConfirmPassword.addTextChangedListener {
            val confirmPassword = it.toString()
            val password = binding.etPassword.text.toString()
            when {
                confirmPassword.isEmpty() -> {
                    binding.etConfirmPassword.error = "Please confirm your password"
                    setHiddenError("Please confirm your password")
                }
                confirmPassword != password -> {
                    binding.etConfirmPassword.error = "Passwords do not match"
                    setHiddenError("Passwords do not match")
                }
                else -> {
                    binding.etConfirmPassword.error = null
                    setHiddenError(null)
                }
            }
        }

        // Phone validation
        binding.etPhone.addTextChangedListener {
            val phone = it.toString().trim()
            when {
                phone.isEmpty() -> {
                    binding.etPhone.error = "Phone number is required"
                    setHiddenError("Phone number is required")
                }
                !phone.matches(phoneRegex) -> {
                    binding.etPhone.error = "Please enter a valid phone number"
                    setHiddenError("Please enter a valid phone number")
                }
                else -> {
                    binding.etPhone.error = null
                    setHiddenError(null)
                }
            }
        }
    }

    override fun initListener() {
//        binding.ivUpload.singleClick {
//            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
//        }
        binding.ivTogglePassword.singleClick {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.etConfirmPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.eye)
            } else {
                binding.etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.etConfirmPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.eye_slash)
            }
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text?.length ?: 0)
        }
        binding.tvCancel.singleClick {
            popBackStack()
        }
        binding.btnDone.singleClick {
            handleRegistration()
        }
    }

    override fun initData() {
    }
//    private fun openCamera() {
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        cameraLauncher.launch(intent)
//    }

    private fun handleRegistration() {
        val username = binding.etUsername.text.toString().trim()
        val firstName = binding.etFirsname.text.toString().trim()
        val lastName = binding.etLast.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val dateOfBirth = selectedDateOfBirth
        val phone = binding.etPhone.text.toString().trim()
        val gender = selectedGender

        val errorList = mutableListOf<String>()

        // --- Validation checks ---
        when {
            username.isEmpty() -> errorList.add("Username is required")
            username.length < 3 -> errorList.add("Username must be at least 3 characters")
        }
        when {
            firstName.isEmpty() -> errorList.add("First name is required")
            firstName.length < 2 -> errorList.add("First name is too short")
        }
        when {
            lastName.isEmpty() -> errorList.add("Last name is required")
            lastName.length < 2 -> errorList.add("Last name is too short")
        }
        when {
            email.isEmpty() -> errorList.add("Email is required")
            !email.matches(emailRegex) -> errorList.add("Please enter a valid email address")
        }
        when {
            password.isEmpty() -> errorList.add("Password is required")
            password.length < 6 -> errorList.add("Password must be at least 6 characters")
            !password.any { it.isDigit() } -> errorList.add("Password must contain at least one number")
            !password.any { it.isLetter() } -> errorList.add("Password must contain at least one letter")
        }
        if (password != confirmPassword) {
            errorList.add("Passwords do not match")
        }
        when {
            phone.isEmpty() -> errorList.add("Phone number is required")
            !phone.matches(phoneRegex) -> errorList.add("Please enter a valid phone number")
        }

        // Date of Birth validation
        val (isValidDOB, dobError) = isValidDateOfBirth(dateOfBirth)
        if (!isValidDOB) {
            errorList.add(dobError ?: "Date of birth is invalid")
        }

        // Nếu có lỗi validation, show Toast tổng hợp
        if (errorList.isNotEmpty()) {
            val errorMessage = errorList.joinToString("; ")
            Toast.makeText(requireContext(), "Register Fail: $errorMessage", Toast.LENGTH_LONG).show()
            setHiddenError(errorMessage)
            return
        }

        // --- Build request ---
        val fullName = "$firstName $lastName"
        val registerRequest = RegisterRequest(
            username = username,
            password = password,
            email = email,
            fullName = fullName,
            dateOfBirth = if (dateOfBirth.isBlank()) null else dateOfBirth,
            phone = phone,
            avatarUrl = null,
            gender = if (gender.isBlank()) null else gender
        )

        authRepository.register(registerRequest) { response ->
            activity?.runOnUiThread {
                if (response != null) {
                    // Success - create shopping cart for the new user
                    Toast.makeText(requireContext(), "✅ Registration successful! Creating shopping cart...", Toast.LENGTH_LONG).show()
                    setHiddenError("Registration successful")
                    
                    try {
                        val userId = response.userId
                        if (!userId.isNullOrBlank()) {
                            com.pbl6.fitme.session.SessionManager.getInstance().saveUserId(requireContext(), userId)
                        }
                    } catch (ex: Exception) {
                        // ignore
                    }
                    
                    // Create shopping cart for the newly registered user
                    try {
                        val userId = response.userId
                        if (!userId.isNullOrBlank()) {
                            val mainRepository = com.pbl6.fitme.repository.MainRepository()
                            mainRepository.createCartForNewUser(userId) { cartId ->
                                activity?.runOnUiThread {
                                    if (!cartId.isNullOrBlank()) {
                                        android.util.Log.d("RegisterFragment", "Shopping cart created: $cartId")
                                        com.pbl6.fitme.session.SessionManager.getInstance().savePersistentCartId(requireContext(), java.util.UUID.fromString(cartId))
                                    }
                                    // Navigate to login after a delay
                                    binding.root.postDelayed({
                                        navigate(R.id.loginFragment)
                                    }, 1500)
                                }
                            }
                        } else {
                            binding.root.postDelayed({
                                navigate(R.id.loginFragment)
                            }, 1500)
                        }
                    } catch (ex: Exception) {
                        android.util.Log.e("RegisterFragment", "Error creating cart", ex)
                        binding.root.postDelayed({
                            navigate(R.id.loginFragment)
                        }, 1500)
                    }
                } else {
                    // Server returned error, tổng hợp nhiều lý do nếu có
                    val serverErrors = mutableListOf<String>()
                    serverErrors.add("Registration failed")

                    // Ví dụ check username/email/phone trùng
                    when {
                        email.contains("@") && response.toString().lowercase().contains("email") ->
                            serverErrors.add("Email address is already registered")
                        response.toString().lowercase().contains("username") ->
                            serverErrors.add("Username is already taken")
                        response.toString().lowercase().contains("phone") ->
                            serverErrors.add("Phone number is already registered")
                    }

                    val serverErrorMessage = serverErrors.joinToString("; ")
                    Toast.makeText(requireContext(), "Register Fail: $serverErrorMessage", Toast.LENGTH_LONG).show()
                    setHiddenError(serverErrorMessage)
                }
            }
        }
    }
}