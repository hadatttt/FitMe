package com.pbl6.fitme.register

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentRegisterBinding
import com.pbl6.fitme.untils.singleClick

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private var editTextValue1: String = ""

    // Launcher để mở camera và nhận bitmap
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // CountryCodePicker + Phone
//        binding.countryCodePicker.setOnCountryChangeListener {
//            val countryCode = binding.countryCodePicker.selectedCountryCode
//            val phone = binding.etPhoneNumber.text.toString()
//            val fullNumber = "+$countryCode$phone"
//            // TODO: xử lý fullNumber nếu cần
//        }

        // TextWatcher cho email
        binding.etEmail.addTextChangedListener { s ->
            editTextValue1 = s.toString()
        }

        // Upload / mở camera
        binding.ivUpload.singleClick {
            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
        }

        // Cancel
        binding.tvCancel.singleClick {
            findNavController().navigate(R.id.action_register_to_slash)
        }

        // Done
        binding.btnDone.singleClick {
            findNavController().navigate(R.id.action_register_to_hello)
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
