package com.example.grocerly.cart.domain.repository

import com.example.grocerly.cart.domain.model.CartItem
import com.example.grocerly.utils.NetworkResult
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    suspend fun addProductToCart(cartItem: CartItem): NetworkResult<Unit>
    suspend fun updateQuantity(cartItem: CartItem): NetworkResult<Unit>
    suspend fun deleteItemFromCart(cartItem: CartItem): NetworkResult<Unit>
    fun fetchAllCartItems(): Flow<NetworkResult<List<CartItem>>>
    fun fetchTotalAmountFromCart(): Flow<NetworkResult<Float>>
    fun fetchTotalPriceDetails(cartItems: List<CartItem>, couponAmount: Int): Flow<NetworkResult<Map<String, Int>>>
}
