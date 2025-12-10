package com.example.neuronexus.patient.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.neuronexus.patient.ui.history.tabs.HistoryAnalysisFragment
import com.example.neuronexus.patient.ui.history.tabs.HistoryConsultationsFragment
import com.example.neuronexus.patient.ui.history.tabs.HistoryLabsFragment

class PatientHistoryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HistoryConsultationsFragment()
            1 -> HistoryLabsFragment()
            2 -> HistoryAnalysisFragment()
            else -> HistoryConsultationsFragment()
        }
    }
}

