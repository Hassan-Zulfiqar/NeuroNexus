package com.example.neuronexus.patient.ui.history

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentPatientHistoryBinding
import com.example.neuronexus.patient.adapters.PatientHistoryPagerAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.Locale
import kotlin.math.roundToInt

class PatientHistoryFragment : Fragment() {

    private var _binding: FragmentPatientHistoryBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var currentHistoryStatusFilter: String = "all"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PatientHistoryPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // 2. Connect TabLayout with ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Consultations"
                1 -> "Labs"
                else -> null
            }
        }.attach()

        currentHistoryStatusFilter =
            sharedViewModel.patientHistoryStatusFilter.value?.lowercase(Locale.getDefault()) ?: "all"
        refreshHistoryFilterChips()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }

    private fun getHistoryPastStatusOptions(): List<Pair<String, String>> {
        return listOf(
            "All" to "all",
            "Completed" to "completed",
            "Cancelled" to "cancelled",
            "Expired" to "expired",
            "Rejected" to "rejected"
        )
    }

    private fun refreshHistoryFilterChips() {
        val ctx = requireContext()
        binding.chipGroupHistoryStatusFilter.removeAllViews()
        getHistoryPastStatusOptions().forEach { (label, value) ->
            val chip = Chip(ctx).apply {
                text = label
                tag = value
                isCheckable = false
                isCloseIconVisible = false
                chipStartPadding = dpToPx(14f).toFloat()
                chipEndPadding = dpToPx(14f).toFloat()
                textStartPadding = 0f
                textEndPadding = 0f
                chipMinHeight = dpToPx(36f).toFloat()
                chipCornerRadius = dpToPx(18f).toFloat()
                textSize = 13f
                ResourcesCompat.getFont(ctx, R.font.poppins)?.let { typeface = it }
                setOnClickListener {
                    if (currentHistoryStatusFilter.equals(value, ignoreCase = true)) return@setOnClickListener
                    currentHistoryStatusFilter = value
                    sharedViewModel.setPatientHistoryStatusFilter(value)
                    applyHistoryStatusChipStyles()
                }
            }
            binding.chipGroupHistoryStatusFilter.addView(chip)
        }
        applyHistoryStatusChipStyles()
    }

    private fun applyHistoryStatusChipStyles() {
        val ctx = requireContext()
        val blue = ContextCompat.getColor(ctx, R.color.primary_blue)
        val white = ContextCompat.getColor(ctx, R.color.text_white)
        val inactiveBg = ContextCompat.getColor(ctx, R.color.colorCard)
        val inactiveText = ContextCompat.getColor(ctx, R.color.textSecondary)
        val strokeColor = ContextCompat.getColor(ctx, R.color.gray_2)
        val strokeW = dpToPx(1f).toFloat()

        for (i in 0 until binding.chipGroupHistoryStatusFilter.childCount) {
            val chip = binding.chipGroupHistoryStatusFilter.getChildAt(i) as Chip
            val value = chip.tag as? String ?: continue
            val selected = value.equals(currentHistoryStatusFilter, ignoreCase = true)
            if (selected) {
                chip.chipBackgroundColor = ColorStateList.valueOf(blue)
                chip.setTextColor(white)
                chip.chipStrokeWidth = 0f
            } else {
                chip.chipBackgroundColor = ColorStateList.valueOf(inactiveBg)
                chip.setTextColor(inactiveText)
                chip.chipStrokeWidth = strokeW
                chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}