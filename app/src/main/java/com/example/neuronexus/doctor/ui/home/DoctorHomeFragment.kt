package com.example.neuronexus.doctor.ui.home

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.ui.ImagePickerHelper
import com.example.neuronexus.common.ui.ImageSelectionDialog
import com.example.neuronexus.common.utils.Constant.analyzeResponse
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentDoctorHomeBinding
import com.example.neuronexus.doctor.activities.DetectionActivity
import com.example.neuronexus.doctor.adapters.DoctorAppointmentAdapter
import com.example.neuronexus.doctor.util.RetrofitClient
import com.example.neuronexus.patient.models.DoctorAppointment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream

class DoctorHomeFragment : Fragment() {

    private var _binding: FragmentDoctorHomeBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    var progressDialog: ProgressDialog? = null

    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorHomeBinding.inflate(inflater, container, false)

        imagePickerHelper = ImagePickerHelper(this) { imageUri ->
            Log.e("CHECK_IMAGE", "onCreateView: $imageUri")
            onImageSelected(imageUri)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupUI()
        setupObservers()
        fetchData()
    }

    private fun setupUI() {
        // Determine time-based greeting
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (currentHour) {
            in 0..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            else -> "Good Evening"
        }
        binding.textView6.text = greeting

        setupQuickAccess()
        setupNotificationClick()

        // Wire SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchData()
        }

        // Setup RecyclerView
        binding.rvAppointments.layoutManager = LinearLayoutManager(context)
        binding.rvAppointments.isNestedScrollingEnabled = false

