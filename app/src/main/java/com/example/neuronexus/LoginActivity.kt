package com.example.neuronexus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.neuronexus.databinding.ActivityLoginBinding
import com.example.neuronexus.models.User
import com.example.neuronexus.repository.AuthRepository

class LoginActivity : AppCompatActivity() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.textGoToSignUp.setOnClickListener {
            val intent = Intent(this, SignUpRoleActivity::class.java)
            startActivity(intent)
        }

        binding.btnForgotPass.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.inputLayoutEmail.editText?.text.toString().trim()
            val password = binding.inputLayoutPass.editText?.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                performLogin(email, password)
            }
        }
    }

    private fun performLogin(email: String, pass: String) {
        showLoading(true)

        authRepository.loginUser(email, pass, object : AuthRepository.LoginCallback {
            override fun onSuccess(user: User) {
                showLoading(false)

                if (user.status == "blocked") {
                    Toast.makeText(this@LoginActivity, "Your account has been blocked. Contact Admin.", Toast.LENGTH_LONG).show()
                    return
                }

                when (user.role) {
                    "doctor" -> {
                        val intent = Intent(this@LoginActivity, DoctorDashboardActivity::class.java)
                        // Clear stack so they can't go back to Login
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    "patient" -> {
                        val intent = Intent(this@LoginActivity, PatientDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    "admin" -> {

                        Toast.makeText(this@LoginActivity, "Please login via Web Portal", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(this@LoginActivity, "Unknown role: ${user.role}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onError(message: String) {
                showLoading(false)
                Toast.makeText(this@LoginActivity, "Error: $message", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        val progressBar = findViewById<View>(R.id.progressBar) // Ensure ID matches XML
        if (isLoading) {
            progressBar?.visibility = View.VISIBLE
            binding.btnSignIn.isEnabled = false
            binding.btnSignIn.text = "Signing In..."
        } else {
            progressBar?.visibility = View.GONE
            binding.btnSignIn.isEnabled = true
            binding.btnSignIn.text = "SIGN IN"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}