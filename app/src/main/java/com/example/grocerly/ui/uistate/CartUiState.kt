package com.example.grocerly.ui.uistate

import com.example.grocerly.model.CartProduct

data class CartUiState(
    val isLoading: Boolean = true,
    val cartItems: List<CartProduct> = emptyList(),
    val totalAmount: Float = 0f,
    val freeDeliveryThreshold: Float = 500f
) {
    val isEmpty: Boolean get() = cartItems.isEmpty()
    val remainingForFreeDelivery: Float get() = (freeDeliveryThreshold - totalAmount).coerceAtLeast(0f)
    val isFreeDeliveryEligible: Boolean get() = totalAmount >= freeDeliveryThreshold
}