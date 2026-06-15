package com.example.neuronexus.doctor.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.neuronexus.BuildConfig
import com.example.neuronexus.doctor.models.TumorReport
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class TumorDetectionRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    interface SaveCallback {
        fun onSuccess(recordId: String)
        fun onError(message: String)
    }

    // Keep for backward compatibility
    fun saveTumorDetection(
        doctorId: String,
        imageUri: Uri,
        location: String,
        tumorDetected: String,
        size: String,
        callback: SaveCallback
    ) {
        val recordId = db.child("detect_tumor").push().key
            ?: return callback.onError("Failed to generate record ID")

        MediaManager.get()
            .upload(imageUri)
            .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .option("folder", "neuronexus/tumor_scans")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as? String
                    if (!secureUrl.isNullOrBlank()) {
                        val timestamp = System.currentTimeMillis()
                        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        val formattedDate = dateFormat.format(Date(timestamp))

                        val detectionRecord = mapOf(
                            "recordId" to recordId,
                            "doctorId" to doctorId,
                            "imageUrl" to secureUrl,
                            "location" to location,
                            "tumorDetected" to tumorDetected,
                            "size" to size,
                            "timestamp" to timestamp,
                            "date" to formattedDate
                        )

                        db.child("detect_tumor").child(recordId).setValue(detectionRecord)
                            .addOnSuccessListener { callback.onSuccess(recordId) }
                            .addOnFailureListener { e -> callback.onError(e.message ?: "Failed to save record") }
                    } else {
                        callback.onError("Upload succeeded but URL is missing")
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    callback.onError(error.description ?: "Upload failed")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    callback.onError("Upload rescheduled: ${error.description}")
                }
            })
            .dispatch()
    }

    fun saveTumorDetectionComplete(
        report: TumorReport,
        imageUri: Uri,
        overlayBitmap: Bitmap?,
        context: Context,
        callback: SaveCallback
    ) {
        val recordId = db.child("detect_tumor").push().key
            ?: return callback.onError("Failed to generate record ID")

        // Step 1: Upload original MRI image
        MediaManager.get()
            .upload(imageUri)
            .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .option("folder", "neuronexus/tumor_scans")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val originalImageUrl = resultData["secure_url"] as? String
                    if (originalImageUrl.isNullOrBlank()) {
                        callback.onError("Original image upload failed: URL missing")
                        return
                    }

                    // Step 2: Upload overlay bitmap if available
                    if (overlayBitmap != null) {
                        uploadBitmapToCloudinary(
                            bitmap = overlayBitmap,
                            folder = "neuronexus/tumor_overlays",
                            context = context,
                            onComplete = { overlayUrl ->
                                generateAndSavePdf(context, report, originalImageUrl, overlayUrl, recordId, callback)
                            },
                            onError = {
                                // Overlay failed — proceed without overlay
                                generateAndSavePdf(context, report, originalImageUrl, null, recordId, callback)
                            }
                        )
                    } else {
                        generateAndSavePdf(context, report, originalImageUrl, null, recordId, callback)
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    callback.onError(error.description ?: "Original image upload failed")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    callback.onError("Upload rescheduled: ${error.description}")
                }
            })
            .dispatch()
    }

    private fun uploadBitmapToCloudinary(
        bitmap: Bitmap,
        folder: String,
        context: Context,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val tempFile = File(context.cacheDir, "overlay_${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: Exception) {
            onError("Failed to write overlay bitmap: ${e.message}")
            return
        }

        MediaManager.get()
            .upload(Uri.fromFile(tempFile))
            .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .option("folder", folder)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    tempFile.delete()
                    val url = resultData["secure_url"] as? String
                    if (!url.isNullOrBlank()) {
                        onComplete(url)
                    } else {
                        onError("Overlay upload returned empty URL")
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    tempFile.delete()
                    onError(error.description ?: "Overlay upload failed")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    tempFile.delete()
                    onError("Overlay upload rescheduled")
                }
            })
            .dispatch()
    }

    private fun generateAndSavePdf(
        context: Context,
        report: TumorReport,
        originalImageUrl: String,
        overlayImageUrl: String?,
        recordId: String,
        callback: SaveCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val originalBitmap = downloadBitmap(originalImageUrl)
                val overlayBitmap = overlayImageUrl?.let { downloadBitmap(it) }

                val pdfFile = generatePdf(context, report, originalBitmap, overlayBitmap, recordId)

                // Upload PDF — resource_type=raw required for non-image files
                MediaManager.get()
                    .upload(Uri.fromFile(pdfFile))
                    .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                    .option("asset_folder", "neuronexus/tumor_reports")
                    .option("resource_type", "auto")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {}
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            pdfFile.delete()
                            val pdfUrl = resultData["secure_url"] as? String
                            if (!pdfUrl.isNullOrBlank()) {
                                saveRecordToFirebase(
                                    recordId = recordId,
                                    report = report,
                                    originalImageUrl = originalImageUrl,
                                    overlayImageUrl = overlayImageUrl,
                                    pdfUrl = pdfUrl,
                                    callback = callback
                                )
                            } else {
                                callback.onError("PDF upload returned empty URL")
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            pdfFile.delete()
                            callback.onError(error.description ?: "PDF upload failed")
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            pdfFile.delete()
                            callback.onError("PDF upload rescheduled")
                        }
                    })
                    .dispatch()

            } catch (e: Exception) {
                callback.onError("PDF generation failed: ${e.message}")
            }
        }
    }

    private fun downloadBitmap(url: String): Bitmap? {
        return try {
            URL(url).openStream().use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun generatePdf(
        context: Context,
        report: TumorReport,
        originalBitmap: Bitmap?,
        overlayBitmap: Bitmap?,
        recordId: String
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40

        // Page 1: Report text
        val page1 = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        )
        val canvas1: Canvas = page1.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            textSize = 22f
            isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            strokeWidth = 2f
        }

        canvas1.drawText("NeuroNexus — Tumor Detection Report", margin.toFloat(), 70f, titlePaint)
        canvas1.drawLine(margin.toFloat(), 80f, (pageWidth - margin).toFloat(), 80f, linePaint)

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(System.currentTimeMillis()))

        var y = 110f
        y = drawSectionHeader(canvas1, "Patient Information", margin.toFloat(), y)
        y = drawInfoRow(canvas1, "Patient Name", report.patientName, margin.toFloat(), y)
        y = drawInfoRow(canvas1, "Age", report.patientAge, margin.toFloat(), y)
        y = drawInfoRow(canvas1, "Gender", report.patientGender.ifBlank { "N/A" }, margin.toFloat(), y)
        y = drawInfoRow(canvas1, "Report Date", formattedDate, margin.toFloat(), y)

        y += 20f
        y = drawSectionHeader(canvas1, "Tumor Analysis", margin.toFloat(), y)
        y = drawInfoRow(canvas1, "Tumor Detected", report.tumorDetected, margin.toFloat(), y)
        y = drawInfoRow(canvas1, "Prediction", report.prediction.ifBlank { "N/A" }, margin.toFloat(), y)
        y = drawInfoRow(canvas1, "Confidence", "${(report.confidence * 100).toInt()}%", margin.toFloat(), y)
        y = drawInfoRow(canvas1, "Location", report.location.ifBlank { "N/A" }, margin.toFloat(), y)
        y = drawInfoRow(canvas1, "Size (Area %)", "${report.areaPercentage}%", margin.toFloat(), y)
        y = drawInfoRow(canvas1, "Pixel Count", "${report.pixelCount}", margin.toFloat(), y)
        drawInfoRow(canvas1, "Max Diameter (px)", "${report.maxDiameterPixels}", margin.toFloat(), y)

        document.finishPage(page1)

        // Page 2: MRI Images
        val page2 = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        )
        val canvas2: Canvas = page2.canvas
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            textSize = 16f
            isFakeBoldText = true
        }

        val maxImageWidth = pageWidth - (margin * 2)
        val maxImageHeight = 340
        var imageY = margin.toFloat()

        if (originalBitmap != null) {
            canvas2.drawText("Original MRI Scan", margin.toFloat(), imageY + 16f, sectionPaint)
            imageY += 30f
            val scaled = scaleBitmapToFit(originalBitmap, maxImageWidth, maxImageHeight)
            canvas2.drawBitmap(scaled, margin.toFloat(), imageY, null)
            imageY += scaled.height + 30f
        }

        if (overlayBitmap != null) {
            canvas2.drawText("Tumor Overlay", margin.toFloat(), imageY + 16f, sectionPaint)
            imageY += 30f
            val scaled = scaleBitmapToFit(overlayBitmap, maxImageWidth, maxImageHeight)
            canvas2.drawBitmap(scaled, margin.toFloat(), imageY, null)
        }

        document.finishPage(page2)

        val pdfFile = File(context.cacheDir, "tumor_report_${recordId}.pdf")
        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    private fun scaleBitmapToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val scale = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height, 1f)
        if (scale >= 1f) return bitmap
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )
    }

    private fun drawSectionHeader(canvas: Canvas, title: String, x: Float, y: Float): Float {
        val headerPaint = Paint().apply {
            color = Color.parseColor("#3F51B5")
            textSize = 16f
            isFakeBoldText = true
        }
        val underlinePaint = Paint().apply {
            color = Color.parseColor("#3F51B5")
            strokeWidth = 1f
        }
        canvas.drawText(title, x, y, headerPaint)
        canvas.drawLine(x, y + 4f, x + headerPaint.measureText(title), y + 4f, underlinePaint)
        return y + 24f
    }

    private fun drawInfoRow(canvas: Canvas, label: String, value: String, x: Float, y: Float): Float {
        val labelPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 13f
            isFakeBoldText = true
        }
        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
        }
        canvas.drawText("$label:", x, y, labelPaint)
        canvas.drawText(value, x + 200f, y, valuePaint)
        return y + 22f
    }

    private fun saveRecordToFirebase(
        recordId: String,
        report: TumorReport,
        originalImageUrl: String,
        overlayImageUrl: String?,
        pdfUrl: String,
        callback: SaveCallback
    ) {
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(timestamp))

        val record = mutableMapOf<String, Any>(
            "recordId" to recordId,
            "doctorId" to report.doctorId,
            "patientName" to report.patientName,
            "patientAge" to report.patientAge,
            "patientGender" to report.patientGender,
            "imageUrl" to originalImageUrl,
            "prediction" to report.prediction,
            "confidence" to report.confidence,
            "hasTumor" to report.hasTumor,
            "tumorDetected" to report.tumorDetected,
            "location" to report.location,
            "size" to report.size,
            "areaPercentage" to report.areaPercentage,
            "pixelCount" to report.pixelCount,
            "maxDiameterPixels" to report.maxDiameterPixels,
            "pdfUrl" to pdfUrl,
            "timestamp" to timestamp,
            "date" to formattedDate
        )

        overlayImageUrl?.let { record["overlayUrl"] = it }

        db.child("detect_tumor").child(recordId).setValue(record)
            .addOnSuccessListener { callback.onSuccess(recordId) }
            .addOnFailureListener { e -> callback.onError(e.message ?: "Failed to save record") }
    }
}