        // Initialize empty adapter with the real model and click lambda
        binding.rvAppointments.adapter = DoctorAppointmentAdapter(emptyList()) { appointment ->
            handleAppointmentClick(appointment)
        }
    }

    private fun setupObservers() {

        // Observe Doctor Profile Details
        networkViewModel.doctorDetails.observe(viewLifecycleOwner) { result ->
            result.onSuccess { doctor ->
                binding.tvGreeting.text = "Hello, Dr. ${doctor.name}"

                if (doctor.profileImageUrl.isNotEmpty()) {
                    com.bumptech.glide.Glide.with(this)
                        .load(doctor.profileImageUrl)
                        .placeholder(R.drawable.doctor)
                        .error(R.drawable.doctor)
                        .into(binding.ivDocProfile)
                } else {
                    binding.ivDocProfile.setImageResource(R.drawable.doctor)
                }
            }
        }

        // Observe Loading State for SwipeRefreshLayout
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // Observe Real Appointments Data
        networkViewModel.doctorAppointments.observe(viewLifecycleOwner) { result ->
            result.onSuccess { appointments ->
                val dateFormat =
                    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())

                // Calculate today and 7 days from now at midnight for an accurate window
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val sevenDaysFromNow = today + (7L * 24 * 60 * 60 * 1000)

                // Filter out cancelled, completed, and appointments outside the 7-day window
                val activeAppointments = appointments.filter { booking ->
                    if (booking.status == "cancelled" || booking.status == "completed") {
                        return@filter false
                    }

                    try {
                        val date = dateFormat.parse(booking.appointmentDate)
                        if (date != null) {
                            date.time in today..sevenDaysFromNow
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false // Gracefully exclude on parse failure
                    }
                }

                val timeFormat =
                    java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val sortedAppointments = activeAppointments.sortedWith { a, b ->
                    try {
                        val dateA = dateFormat.parse(a.appointmentDate)
                        val dateB = dateFormat.parse(b.appointmentDate)

                        // Primary Sort: By Date
                        val dateCompare =
                            if (dateA != null && dateB != null) dateA.compareTo(dateB) else 0

                        if (dateCompare != 0) {
                            dateCompare
                        } else {
                            // Secondary Sort: By Time (only if dates are identical)
                            val timeA = timeFormat.parse(a.appointmentTime)
                            val timeB = timeFormat.parse(b.appointmentTime)
                            if (timeA != null && timeB != null) timeA.compareTo(timeB) else 0
                        }
                    } catch (e: Exception) {
                        0 // Graceful fallback: treat as equal to maintain original insertion order
                    }
                }

                if (activeAppointments.isEmpty()) {
                    // Success with empty list
                    binding.layoutEmptyAppointments.visibility = View.VISIBLE
                    binding.rvAppointments.visibility = View.GONE
                } else {
                    // Success with data
                    binding.layoutEmptyAppointments.visibility = View.GONE
                    binding.rvAppointments.visibility = View.VISIBLE

                    // Re-instantiate adapter with valid list
                    binding.rvAppointments.adapter =
                        DoctorAppointmentAdapter(sortedAppointments) { appointment ->
                            handleAppointmentClick(appointment)
                        }
                }
            }.onFailure { error ->
                // Failure
                binding.layoutEmptyAppointments.visibility = View.VISIBLE
                binding.rvAppointments.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to load schedule",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        networkViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            updateNotificationBadge(count)
        }
    }

    private fun fetchData() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        networkViewModel.fetchDoctorDetails(uid)
        networkViewModel.fetchDoctorAppointments()
    }

    private fun handleAppointmentClick(appointment: DoctorAppointment) {
            sharedViewModel.selectDoctorAppointment(appointment)
            findNavController().navigate(R.id.action_home_to_detail)
    }

    // ----------------------------------------------------------------
    // Existing intact logic for Quick Access, Notifications, and Images
    // ----------------------------------------------------------------

    private fun setupNotificationClick() {
        binding.btnNotification.setOnClickListener {
            val intent = Intent(
                requireContext(),
                com.example.neuronexus.common.activities.NotificationsActivity::class.java
            )
            startActivity(intent)
        }
    }

    private fun setupQuickAccess() {
        // Tumor Detection Card
        binding.cardDetectTumor.tvTitle.text = "Detect Tumor"
        binding.cardDetectTumor.imgIcon.setImageResource(R.drawable.brain_tumor)

        binding.cardDetectTumor.root.setOnClickListener {
            showImageSelectionDialog()
        }

        // History Card
        binding.cardHistory.tvTitle.text = "History"
        binding.cardHistory.imgIcon.setImageResource(R.drawable.history)
        binding.cardHistory.root.setOnClickListener {
            androidx.navigation.Navigation.findNavController(binding.root)
                .navigate(R.id.navigation_doctor_history)
        }
    }

    private fun showImageSelectionDialog() {
        val dialog = ImageSelectionDialog(
            onCameraClick = {
                imagePickerHelper.openCamera()
            },
            onGalleryClick = {
                imagePickerHelper.openGallery()
            }
        )
        dialog.show(parentFragmentManager, "ImageSelectionDialog")
    }

    private fun onImageSelected(uri: Uri) {
        activity?.let { mActivity ->
            progressDialog = ProgressDialog(mActivity)
            progressDialog?.setTitle("Detection")
            progressDialog?.setMessage("Loading...")
            progressDialog?.setCancelable(false)
            progressDialog?.show()
            val file = uriToFile(mActivity, uri)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestFile
            )
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.analyzeImage(multipartBody)
                    if (response.status == "success") {

                        Log.d("API", "Prediction: ${response.prediction}")
                        Log.d("API", "Confidence: ${response.confidence}")
                        Log.d("API", "Tumor: ${response.has_tumor}")

                        // 👉 Important for you
                        val maskBase64 = response.mask

                        analyzeResponse = response

                        // Next step: decode + overlay (you already know this)
                        withContext(Dispatchers.Main) {
                            progressDialog?.dismiss()
                            val intent = Intent(requireContext(), DetectionActivity::class.java)
                            intent.putExtra("image_uri", uri)
                            startActivity(intent)
                        }

                    } else {
                        withContext(Dispatchers.Main) {
                            progressDialog?.dismiss()
                            Toast.makeText(mActivity, "Failed to detect tumor", Toast.LENGTH_SHORT)
                                .show()
                        }

                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    progressDialog?.dismiss()
                    Toast.makeText(
                        mActivity,
                        "Something went wrong, ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }

    }

    fun uriToFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "upload_image.jpg")
        val outputStream = FileOutputStream(file)

        inputStream?.copyTo(outputStream)

        return file
    }

    private fun createImageUri(): Uri {
        val imageFileName = "JPEG_" + System.currentTimeMillis() + ".jpg"
        val imageFile = File(requireContext().cacheDir, imageFileName)
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
    }

    private fun updateNotificationBadge(count: Int) {
        val badge = binding?.tvNotificationBadge ?: return
        if (count <= 0) {
            badge.visibility = View.GONE
        } else {
            badge.visibility = View.VISIBLE
            badge.text = if (count > 9) "9+" else count.toString()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchData()

        val uid = networkViewModel.getCurrentUserUid()
        if (!uid.isNullOrEmpty()) {
            networkViewModel.startListeningToUnreadCount(uid)
        }
    }

    override fun onPause() {
        super.onPause()
        networkViewModel.stopListeningToUnreadCount()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}