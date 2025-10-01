package com.example.grocerly.utils

import com.example.grocerly.model.Card
import com.google.gson.annotations.SerializedName


sealed class PaymentMethodItem {
    data class Header(
        @SerializedName("id")
        val id: Int = 0,
        @SerializedName("title")
        val title: String = "",
        @SerializedName("icon")
        val icon: String = ""
    ): PaymentMethodItem()
    data class Content(val type:Type,val card: Card?=null): PaymentMethodItem()

    enum class Type{
        CARD,UPI,COD
    }
}