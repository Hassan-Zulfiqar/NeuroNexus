package com.example.neuronexus.patient.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.common.activities.NotificationsActivity
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentPatientHomeBinding
import com.example.neuronexus.models.Doctor
import com.example.neuronexus.models.PatientDashboardService
import com.example.neuronexus.patient.activities.DoctorDiscoveryActivity
import com.example.neuronexus.patient.adapters.PatientServiceAdapter
import com.example.neuronexus.patient.models.DoctorAppointment
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

class PatientHomeFragment : Fragment() {

    private var _binding: FragmentPatientHomeBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        fetchData()
    }

    private fun setupUI() {
        // 1. Setup Services RecyclerView (Static Data for now)
        val serviceList = getServiceList()
        val adapter = PatientServiceAdapter(serviceList)
        binding.rvServices.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvServices.adapter = adapter

        // 2. Setup Click Listeners
        binding.btnNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }

        binding.btnSeeAllDoctors.setOnClickListener {
            startActivity(Intent(requireContext(), DoctorDiscoveryActivity::class.java))
        }

        binding.btnSeeAllServices.setOnClickListener {
            // Logic to see all services
        }

        binding.btnSeeAllSchedule.setOnClickListener {
            // Navigate to the Schedule Tab
            try {
                findNavController().navigate(R.id.navigation_schedule)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchData() {
        // Trigger data fetching from Firebase
        networkViewModel.fetchPatientDetails() // Fetch Patient Profile (Name + Image)
        networkViewModel.fetchMyAppointments()
        networkViewModel.fetchAllDoctors()
    }

    private fun setupObservers() {
        // 1. Observe Patient Details (Greeting & Image)
        networkViewModel.patientDetails.observe(viewLifecycleOwner) { result ->
            result.onSuccess { patient ->
                // Bind Name
                binding.tvGreeting.text = "Hello, ${patient.name}!"

                // Bind Profile Image
                Glide.with(this)
                    .load(patient.profileImageUrl)
                    .placeholder(R.drawable.doctor)
                    .error(R.drawable.doctor)
                    .into(binding.ivProfile)
            }
        }

        // 2. Observe Appointments
        networkViewModel.myAppointments.observe(viewLifecycleOwner) { result ->
            result.onSuccess { bookings ->
                // Date Format matching database: "30 Jan 2026" "08:00 PM"
                val dateFormat = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
                val currentTime = System.currentTimeMillis()

                val upcomingBooking = bookings
                    .filterIsInstance<DoctorAppointment>() // Explicitly cast to DoctorAppointment
                    .mapNotNull { appointment ->
                        try {
                            // Combine date and time strings to parse logic
                            val dateString = "${appointment.appointmentDate} ${appointment.appointmentTime}"
                            val date = dateFormat.parse(dateString)

                            if (date != null && date.time > currentTime) {
                                // Return Pair of (Appointment, Timestamp) for sorting
                                appointment to date.time
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }

                    }
                    .minByOrNull { it.second } // Sort by timestamp (nearest first)
                    ?.first // Get the Appointment object

                if (upcomingBooking != null) {
                    binding.cardUpcoming.root.isVisible = true
                    binding.labelUpcoming.isVisible = true
                    binding.btnSeeAllSchedule.isVisible = true

                    // Bind Text Data
                    binding.cardUpcoming.tvDocName.text = "Dr. ${upcomingBooking.doctorName}"
                    binding.cardUpcoming.tvDocSpec.text = upcomingBooking.doctorSpecialization
                    binding.cardUpcoming.tvDate.text = "${upcomingBooking.appointmentDate}, ${upcomingBooking.appointmentTime}"

                    if (upcomingBooking.doctorImageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(upcomingBooking.doctorImageUrl)
                            .placeholder(R.drawable.doctor)
                            .error(R.drawable.doctor)
                            .into(binding.cardUpcoming.imgDoc)
                    } else {
                        // Fallback for existing appointments or missing URLs
                        binding.cardUpcoming.imgDoc.setImageResource(R.drawable.doctor)
                    }
//                    // Fetch Doctor Image
//                    if (upcomingBooking.doctorId.isNotEmpty()) {
//                        networkViewModel.fetchDoctorDetails(upcomingBooking.doctorId)
//                    }
                } else {
                    // No upcoming appointments
                    binding.cardUpcoming.root.isVisible = false
                    binding.labelUpcoming.isVisible = false
                    binding.btnSeeAllSchedule.isVisible = false
                }
            }.onFailure { error ->
                binding.cardUpcoming.root.isVisible = false
                // AlertUtils.showError(requireContext(), error.message ?: "Failed to load appointments")
            }
        }

//        // 3. Observe Doctor Details (For Appointment Card Image)
//        networkViewModel.doctorDetails.observe(viewLifecycleOwner) { result ->
//            result.onSuccess { doctor ->
//                // Load the image into the appointment card
//                Glide.with(this)
//                    .load(doctor.profileImageUrl)
//                    .placeholder(R.drawable.doctor)
//                    .error(R.drawable.doctor)
//                    .into(binding.cardUpcoming.imgDoc)
//            }
//        }

        // 4. Observe Top Doctors
        networkViewModel.doctorsList.observe(viewLifecycleOwner) { result ->
            result.onSuccess { doctors ->
                if (doctors.isNotEmpty()) {
                    // Sort by Rating (Highest first) and take the top one
                    val topDoctor = doctors.maxByOrNull { it.rating }

                    if (topDoctor != null) {
                        bindTopDoctor(topDoctor)
                    } else {
                        binding.cardTopDoctor.root.isVisible = false
                    }
                } else {
                    binding.cardTopDoctor.root.isVisible = false
                }
            }.onFailure {
                binding.cardTopDoctor.root.isVisible = false
            }
        }

        // 5. Loading State
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Optional: Show/Hide a progress bar
        }
    }

    private fun bindTopDoctor(doctor: Doctor) {
        binding.cardTopDoctor.root.isVisible = true
        binding.cardTopDoctor.tvDoctorName.text = doctor.name
        binding.cardTopDoctor.tvDoctorSpec.text = doctor.specialization
        binding.cardTopDoctor.tvLocation.text = doctor.clinicAddress
        binding.cardTopDoctor.tvReviews.text = "(200)"

        Glide.with(this)
            .load(doctor.profileImageUrl)
            .placeholder(R.drawable.doctor)
            .error(R.drawable.doctor)
            .into(binding.cardTopDoctor.imageDoctor)

        val starViews = listOf(binding.cardTopDoctor.ivStar1, binding.cardTopDoctor.ivStar2, binding.cardTopDoctor.ivStar3,
            binding.cardTopDoctor.ivStar4, binding.cardTopDoctor.ivStar5)
        val rating = doctor.rating.toInt()

        for (i in starViews.indices) {
            if (i < rating) {
                starViews[i].setColorFilter(Color.parseColor("#FFD700"))
            } else {
                starViews[i].setColorFilter(Color.parseColor("#E0E0E0"))
            }
        }

        binding.cardTopDoctor.root.setOnClickListener {
            // Navigate to Doctor Details
            val intent = Intent(requireContext(), DoctorDiscoveryActivity::class.java)
            // Passing ID if needed
            startActivity(intent)
        }
    }

    private fun getServiceList(): List<PatientDashboardService> {
        return listOf(
            PatientDashboardService("Heart Surgeon", R.drawable.heart),
            PatientDashboardService("Dentistry", R.drawable.tooth),
            PatientDashboardService("Neurology", R.drawable.brain),
            PatientDashboardService("Orthopedic", R.drawable.orthopedics)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}