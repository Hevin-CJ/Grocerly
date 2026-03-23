package com.example.grocerly.model

import com.google.firebase.firestore.PropertyName

data class PartnerCouponRule(
    val couponId: String = "",
    val partnerId: String = "",

    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = false,
    val minimumSpendToEarn: Int = 0,
    val discountAmountToGive: Int = 0,
    val minOrderValueForNextPurchase: Int = 0,
    val validityInDays: Int = 1,
    val expiryDate: Long = 0L
)