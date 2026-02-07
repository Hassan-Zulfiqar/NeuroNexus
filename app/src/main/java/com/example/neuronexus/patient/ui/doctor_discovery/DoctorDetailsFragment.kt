package com.example.neuronexus.patient.ui.doctor_discovery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentDoctorDetailsBinding
import com.example.neuronexus.models.Doctor
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.Locale

class DoctorDetailsFragment : Fragment() {

    private var _binding: FragmentDoctorDetailsBinding? = null
    private val binding get() = _binding!!

    // SharedViewModel holds the selected doctor from the previous screen
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var currentDoctor: Doctor? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        sharedViewModel.selectedDoctor.observe(viewLifecycleOwner) { doctor ->
            if (doctor != null) {
                currentDoctor = doctor
                populateUI(doctor)
            } else {
                AlertUtils.showError(requireContext(), "Error loading doctor details.")
                findNavController().popBackStack()
            }
        }
    }

    private fun populateUI(doctor: Doctor) {
        // 1. Name with Dr. Prefix Logic
        val cleanName = doctor.name.trim()
        val displayName = if (cleanName.lowercase(Locale.ROOT).startsWith("dr.") ||
            cleanName.lowercase(Locale.ROOT).startsWith("dr ")) {
            cleanName
        } else {
            "Dr. $cleanName"
        }
        binding.tvDoctorName.text = displayName

        // 2. Qualifications & Specialization
        binding.tvQualifications.text = "${doctor.qualification} - ${doctor.specialization}"

        // 3. Rating
        binding.tvRating.text = String.format("%.1f", doctor.rating)

        // 4. Consultation Fee
        binding.tvConsultationFee.text = if (doctor.consultationFee.isNotEmpty())
            "RS ${doctor.consultationFee}" else "N/A"
        // 5. Patient Count (Placeholder)
        binding.tvPatientCount.text = "100+"

        // 6. Location / Address
        binding.tvLocation.text = if (doctor.clinicAddress.isNotEmpty()) doctor.clinicAddress else "Address not available"

        // 7. Image Loading
        Glide.with(this)
            .load(doctor.profileImageUrl)
            .placeholder(R.drawable.doctor)
            .error(R.drawable.doctor)
            .centerCrop()
            .into(binding.imgDoctor)
    }

    private fun setupListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Book Appointment Button
        binding.btnBookNow.setOnClickListener {
            if (currentDoctor != null) {
                // Navigate to Booking Fragment
                findNavController().navigate(R.id.action_details_to_booking)
            }
        }

        // See All Reviews Click
        binding.tvSeeAllReviews.setOnClickListener {
            Toast.makeText(requireContext(), "See All Reviews Clicked", Toast.LENGTH_SHORT).show()
        }

        // Map Card Click Listener -> Open Google Maps
        binding.cardMap.setOnClickListener {
            val address = currentDoctor?.clinicAddress
            if (!address.isNullOrEmpty()) {
                val uri = "geo:0,0?q=${Uri.encode(address)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.setPackage("com.google.android.apps.maps")

                // Verify that Google Maps is installed
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                } else {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}"))
                    startActivity(browserIntent)
                }
            } else {
                Toast.makeText(requireContext(), "Address not available for map", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}