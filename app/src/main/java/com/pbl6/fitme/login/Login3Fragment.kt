package com.pbl6.fitme.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pbl6.fitme.R

class Login3Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login_3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val button1: View = view.findViewById(R.id.rxvksek4dujg)
        button1.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_login_2)
        }
        val button2: View = view.findViewById(R.id.tv_cancel1)
        button2.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_slash)
        }
    }
}