package com.example.grocerly.ui.uistate

import com.example.grocerly.model.Address
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.EarnedCoupon

data class CheckoutUiState(
    val isLoading: Boolean = false,
    val cartItems: List<CartProduct> = emptyList(),
    val defaultAddress: Address? = null,
    val savedAddresses: List<Address> = emptyList(),
    val priceBreakdown: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null,
    val appliedCoupon: EarnedCoupon? = null,

    val couponMessage: String? = null,
    val isCouponError: Boolean = false
) {
    val isDefaultAddressEmpty: Boolean
        get() = defaultAddress?.deliveryAddress?.isEmpty() ?: true
}