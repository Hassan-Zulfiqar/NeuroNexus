package com.example.neuronexus.doctor.ui.history.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.databinding.FragmentDoctorAppointmentsBinding
import com.example.neuronexus.doctor.adapters.DoctorAppointmentHistoryAdapter

class DoctorAppointmentsFragment : Fragment() {

    private var _binding: FragmentDoctorAppointmentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DoctorAppointmentsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(DoctorAppointmentsViewModel::class.java)
        _binding = FragmentDoctorAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAppointments.layoutManager = LinearLayoutManager(context)

        viewModel.appointments.observe(viewLifecycleOwner) { list ->
            val adapter = DoctorAppointmentHistoryAdapter(list, { item->
                Toast.makeText(context, "Opening details for ${item.patientName}", Toast.LENGTH_SHORT).show()
            })
            binding.rvAppointments.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}