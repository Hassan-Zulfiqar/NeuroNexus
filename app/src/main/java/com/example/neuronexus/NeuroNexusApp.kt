package com.example.neuronexus

import android.app.Application
import com.example.neuronexus.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NeuroNexusApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@NeuroNexusApp)
            modules(appModule)
        }
    }
}
