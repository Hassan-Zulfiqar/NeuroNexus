package com.example.neuronexus.patient.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemLabReportCardBinding
import com.example.neuronexus.patient.models.LabReport

class LabReportAdapter(
    private var reports: List<LabReport>,
    private val onViewReportClick: (LabReport) -> Unit
) : RecyclerView.Adapter<LabReportAdapter.LabReportViewHolder>() {

    class LabReportViewHolder(val binding: ItemLabReportCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabReportViewHolder {
        val binding = ItemLabReportCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LabReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabReportViewHolder, position: Int) {
        val report = reports[position]

        // Text Bindings
        holder.binding.tvTestName.text = report.testName.ifBlank { "Lab Test" }
        holder.binding.tvLabName.text = report.labName.ifBlank { "Unknown Lab" }
        holder.binding.tvTestDate.text = report.testDate.ifBlank { "Date not available" }

        // Dynamic Visibility: Result Summary
        if (report.resultSummary.isBlank()) {
            holder.binding.tvResultSummary.visibility = View.GONE
        } else {
            holder.binding.tvResultSummary.visibility = View.VISIBLE
            holder.binding.tvResultSummary.text = report.resultSummary
        }

        // Dynamic Visibility: View Report Button
        if (report.fileUrl.isNotBlank()) {
            holder.binding.btnViewReport.visibility = View.VISIBLE
            holder.binding.btnViewReport.setOnClickListener {
                onViewReportClick(report)
            }
        } else {
            holder.binding.btnViewReport.visibility = View.GONE
            holder.binding.btnViewReport.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = reports.size

    fun updateList(newList: List<LabReport>) {
        reports = newList
        notifyDataSetChanged()
    }
}