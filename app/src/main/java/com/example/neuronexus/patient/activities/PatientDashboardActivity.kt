package com.example.neuronexus.patient.activities

import android.content.Intent
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
import com.example.neuronexus.common.utils.Constant
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class PatientDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDashboardBinding

    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by viewModel()

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

                R.id.navigation_search,
                R.id.navigation_lab_details -> {
                    binding.bottomNavView.visibility = View.GONE
                    binding.bottomAppBar.visibility = View.GONE
                    binding.fab.hide()

                    params.bottomMargin = 0
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

        networkViewModel.bookingByIdResult.observe(this) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                val booking = result.getOrNull()
                if (booking != null) {
                    // Set booking in SharedViewModel
                    sharedViewModel.selectPatientBooking(booking)
                    // Navigate to detail screen
//                    val navController = androidx.navigation.Navigation.findNavController(
//                        this,
//                        R.id.nav_host_fragment
//                    )
//                    navController.navigate(R.id.action_global_to_patient_appointment_detail)
                }
            }
            networkViewModel.resetBookingByIdResult()
        }

        binding.fab.setOnClickListener {
            val navController = androidx.navigation.Navigation.findNavController(
                this,
                R.id.nav_host_fragment
            )
            navController.navigate(R.id.action_global_to_search)
        }

        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        val action =
            intent?.getStringExtra(com.example.neuronexus.common.activities.NotificationsActivity.EXTRA_ACTION)
                ?: return
        val bookingId =
            intent.getStringExtra(com.example.neuronexus.common.activities.NotificationsActivity.EXTRA_BOOKING_ID)
                ?: ""

        when (action) {
            com.example.neuronexus.common.activities.NotificationsActivity.ACTION_OPEN_BOOKING_DETAIL -> {
                if (bookingId.isNotEmpty()) {
                    // Fetch booking then navigate to detail
                    Constant.isFromNoti = true
                    networkViewModel.fetchBookingById(bookingId)

                    val navController = androidx.navigation.Navigation.findNavController(
                        this,
                        R.id.nav_host_fragment
                    )
                    navController.navigate(R.id.navigation_patient_appointment_detail)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }
}


