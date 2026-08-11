package com.example.grocerly.ui.uistate

import com.example.grocerly.model.FavouriteItem

data class FavouriteUiState(
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val favouriteItems: List<FavouriteItem> = emptyList()
)