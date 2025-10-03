package com.pbl6.fitme

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.pbl6.fitme.session.SessionManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bật Edge-to-Edge
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Padding cho hệ thống bars
        val navHostFragmentView = findViewById<android.view.View>(R.id.navHostFragment)
        ViewCompat.setOnApplyWindowInsetsListener(navHostFragmentView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Lấy NavHostFragment và NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.app_nav)

        // Kiểm tra session để set startDestination
        val session = SessionManager.getInstance()
        val token = session.getAccessToken(this)
        val startDestinationId = if (!token.isNullOrEmpty() && !session.isAccessTokenExpired(this)) {
            R.id.profileFragment
        } else {
            R.id.splashFragment
        }

        // Set startDestination bằng setter
        navGraph.setStartDestination(startDestinationId)

        // Gán graph cho navController
        navController.graph = navGraph
    }
}
