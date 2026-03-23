package com.example.grocerly.model.uievents

sealed interface CheckoutUiEvent {
    data class ShowMessage(val message: String) : CheckoutUiEvent
    data object ItemDeletedSuccess : CheckoutUiEvent
    data object AddressUpdatedSuccess : CheckoutUiEvent
}