package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.HelpCenterRepoImpl
import com.example.grocerly.model.ChatMessageModel
import com.example.grocerly.model.uistate.ChatUiState
import com.example.grocerly.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HelpCenterViewModel @Inject constructor(private val helpCenterRepo: HelpCenterRepoImpl,application: Application): AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun sendMessage(question: String) {
        if (question.isBlank() || _uiState.value.isLoading) return

        val currentHistory = _uiState.value.messages

        helpCenterRepo.getQuestionAndSendMessage(question, currentHistory).onEach { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        val userMessage = ChatMessageModel(question, "user")

                        val loadingMessage = ChatMessageModel(message = "", role = "model")

                        _uiState.update {
                            it.copy(
                                isLoading = true,
                                messages = currentHistory + userMessage + loadingMessage
                            )
                        }
                    }
                    is NetworkResult.Success -> {

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                messages = result.data ?: currentHistory
                            )
                        }
                    }
                    is NetworkResult.Error -> {

                        val historyWithoutLoading = _uiState.value.messages.dropLast(1)
                        val errorMessage = ChatMessageModel("Error: ${result.message}", "model")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                messages = historyWithoutLoading + errorMessage,
                                error = result.message
                            )
                        }
                    }
                    is NetworkResult.UnSpecified<*> ->{
                    }
                }
        }.launchIn(viewModelScope)
    }



}