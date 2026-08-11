package com.example.grocerly.ui.uistate

import com.example.grocerly.model.Product

sealed class MenuItemUiState {
    data class ProductItem(val product: Product): MenuItemUiState()
    object AccountItem: MenuItemUiState()
}