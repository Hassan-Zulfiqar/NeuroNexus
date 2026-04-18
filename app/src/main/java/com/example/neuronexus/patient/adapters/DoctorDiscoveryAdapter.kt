package com.example.neuronexus.patient.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.databinding.LayoutTopDoctorCardBinding
import com.example.neuronexus.doctor.models.Doctor
import java.util.Locale

class DoctorDiscoveryAdapter(
    private var doctors: List<Doctor>,
    private val onDoctorClick: (Doctor) -> Unit,       // Lambda for Card Click (Details)
    private val onMoreClick: (Doctor, View) -> Unit    // Lambda for 3-Dots Click (Popup Menu)
) : RecyclerView.Adapter<DoctorDiscoveryAdapter.DoctorViewHolder>() {

    inner class DoctorViewHolder(val binding: LayoutTopDoctorCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val binding = LayoutTopDoctorCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DoctorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doctor = doctors[position]

        with(holder.binding) {
            val cleanName = doctor.name.trim()
            val displayName = if (cleanName.lowercase(Locale.ROOT).startsWith("dr.") ||
                cleanName.lowercase(Locale.ROOT).startsWith("dr ")) {
                cleanName
            } else {
                "Dr. $cleanName"
            }
            tvDoctorName.text = displayName

            tvDoctorSpec.text = doctor.specialization

            tvLocation.text = if (doctor.clinicAddress.isNotEmpty()) doctor.clinicAddress else "Online"

            tvReviews.text = when {
                doctor.reviewCount < 1000 -> "(${doctor.reviewCount})"
                else -> "(${doctor.reviewCount / 1000}k)"
            }

            val starViews = listOf(ivStar1, ivStar2, ivStar3, ivStar4, ivStar5)
            val rating = doctor.rating.toInt()

            for (i in starViews.indices) {
                if (i < rating) {
                    starViews[i].setColorFilter(Color.parseColor("#FFD700"))
                } else {
                    starViews[i].setColorFilter(Color.parseColor("#E0E0E0"))
                }
            }

            // Image Loading
            Glide.with(root.context)
                .load(doctor.profileImageUrl)
                .placeholder(R.drawable.doctor)
                .error(R.drawable.doctor)
                .centerCrop()
                .into(imageDoctor)

            //Click Listeners
            root.setOnClickListener {
                onDoctorClick(doctor)
            }

            btnMore.setOnClickListener { view ->
                onMoreClick(doctor, view)
            }
        }
    }

    override fun getItemCount() = doctors.size

    fun updateList(newDoctors: List<Doctor>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }
}