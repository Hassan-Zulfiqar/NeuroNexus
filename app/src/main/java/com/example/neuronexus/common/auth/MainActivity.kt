package com.example.neuronexus.common.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.AnimationUtils
import com.example.neuronexus.R
import com.example.neuronexus.common.auth.OnboardingActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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
    }

    private fun checkOnboardingStatus() {
        val sharedPref = getSharedPreferences("OnboardingPref", MODE_PRIVATE)
        val isFinished = sharedPref.getBoolean("isOnboardingFinished", false)

        if (isFinished) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
        }

        finish()
    }
}

