package com.pbl6.fitme.settings

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.pbl6.fitme.databinding.FragmentSettingProfileBinding
import com.pbl6.fitme.home.HomeMainViewModel
import com.pbl6.fitme.repository.UserRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.*

class SettingProfile : BaseFragment<FragmentSettingProfileBinding, HomeMainViewModel>() {

    private val userRepository = UserRepository()
    private var avatarFile: File? = null

    // Chọn ảnh từ Gallery
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            binding.imgProfile.setImageURI(uri)
            avatarFile = uriToFile(requireContext(), uri)
        }
    }

    // Chụp ảnh Camera
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && avatarFile != null) {
            binding.imgProfile.setImageURI(Uri.fromFile(avatarFile))
        }
    }

    override fun initView() {
        val token = SessionManager.getInstance().getAccessToken(requireContext()) ?: ""
        val userId = SessionManager.getInstance().getUserId(requireContext())?.toString()
        if (userId.isNullOrBlank()) return

        // Load user info
        userRepository.getUserDetail(token, userId) { user ->
            activity?.runOnUiThread {
                user?.let {
                    binding.etFullName.setText(it.fullName)
                    binding.etEmail.setText(it.email)
                    binding.etPhone.setText(it.phone)
                    binding.etAddress.setText(it.address)
                    binding.etDateOfBirth.setText(it.dateOfBirth)

                    it.avatarUrl?.let { url ->
                        val fullUrl = if (url.startsWith("/")) {
                            "http://2.2:8080/api$url"
                        } else {
                            "http://10.48.170.123/api/$url"
                        }
                        Log.d("hehe",fullUrl)
                        Glide.with(requireContext())
                            .load(fullUrl)
                            .circleCrop()
                            .into(binding.imgProfile)
                    }
                }
            }
        }
    }

    override fun initListener() {
        // Back button
        binding.ivBack.setOnClickListener { activity?.onBackPressed() }

        // Date picker
        binding.etDateOfBirth.setOnClickListener { showDatePicker() }

        // Edit avatar
        binding.btnEditImage.setOnClickListener { showImagePickerDialog() }

        // Save changes
        binding.btnSave.setOnClickListener { saveUserChanges() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val monthStr = String.format("%02d", month + 1)
                val dayStr = String.format("%02d", dayOfMonth)
                binding.etDateOfBirth.setText("$year-$monthStr-$dayStr")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Camera", "Gallery")
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Choose Avatar")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> pickImageLauncher.launch("image/*")
            }
        }
        builder.show()
    }

    private fun openCamera() {
        val file = File(requireContext().cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        avatarFile = file
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        takePictureLauncher.launch(uri)
    }

    private fun uriToFile(context: Context, uri: Uri): File {
        val file = File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun saveUserChanges() {
        val token = SessionManager.getInstance().getAccessToken(requireContext()) ?: ""
        val userId = SessionManager.getInstance().getUserId(requireContext())?.toString()
        if (userId.isNullOrBlank()) return

        val roleIds = listOf("6828a645-b7a7-45ab-8568-0268b0085268") // Default role

        val username = binding.etEmail.text.toString()
        val email = binding.etEmail.text.toString()
        val fullName = binding.etFullName.text.toString()
        val dateOfBirth = binding.etDateOfBirth.text.toString()
        val phone = binding.etPhone.text.toString()
        val password = "" // nếu không đổi

        // Tạo Map và Multipart cho API
        val params = HashMap<String, RequestBody>()
        params["username"] = username.toRequestBody("text/plain".toMediaType())
        params["password"] = password.toRequestBody("text/plain".toMediaType())
        params["email"] = email.toRequestBody("text/plain".toMediaType())
        params["fullName"] = fullName.toRequestBody("text/plain".toMediaType())
        params["dateOfBirth"] = dateOfBirth.toRequestBody("text/plain".toMediaType())
        params["phone"] = phone.toRequestBody("text/plain".toMediaType())
        params["roleIds"] = roleIds.joinToString(",").toRequestBody("text/plain".toMediaType())

        val avatarPart = avatarFile?.let {
            val requestFile = it.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("avatar", it.name, requestFile)
        }

        userRepository.updateUser(token, userId, params, avatarPart) { updatedUser ->
            activity?.runOnUiThread {
                if (updatedUser != null) {
                    Toast.makeText(requireContext(), "Update success!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Update failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun initData() {
        // Init thêm dữ liệu nếu cần
    }
}
