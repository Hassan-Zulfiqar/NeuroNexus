package com.example.neuronexus.common.ui

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
        if (success && tempImageUri != null) {
            onImagePicked(tempImageUri!!)
        }
    }
    private val requestPermissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(fragment.context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    fun openCamera() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile(
            "IMG_",
            ".jpg",
            fragment.requireContext().cacheDir
        )

        val authority = "${fragment.requireContext().packageName}.fileprovider"

        tempImageUri = FileProvider.getUriForFile(
            fragment.requireContext(),
            authority,
            photoFile
        )

        cameraLauncher.launch(tempImageUri)
    }
}

