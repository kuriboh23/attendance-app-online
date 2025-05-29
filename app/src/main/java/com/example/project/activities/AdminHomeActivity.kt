package com.example.project.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.project.R
import com.example.project.UserPrefs
import com.example.project.data.ConnectivityObserver
import com.example.project.databinding.ActivityAdminHomeBinding
import com.example.project.utils.observeConnectivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors

class AdminHomeActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var binding: ActivityAdminHomeBinding

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.networkOverlay.visibility = android.view.View.VISIBLE

        observeConnectivity { status ->
            when (status) {
                ConnectivityObserver.Status.Available -> {
                    binding.networkOverlay.visibility = android.view.View.GONE
                }
                ConnectivityObserver.Status.Unavailable,
                ConnectivityObserver.Status.Losing,
                ConnectivityObserver.Status.Lost -> {
                    binding.networkOverlay.visibility = android.view.View.VISIBLE
                }
            }
        }

        DynamicColors.applyToActivitiesIfAvailable(application)

        bottomNav = findViewById(R.id.bottomNavigationView)
        bottomNav.itemIconTintList = null

        // Setup navigation controller from NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        // Connect bottom nav to navController
        bottomNav.setupWithNavController(navController)
        bottomNav.isItemActiveIndicatorEnabled = false

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_logout) {
                signOut()
                true
            } else {
                navController.navigate(item.itemId)
                true
            }
        }

    }

    private fun signOut() {
        UserPrefs.savedIsLoggedIn(this, false)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }


    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (navController.currentDestination?.id != R.id.nav_home) {
            navController.navigate(R.id.nav_home)
        } else {
            finish()
        }
    }
}
