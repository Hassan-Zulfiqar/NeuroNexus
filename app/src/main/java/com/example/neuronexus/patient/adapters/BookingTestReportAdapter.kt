package com.example.neuronexus.patient.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemBookingTestReportBinding
import com.example.neuronexus.patient.models.LabReport
import com.example.neuronexus.patient.models.SelectedTest

/**
 * Adapter for displaying individual test reports in multi-test bookings.
 * Matches SelectedTest with corresponding LabReport and shows report status.
 */
class BookingTestReportAdapter(
    private val tests: List<SelectedTest>,
    private var reports: Map<String, LabReport>,
    private val showReportStatus: Boolean = false,
    private val onViewReportClick: (String, String) -> Unit  // testId, reportUrl
) : RecyclerView.Adapter<BookingTestReportAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemBookingTestReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(test: SelectedTest) {
            // Bind test info
            binding.tvReportTestName.text = test.testName
            binding.tvReportSampleType.text = "Sample: ${test.sampleType}"

            // Only show report status and buttons if this is a completed booking
            if (showReportStatus) {
                // Completed booking — show report status
                binding.chipReportStatus.visibility = View.VISIBLE

                // Find matching report
                val report = reports[test.testId]

                if (report != null) {
                    // Report exists — show status and button
                    binding.chipReportStatus.text = "Ready"
                    binding.btnViewTestReport.visibility = View.VISIBLE

                    report.resultSummary?.let { summary ->
                        if (summary.isNotBlank()) {
                            binding.tvReportResultSummary.text = summary
                            binding.tvReportResultSummary.visibility = View.VISIBLE
                        } else {
                            binding.tvReportResultSummary.visibility = View.GONE
                        }
                    }

                    binding.btnViewTestReport.setOnClickListener {
                        report.fileUrl?.let { url ->
                            onViewReportClick(test.testId, url)
                        }
                    }
                } else {
                    // Report not ready yet
                    binding.chipReportStatus.text = "Pending"
                    binding.btnViewTestReport.visibility = View.GONE
                    binding.tvReportResultSummary.visibility = View.GONE
                }
            } else {
                // Not completed — hide all report UI
                binding.chipReportStatus.visibility = View.GONE
                binding.btnViewTestReport.visibility = View.GONE
                binding.tvReportResultSummary.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookingTestReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tests[position])
    }

    override fun getItemCount(): Int = tests.size

    fun updateReports(newReports: Map<String, LabReport>) {
        reports = newReports
        notifyDataSetChanged()
    }
}
