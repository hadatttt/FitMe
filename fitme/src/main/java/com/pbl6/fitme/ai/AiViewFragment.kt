package com.pbl6.fitme.ai

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.pbl6.fitme.databinding.FragmentAiBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import java.io.IOException

class AiViewFragment : BaseFragment<FragmentAiBinding, AiViewModel>() {

    // Biến lưu giữ URI ảnh người dùng (Local)
    private var personImageUri: Uri? = null

    // Biến lưu giữ ảnh quần áo (Có thể từ URI local hoặc Bitmap từ URL)
    private var clothImageUri: Uri? = null
    private var clothBitmapFromUrl: Bitmap? = null // Biến mới để lưu Bitmap tải từ URL sản phẩm

    // Launcher chọn ảnh Người
    private val pickPersonLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            personImageUri = uri
            Glide.with(this).load(uri).into(binding.ivUserPreview)
            binding.layoutAddPhoto.visibility = View.GONE
        }
    }

    // Launcher chọn ảnh Quần áo (Trường hợp muốn đổi ảnh khác)
    private val pickClothLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            clothImageUri = uri
            clothBitmapFromUrl = null // Reset bitmap URL nếu người dùng chọn ảnh local
            Glide.with(this).load(uri).into(binding.ivClothPreview)
        }
    }

    override fun initView() {
        binding.progressBar.visibility = View.GONE
        binding.ivResult.visibility = View.GONE
        binding.layoutEmptyResult.visibility = View.VISIBLE

        // --- MỚI: Nhận dữ liệu từ ProductDetailFragment ---
        val clothUrl = arguments?.getString("clothImageUrl")
        if (!clothUrl.isNullOrBlank()) {
            loadClothFromUrl(clothUrl)
        }
    }

    private fun loadClothFromUrl(url: String) {
        // 1. Hiển thị lên UI ngay lập tức
        Glide.with(this).load(url).into(binding.ivClothPreview)

        // 2. Tải Bitmap ngầm để sẵn sàng gửi lên Server AI
        Glide.with(this)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    clothBitmapFromUrl = resource
                    // Toast.makeText(context, "Đã tải xong ảnh trang phục", Toast.LENGTH_SHORT).show()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Xử lý khi dọn dẹp (nếu cần)
                }
            })
    }

    override fun initListener() {
        // 1. Click vào Card ảnh người -> Mở thư viện
        binding.cvUserImage.setOnClickListener {
            pickPersonLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // 2. Click vào Card ảnh quần áo -> Mở thư viện (Cho phép đổi ảnh khác nếu muốn)
        binding.cvClothImage.setOnClickListener {
            pickClothLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // 3. Click nút "Try On Now"
        binding.btnGenerate.setOnClickListener {
            validateAndTryOn()
        }
    }

    override fun initData() {
        // Lắng nghe Loading
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnGenerate.isEnabled = false
                binding.btnGenerate.text = "Processing..."
                binding.layoutEmptyResult.visibility = View.GONE
                binding.ivResult.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.btnGenerate.isEnabled = true
                binding.btnGenerate.text = "Try On Now ✨"
            }
        }

        // Lắng nghe Kết quả thành công
        viewModel.tryOnResult.observe(this) { bitmap ->
            if (bitmap != null) {
                binding.ivResult.visibility = View.VISIBLE
                binding.ivResult.setImageBitmap(bitmap)
                binding.layoutEmptyResult.visibility = View.GONE
                Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
            }
        }

        // Lắng nghe Lỗi
        viewModel.errorMessage.observe(this) { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            if (binding.ivResult.drawable == null) {
                binding.layoutEmptyResult.visibility = View.VISIBLE
            }
        }
    }

    private fun validateAndTryOn() {
        val personUri = personImageUri

        // Kiểm tra ảnh người
        if (personUri == null) {
            Toast.makeText(context, "Please select your photo first!", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra ảnh quần áo (Ưu tiên Bitmap từ URL, sau đó đến URI local)
        if (clothBitmapFromUrl == null && clothImageUri == null) {
            Toast.makeText(context, "Please select an outfit or wait for loading!", Toast.LENGTH_SHORT).show()
            return
        }

        // Chuẩn bị Bitmap
        val personBitmap = uriToBitmap(personUri)

        // Logic lấy Bitmap quần áo
        val clothBitmap = if (clothBitmapFromUrl != null) {
            clothBitmapFromUrl // Dùng ảnh từ Product Detail
        } else {
            uriToBitmap(clothImageUri!!) // Dùng ảnh từ thư viện
        }

        if (personBitmap != null && clothBitmap != null) {
            viewModel.performVirtualTryOn(requireContext(), personBitmap, clothBitmap)
        } else {
            Toast.makeText(context, "Error loading image data", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper: Chuyển URI thành Bitmap (Giữ nguyên)
    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val contentResolver = requireContext().contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}