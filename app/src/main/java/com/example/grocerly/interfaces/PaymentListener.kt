package com.example.grocerly.interfaces

interface PaymentListener {
    fun onCvvCheckListener(cardId:String,cvv: String,onResult:(String) -> Unit)
    fun onUpiListener(upi: String)
    fun onCodListener(isSet:Boolean)
}