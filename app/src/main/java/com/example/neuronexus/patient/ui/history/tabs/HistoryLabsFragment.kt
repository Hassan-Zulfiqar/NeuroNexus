package com.example.neuronexus.patient.ui.history.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.databinding.FragmentHistoryLabsBinding
import com.example.neuronexus.patient.adapters.PatientHistoryLabAdapter

class HistoryLabsFragment : Fragment() {

    private var _binding: FragmentHistoryLabsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HistoryLabsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(HistoryLabsViewModel::class.java)
        _binding = FragmentHistoryLabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvLabs.layoutManager = LinearLayoutManager(context)

        viewModel.labs.observe(viewLifecycleOwner) { list ->
            val adapter = PatientHistoryLabAdapter(list) { item ->
                Toast.makeText(context, "Opening report for ${item.testName}", Toast.LENGTH_SHORT).show()
            }
            binding.rvLabs.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}