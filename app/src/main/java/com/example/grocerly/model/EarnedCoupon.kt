package com.example.grocerly.model

import com.google.firebase.firestore.PropertyName

data class EarnedCoupon(
    val couponId: String = "",
    val code: String = "",
    val partnerId: String = "",
    val discountAmount: Int = 0,
    val minOrderValue: Int = 0,
    val expiryTimestamp: Long = 0L,
    @get:PropertyName("isCouponUsed")
    @set:PropertyName("isCouponUsed")
    var isCouponUsed: Boolean = false
)