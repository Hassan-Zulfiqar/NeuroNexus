package com.example.neuronexus.patient.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.example.neuronexus.R
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel

class DoctorDiscoveryActivity : AppCompatActivity() {

    // ADDED: Koin ViewModel Initializations
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by viewModel()
    
    // Flag to track if we came from search
    private var fromSearch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_discovery)

        // Check if coming from search flow
        fromSearch = intent.getBooleanExtra("fromSearch", false)

        // Handle back press when coming from search
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fromSearch) {
                    // Coming from search - finish activity to return to search
                    finish()
                } else {
                    // Normal flow - use default navigation back behavior
                    isEnabled = false
                    onBackPressed()
                }
            }
        })

        // ==========================================
        // ADDED: Handle Search Flow (Direct to Doctor Details)
        // ==========================================
        val doctorIdFromSearch = intent.getStringExtra("doctorId")
        if (!doctorIdFromSearch.isNullOrEmpty()) {
            // Show loading dialog
            val loadingDialog = android.app.AlertDialog.Builder(this)
                .setTitle("Loading Doctor")
                .setMessage("Please wait...")
                .setCancelable(false)
                .show()

            // Fetch the doctor details
            networkViewModel.fetchDoctorDetails(doctorIdFromSearch)

            // Observe and navigate to details
            networkViewModel.doctorDetails.observe(this) { result ->
                result.onSuccess { doctor ->
                    sharedViewModel.selectDoctor(doctor)
                    loadingDialog.dismiss()

                    val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as androidx.navigation.fragment.NavHostFragment
                    val navController = navHostFragment.navController
                    
                    // Clear back stack when coming from search
                    val navOptions = androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.doctorListFragment, inclusive = true)
                        .build()
                    navController.navigate(R.id.doctorDetailsFragment, null, navOptions)
                }.onFailure { error ->
                    loadingDialog.dismiss()
                    android.widget.Toast.makeText(this, "Failed to load doctor: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            return
        }

        // ==========================================
        // ADDED: Intercept Logic for Doctor Rebook
        // ==========================================
        if (intent.getStringExtra("NAVIGATE_TO") == "SCHEDULE") {
            val doctorId = intent.getStringExtra("DOCTOR_ID")
            val previousBookingId = intent.getStringExtra("PREVIOUS_BOOKING_ID")

            if (!doctorId.isNullOrEmpty()) {
                // 1. Show non-cancelable loading dialog immediately
                val loadingDialog = android.app.AlertDialog.Builder(this)
                    .setTitle("Loading Doctor")
                    .setMessage("Please wait while we retrieve current availability...")
                    .setCancelable(false)
                    .show()

                // 2. Fetch the live Doctor object
                networkViewModel.fetchDoctorDetails(doctorId)

                // 3. Observe the result to populate and navigate
                networkViewModel.doctorDetails.observe(this) { result ->
                    result.onSuccess { liveDoctor ->
                        // Populate SharedViewModel for the booking flow
                        sharedViewModel.selectDoctor(liveDoctor)

                        if (!previousBookingId.isNullOrEmpty()) {
                            sharedViewModel.setPreviousBookingId(previousBookingId)
                        }

                        loadingDialog.dismiss()

                        // Navigate safely using supportFragmentManager pattern
                        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as androidx.navigation.fragment.NavHostFragment
                        val navController = navHostFragment.navController
                        navController.navigate(R.id.scheduleSelectionFragment)

                    }.onFailure { error ->
                        loadingDialog.dismiss()
                        android.widget.Toast.makeText(this, "Failed to load doctor: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
                        finish() // Return user to the previous screen safely
                    }
                }
            }
        }
    }

    // override fun onBackPressed() {
    //     // If coming from search, finish the activity to return to search
    //     if (fromSearch) {
    //         finish()
    //     } else {
    //         super.onBackPressed()
    //     }
    // }
}