package com.example.grocerly.utils

sealed class CardValidation {
    object Success: CardValidation()
    class Failed(val message: String): CardValidation()
}


data class savedCardState(
    val holderName: CardValidation,
    val cardNumber: CardValidation,
    val expiryDate: CardValidation,
    val cvv: CardValidation
)