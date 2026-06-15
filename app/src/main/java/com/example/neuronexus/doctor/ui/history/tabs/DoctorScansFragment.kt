package com.example.neuronexus.doctor.ui.history.tabs

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
import com.example.neuronexus.databinding.FragmentDoctorScansBinding
import com.example.neuronexus.doctor.adapters.DoctorScanHistoryAdapter
import com.example.neuronexus.doctor.models.TumorReport
import org.koin.androidx.viewmodel.ext.android.viewModel

class DoctorScansFragment : Fragment() {

    private var _binding: FragmentDoctorScansBinding? = null
    private val binding get() = _binding!!

    private val networkViewModel: NetworkViewModel by viewModel()

    private var scansAdapter: DoctorScanHistoryAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorScansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
        fetchData()
    }

    private fun setupUI() {
        scansAdapter = DoctorScanHistoryAdapter(emptyList()) { report ->
            showReportDetailDialog(report)
        }
        binding.rvScans.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scansAdapter
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchData()
        }
    }

    private fun setupObservers() {
        networkViewModel.tumorReports.observe(viewLifecycleOwner) { result ->
            result ?: return@observe

            result.onSuccess { reports ->
                if (reports.isEmpty()) {
                    binding.layoutEmptyScans.visibility = View.VISIBLE
                    binding.rvScans.visibility = View.GONE
                } else {
                    binding.layoutEmptyScans.visibility = View.GONE
                    binding.rvScans.visibility = View.VISIBLE
                    scansAdapter?.updateList(reports)
                }
            }.onFailure { error ->
                binding.layoutEmptyScans.visibility = View.VISIBLE
                binding.rvScans.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to load scan history",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun fetchData() {
        val uid = networkViewModel.getCurrentUserUid() ?: return
        networkViewModel.fetchTumorDetectionRecords(uid)
    }

    private fun showReportDetailDialog(report: TumorReport) {
        val message = buildString {
            append("Patient: ${report.patientName.ifBlank { "Not specified" }}\n")
            append("Age: ${report.patientAge.ifBlank { "N/A" }}\n")
            append("Gender: ${report.patientGender.ifBlank { "N/A" }}\n\n")
            append("Result: ${report.tumorDetected.ifBlank { "N/A" }}\n")
            append("Prediction: ${report.prediction.ifBlank { "N/A" }}\n")
            append("Confidence: ${(report.confidence * 100).toInt()}%\n")
            if (report.areaPercentage > 0) {
                append("Tumor Area: ${report.areaPercentage}% of scan\n")
            }
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Scan Report")
            .setMessage(message)

        val imageUrl = report.overlayImageUrl.ifBlank { report.originalImageUrl }
        if (imageUrl.isNotBlank()) {
            builder.setNegativeButton("View Image") { _, _ ->
                openUrl(imageUrl)
            }
        }

        if (report.pdfUrl.isNotBlank()) {
            builder.setNeutralButton("Download PDF Report") { _, _ ->
                openUrl(report.pdfUrl)
            }
        }

        builder.setPositiveButton("Close", null)

        builder.show()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scansAdapter = null
        _binding = null
    }
}
