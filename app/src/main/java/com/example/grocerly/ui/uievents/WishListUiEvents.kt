package com.example.grocerly.ui.uievents

sealed interface WishListUiEvents {
    data class ShowMessage(val message: String) : WishListUiEvents
    object NavigateToLogin : WishListUiEvents
}


