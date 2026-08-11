package com.example.grocerly.utils

sealed class RazorpayResult {
    data class Success(val paymentId: String?) : RazorpayResult()
    data class Error(val code: Int, val description: String?) : RazorpayResult()
}