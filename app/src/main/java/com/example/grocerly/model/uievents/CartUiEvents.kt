package com.example.grocerly.model.uievents

sealed interface CartUiEvents {
    data class ShowMessage(val message: String) : CartUiEvents
}


