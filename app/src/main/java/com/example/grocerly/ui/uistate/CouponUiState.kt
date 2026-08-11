package com.example.grocerly.ui.uistate

import com.example.grocerly.model.EarnedCoupon

data class CouponUiState(
    val isLoading: Boolean = false,
    val couponList: List<EarnedCoupon> = emptyList(),
    val errorMessage: String? = null
)