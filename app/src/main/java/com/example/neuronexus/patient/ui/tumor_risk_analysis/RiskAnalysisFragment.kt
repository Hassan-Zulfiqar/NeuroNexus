package com.example.neuronexus.patient.ui.tumor_risk_analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.common.models.Symptoms
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentRiskAnalysisBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class RiskAnalysisFragment : Fragment() {

    private var _binding: FragmentRiskAnalysisBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var selectedAnswer: String? = null
    private var selectedOptionIndex: Int = -1
    private var isFinished: Boolean = false
    private var isResetting: Boolean = false  // blocks callbacks during programmatic reset

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiskAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isFinished = false
        sharedViewModel.initSymptomChecker()

        setupClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        sharedViewModel.symptomQuestionIndex.observe(viewLifecycleOwner) { index ->
            if (isFinished) return@observe

            val questions = sharedViewModel.symptomQuestions.value ?: return@observe
            val total = questions.size

            if (index >= total) {
                isFinished = true
                navigateToResult()
                return@observe
            }

            // Block click listeners while we programmatically reset state
            isResetting = true
            selectedAnswer = null
            selectedOptionIndex = -1
            clearOptionSelection()
            resetButtonState()
            isResetting = false

            bindQuestion(questions[index], index, total)
        }
    }

    private fun bindQuestion(question: Symptoms, index: Int, total: Int) {
        val progressPercent = ((index.toFloat() / total.toFloat()) * 100).toInt()
        binding.progressBar.progress = progressPercent
        binding.tvProgressPercent.text = "$progressPercent%"
//
//        val sectionNumber = (index / 5) + 1
//        val sectionNames = listOf("GENERAL", "NEUROLOGICAL", "PHYSICAL", "COGNITIVE", "SENSORY")
//        val sectionName = sectionNames.getOrElse(sectionNumber - 1) { "GENERAL" }
//        binding.tvSectionLabel.text = "SECTION $sectionNumber: $sectionName"

        binding.tvQuestion.text = question.question
        binding.tvDescription.text = question.description

        binding.tvOption1.text = question.optionA
        binding.tvOption2.text = question.optionB
        binding.tvOption3.text = question.optionC

        binding.cardOption4.visibility = View.GONE
        binding.cardClinicalNote.visibility = View.GONE

        binding.btnNextStep.text = if (index == total - 1) "Submit  ✓" else "Next Step  →"
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            isFinished = true
            sharedViewModel.clearSymptomState()
            findNavController().navigateUp()
        }

        binding.cardOption1.setOnClickListener {
            if (!isResetting) selectOption(1, binding.tvOption1.text.toString())
        }
        binding.cardOption2.setOnClickListener {
            if (!isResetting) selectOption(2, binding.tvOption2.text.toString())
        }
        binding.cardOption3.setOnClickListener {
            if (!isResetting) selectOption(3, binding.tvOption3.text.toString())
        }

        binding.btnNextStep.setOnClickListener {
            if (isResetting) return@setOnClickListener
            val answer = selectedAnswer ?: return@setOnClickListener
            sharedViewModel.submitSymptomAnswer(answer)
        }
    }

    private fun selectOption(optionNumber: Int, answer: String) {
        if (selectedOptionIndex == optionNumber) return

        selectedAnswer = answer
        selectedOptionIndex = optionNumber

        // Reset all then apply to selected — wrapped in isResetting to prevent
        // any programmatic isChecked change from firing card click callbacks
        isResetting = true
        clearOptionSelection()
        isResetting = false

        val selectedCard = when (optionNumber) {
            1 -> binding.cardOption1
            2 -> binding.cardOption2
            3 -> binding.cardOption3
            else -> return
        }
        val selectedRadio = when (optionNumber) {
            1 -> binding.rbOption1
            2 -> binding.rbOption2
            3 -> binding.rbOption3
            else -> return
        }

        selectedCard.strokeColor = ContextCompat.getColor(requireContext(), R.color.primary_blue)
        selectedCard.strokeWidth = 2

    
        isResetting = true
        selectedRadio.isChecked = true
        isResetting = false

        binding.btnNextStep.isEnabled = true
        binding.btnNextStep.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.primary_blue)
    }

    private fun clearOptionSelection() {
        val greyColor = ContextCompat.getColor(requireContext(), R.color.text_hint)

        binding.cardOption1.strokeColor = greyColor
        binding.cardOption2.strokeColor = greyColor
        binding.cardOption3.strokeColor = greyColor
        binding.cardOption1.strokeWidth = 1
        binding.cardOption2.strokeWidth = 1
        binding.cardOption3.strokeWidth = 1

        binding.rbOption1.isChecked = false
        binding.rbOption2.isChecked = false
        binding.rbOption3.isChecked = false
    }

    private fun resetButtonState() {
        binding.btnNextStep.isEnabled = false
        binding.btnNextStep.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.text_hint)
    }

    private fun navigateToResult() {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Submit Assessment")
            .setMessage("Are you sure you want to submit your answers? You cannot change them after submission.")
            .setPositiveButton("Yes, Submit") { _, _ ->
                val finalScore = sharedViewModel.symptomScore.value ?: 0
                sharedViewModel.setFinalRiskScore(finalScore)  // store in SharedViewModel
                sharedViewModel.clearSymptomState()
                findNavController().navigate(R.id.action_symptom_checker_to_risk_result)
            }
            .setNegativeButton("No") { dialog, _ ->
                isFinished = false
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
        dialog.show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}