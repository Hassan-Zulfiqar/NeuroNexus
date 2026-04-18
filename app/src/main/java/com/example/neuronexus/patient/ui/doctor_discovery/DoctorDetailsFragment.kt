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
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentDoctorDetailsBinding
import com.example.neuronexus.doctor.models.Doctor
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class DoctorDetailsFragment : Fragment() {

    private var _binding: FragmentDoctorDetailsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by sharedViewModel()
    private val networkViewModel: NetworkViewModel by viewModel()

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

                if (doctor.reviewCount > 0) {
                    networkViewModel.fetchEntityReviews(doctor.uid)
                }
            } else {
                AlertUtils.showError(requireContext(), "Error loading doctor details.")
                findNavController().popBackStack()
            }
        }

        // Observe and bind the fetched reviews
        networkViewModel.entityReviews.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                val reviews = result.getOrNull() ?: emptyList()
                populateReviews(reviews)
            }
            networkViewModel.resetEntityReviews()
        }
    }

    private fun populateUI(doctor: Doctor) {

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
        binding.tvRating.text = String.format(Locale.getDefault(), "%.1f", doctor.rating)

        // 4. Consultation Fee
        binding.tvConsultationFee.text = if (doctor.consultationFee.isNotEmpty())
            "RS ${doctor.consultationFee}" else "N/A"

        // Dynamically parse the total review count
        binding.tvReviewCount.text = when {
            doctor.reviewCount == 0 -> "No reviews yet"
            doctor.reviewCount == 1 -> "1 review"
            doctor.reviewCount < 1000 -> "${doctor.reviewCount} reviews"
            else -> "${doctor.reviewCount / 1000}k reviews"
        }

        if (doctor.reviewCount == 0) {
            binding.reviewsHeader.visibility = View.GONE
            binding.cardReview1.visibility = View.GONE
            binding.cardReview2.visibility = View.GONE
        }
        else if (doctor.reviewCount == 1)
        {
            binding.reviewsHeader.visibility = View.VISIBLE
            binding.tvSeeAllReviews.visibility = View.GONE
            binding.cardReview1.visibility = View.VISIBLE
            binding.cardReview2.visibility = View.GONE
        }
        else
        {
            binding.reviewsHeader.visibility = View.VISIBLE
            binding.cardReview1.visibility = View.VISIBLE
            binding.cardReview2.visibility = View.VISIBLE
        }

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
            // Try to pop back stack, if nothing to pop, finish the activity
            if (!findNavController().popBackStack()) {
                requireActivity().finish()
            }
        }

        // Book Appointment Button
        binding.btnBookNow.setOnClickListener {
            if (currentDoctor != null) {
                sharedViewModel.selectDate(0L)
                sharedViewModel.selectTimeSlot("")
                sharedViewModel.setBookingReason("")

                findNavController().navigate(R.id.action_details_to_booking)
            } else {
                AlertUtils.showError(requireContext(), "Please wait for doctor details to load.")
            }
        }

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

    private fun populateReviews(reviews: List<com.example.neuronexus.patient.models.Review>) {
        val b = binding ?: return

        if (reviews.isEmpty()) {
            b.reviewsHeader.visibility = View.GONE
            b.cardReview1.visibility = View.GONE
            b.cardReview2.visibility = View.GONE
        } else {
            b.reviewsHeader.visibility = View.VISIBLE

            // Bind Review 1
            b.cardReview1.visibility = View.VISIBLE
            val r1 = reviews[0]
            if (r1.comment.isNotBlank()) {
                b.tvReview1.visibility = View.VISIBLE
                b.tvReview1.text = "\"${r1.comment}\""
            } else {
                b.tvReview1.visibility = View.VISIBLE
                b.tvReview1.text = "\"No comments\""
            }
            b.tvReviewer1.text = "- ${r1.reviewerName}"
            b.tvInitials1.text = getInitials(r1.reviewerName)

            // Bind Review 2 (if it exists)
            if (reviews.size > 1) {
                b.tvSeeAllReviews.visibility = View.VISIBLE
                b.cardReview2.visibility = View.VISIBLE
                val r2 = reviews[1]
                if (r2.comment.isNotBlank()) {
                    b.tvReview2.visibility = View.VISIBLE
                    b.tvReview2.text = "\"${r2.comment}\""
                } else {
                    b.tvReview2.visibility = View.VISIBLE
                    b.tvReview2.text = "\"No comments\""
                }
                b.tvReviewer2.text = "- ${r2.reviewerName}"
                b.tvInitials2.text = getInitials(r2.reviewerName)
            } else {
                b.tvSeeAllReviews.visibility = View.GONE
                b.cardReview2.visibility = View.GONE
            }
        }
    }

    private fun getInitials(name: String): String {
        if (name.isBlank()) return "AN" // Anonymous
        val parts = name.trim().split(" ")
        return if (parts.size >= 2) {
            "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        } else {
            parts[0].take(2).uppercase()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}