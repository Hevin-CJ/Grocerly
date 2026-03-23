package com.example.grocerly.model.uistate

import com.example.grocerly.model.EarnedCoupon

data class CouponUiState(
    val isLoading: Boolean = false,
    val couponList: List<EarnedCoupon> = emptyList(),
    val errorMessage: String? = null
)