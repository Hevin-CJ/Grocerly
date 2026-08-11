package com.example.grocerly.ui.uievents

sealed interface CartUiEvents {
    data class ShowMessage(val message: String) : CartUiEvents
}


