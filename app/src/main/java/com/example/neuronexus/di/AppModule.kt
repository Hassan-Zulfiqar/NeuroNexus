package com.example.neuronexus.di

import com.example.neuronexus.common.repository.AppRepository
import com.example.neuronexus.common.viewmodel.AuthViewModel
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { AppRepository() }

    // "get()" tells Koin to automatically find and inject the AppRepository created above
    viewModel { AuthViewModel(get()) }
    viewModel { NetworkViewModel(get()) }
    viewModel { SharedViewModel(get()) }
}