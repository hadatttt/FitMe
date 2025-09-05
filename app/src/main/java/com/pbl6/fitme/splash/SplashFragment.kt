package com.pbl6.fitme.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    // Biến binding
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Chuyển sang RegisterFragment
        binding.btnGetStarted.setOnClickListener {
            findNavController().navigate(R.id.action_splash_to_register)
        }

        // Chuyển sang LoginFragment
        binding.ivNextLogin.setOnClickListener {
            findNavController().navigate(R.id.action_splash_to_login)
        }
    }
}
