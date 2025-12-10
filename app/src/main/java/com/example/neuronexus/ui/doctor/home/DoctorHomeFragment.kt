package com.example.neuronexus.ui.doctor.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.adapters.DoctorAppointmentAdapter
import com.example.neuronexus.databinding.FragmentDoctorHomeBinding
import com.example.neuronexus.ui.common.ImagePickerHelper
import com.example.neuronexus.ui.common.ImageSelectionDialog
import com.example.neuronexus.doctor.activities.DetectionActivity

class DoctorHomeFragment : Fragment() {

    private var _binding: FragmentDoctorHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DoctorHomeViewModel

    // 1. Declare the Helper
    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(DoctorHomeViewModel::class.java)
        _binding = FragmentDoctorHomeBinding.inflate(inflater, container, false)

        // 2. Initialize the Helper
        imagePickerHelper = ImagePickerHelper(this) { imageUri ->
            onImageSelected(imageUri)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupQuickAccess()
        setupAppointmentsList()
        setupNotificationClick()
    }

    private fun setupNotificationClick() {
        binding.btnNotification.setOnClickListener {
            val intent = Intent(requireContext(), com.example.neuronexus.common.activities.NotificationsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupQuickAccess() {
        // Tumor Detection Card
        binding.cardDetectTumor.tvTitle.text = "Detect Tumor"
        binding.cardDetectTumor.imgIcon.setImageResource(R.drawable.brain_tumor) // or ic_brain if you have it

        binding.cardDetectTumor.root.setOnClickListener {
            showImageSelectionDialog()
        }

        // History Card (Placeholder)
        binding.cardHistory.tvTitle.text = "History"
        binding.cardHistory.imgIcon.setImageResource(R.drawable.history)
        binding.cardHistory.root.setOnClickListener {
            androidx.navigation.Navigation.findNavController(binding.root)
                .navigate(R.id.navigation_doctor_history)
        }
    }

    private fun showImageSelectionDialog() {
        // Create the dialog and pass the click actions
        val dialog = ImageSelectionDialog(
            onCameraClick = {
                imagePickerHelper.openCamera()
            },
            onGalleryClick = {
                imagePickerHelper.openGallery()
            }
        )
        dialog.show(parentFragmentManager, "ImageSelectionDialog")
    }

    private fun onImageSelected(uri: Uri) {
        val intent = Intent(requireContext(), DetectionActivity::class.java)
        intent.putExtra("image_uri", uri)
        startActivity(intent)
    }

    private fun setupAppointmentsList() {
        binding.rvAppointments.layoutManager = LinearLayoutManager(context)
        binding.rvAppointments.isNestedScrollingEnabled = false

        viewModel.appointments.observe(viewLifecycleOwner) { appointmentList ->
            val adapter = DoctorAppointmentAdapter(appointmentList)
            binding.rvAppointments.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}