package com.example.grocerly.cart.domain.usecase

import com.example.grocerly.cart.domain.model.CartItem
import com.example.grocerly.cart.domain.repository.CartRepository
import com.example.grocerly.utils.NetworkResult
import javax.inject.Inject

class UpdateCartQuantityUseCase @Inject constructor(
    private val cartRepository: CartRepository
) {
    suspend operator fun invoke(cartItem: CartItem): NetworkResult<Unit> {
        val maxQuantity = cartItem.product.maxQuantity ?: 1
        if (cartItem.quantity > maxQuantity) {
            return NetworkResult.Error("Maximum quantity allowed for \n${cartItem.product.itemName} is $maxQuantity.")
        }
        return cartRepository.updateQuantity(cartItem)
    }
}
