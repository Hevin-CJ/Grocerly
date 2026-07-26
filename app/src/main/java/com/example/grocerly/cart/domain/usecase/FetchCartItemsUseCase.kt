package com.example.grocerly.cart.domain.usecase

import com.example.grocerly.cart.domain.model.CartItem
import com.example.grocerly.cart.domain.repository.CartRepository
import com.example.grocerly.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchCartItemsUseCase @Inject constructor(
    private val cartRepository: CartRepository
) {
    operator fun invoke(): Flow<NetworkResult<List<CartItem>>> {
        return cartRepository.fetchAllCartItems()
    }
}
