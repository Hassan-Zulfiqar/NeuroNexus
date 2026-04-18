package com.example.neuronexus.common.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.neuronexus.R
import com.example.neuronexus.common.models.User
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.AuthViewModel
import com.example.neuronexus.databinding.ActivityLoginBinding
import com.example.neuronexus.doctor.activities.DoctorDashboardActivity
import com.example.neuronexus.patient.activities.PatientDashboardActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import org.koin.androidx.viewmodel.ext.android.viewModel // Import Koin

class LoginActivity : AppCompatActivity() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!

    // 1. INJECT VIEWMODEL
    private val viewModel: AuthViewModel by viewModel()

    private var googleSignInClient: GoogleSignInClient? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    // 2. Call ViewModel
                    viewModel.googleLogin(token)
                }
            } catch (e: ApiException) {
                AlertUtils.showError(this, "Google Sign-In failed: ${e.message}")            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleClient()
        setupClickListeners()

        // 3. SETUP OBSERVERS
        observeViewModel()
    }

    private fun setupGoogleClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.textGoToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpRoleActivity::class.java))
        }

        binding.btnForgotPass.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.inputLayoutEmail.editText?.text.toString().trim()
            val password = binding.inputLayoutPass.editText?.text.toString().trim()

            if (email.isEmpty() || password.isEmpty())
            {
                AlertUtils.showError(this, "Please enter both email and password.",
                    "Missing Input")
            }
            else
            {
                viewModel.login(email, password)
            }
        }

        binding.btnGoogle.setOnClickListener {
            googleSignInClient?.signOut()?.addOnCompleteListener {
                googleSignInClient?.signInIntent?.let { intent ->
                    googleSignInLauncher.launch(intent)
                }
            }
        }

        binding.btnFacebook.setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    // 4. OBSERVE DATA CHANGES
    private fun observeViewModel() {
        // Observe Loading State
        viewModel.loading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        // Observe Login Success/Failure
        viewModel.userState.observe(this) { result ->
            result.onSuccess { user ->
                handleLoginSuccess(user)
            }
            result.onFailure { error ->
                AlertUtils.showError(this, error.message ?: "Authentication failed")
            }
        }
    }

    private fun handleLoginSuccess(user: User) {
        if (user.status == "blocked" || user.status == "block") {
            AlertUtils.showError(this, "Your account has been blocked. " +
                    "Please contact support.", "Access Denied")
            return
        }

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val roleToSave = if (user.role == "doctor") "doctors" else "patients"
        prefs.edit().putString("user_role", roleToSave).apply()

        when (user.role) {
            "doctor" -> navigateTo(DoctorDashboardActivity::class.java)
            "patient" -> navigateTo(PatientDashboardActivity::class.java)
            "new_user" -> {
                AlertUtils.showInfo(this, "Please complete your registration profile.")
                startActivity(Intent(this, SignUpRoleActivity::class.java))
            }
            "admin" -> AlertUtils.showError(this, "Please login via the Web Portal", "Admin Access")
            else -> AlertUtils.showError(this, "Unknown role: ${user.role}")
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        val progressBar = binding.root.findViewById<View>(R.id.progressBar)
        if (isLoading) {
            progressBar?.visibility = View.VISIBLE
            binding.btnSignIn.isEnabled = false
            binding.btnGoogle.isEnabled = false
        } else {
            progressBar?.visibility = View.GONE
            binding.btnSignIn.isEnabled = true
            binding.btnGoogle.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}