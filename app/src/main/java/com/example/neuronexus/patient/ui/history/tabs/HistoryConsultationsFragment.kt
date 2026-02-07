package com.example.neuronexus.patient.ui.history.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.databinding.FragmentHistoryConsultationsBinding
import com.example.neuronexus.patient.adapters.PatientHistoryConsultationAdapter

class HistoryConsultationsFragment : Fragment() {

    private var _binding: FragmentHistoryConsultationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HistoryConsultationsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(HistoryConsultationsViewModel::class.java)
        _binding = FragmentHistoryConsultationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView
        binding.rvConsultations.layoutManager = LinearLayoutManager(context)

        // 2. Observe Data
        viewModel.consultations.observe(viewLifecycleOwner) { list ->
            val adapter = PatientHistoryConsultationAdapter(list) { item ->
                // Handle "View Prescription" click
                Toast.makeText(context, "Opening prescription for ${item.doctorName}", Toast.LENGTH_SHORT).show()
            }
            binding.rvConsultations.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}