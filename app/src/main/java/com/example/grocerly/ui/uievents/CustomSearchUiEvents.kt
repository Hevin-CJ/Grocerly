package com.example.grocerly.ui.uievents

sealed interface CustomSearchUiEvents {
    data class ShowMessage(val message: String) : CustomSearchUiEvents
}