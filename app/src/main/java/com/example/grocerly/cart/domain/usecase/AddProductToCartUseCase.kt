package com.example.grocerly.cart.domain.usecase

import com.example.grocerly.cart.domain.model.CartItem
import com.example.grocerly.cart.domain.repository.CartRepository
import com.example.grocerly.utils.NetworkResult
import javax.inject.Inject

class AddProductToCartUseCase @Inject constructor(
    private val cartRepository: CartRepository
) {
    suspend operator fun invoke(cartItem: CartItem): NetworkResult<Unit> {
        return cartRepository.addProductToCart(cartItem)
    }
}
