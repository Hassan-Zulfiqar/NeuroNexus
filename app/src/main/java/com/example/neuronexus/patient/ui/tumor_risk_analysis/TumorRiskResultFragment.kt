package com.example.neuronexus.patient.ui.tumor_risk_analysis

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.Constant
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentTumorRiskResultBinding
import com.example.neuronexus.patient.activities.DoctorDiscoveryActivity
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class TumorRiskResultFragment : Fragment() {

    private var _binding: FragmentTumorRiskResultBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by sharedViewModel()

    // Uncertainty buffer — 7% subtracted from actual score percentage
    // Wide enough to show uncertainty, narrow enough to feel meaningful
    private val UNCERTAINTY_BUFFER = 7

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTumorRiskResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        sharedViewModel.finalRiskScore.observe(viewLifecycleOwner) { score ->
            bindResults(score)
        }
    }

    private fun bindResults(score: Int) {
        val maxScore = Constant.getSymptomsData().size * 2  // 18 questions → 36

        // Actual percentage from score — capped at 97% (never show 100% per guidelines)
        val actualPercent = minOf((score.toFloat() / maxScore.toFloat() * 100).toInt(), 97)

        // Lower bound = actual - buffer, floored at 0
        val lowerPercent = maxOf(actualPercent - UNCERTAINTY_BUFFER, 0)

        val percentRange = "$lowerPercent–$actualPercent%"

        // Risk tier is still determined by actual percent — not the range
        val riskLevel = getRiskLevel(actualPercent)

        // Big range display e.g. "29–36%"
        binding.tvRiskPercent.text = percentRange
        binding.tvRiskPercent.setTextColor(
            ContextCompat.getColor(requireContext(), riskLevel.percentColor)
        )

        // Tier label e.g. "Moderate Risk of Future Tumor"
        binding.tvRiskLabel.text = "${riskLevel.tierLabel} Risk of Future Tumor"
        binding.tvRiskLabel.setTextColor(
            ContextCompat.getColor(requireContext(), riskLevel.percentColor)
        )

        // Summary sentence with colored bold range inline
        binding.tvSummary.text = buildSummarySpan(percentRange, riskLevel)

        // Detail message
        binding.tvDetailMessage.text = riskLevel.detailMessage
    }

    private fun buildSummarySpan(percentRange: String, riskLevel: RiskLevel): SpannableString {
        val fullText = "Your current symptom profile suggests a $percentRange chance " +
                "of a concerning condition. ${riskLevel.summaryTail}"
        val spannable = SpannableString(fullText)

        val start = fullText.indexOf(percentRange)
        val end = start + percentRange.length

        if (start >= 0) {
            val color = ContextCompat.getColor(requireContext(), riskLevel.percentColor)
            spannable.setSpan(
                ForegroundColorSpan(color), start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD), start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannable
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { goToHome() }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { goToHome() }
            }
        )

        binding.btnConsultDoctor.setOnClickListener {
            startActivity(Intent(requireContext(), DoctorDiscoveryActivity::class.java))
        }
    }

    private fun goToHome() {
        findNavController().popBackStack(R.id.navigation_home, false)
    }

    // -----------------------------------------------------------
    // Risk tier determined by actual calculated percentage
    // Balanced strategy per guidelines:
    // Low:       0–33%
    // Moderate:  34–58%
    // High:      59–80%
    // Very High: 81–97%
    // -----------------------------------------------------------
    private fun getRiskLevel(actualPercent: Int): RiskLevel {
        return when {
            actualPercent <= 33 -> RiskLevel(
                tierLabel    = "Low",
                percentColor = R.color.primary_blue,
                summaryTail  = "This is relatively reassuring.",
                detailMessage = "Your symptom score suggests a low likelihood of a serious brain " +
                        "condition. This does not rule out other causes, but it is reassuring. " +
                        "Continue usual care. If your symptoms persist or worsen, please see your " +
                        "doctor. Having these symptoms does not confirm any diagnosis — only a " +
                        "healthcare provider can do that."
            )
            actualPercent <= 58 -> RiskLevel(
                tierLabel    = "Moderate",
                percentColor = android.R.color.holo_orange_light,
                summaryTail  = "This warrants follow-up with a doctor.",
                detailMessage = "Your symptom score indicates a possible risk of an underlying " +
                        "issue. It is not certain, but it is enough to recommend follow-up. " +
                        "Consider making an appointment with your doctor for evaluation. " +
                        "Early detection significantly improves outcomes — do not delay " +
                        "seeking professional advice."
            )
            actualPercent <= 80 -> RiskLevel(
                tierLabel    = "High",
                percentColor = android.R.color.holo_orange_dark,
                summaryTail  = "Please seek medical advice soon.",
                detailMessage = "Your symptoms suggest a significant risk of a serious condition. " +
                        "Please seek medical advice soon — contact your doctor within a few days " +
                        "and mention these symptoms. Further evaluation such as imaging tests may " +
                        "be required. Having these symptoms does not confirm a tumor, but prompt " +
                        "assessment is strongly advised."
            )
            else -> RiskLevel(
                tierLabel    = "Very High",
                percentColor = android.R.color.holo_red_dark,
                summaryTail  = "We strongly recommend immediate medical evaluation.",
                detailMessage = "Your symptom score is extremely high. While this still cannot " +
                        "confirm a diagnosis, it strongly suggests a serious issue that requires " +
                        "immediate evaluation. Please go to the nearest hospital or contact your " +
                        "doctor today. If you experience severe symptoms such as seizures, sudden " +
                        "weakness, or uncontrollable vomiting, call emergency services immediately."
            )
        }
    }

    data class RiskLevel(
        val tierLabel: String,
        val percentColor: Int,
        val summaryTail: String,
        val detailMessage: String
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}