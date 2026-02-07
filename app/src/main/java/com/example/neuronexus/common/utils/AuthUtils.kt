package com.example.neuronexus.common.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.neuronexus.common.auth.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

object AuthUtils {

    fun logout(activity: Activity) {
        val sharedPref = activity.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.clear()
        editor.apply()

        FirebaseAuth.getInstance().signOut()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val googleSignInClient = GoogleSignIn.getClient(activity, gso)

        googleSignInClient.signOut().addOnCompleteListener(activity) {

            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            activity.startActivity(intent)
            activity.finishAffinity()
        }
    }
}