package com.example.neuronexus.patient.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemHistoryAnalysisBinding
import com.example.neuronexus.models.HistoryAnalysisItem

class PatientHistoryAnalysisAdapter(
    private val analysisList: List<HistoryAnalysisItem>,
    private val onViewClick: (HistoryAnalysisItem) -> Unit
) : RecyclerView.Adapter<PatientHistoryAnalysisAdapter.AnalysisViewHolder>() {

    class AnalysisViewHolder(val binding: ItemHistoryAnalysisBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnalysisViewHolder {
        val binding = ItemHistoryAnalysisBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnalysisViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnalysisViewHolder, position: Int) {
        val item = analysisList[position]

        holder.binding.tvTitle.text = item.title
        holder.binding.tvSymptomsCount.text = item.symptomsCount
        holder.binding.tvDate.text = item.date
        holder.binding.chipRisk.text = item.riskLevel

        when (item.riskLevel) {
            "High Risk" -> {
                holder.binding.chipRisk.setTextColor(Color.parseColor("#C62828"))
                holder.binding.chipRisk.background.setTint(Color.parseColor("#FFEBEE"))
            }

            "Moderate Risk" -> {
                holder.binding.chipRisk.setTextColor(Color.parseColor("#F57C00"))
                holder.binding.chipRisk.background.setTint(Color.parseColor("#FFF3E0"))
            }

            else -> {
                holder.binding.chipRisk.setTextColor(Color.parseColor("#2E7D32"))
                holder.binding.chipRisk.background.setTint(Color.parseColor("#E8F5E9"))
            }
        }
        holder.binding.btnViewAnalysis.setOnClickListener {
            onViewClick(item)
        }
    }

    override fun getItemCount(): Int {
        return analysisList.size
    }
}

