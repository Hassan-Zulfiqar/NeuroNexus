package com.example.neuronexus.patient.ui.medicalrecords

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentMedicalRecordsBinding
import com.example.neuronexus.patient.adapters.ProfileSwitcherAdapter
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class MedicalRecordsFragment : Fragment() {

    private var _binding: FragmentMedicalRecordsBinding? = null
    private val binding get() = _binding

    private val sharedViewModel: SharedViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMedicalRecordsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        val b = binding ?: return

        b.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val pagerAdapter = MedicalRecordsPagerAdapter(this)
        b.viewPager.adapter = pagerAdapter

        TabLayoutMediator(b.tabLayout, b.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Prescriptions"
                1 -> "Lab Reports"
                else -> ""
            }
        }.attach()

        b.viewPager.isUserInputEnabled = true
    }

    private fun setupObservers() {
        sharedViewModel.selectedMedicalRecordTab.observe(viewLifecycleOwner) { tabIndex ->
            val b = binding ?: return@observe
            val index = tabIndex ?: 0
            b.viewPager.post {
                b.viewPager.setCurrentItem(index, false)
            }
        }

        sharedViewModel.selectedPatientProfile.observe(viewLifecycleOwner) { profile ->
            val b = binding ?: return@observe
            if (profile == null) return@observe

            b.tvPatientName.text = "Records for ${profile.fullName}"

            val initial = profile.fullName
                .trim()
                .firstOrNull()
                ?.uppercaseChar()
                ?.toString() ?: "?"
            b.tvProfileSwitcher.text = initial
        }

        sharedViewModel.patientProfiles.observe(viewLifecycleOwner) { profiles ->
            val b = binding ?: return@observe

            if (profiles.isNullOrEmpty() || profiles.size <= 1) {
                b.tvProfileSwitcher.visibility = View.GONE
                b.tvProfileSwitcher.setOnClickListener(null)
            } else {
                b.tvProfileSwitcher.visibility = View.VISIBLE

                b.tvProfileSwitcher.setOnClickListener { anchor ->
                    val currentProfileId = sharedViewModel.selectedPatientProfile.value?.profileId

                    val listPopup = ListPopupWindow(requireContext())
                    listPopup.anchorView = anchor

                    // Fix 3C: Use custom ProfileSwitcherAdapter
                    listPopup.setAdapter(
                        ProfileSwitcherAdapter(
                            context = requireContext(),
                            profiles = profiles,
                            currentProfileId = currentProfileId ?: ""
                        )
                    )

                    // Fix 1: 60% Width, Offsets, and styling
                    val displayMetrics = resources.displayMetrics
                    listPopup.width = (displayMetrics.widthPixels * 0.6).toInt()
                    listPopup.height = ListPopupWindow.WRAP_CONTENT
                    listPopup.horizontalOffset = 0
                    listPopup.verticalOffset = 8
                    listPopup.isModal = true
                    listPopup.setBackgroundDrawable(
                        ContextCompat.getDrawable(requireContext(), R.drawable.bg_popup_window)
                    )

                    // Selection Click
                    listPopup.setOnItemClickListener { _, _, position, _ ->
                        val selectedProfile = profiles[position]
                        if (selectedProfile.profileId != currentProfileId) {
                            sharedViewModel.selectPatientProfile(selectedProfile)
                            sharedViewModel.selectMedicalRecordTab(0)

                            val newInitial = selectedProfile.fullName
                                .trim()
                                .firstOrNull()
                                ?.uppercaseChar()
                                ?.toString() ?: "?"
                            binding?.tvProfileSwitcher?.text = newInitial
                            binding?.tvPatientName?.text = "Records for ${selectedProfile.fullName}"
                        }
                        listPopup.dismiss()
                    }

                    listPopup.show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.viewPager?.adapter = null
        _binding = null
    }
}