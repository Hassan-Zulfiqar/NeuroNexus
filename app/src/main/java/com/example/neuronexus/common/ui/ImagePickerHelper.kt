package com.example.neuronexus.common.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

class ImagePickerHelper(
    private val fragment: Fragment,
    private val onImagePicked: (Uri) -> Unit
) {

    private var tempImageUri: Uri? = null

    private val galleryLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImagePicked(it) }
    }

    private val cameraLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Only deliver if capture actually succeeded
            tempImageUri?.let { onImagePicked(it) }
        }
        // Always clear stale URI after attempt — success or failure
        tempImageUri = null
    }

    private val requestPermissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(
                fragment.context,
                "Camera permission required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    fun openCamera() {
        val context = fragment.context ?: return

        // Check if permission already granted — avoid redundant request
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val context = fragment.context ?: return

        try {
            // Always create fresh temp file — never reuse stale URI
            val photoFile = File.createTempFile(
                "IMG_${System.currentTimeMillis()}_",
                ".jpg",
                context.cacheDir
            )

            val authority = "${context.packageName}.fileprovider"

            tempImageUri = FileProvider.getUriForFile(
                context,
                authority,
                photoFile
            )

            cameraLauncher.launch(tempImageUri)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Failed to open camera: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
