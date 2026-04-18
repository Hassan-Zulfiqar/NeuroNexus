package com.example.neuronexus.patient.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentWriteReviewBinding
import com.example.neuronexus.patient.models.Booking
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.models.LabTestBooking
import com.example.neuronexus.patient.models.Review
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class WriteReviewFragment : Fragment() {

    private var _binding: FragmentWriteReviewBinding? = null
    private val binding get() = _binding

    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var currentBooking: Booking? = null
    private var selectedRating: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWriteReviewBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        val b = binding ?: return

        b.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Dynamically load appointment data (Safely handles both Doctor and Lab)
        currentBooking = sharedViewModel.selectedPatientBooking.value
        if (currentBooking == null) {
            findNavController().popBackStack()
            return
        }

        when (val booking = currentBooking) {
            is DoctorAppointment -> {
                b.tvReviewEntityName.text = "Dr. ${booking.doctorName}".trim()
                b.tvReviewSpecialization.text = booking.doctorSpecialization.ifBlank { "General Physician" }
                b.tvReviewDate.text = "Appointment: ${booking.appointmentDate}"
            }
            is LabTestBooking -> {
                b.tvReviewEntityName.text = booking.labName.ifBlank { "Laboratory" }
                b.tvReviewSpecialization.text = booking.testName.ifBlank { "Lab Test" }
                b.tvReviewDate.text = "Appointment: ${booking.testDate}"
            }
            else -> {
                findNavController().popBackStack()
                return
            }
        }

        // Rating Bar setup
        b.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            selectedRating = rating
            updateRatingLabel(rating)
            if (rating > 0)
            {
                b.btnSubmitReview.isEnabled = true
                b.btnSubmitReview.setBackgroundColor(resources.getColor(R.color.blue_button))
            }
        }

        // Submit Action
        b.btnSubmitReview.setOnClickListener {
            submitReview()
        }
    }

    private fun updateRatingLabel(rating: Float) {
        val label = when (rating.toInt()) {
            1 -> "1 — Poor"
            2 -> "2 — Fair"
            3 -> "3 — Good"
            4 -> "4 — Very Good"
            5 -> "5 — Excellent"
            else -> "Tap to rate"
        }
        binding?.tvRatingLabel?.text = label
    }

    private fun submitReview() {
        val booking = currentBooking ?: return
        val rating = selectedRating
        if (rating == 0f) return

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val comment = binding?.etComment?.text?.toString()?.trim() ?: ""

        val entityId: String
        val entityType: String

        // Polymorphic Extraction
        if (booking is DoctorAppointment) {
            entityId = booking.doctorId
            entityType = "doctor"
        } else if (booking is LabTestBooking) {
            entityId = booking.labId
            entityType = "lab"
        } else {
            return
        }

        val review = Review(
            appointmentId = booking.bookingId,
            reviewedEntityId = entityId,
            reviewedEntityType = entityType,
            reviewerId = currentUser.uid,
            reviewerName = booking.patientNameSnapshot,
            reviewerRole = "patient",
            rating = rating,
            comment = comment
        )

        networkViewModel.saveReview(review)
    }

    private fun setupObservers() {
        networkViewModel.saveReviewResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    "Review submitted successfully",
                    Toast.LENGTH_SHORT
                ).show()
                networkViewModel.resetSaveReviewResult()
                findNavController().popBackStack()
            } else {
                Toast.makeText(
                    requireContext(),
                    result.exceptionOrNull()?.message ?: "Failed to submit review",
                    Toast.LENGTH_SHORT
                ).show()
                networkViewModel.resetSaveReviewResult()
            }
        }

        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            val b = binding ?: return@observe
            b.btnSubmitReview.isEnabled = !isLoading && selectedRating > 0
            if (isLoading) {
                b.btnSubmitReview.text = "Submitting..."
            } else {
                b.btnSubmitReview.text = "Submit Review"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}