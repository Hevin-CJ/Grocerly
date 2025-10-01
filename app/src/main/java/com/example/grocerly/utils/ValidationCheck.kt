package com.example.grocerly.utils

import android.util.Patterns

fun validateEmail(email:String):RegisterValidation{
    if (email.isEmpty()) return RegisterValidation.Failed("Required Field:Email cannot be empty")

    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return RegisterValidation.Failed("Email format incorrect")

    return RegisterValidation.Success
}

fun validatePassword(password:String):RegisterValidation {

    if (password.isEmpty()) return RegisterValidation.Failed("Required Field:Password cannot be empty")

    if (password.length<6) return RegisterValidation.Failed("Password should be atleast 6 characters")

    return RegisterValidation.Success
}


fun validateName(name:String):RegisterValidation{

    if (name.isEmpty()) return RegisterValidation.Failed("Required Field:Name cannot be empty")

    if (name.length<5) return RegisterValidation.Failed("Field Should Contain 5 Characters")

    return RegisterValidation.Success
}


fun validatePhoneNumber(num: String): RegisterValidation  {

    if (num.isEmpty()) return RegisterValidation.Failed("Required Field: Phone number cannot be empty")

    if (!Patterns.PHONE.matcher(num).matches()) return RegisterValidation.Failed("Invalid phone number format")

    if (num.length !in 8..10) return RegisterValidation.Failed("Invalid phone number")

    if (num.contains("+")) return RegisterValidation.Failed("Country code exists")

    return RegisterValidation.Success
}
