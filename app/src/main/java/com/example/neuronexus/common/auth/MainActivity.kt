package com.example.neuronexus.common.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.AuthViewModel
import com.example.neuronexus.doctor.activities.DoctorDashboardActivity
import com.example.neuronexus.patient.activities.PatientDashboardActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    // Inject ViewModel
    private val viewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // UI Animations
        val logo = findViewById<ImageView>(R.id.logo)
        val tagline = findViewById<TextView>(R.id.tagline)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)

        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_logo_animation)
        val taglineAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_tagline_animation)

        logo.startAnimation(logoAnimation)
        tagline.startAnimation(taglineAnimation)

        Handler(Looper.getMainLooper()).postDelayed({
            progressBar.visibility = android.view.View.VISIBLE
        }, 1500)

        Handler(Looper.getMainLooper()).postDelayed({
            checkOnboardingStatus()
        }, 3000)

        // Setup Observer for Auto-Login Logic
        setupObservers()
    }

    private fun checkOnboardingStatus() {
        val sharedPref = getSharedPreferences("OnboardingPref", MODE_PRIVATE)
        val isFinished = sharedPref.getBoolean("isOnboardingFinished", false)

        if (isFinished)
        {
            // Onboarding is done, check if user is logged in
            //viewModel.checkCurrentUser()

            // for testing
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        }
        else
        {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupObservers()
    {
        viewModel.userState.observe(this) { result ->
            result.onSuccess { user ->
                // User Found! Navigate to Dashboard based on Role
                when (user.role)
                {
                    "doctor" -> navigateTo(DoctorDashboardActivity::class.java)
                    "patient" -> navigateTo(PatientDashboardActivity::class.java)
                    "admin" -> AlertUtils.showError(this, "Please login via Web Portal")
                    else -> navigateTo(LoginActivity::class.java) // Fallback for new/unknown users
                }
            }
            result.onFailure {
                // No user logged in (or error), go to Login
                navigateTo(LoginActivity::class.java)
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>)
    {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}