package com.example.neuronexus.patient.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.common.activities.NotificationsActivity
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentPatientHomeBinding
import com.example.neuronexus.doctor.models.Doctor
import com.example.neuronexus.models.PatientDashboardService
import com.example.neuronexus.patient.activities.DoctorDiscoveryActivity
import com.example.neuronexus.patient.adapters.PatientServiceAdapter
import com.example.neuronexus.patient.models.Booking
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.models.LabTestBooking
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

class PatientHomeFragment : Fragment() {

    private var _binding: FragmentPatientHomeBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()

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

        // 3. New Card Click Listeners
        binding.cardFindDoctor.setOnClickListener {
            startActivity(Intent(requireContext(), DoctorDiscoveryActivity::class.java))
        }

        binding.cardBookLabTest.setOnClickListener {
            try {
                findNavController().navigate(R.id.navigation_lab)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.cardTumorRiskAnalysis.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_disclaimer)
        }

        // 4. SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchData()
        }
    }

    private fun fetchData() {
        // Trigger data fetching from Firebase
        networkViewModel.fetchPatientDetails() // Fetch Patient Profile (Name + Image)
        networkViewModel.fetchMyAppointments()
        networkViewModel.fetchAllDoctors()
    }

    // Helper to safely extract fields for sorting without crashing
    private fun extractBookingDetails(booking: Booking): Triple<String, String, String> {
        return when (booking) {
            is DoctorAppointment -> Triple(booking.appointmentDate, booking.appointmentTime, booking.status)
            is LabTestBooking -> Triple(booking.testDate, booking.testTime, booking.status)
            else -> Triple("", "", "")
        }
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

        // 2. Observe Appointments (UNIFIED DOCTOR + LAB)
        networkViewModel.myAppointments.observe(viewLifecycleOwner) { result ->
            result.onSuccess { bookings ->
                val dateFormat = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
                val currentTime = System.currentTimeMillis()

                val upcomingBooking = bookings
                    .mapNotNull { booking ->
                        try {
                            // Extract dates safely regardless of model type
                            val (dateStr, timeStr, status) = extractBookingDetails(booking)

                            // Safety check: don't show cancelled or completed appointments on the dashboard
                            if (status == "cancelled" || status == "completed") {
                                return@mapNotNull null
                            }

                            val dateString = "$dateStr $timeStr"
                            val date = dateFormat.parse(dateString)

                            if (date != null && date.time > currentTime) {
                                // Return Pair of (Booking, Timestamp) for sorting
                                booking to date.time
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .minByOrNull { it.second } // Sort by timestamp (nearest first)
                    ?.first // Get the actual Booking object

                if (upcomingBooking != null) {
                    binding.cardUpcoming.root.isVisible = true
                    binding.labelUpcoming.isVisible = true
                    binding.btnSeeAllSchedule.isVisible = true

                    // Dynamically bind the UI based on what type won the "Upcoming" slot
                    when (upcomingBooking) {
                        is DoctorAppointment -> {
                            binding.cardUpcoming.tvProviderName.text = "Dr. ${upcomingBooking.doctorName}"
                            binding.cardUpcoming.tvServiceName.text = upcomingBooking.doctorSpecialization
                            binding.cardUpcoming.tvDate.text = "${upcomingBooking.appointmentDate}, ${upcomingBooking.appointmentTime}"

                            if (upcomingBooking.doctorImageUrl.isNotEmpty()) {
                                Glide.with(this)
                                    .load(upcomingBooking.doctorImageUrl)
                                    .placeholder(R.drawable.doctor)
                                    .error(R.drawable.doctor)
                                    .into(binding.cardUpcoming.imgDoc)
                            } else {
                                binding.cardUpcoming.imgDoc.setImageResource(R.drawable.doctor)
                            }
                        }
                        is LabTestBooking -> {
                            // Map Lab properties to the existing UI Views
                            binding.cardUpcoming.tvProviderName.text = upcomingBooking.labName
                            binding.cardUpcoming.tvServiceName.text = upcomingBooking.testName
                            binding.cardUpcoming.tvDate.text = "${upcomingBooking.testDate}, ${upcomingBooking.testTime}"

                            if (upcomingBooking.labImageUrl.isNotEmpty()) {
                                Glide.with(this)
                                    .load(upcomingBooking.labImageUrl)
                                    .placeholder(R.drawable.doctor) // fallback placeholder
                                    .error(R.drawable.doctor)
                                    .into(binding.cardUpcoming.imgDoc)
                            } else {
                                binding.cardUpcoming.imgDoc.setImageResource(R.drawable.doctor)
                            }
                        }
                    }

//                    // Fetch Doctor Image (Legacy commented code preserved)
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

            networkViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
                updateNotificationBadge(count)
            }

        }


        // 5. Loading State — also stops SwipeRefresh spinner when done
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
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

    private fun updateNotificationBadge(count: Int) {
        val badge = binding?.tvNotificationBadge ?: return
        if (count <= 0) {
            badge.visibility = View.GONE
        } else {
            badge.visibility = View.VISIBLE
            badge.text = if (count > 9) "9+" else count.toString()
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = networkViewModel.getCurrentUserUid()
        if (!uid.isNullOrEmpty()) {
            networkViewModel.startListeningToUnreadCount(uid)
        }
        networkViewModel.checkAndExpirePendingAppointments()
    }

    override fun onPause() {
        super.onPause()
        networkViewModel.stopListeningToUnreadCount()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}