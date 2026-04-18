package com.example.neuronexus.patient.ui.medicalrecords.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentPrescriptionsBinding
import com.example.neuronexus.patient.adapters.PrescriptionAdapter
import com.example.neuronexus.patient.models.Prescription
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrescriptionsFragment : Fragment() {

    private var _binding: FragmentPrescriptionsBinding? = null
    private val binding get() = _binding

    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var prescriptionAdapter: PrescriptionAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPrescriptionsBinding.inflate(inflater, container, false)
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
        networkViewModel.fetchPatientPrescriptions(profileId)
    }

    private fun setupUI() {
        val b = binding ?: return

        prescriptionAdapter = PrescriptionAdapter(
            prescriptions = emptyList(),
            onPrescriptionClick = { prescription ->
                showPrescriptionBottomSheet(prescription)
            }
        )

        b.rvPrescriptions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = prescriptionAdapter
            isNestedScrollingEnabled = true
        }

        b.swipeRefreshLayout.setOnRefreshListener {
            fetchData()
        }
    }

    private fun showPrescriptionBottomSheet(prescription: Prescription) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_prescription, null)

        // Bind prescription data to bottom sheet views
        view.findViewById<TextView>(R.id.tvBsDoctorName)?.text =
            "Dr. ${prescription.doctorName.ifBlank { "Unknown Doctor" }}"

        view.findViewById<TextView>(R.id.tvBsDiagnosis)?.text =
            prescription.diagnosis.ifBlank { "Not specified" }

        val medicationsFormatted = prescription.medications
            .filter { it.isNotBlank() }
            .joinToString("\n") { "• $it" }
        view.findViewById<TextView>(R.id.tvBsMedications)?.text =
            if (medicationsFormatted.isBlank()) "No medications listed" else medicationsFormatted

        view.findViewById<TextView>(R.id.tvBsInstructions)?.apply {
            if (prescription.instructions.isBlank()) {
                visibility = View.GONE
                view.findViewById<View>(R.id.dividerInstructions)?.visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                view.findViewById<View>(R.id.dividerInstructions)?.visibility = View.VISIBLE
                text = "Instructions: ${prescription.instructions}"
            }
        }

        view.findViewById<TextView>(R.id.tvBsFollowUpDate)?.apply {
            val followUp = prescription.followUpDate
            if (followUp == null) {
                visibility = View.GONE
                view.findViewById<View>(R.id.dividerFollowUp)?.visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                view.findViewById<View>(R.id.dividerFollowUp)?.visibility = View.VISIBLE
                val formatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(Date(followUp))
                text = "Follow-up: $formatted"
            }
        }

        view.findViewById<TextView>(R.id.tvBsIssuedDate)?.text =
            "Issued: ${
                if (prescription.issuedDate == 0L) "Date not available"
                else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(prescription.issuedDate))
            }"

        view.findViewById<ImageView>(R.id.btnBsClose)?.setOnClickListener {
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    private fun setupObservers() {

        //Observe profile changes to seamlessly re-fetch data for the new patient
        sharedViewModel.selectedPatientProfile.observe(viewLifecycleOwner) { profile ->
            profile ?: return@observe
            // Show loading before fetch
            binding?.swipeRefreshLayout?.isRefreshing = true
            fetchData()
        }

        networkViewModel.prescriptionsResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val b = binding ?: return@observe

            b.swipeRefreshLayout.isRefreshing = false

            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                if (list.isEmpty()) {
                    showEmptyState()
                } else {
                    b.rvPrescriptions.visibility = View.VISIBLE
                    b.layoutEmptyState.visibility = View.GONE
                    prescriptionAdapter?.updateList(list)
                }
            } else {
                showEmptyState()
                Toast.makeText(
                    requireContext(),
                    result.exceptionOrNull()?.message ?: "Failed to load prescriptions",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // Mandatory reset
            networkViewModel.resetPrescriptionsResult()
        }

        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding?.swipeRefreshLayout?.isRefreshing = false
            }
        }
    }

    private fun showEmptyState() {
        val b = binding ?: return
        b.rvPrescriptions.visibility = View.GONE
        b.layoutEmptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}