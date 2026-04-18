package com.example.neuronexus.patient.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.neuronexus.R
import com.example.neuronexus.patient.models.PatientProfile

class ProfileSwitcherAdapter(
    context: Context,
    private val profiles: List<PatientProfile>,
    private val currentProfileId: String
) : ArrayAdapter<PatientProfile>(context, 0, profiles) {

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_profile_switcher_popup, parent, false)

        val profile = profiles[position]

        // Avatar initial
        val tvInitial = view.findViewById<TextView>(R.id.tvPopupInitial)
        tvInitial?.text = profile.fullName
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString() ?: "?"

        // Name
        val tvName = view.findViewById<TextView>(R.id.tvPopupName)
        tvName?.text = profile.fullName.ifBlank { "Unknown" }

        // Relation
        val tvRelation = view.findViewById<TextView>(R.id.tvPopupRelation)
        tvRelation?.text = profile.relation.ifBlank { "" }

        // Selected checkmark
        val ivSelected = view.findViewById<ImageView>(R.id.ivSelected)
        ivSelected?.visibility = if (profile.profileId == currentProfileId) {
            View.VISIBLE
        } else {
            View.GONE
        }

        return view
    }
}