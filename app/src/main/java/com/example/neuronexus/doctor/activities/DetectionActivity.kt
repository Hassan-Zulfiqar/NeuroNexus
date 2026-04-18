package com.example.neuronexus.doctor.activities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.Constant.analyzeResponse
import com.example.neuronexus.databinding.ActivityDetectionBinding
import com.example.neuronexus.doctor.repository.TumorDetectionRepository
import com.google.firebase.auth.FirebaseAuth

class DetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetectionBinding
    private var imageUri: Uri? = null
    private val tumorDetectionRepository = TumorDetectionRepository()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageUri = intent.getParcelableExtra("image_uri")

        Log.e("CHECK_IMG", "Detection: ${imageUri.toString()}")

        if (imageUri != null) {
            displayImage(imageUri!!)
        } else {
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
        try {
            val originalBitmap = uriToBitmap(imageUri!!)
            val maskBitmap = base64ToBitmap(analyzeResponse?.mask ?: "")

            binding.imageViewScan.setImageBitmap(originalBitmap)

            if (maskBitmap != null) {
                val resizedMask = Bitmap.createScaledBitmap(
                    maskBitmap,
                    originalBitmap.width,
                    originalBitmap.height,
                    true
                )

                val transparentMask = createTransparentMask(resizedMask)

                binding.maskImage.setImageBitmap(transparentMask)
            } else {
                binding.imageViewScan.setImageURI(uri)
            }


/*
            val maskBitmap = base64ToBitmap(analyzeResponse?.mask ?: "")
            val originalBitmap = imageUri?.let { uriToBitmap(it) }

            if (maskBitmap != null && originalBitmap != null) {
                binding.maskImage.setImageBitmap(maskBitmap)
                val finalBitmap = overlayMask(originalBitmap, maskBitmap)
                binding.imageViewScan.setImageBitmap(finalBitmap)
            } else {
                binding.imageViewScan.setImageURI(uri)
            }
*/

        } catch (e: Exception) {
            binding.imageViewScan.setImageResource(R.drawable.brain_tumor)
        }
    }

    fun uriToBitmap(uri: Uri): Bitmap {
        return MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }
/*

    fun overlayMask(original: Bitmap, mask: Bitmap): Bitmap {
        val width = original.width
        val height = original.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val resizedMask = Bitmap.createScaledBitmap(mask, width, height, true)

        for (x in 0 until width) {
            for (y in 0 until height) {

                val originalPixel = original.getPixel(x, y)
                val maskPixel = resizedMask.getPixel(x, y)

                val maskValue = android.graphics.Color.red(maskPixel)

                if (maskValue > 128) {
                    // Tumor area → highlight RED
                    val highlightedPixel = android.graphics.Color.argb(
                        150, 255, 0, 0 // semi-transparent red
                    )
                    result.setPixel(x, y, blendColors(originalPixel, highlightedPixel))
                } else {
                    // Normal area
                    result.setPixel(x, y, originalPixel)
                }
            }
        }

        return result
    }

    fun blendColors(base: Int, overlay: Int): Int {
        val alpha = android.graphics.Color.alpha(overlay) / 255f

        val r = (android.graphics.Color.red(base) * (1 - alpha) +
                android.graphics.Color.red(overlay) * alpha).toInt()

        val g = (android.graphics.Color.green(base) * (1 - alpha) +
                android.graphics.Color.green(overlay) * alpha).toInt()

        val b = (android.graphics.Color.blue(base) * (1 - alpha) +
                android.graphics.Color.blue(overlay) * alpha).toInt()

        return android.graphics.Color.rgb(r, g, b)
    }
*/

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
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
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

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        tumorDetectionRepository.saveTumorDetection(
            doctorId = currentUser.uid,
            imageUri = imageUri!!,
            location = location,
            tumorDetected = tumorDetected,
            size = size,
            callback = object : TumorDetectionRepository.SaveCallback {
                override fun onSuccess(recordId: String) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(
                        this@DetectionActivity,
                        "Record saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

                override fun onError(message: String) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(this@DetectionActivity, "Error: $message", Toast.LENGTH_LONG)
                        .show()
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
                val value = android.graphics.Color.red(pixel) // grayscale mask

                if (value > 128) {
                    // Tumor area → visible (red)
                    result.setPixel(
                        x, y,
                        android.graphics.Color.argb(150, 255, 0, 0) // semi-transparent red
                    )
                } else {
                    // Background → fully transparent
                    result.setPixel(
                        x, y,
                        android.graphics.Color.TRANSPARENT
                    )
                }
            }
        }

        return result
    }

}

