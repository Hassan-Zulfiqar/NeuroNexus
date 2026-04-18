package com.example.neuronexus.patient.ui.more

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AuthUtils
import com.example.neuronexus.databinding.FragmentPatientMoreBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class PatientMoreFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPatientMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnProfile.setOnClickListener {
            dismiss()

            val navController = findNavController(
                requireActivity(),
                R.id.nav_host_fragment
            )

            navController.navigate(R.id.navigation_profile)
        }

        binding.btnHistory.setOnClickListener {
            dismiss()

            findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(R.id.navigation_patient_history)
        }

        binding.btnMedicalRecords.setOnClickListener {
            dismiss()

            findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(R.id.action_global_to_patient_profile_selector)
        }

        binding.btnSettings.setOnClickListener {
            Toast.makeText(context, "Opening Settings...", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.btnHelp.setOnClickListener {
            dismiss()
            val sharedViewModel: com.example.neuronexus.common.viewmodel.SharedViewModel by sharedViewModel()
            sharedViewModel.selectPatientBooking(null)

            findNavController(
                requireActivity(),
                R.id.nav_host_fragment
            ).navigate(R.id.action_global_to_submit_complaint)
        }

        binding.btnLogout.setOnClickListener {
            requireActivity().let { activity ->
                AuthUtils.logout(activity)
            }
        }
    }


    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

