package com.example.neuronexus.common.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.neuronexus.databinding.ActivityForgotPasswordBinding
import com.example.neuronexus.common.repository.AuthRepository

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSend.setOnClickListener {
            val email = binding.inputEmail.editText?.text.toString().trim()

            if (validateEmail(email)) {
                sendResetLink(email)
            }
        }
    }

    private fun sendResetLink(email: String) {
        showLoading(true)

        authRepository.sendPasswordResetEmail(email, object : AuthRepository.RegisterCallback {
            override fun onSuccess(message: String) {
                showLoading(false)
                showSuccessDialog(email)
            }

            override fun onError(message: String) {
                showLoading(false)
                Toast.makeText(this@ForgotPasswordActivity, "Error: $message", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showSuccessDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Link Sent!")
            .setMessage("We have sent a password reset link to $email.\n\nPlease check your inbox (and spam folder).")
            .setPositiveButton("Go to Login") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnSend.isEnabled = false
            binding.btnSend.text = "Sending..."
        } else {
            binding.btnSend.isEnabled = true
            binding.btnSend.text = "SEND RESET LINK"
        }
    }

    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            binding.inputEmail.error = "Email is required"
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.error = "Invalid email format"
            false
        } else {
            binding.inputEmail.error = null
            true
        }
    }
}

