package com.example.neuronexus.patient.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.neuronexus.databinding.ActivityPatientDashboardBinding
import com.example.neuronexus.patient.ui.more.PatientMoreFragment
import com.example.neuronexus.R

class PatientDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->

            val params = binding.navHostFragment.layoutParams as androidx.coordinatorlayout.
            widget.CoordinatorLayout.LayoutParams
            val density = resources.displayMetrics.density

            when (destination.id) {
                R.id.navigation_home,
                R.id.navigation_schedule,
                R.id.navigation_lab -> {
                    binding.bottomNavView.visibility = View.VISIBLE
                    binding.bottomAppBar.visibility = View.VISIBLE
                    binding.fab.show()

                    params.bottomMargin = (60 * density).toInt()
                }
                else -> {
                    binding.bottomNavView.visibility = View.GONE
                    binding.bottomAppBar.visibility = View.GONE
                    binding.fab.hide()

                    params.bottomMargin = 0
                }
            }
            binding.navHostFragment.layoutParams = params
        }

        binding.bottomNavView.setOnItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.navigation_more) {
                val bottomSheet = PatientMoreFragment()
                bottomSheet.show(supportFragmentManager, "PatientMoreFragment")
                false
            } else {
                NavigationUI.onNavDestinationSelected(menuItem, navController)
                true
            }
        }

        binding.fab.setOnClickListener {
            Toast.makeText(this, "Quick Booking Clicked", Toast.LENGTH_SHORT).show()
        }
    }
}

