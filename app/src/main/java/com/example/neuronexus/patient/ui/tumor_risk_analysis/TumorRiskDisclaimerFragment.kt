package com.example.neuronexus.patient.ui.tumor_risk_analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.databinding.FragmentTumorRiskDisclaimerBinding

class TumorRiskDisclaimerFragment : Fragment() {

    private var _binding: FragmentTumorRiskDisclaimerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTumorRiskDisclaimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Button starts disabled with grey tint — same pattern as symptom checker
        binding.btnStartAssessment.isEnabled = false
        binding.btnStartAssessment.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.text_hint)
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        // Checkbox toggles the button state
        binding.cbUnderstand.setOnCheckedChangeListener { _, isChecked ->
            binding.btnStartAssessment.isEnabled = isChecked
            binding.btnStartAssessment.backgroundTintList = if (isChecked) {
                ContextCompat.getColorStateList(requireContext(), R.color.primary_blue)
            } else {
                ContextCompat.getColorStateList(requireContext(), R.color.text_hint)
            }
        }

        binding.btnStartAssessment.setOnClickListener {
            findNavController().navigate(R.id.action_disclaimer_to_risk_analysis)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}