package com.example.neuronexus.common.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.example.neuronexus.R

object AlertUtils {

    fun showError(context: Context, message: String, title: String = "Error") {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    fun showInfo(context: Context, message: String, title: String = "Info") {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}