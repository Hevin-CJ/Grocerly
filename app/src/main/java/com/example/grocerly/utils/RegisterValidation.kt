package com.example.grocerly.utils

sealed class RegisterValidation {
     object Success : RegisterValidation()
    class Failed(val message:String):RegisterValidation()
}

data class RegisterFieldState(
    val firstname:RegisterValidation,
    val lastname:RegisterValidation,
    val email:RegisterValidation,
    val password:RegisterValidation
)

data class ProfileFieldState(
    val firstname:RegisterValidation,
    val lastname:RegisterValidation,
    val email:RegisterValidation,
    val phoneNo:RegisterValidation
)

data class LoginRegisterFieldState(
    val email:RegisterValidation,
    val password:RegisterValidation
)

data class EmailChangeFieldState(
    val email: RegisterValidation,
    val password: RegisterValidation,
    val newEmail: RegisterValidation
)

data class AddressFieldState(
    val firstName: RegisterValidation,
    val phoneNo: RegisterValidation,
    val alternatePhNo: RegisterValidation,
    val state: RegisterValidation,
    val city: RegisterValidation,
    val deliveryAddress: RegisterValidation,
    val landMark: RegisterValidation,
    val pincode: RegisterValidation
)