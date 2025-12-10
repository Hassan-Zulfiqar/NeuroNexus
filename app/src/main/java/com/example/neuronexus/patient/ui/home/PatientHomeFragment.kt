package com.example.neuronexus.patient.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.patient.adapters.PatientServiceAdapter
import com.example.neuronexus.databinding.FragmentPatientHomeBinding
import com.example.neuronexus.common.activities.NotificationsActivity

class PatientHomeFragment : Fragment() {

    private var _binding: FragmentPatientHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(PatientHomeViewModel::class.java)

        _binding = FragmentPatientHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val serviceList = homeViewModel.getServiceList()
        val adapter = PatientServiceAdapter(serviceList)

        binding.rvServices.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvServices.adapter = adapter

        binding.btnNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationsActivity::class.java)
            startActivity(intent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

