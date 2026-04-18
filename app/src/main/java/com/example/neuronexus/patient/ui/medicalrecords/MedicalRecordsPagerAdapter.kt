package com.example.neuronexus.patient.ui.medicalrecords

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.neuronexus.patient.ui.medicalrecords.tabs.LabReportsFragment
import com.example.neuronexus.patient.ui.medicalrecords.tabs.PrescriptionsFragment

class MedicalRecordsPagerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PrescriptionsFragment()
            1 -> LabReportsFragment()
            else -> PrescriptionsFragment()
        }
    }
}