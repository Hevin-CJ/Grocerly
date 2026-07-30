package com.example.grocerly.model.uistate
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.WishItem

data class WishListUiState(
    val isLoading: Boolean = false,
    val wishList: List<WishItem> = emptyList(),
    val cartItems: List<CartProduct> = emptyList()
)
