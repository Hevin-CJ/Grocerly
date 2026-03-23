package com.example.grocerly.model.uievents

import org.json.JSONObject

sealed class PaymentUiEvent {
    data class ShowMessage(val message: String) : PaymentUiEvent()
    object NavigateToOrderPlaced : PaymentUiEvent()
    data class LaunchRazorpay(val options: JSONObject) : PaymentUiEvent()
}