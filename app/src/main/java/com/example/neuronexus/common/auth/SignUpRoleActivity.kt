package com.example.neuronexus.common.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.neuronexus.databinding.ActivitySignUpRoleBinding
import com.example.neuronexus.doctor.activities.DoctorSignUpActivity
import com.example.neuronexus.patient.activities.PatientSignUpActivity

class SignUpRoleActivity : AppCompatActivity() {

    private var _binding: ActivitySignUpRoleBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySignUpRoleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnDoctorRole.setOnClickListener {
            startActivity(Intent(this, DoctorSignUpActivity::class.java))
        }

        binding.btnPatientRole.setOnClickListener {
            startActivity(Intent(this, PatientSignUpActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}