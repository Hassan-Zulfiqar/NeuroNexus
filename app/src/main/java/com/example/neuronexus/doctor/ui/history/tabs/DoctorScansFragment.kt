package com.example.neuronexus.doctor.ui.history.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.databinding.FragmentDoctorScansBinding
import com.example.neuronexus.doctor.adapters.DoctorScanHistoryAdapter

class DoctorScansFragment : Fragment() {

    private var _binding: FragmentDoctorScansBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DoctorScansViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(DoctorScansViewModel::class.java)
        _binding = FragmentDoctorScansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvScans.layoutManager = LinearLayoutManager(context)

        viewModel.scans.observe(viewLifecycleOwner) { list ->
            val adapter = DoctorScanHistoryAdapter(list) { item ->
                Toast.makeText(context, "Viewing scan: ${item.prediction}", Toast.LENGTH_SHORT).show()
            }
            binding.rvScans.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}