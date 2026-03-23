package com.example.grocerly.model.uistate

import com.example.grocerly.model.Product

sealed class MenuItemUiState {
    data class ProductItem(val product: Product): MenuItemUiState()
    object AccountItem: MenuItemUiState()
}