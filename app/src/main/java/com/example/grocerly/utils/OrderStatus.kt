package com.example.grocerly.utils

import com.google.android.play.core.integrity.t

enum class OrderStatus(val status: String) {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    READY("Ready"),
    SHIPPED("Shipped"),
    OUTFORDELIVERY("OutForDelivery"),
    DELIVERED("Delivered");

    companion object{
        fun fromStatus(status: String): OrderStatus{
            return OrderStatus.entries.firstOrNull { it.status.equals(status, true) } ?: PENDING
        }
    }
}