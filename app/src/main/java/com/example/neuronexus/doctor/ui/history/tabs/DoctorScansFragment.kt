package com.example.neuronexus.doctor.ui.history.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.databinding.FragmentDoctorScansBinding
import com.example.neuronexus.doctor.adapters.DoctorScanHistoryAdapter
import com.example.neuronexus.models.DoctorScanHistoryItem

class DoctorScansFragment : Fragment() {

    private var _binding: FragmentDoctorScansBinding? = null
    private val binding get() = _binding!!

    private lateinit var scansAdapter: DoctorScanHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorScansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // 1. Create Hardcoded Dummy List
        val dummyList = listOf(
            DoctorScanHistoryItem(
                prediction = "Meningioma",
                confidence = "98%",
                patientName = "Eleanor Vance",
                date = "Dec 10, 10:45 AM",
                imageResId = R.drawable.ic_mri
            ),
            DoctorScanHistoryItem(
                prediction = "Glioma",
                confidence = "85%",
                patientName = "Robert Ford",
                date = "Dec 09, 02:30 PM",
                imageResId = R.drawable.ic_mri
            )
        )

        // 2. Initialize Adapter
        scansAdapter = DoctorScanHistoryAdapter(dummyList) { item ->
            Toast.makeText(requireContext(), "Viewing scan: ${item.prediction}", Toast.LENGTH_SHORT).show()
        }

        // 3. Setup RecyclerView
        binding.rvScans.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scansAdapter
        }

        // 4. Wire SwipeRefreshLayout (Dummy action)
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Briefly delay to simulate a network call, then dismiss spinner
            binding.swipeRefreshLayout.postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, 500)
        }

        // 5. Handle Empty State UI
        if (dummyList.isEmpty()) {
            binding.layoutEmptyScans.visibility = View.VISIBLE
            binding.rvScans.visibility = View.GONE
        } else {
            binding.layoutEmptyScans.visibility = View.GONE
            binding.rvScans.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}