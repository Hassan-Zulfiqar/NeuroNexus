package com.example.neuronexus.common.models

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "",
    val status: String = "active",
    val createdAt: Long = 0L,
    val lastLogin: Long = 0L
)

