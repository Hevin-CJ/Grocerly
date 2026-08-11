package com.example.grocerly.ui.uievents

sealed class FavouriteUiEvents {
    data class ShowMessage(val message: String) : FavouriteUiEvents()
}