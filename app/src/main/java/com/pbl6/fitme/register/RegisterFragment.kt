package com.pbl6.fitme.register

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.pbl6.fitme.R

class RegisterFragment : Fragment() {

    private var editTextValue1: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ccp = view.findViewById<com.hbb20.CountryCodePicker>(R.id.countryCodePicker)
        val etPhone = view.findViewById<EditText>(R.id.etPhoneNumber)

        // Khi người dùng chọn quốc gia
        ccp.setOnCountryChangeListener {
            val countryCode = ccp.selectedCountryCode   // vd: "84"
            val fullNumber = "+$countryCode${etPhone.text}"
            println("Số đầy đủ: $fullNumber")
        }

        // EditText
        val editText1: EditText = view.findViewById(R.id.action_register_to_slash)
        editText1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                editTextValue1 = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

//        // Button Done
//        val button1: View = view.findViewById(R.id.rbgl7d5rbxk9)
//        button1.setOnClickListener {
//            if (editTextValue1.isNotEmpty()) {
//                Toast.makeText(requireContext(), "Email: $editTextValue1", Toast.LENGTH_SHORT).show()
//                // Chuyển sang HomeFragment bằng Navigation
//                findNavController().navigate(R.id.action_register_to_slash)
//            } else {
//                Toast.makeText(requireContext(), "Please enter your email!", Toast.LENGTH_SHORT).show()
//            }
//        }

        val button2: View = view.findViewById(R.id.tv_cancel)
        button2.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_slash)
        }
    }
}
