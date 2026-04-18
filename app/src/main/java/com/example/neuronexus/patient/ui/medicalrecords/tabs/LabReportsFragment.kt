package com.example.neuronexus.patient.ui.medicalrecords.tabs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentLabReportsBinding
import com.example.neuronexus.patient.adapters.LabReportAdapter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LabReportsFragment : Fragment() {

    private var _binding: FragmentLabReportsBinding? = null
    private val binding get() = _binding

    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var labReportAdapter: LabReportAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLabReportsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        fetchData()
    }

    private fun fetchData() {
        val profileId = sharedViewModel.selectedPatientProfile.value?.profileId
        if (profileId.isNullOrEmpty()) {
            showEmptyState()
            return
        }
        networkViewModel.fetchPatientLabReports(profileId)
    }

    private fun setupUI() {
        val b = binding ?: return

        labReportAdapter = LabReportAdapter(
            reports = emptyList(),
            onViewReportClick = { report ->
                openReportFile(report.fileUrl)
            }
        )

        b.rvLabReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = labReportAdapter
            isNestedScrollingEnabled = true
        }

        b.swipeRefreshLayout.setOnRefreshListener {
            fetchData()
        }
    }

    private fun openReportFile(fileUrl: String) {
        if (fileUrl.isBlank()) {
            Toast.makeText(requireContext(), "Report file not available", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(fileUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        // Observe profile changes to seamlessly re-fetch data for the new patient
        sharedViewModel.selectedPatientProfile.observe(viewLifecycleOwner) { profile ->
            profile ?: return@observe
            // Show loading before fetch
            binding?.swipeRefreshLayout?.isRefreshing = true
            fetchData()
        }

        networkViewModel.labReportsResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val b = binding ?: return@observe

            b.swipeRefreshLayout.isRefreshing = false

            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                if (list.isEmpty()) {
                    showEmptyState()
                } else {
                    b.rvLabReports.visibility = View.VISIBLE
                    b.layoutEmptyState.visibility = View.GONE
                    labReportAdapter?.updateList(list)
                }
            } else {
                showEmptyState()
                Toast.makeText(
                    requireContext(),
                    result.exceptionOrNull()?.message ?: "Failed to load lab reports",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // Mandatory reset
            networkViewModel.resetLabReportsResult()
        }

        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding?.swipeRefreshLayout?.isRefreshing = false
            }
        }
    }

    private fun showEmptyState() {
        val b = binding ?: return
        b.rvLabReports.visibility = View.GONE
        b.layoutEmptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}