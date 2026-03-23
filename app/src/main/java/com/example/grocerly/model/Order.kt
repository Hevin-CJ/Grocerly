package com.example.grocerly.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Order(
    val orderId: String = "",
    val userId: String = "",
    val address: Address = Address(),
    val items: List<CartProduct> = emptyList(),
    val timestamp:Long = 0,
    val totalOrderPrice:Int = 0,
    val paymentType: String = ""
): Parcelable
