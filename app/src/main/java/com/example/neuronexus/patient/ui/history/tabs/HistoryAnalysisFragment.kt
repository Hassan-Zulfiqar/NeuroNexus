package com.example.neuronexus.patient.ui.history.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.databinding.FragmentHistoryAnalysisBinding
import com.example.neuronexus.patient.adapters.PatientHistoryAnalysisAdapter

class HistoryAnalysisFragment : Fragment() {

    private var _binding: FragmentHistoryAnalysisBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HistoryAnalysisViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(HistoryAnalysisViewModel::class.java)
        _binding = FragmentHistoryAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAnalysis.layoutManager = LinearLayoutManager(context)

        viewModel.analysisList.observe(viewLifecycleOwner) { list ->
            val adapter = PatientHistoryAnalysisAdapter(list) { item ->
                Toast.makeText(context, "Opening analysis details...", Toast.LENGTH_SHORT).show()
            }
            binding.rvAnalysis.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}