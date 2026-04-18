package com.example.neuronexus.doctor.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.neuronexus.databinding.ActivityDoctorDashboardBinding
import com.example.neuronexus.R
import com.example.neuronexus.common.activities.NotificationsActivity
import com.example.neuronexus.common.utils.Constant
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.doctor.ui.more.DoctorMoreFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorDashboardBinding
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_doctor) as NavHostFragment
        val navController = navHostFragment.navController

        binding.doctorBottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->

            when (destination.id) {
                R.id.navigation_doctor_home,
                R.id.navigation_doctor_search,
                R.id.navigation_doctor_schedule -> {

                    binding.doctorBottomNav.visibility = View.VISIBLE
                }
                else -> {

                    binding.doctorBottomNav.visibility = View.GONE
                }
            }
        }

        // Observe Doctor Appointment By ID (From Notification)
        networkViewModel.doctorAppointmentByIdResult.observe(this) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                val appointment = result.getOrNull()
                if (appointment != null) {
                    // Set appointment in SharedViewModel and navigate
                    sharedViewModel.selectDoctorAppointment(appointment)
                    // val navController = androidx.navigation.Navigation.findNavController(
                    //     this,
                    //     R.id.nav_host_fragment_doctor
                    // )
                    // navController.navigate(R.id.navigation_doctor_appointment_detail)
                }
            }
            networkViewModel.resetDoctorAppointmentByIdResult()
        }

        binding.doctorBottomNav.setOnItemSelectedListener { menuItem ->

            if (menuItem.itemId == R.id.navigation_doctor_more) {
                val bottomSheet = DoctorMoreFragment()
                bottomSheet.show(supportFragmentManager, "DoctorMoreFragment")
                false
            } else {
                NavigationUI.onNavDestinationSelected(menuItem, navController)
                true
            }
        }

        handleNavigationIntent(intent)
    }

//    private fun handleNavigationIntent(intent: Intent?) {
//        val action = intent?.getStringExtra(com.example.neuronexus.common.activities.NotificationsActivity.EXTRA_ACTION) ?: return
//
//        when (action) {
//            NotificationsActivity.ACTION_OPEN_SCHEDULE -> {
//                // Navigate to Schedule tab in bottom nav
//                binding.doctorBottomNav.selectedItemId = R.id.navigation_doctor_schedule
//            }
//            NotificationsActivity.ACTION_OPEN_HISTORY -> {
//                // Use NavController to navigate to history destination
//                val navController = findNavController(
//                    this,
//                    R.id.nav_host_fragment_doctor
//                )
//                navController.navigate(R.id.navigation_doctor_history)
//            }
//        }
//    }



    private fun handleNavigationIntent(intent: Intent?) {
        val action =
            intent?.getStringExtra(com.example.neuronexus.common.activities.NotificationsActivity.EXTRA_ACTION)
                ?: return
        val bookingId =
            intent.getStringExtra(com.example.neuronexus.common.activities.NotificationsActivity.EXTRA_BOOKING_ID)
                ?: ""

        when (action) {
            com.example.neuronexus.common.activities.NotificationsActivity.ACTION_OPEN_DOCTOR_BOOKING_DETAIL -> {
                if (bookingId.isNotEmpty()) {
                    // Fetch appointment then navigate to detail
                    Constant.isFromNoti = true
                    networkViewModel.fetchDoctorAppointmentById(bookingId)

                    val navController = androidx.navigation.Navigation.findNavController(
                        this,
                        R.id.nav_host_fragment_doctor
                    )
                    navController.navigate(R.id.navigation_doctor_appointment_detail)
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

