package com.example.grocerly.ui.uistate

import com.example.grocerly.model.ChatMessageModel

data class ChatUiState(
    val messages: List<ChatMessageModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)