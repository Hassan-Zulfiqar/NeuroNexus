package com.example.neuronexus.common.utils

object ValidationUtils {

    fun isValidName(name: String): Boolean =
        name.trim().length >= 2

    fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    fun isValidPassword(password: String): Boolean {
        val passwordRegex = Regex(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}$"
        )
        return passwordRegex.matches(password)
    }

    fun isValidCnic(cnic: String): Boolean {
        val digits = cnic.trim().replace("-", "")
        return digits.length == 13 && digits.all { it.isDigit() }
    }

    fun isValidContact(contact: String): Boolean {
        val digits = contact.trim()
        return digits.length in 11..13 && digits.all { it.isDigit() }
    }

    fun isValidAge(age: String): Boolean {
        val ageInt = age.trim().toIntOrNull() ?: return false
        return ageInt in 1..120
    }

    fun getPasswordStrengthMessage(): String =
        "Password must be at least 8 characters with uppercase, " +
        "lowercase, number, and special character (@#\$%^&+=!)"
}
