package com.example.grocerly.cart.domain.usecase

import com.example.grocerly.cart.domain.model.CartItem
import com.example.grocerly.cart.domain.repository.CartRepository
import com.example.grocerly.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchTotalCartAmountUseCase @Inject constructor(
    private val cartRepository: CartRepository
) {
    fun fetchTotalAmount(): Flow<NetworkResult<Float>> {
        return cartRepository.fetchTotalAmountFromCart()
    }

    fun fetchTotalPriceDetails(cartItems: List<CartItem>, couponAmount: Int = 0): Flow<NetworkResult<Map<String, Int>>> {
        return cartRepository.fetchTotalPriceDetails(cartItems, couponAmount)
    }
}
