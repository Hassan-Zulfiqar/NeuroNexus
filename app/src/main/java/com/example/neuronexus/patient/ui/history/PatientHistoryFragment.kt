package com.example.neuronexus.patient.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.databinding.FragmentPatientHistoryBinding
import com.example.neuronexus.patient.adapters.PatientHistoryPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class PatientHistoryFragment : Fragment() {

    private var _binding: FragmentPatientHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PatientHistoryPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // 2. Connect TabLayout with ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Consultations"
                1 -> "Labs"
                2 -> "Analysis"
                else -> null
            }
        }.attach()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}