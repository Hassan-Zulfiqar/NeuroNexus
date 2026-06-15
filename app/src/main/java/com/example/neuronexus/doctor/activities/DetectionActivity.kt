package com.example.neuronexus.doctor.activities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.Constant.analyzeResponse
import com.example.neuronexus.databinding.ActivityDetectionBinding
import com.example.neuronexus.doctor.models.TumorReport
import com.example.neuronexus.doctor.repository.TumorDetectionRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetectionBinding
    private var imageUri: Uri? = null
    private val tumorDetectionRepository = TumorDetectionRepository()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    // Class-level bitmap references for save operation
    private var originalBitmap: Bitmap? = null
    private var overlayBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageUri = intent.getParcelableExtra("image_uri")

        Log.e("CHECK_IMG", "Detection: ${imageUri.toString()}")

        imageUri?.let { uri ->
            displayImage(uri)
        } ?: run {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.tvLocation.text = analyzeResponse?.prediction
        if (analyzeResponse?.has_tumor == true) {
            binding.tvTumorDetected.text = "Found"
        } else {
            binding.tvTumorDetected.text = "Not Found"
        }
        binding.tvSize.text = "${analyzeResponse?.size_metrics?.area_percentage}%"

        setupClickListeners()
    }

    private fun displayImage(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val originalBitmapResult = uriToBitmap(uri)
                val maskBitmap = base64ToBitmap(analyzeResponse?.mask ?: "")

                val transparentMask = if (maskBitmap != null) {
                    val resizedMask = Bitmap.createScaledBitmap(
                        maskBitmap,
                        originalBitmapResult.width,
                        originalBitmapResult.height,
                        true
                    )
                    createTransparentMask(resizedMask)
                } else {
                    null
                }

                // Generate overlay bitmap for saving/PDF — runs on IO thread
                val overlayBitmapResult = if (maskBitmap != null) {
                    val resizedMask = Bitmap.createScaledBitmap(
                        maskBitmap,
                        originalBitmapResult.width,
                        originalBitmapResult.height,
                        true
                    )
                    overlayMask(originalBitmapResult, resizedMask)
                } else {
                    null
                }

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.imageViewScan.setImageBitmap(originalBitmapResult)
                    if (transparentMask != null) {
                        binding.maskImage.setImageBitmap(transparentMask)
                    }
                    // Store at class level for save function
                    originalBitmap = originalBitmapResult
                    overlayBitmap = overlayBitmapResult
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.imageViewScan.setImageResource(R.drawable.brain_tumor)
                    Toast.makeText(
                        this@DetectionActivity,
                        "Failed to load image: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun uriToBitmap(uri: Uri): Bitmap {
        val runtime = Runtime.getRuntime()
        val availableMemoryMB = (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / 1024 / 1024
        val targetSize = if (availableMemoryMB < 50) 1024 else 2048

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                if (width > targetSize || height > targetSize) {
                    decoder.setTargetSampleSize(2)
                }
            }.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, options)
            }
            options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, options)
            } ?: throw Exception("Failed to decode image from URI")
        }
    }

    private fun calculateInSampleSize(
        options: android.graphics.BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight
                && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSave.setOnClickListener {
            saveDetectionRecord()
        }
    }

    private fun saveDetectionRecord() {
        val location = binding.tvLocation.text.toString().trim()
        val tumorDetected = binding.tvTumorDetected.text.toString().trim()
        val size = binding.tvSize.text.toString().trim()

        if (location.isEmpty() || tumorDetected.isEmpty() || size.isEmpty()) {
            Toast.makeText(this, "Detection data is missing", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null) {
            Toast.makeText(this, "No image available", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        showPatientInfoDialog(location, tumorDetected, size)
    }

    private fun showPatientInfoDialog(
        location: String,
        tumorDetected: String,
        size: String
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_patient_info, null)

        val etPatientName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPatientName)
        val etPatientAge = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPatientAge)
        val spinnerGender = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerGender)

        val genders = arrayOf("Select Gender", "Male", "Female", "Other")
        val spinnerAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            genders
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = spinnerAdapter

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Patient Information")
            .setMessage("Enter patient details for this report")
            .setView(dialogView)
            .setPositiveButton("Save Report") { dialog, _ ->
                val patientName = etPatientName?.text?.toString()?.trim() ?: ""
                val patientAge = etPatientAge?.text?.toString()?.trim() ?: ""
                val patientGender = if ((spinnerGender?.selectedItemPosition ?: 0) > 0) {
                    spinnerGender?.selectedItem?.toString() ?: ""
                } else ""

                if (patientName.isEmpty()) {
                    Toast.makeText(this, "Patient name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (patientAge.isEmpty()) {
                    Toast.makeText(this, "Patient age is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                dialog.dismiss()
                proceedWithSave(
                    patientName = patientName,
                    patientAge = patientAge,
                    patientGender = patientGender,
                    location = location,
                    tumorDetected = tumorDetected,
                    size = size
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun proceedWithSave(
        patientName: String,
        patientAge: String,
        patientGender: String,
        location: String,
        tumorDetected: String,
        size: String
    ) {
        val uri = imageUri ?: return
        val doctor = currentUser ?: return
        val original = originalBitmap
        val overlay = overlayBitmap

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        val report = TumorReport(
            doctorId = doctor.uid,
            patientName = patientName,
            patientAge = patientAge,
            patientGender = patientGender,
            prediction = analyzeResponse?.prediction ?: "",
            confidence = analyzeResponse?.confidence ?: 0.0,
            hasTumor = analyzeResponse?.has_tumor ?: false,
            tumorDetected = tumorDetected,
            location = location,
            size = size,
            areaPercentage = analyzeResponse?.size_metrics?.area_percentage ?: 0.0,
            pixelCount = analyzeResponse?.size_metrics?.pixel_count ?: 0,
            maxDiameterPixels = analyzeResponse?.size_metrics?.max_diameter_pixels ?: 0.0
        )

        tumorDetectionRepository.saveTumorDetectionComplete(
            report = report,
            imageUri = uri,
            overlayBitmap = overlay,
            context = this,
            callback = object : TumorDetectionRepository.SaveCallback {
                override fun onSuccess(recordId: String) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(
                            this@DetectionActivity,
                            "Report saved successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }

                override fun onError(message: String) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(
                            this@DetectionActivity,
                            "Error: $message",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createTransparentMask(mask: Bitmap): Bitmap {
        val width = mask.width
        val height = mask.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = mask.getPixel(x, y)
                val value = android.graphics.Color.red(pixel)

                if (value > 128) {
                    result.setPixel(x, y, android.graphics.Color.argb(150, 255, 0, 0))
                } else {
                    result.setPixel(x, y, android.graphics.Color.TRANSPARENT)
                }
            }
        }

        return result
    }

    // Produces a merged bitmap (original MRI + red blend) — used for PDF and Cloudinary saving
    fun overlayMask(original: Bitmap, mask: Bitmap): Bitmap {
        val width = original.width
        val height = original.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val resizedMask = Bitmap.createScaledBitmap(mask, width, height, true)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val originalPixel = original.getPixel(x, y)
                val maskPixel = resizedMask.getPixel(x, y)
                val maskValue = Color.red(maskPixel)

                if (maskValue > 128) {
                    val highlightedPixel = Color.argb(150, 255, 0, 0)
                    result.setPixel(x, y, blendColors(originalPixel, highlightedPixel))
                } else {
                    result.setPixel(x, y, originalPixel)
                }
            }
        }
        return result
    }

    fun blendColors(base: Int, overlay: Int): Int {
        val alpha = Color.alpha(overlay) / 255f
        val r = (Color.red(base) * (1 - alpha) + Color.red(overlay) * alpha).toInt()
        val g = (Color.green(base) * (1 - alpha) + Color.green(overlay) * alpha).toInt()
        val b = (Color.blue(base) * (1 - alpha) + Color.blue(overlay) * alpha).toInt()
        return Color.rgb(r, g, b)
    }
}
