package com.example.grocerly.model.uievents

sealed interface CouponUiEvent {
    data class ShowMessage(val message: String): CouponUiEvent
}