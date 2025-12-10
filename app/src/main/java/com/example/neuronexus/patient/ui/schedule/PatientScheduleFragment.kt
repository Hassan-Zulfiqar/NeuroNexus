package com.example.neuronexus.patient.ui.schedule

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.patient.adapters.PatientAppointmentAdapter
import com.example.neuronexus.databinding.FragmentPatientScheduleBinding

class PatientScheduleFragment : Fragment() {

    private var _binding: FragmentPatientScheduleBinding? = null
    private val binding get() = _binding!!

    private lateinit var scheduleViewModel: PatientScheduleViewModel
    private lateinit var adapter: PatientAppointmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        scheduleViewModel = ViewModelProvider(this).get(PatientScheduleViewModel::class.java)

        _binding = FragmentPatientScheduleBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val initialList = scheduleViewModel.getUpcomingList()

        adapter = PatientAppointmentAdapter(initialList)
        binding.rvSchedule.layoutManager = LinearLayoutManager(context)
        binding.rvSchedule.adapter = adapter

        binding.tabUpcoming.setOnClickListener {
            selectUpcomingTab()
        }
        binding.tabPast.setOnClickListener {
            selectPastTab()
        }

        return root
    }

    private fun selectUpcomingTab() {
        val list = scheduleViewModel.getUpcomingList()
        adapter.updateList(list)

        binding.tabUpcoming.setBackgroundResource(R.drawable.bg_white_rounded)
        binding.tabUpcoming.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.primary_blue)
        )
        binding.tabUpcoming.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_white))

        binding.tabPast.setBackgroundResource(android.R.color.transparent)
        binding.tabPast.backgroundTintList = null
        binding.tabPast.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
    }

    private fun selectPastTab() {
        val list = scheduleViewModel.getPastList()
        adapter.updateList(list)

        binding.tabPast.setBackgroundResource(R.drawable.bg_white_rounded)
        binding.tabPast.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.primary_blue)
        )
        binding.tabPast.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_white))

        binding.tabUpcoming.setBackgroundResource(R.drawable.bg_white_rounded)
        binding.tabUpcoming.backgroundTintList = null
        binding.tabUpcoming.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

