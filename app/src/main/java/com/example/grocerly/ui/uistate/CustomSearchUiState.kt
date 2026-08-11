package com.example.grocerly.ui.uistate

import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Product

data class CustomSearchUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val categoryProducts: List<Product> = emptyList(),
    val searchResults: List<Product> = emptyList(),
    val cartItems: List<CartProduct> = emptyList(),
    val favouriteItems: List<FavouriteItem> = emptyList()
) {

    val cartProductMap: Map<String, Int> get() = cartItems.associate { it.product.productId to it.quantity }
    val cartProductIds: Set<String> get() = cartItems.map { it.product.productId }.toSet()
    val favouriteProductIds: Set<String> get() = favouriteItems.map { it.favouriteId }.toSet()
}