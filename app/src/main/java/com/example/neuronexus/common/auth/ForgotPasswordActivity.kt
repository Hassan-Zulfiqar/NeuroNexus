package com.example.neuronexus.common.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.AuthViewModel
import com.example.neuronexus.databinding.ActivityForgotPasswordBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    private val viewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSend.setOnClickListener {
            val email = binding.inputEmail.editText?.text.toString().trim()

            if (validateEmail(email)) {
                // 2. Trigger ViewModel Action
                viewModel.resetPassword(email)
            }
        }
    }

    private fun setupObservers() {

        viewModel.loading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        // Observe Result (Success or Failure)
        viewModel.authMessage.observe(this) { result ->
            result.onSuccess { message ->
                val email = binding.inputEmail.editText?.text.toString().trim()
                showSuccessDialog(email)
            }
            result.onFailure { error ->
                AlertUtils.showError(this, error.message ?: "Failed to send reset link")
            }
        }
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