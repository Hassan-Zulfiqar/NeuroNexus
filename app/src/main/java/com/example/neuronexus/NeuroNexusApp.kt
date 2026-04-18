package com.example.neuronexus

import android.app.Application
import com.example.neuronexus.di.appModule
import com.cloudinary.android.MediaManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NeuroNexusApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = HashMap<String, String>()
        config["cloud_name"] = BuildConfig.CLOUDINARY_CLOUD_NAME

        MediaManager.init(this, config)

        startKoin {
            androidContext(this@NeuroNexusApp)
            modules(appModule)
        }
    }
}