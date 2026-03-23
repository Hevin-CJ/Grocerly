package com.example.grocerly.model.uievents

sealed interface WishListUiEvents {
    data class ShowMessage(val message: String) : WishListUiEvents
    object NavigateToLogin : WishListUiEvents
}


