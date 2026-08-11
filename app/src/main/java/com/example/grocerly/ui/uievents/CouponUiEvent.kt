package com.example.grocerly.ui.uievents

sealed interface CouponUiEvent {
    data class ShowMessage(val message: String): CouponUiEvent
}