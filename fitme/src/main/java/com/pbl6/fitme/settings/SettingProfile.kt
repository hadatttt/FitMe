package com.pbl6.fitme.settings

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class SettingProfile : BaseFragment<FragmentSettingProfileBinding, HomeMainViewModel>() {

    private val userRepository = UserRepository()
    private var avatarFile: File? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val photo: Bitmap? = data?.extras?.get("data") as? Bitmap
                if (photo != null) {
                    // Hiển thị lên View
                    binding.imgProfile.setImageBitmap(photo)
                    // QUAN TRỌNG: Chuyển Bitmap thành File để chuẩn bị Upload
                    avatarFile = bitmapToFile(requireContext(), photo)
                } else {
                    Toast.makeText(requireContext(), "Cannot capture image", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // Launcher xin quyền Camera
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCameraIntent()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // --- 2. LOGIC GALLERY (GIỮ NGUYÊN) ---
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Glide.with(requireContext())
                .load(uri)
                .circleCrop()
                .into(binding.imgProfile)
            avatarFile = uriToFile(requireContext(), uri)
        }
    }

    override fun initView() {
        val token = SessionManager.getInstance().getAccessToken(requireContext()) ?: ""
        val userId = SessionManager.getInstance().getUserId(requireContext())?.toString()
        if (userId.isNullOrBlank()) return

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
                            "http://10.48.170.90:8080/api$url"
                        } else {
                            "http://10.48.170.90:8080/api/$url"
                        }
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
        binding.ivBack.setOnClickListener { activity?.onBackPressed() }
        binding.etDateOfBirth.setOnClickListener { showDatePicker() }
        binding.btnEditImage.setOnClickListener { showImagePickerDialog() }
        binding.btnSave.setOnClickListener { saveUserChanges() }
    }

    // region: Helper Functions

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
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose Avatar")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> requestCameraPermission.launch(Manifest.permission.CAMERA) // Gọi xin quyền giống Home
                1 -> pickImageLauncher.launch("image/*")
            }
        }
        builder.show()
    }

    // Hàm mở Camera đơn giản (giống HomeFragment)
    private fun openCameraIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    // Helper: Chuyển Uri -> File (Cho Gallery)
    private fun uriToFile(context: Context, uri: Uri): File {
        val file = File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    // Helper MỚI: Chuyển Bitmap -> File (Cho Camera)
    // Cần hàm này vì Camera trả về Bitmap nhưng API cần File
    private fun bitmapToFile(context: Context, bitmap: Bitmap): File {
        val file = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos) // Nén ảnh thành JPEG
        val bitmapdata = bos.toByteArray()

        val fos = FileOutputStream(file)
        fos.write(bitmapdata)
        fos.flush()
        fos.close()
        return file
    }

    // endregion

    private fun saveUserChanges() {
        val token = SessionManager.getInstance().getAccessToken(requireContext()) ?: ""
        val userId = SessionManager.getInstance().getUserId(requireContext())?.toString()
        if (userId.isNullOrBlank()) return

        val roleIds = listOf("6828a645-b7a7-45ab-8568-0268b0085268")

        val username = binding.etEmail.text.toString()
        val email = binding.etEmail.text.toString()
        val fullName = binding.etFullName.text.toString()
        val dateOfBirth = binding.etDateOfBirth.text.toString()
        val phone = binding.etPhone.text.toString()

        val params = HashMap<String, RequestBody>()
        params["username"] = username.toRequestBody("text/plain".toMediaType())
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
                    avatarFile?.delete()
                    avatarFile = null
                } else {
                    Toast.makeText(requireContext(), "Update failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun initData() {
    }
}