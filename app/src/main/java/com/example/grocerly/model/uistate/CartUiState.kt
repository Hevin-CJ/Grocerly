package com.example.grocerly.model.uistate

import com.example.grocerly.model.CartProduct
import com.example.grocerly.utils.NetworkResult

data class CartUiState(
    val cartItems: List<CartProduct>? = emptyList(),
    val totalAmount: Float? = 0.0f
)