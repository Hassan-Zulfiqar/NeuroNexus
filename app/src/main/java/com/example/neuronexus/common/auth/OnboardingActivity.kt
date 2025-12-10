package com.example.neuronexus.common.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.neuronexus.common.adapters.OnboardingAdapter
import com.example.neuronexus.common.models.OnboardingItem
import com.example.neuronexus.common.utils.ZoomOutPageTransformer
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.neuronexus.R

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var buttonNext: MaterialButton
    private lateinit var buttonSkip: MaterialButton
    private lateinit var adapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayoutDots)
        buttonNext = findViewById(R.id.buttonNext)
        buttonSkip = findViewById(R.id.buttonSkip)


        val onboardingItems = listOf(
            OnboardingItem(
                title = "Smarter Healthcare With \nAI Assistance",
                description = "Advanced AI supporting accurate and timely healthcare decisions.",
                imageResId = R.drawable.onboarding1
            ),
            OnboardingItem(
                title = "Manage Medical Records\nSeamlessly",
                description = "Store, Organize and access your medical files " +
                        "securely and effortlessly.",
                imageResId = R.drawable.onboarding2
            ),
            OnboardingItem(
                title = "Connect With Trusted\nHealthcare Providers",
                description = "Find trusted clinics, labs, and specialists — all within one " +
                        "unified healthcare platform.",
                imageResId = R.drawable.onboarding3
            )
        )


        adapter = OnboardingAdapter(onboardingItems)
        viewPager.adapter = adapter
        viewPager.setPageTransformer(ZoomOutPageTransformer())

        TabLayoutMediator(tabLayout, viewPager) { _, _ ->

        }.attach()


        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == adapter.itemCount - 1) {
                    buttonNext.text = "Get Started"
                    buttonSkip.visibility = View.INVISIBLE
                } else {
                    buttonNext.text = "Next"
                    buttonSkip.visibility = View.VISIBLE
                }
            }
        })


        buttonNext.setOnClickListener {
            if (viewPager.currentItem < adapter.itemCount - 1) {

                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                navigateToHome()
            }
        }

        buttonSkip.setOnClickListener {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        val sharedPref = getSharedPreferences("OnboardingPref", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("isOnboardingFinished", true)
        editor.apply()

        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}

