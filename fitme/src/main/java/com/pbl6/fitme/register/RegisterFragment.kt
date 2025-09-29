package com.pbl6.fitme.register

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import com.pbl6.fitme.R
import androidx.core.widget.addTextChangedListener
import hoang.dqm.codebase.base.activity.navigate
import com.pbl6.fitme.databinding.FragmentRegisterBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.utils.singleClick
import androidx.activity.result.contract.ActivityResultContracts

class RegisterFragment : BaseFragment<FragmentRegisterBinding, RegisterViewModel>() {
    private var editTextValue1: String = ""
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val photo: Bitmap? = data?.extras?.get("data") as? Bitmap
                if (photo != null) {
                    binding.ivUpload.setImageBitmap(photo)
                } else {
                    Toast.makeText(requireContext(), "Cannot capture image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    override fun initView() {
        // TextWatcher cho email
        binding.etEmail.addTextChangedListener { s ->
            editTextValue1 = s.toString()
        }
    }

    override fun initListener() {
        binding.ivUpload.singleClick {
            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
        }
        binding.tvCancel.singleClick {
            navigate(R.id.action_registerFragment_to_splashFragment)
        }
        binding.btnDone.singleClick {
            navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    override fun initData() {
    }
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }
}
