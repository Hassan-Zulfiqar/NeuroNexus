package com.example.neuronexus.doctor.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.neuronexus.doctor.ui.history.tabs.DoctorAppointmentsFragment
import com.example.neuronexus.doctor.ui.history.tabs.DoctorScansFragment

class DoctorHistoryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DoctorAppointmentsFragment()
            1 -> DoctorScansFragment()
            else -> DoctorAppointmentsFragment()
        }
    }
}

